#!/usr/bin/env python3
"""Erzeugt den Champion-Katalog fuer FACTION aus einer Community-Rohdatenquelle.

Quelle: https://github.com/PatPat1567/RaidShadowLegendsData (Stammdaten und
Skill-Beschreibungen, aus dem Spiel extrahiert).

Bewusst uebernommen werden nur *Fakten*: Name, Fraktion, Seltenheit, Affinitaet,
Rolle, die sechs verlaesslichen Basiswerte (LP, Angriff, Verteidigung, Tempo,
Widerstand, Genauigkeit) sowie die Frage, welche Wirkungen ein Kit mitbringt. Die
Skill-Texte selbst werden nicht mitgeliefert - sie gehoeren Plarium. Aus ihnen
werden lediglich die Utility-Tags abgeleitet, auf denen die Bewertung in
ScoreEngine.kt arbeitet.

Die Quelle fuehrt zusaetzlich Krit-Rate und Krit-Schaden, aber verlaesslich sind
sie nicht: das Feld "crate" enthaelt in 555 von 558 Faellen den Text "RATE" oder
"Rate" statt einer Zahl, und "cdmg" traegt bei 456 von 530 Champions denselben
Wert "15" - ein Platzhalter, kein gemessener Wert. Beide Felder werden deshalb
nicht uebernommen.

Aufruf:
    python tools/generate_champions.py <pfad-zum-datenrepo> [ziel.json]
        [--feed-out=pfad.json] [--feed-version=N] [--released="Text"]

Ohne --feed-out entsteht nur die flache Liste fuer assets/champions.json (das Format,
das ChampionCatalog aus den Assets laedt). Mit --feed-out entsteht zusaetzlich eine
versionierte Kopie im Objekt-Format {"version", "released", "champions"}, das Format,
das CatalogUpdater von der Feed-Adresse erwartet - beide Formen tragen dieselben
Champions, nur unterschiedlich verpackt. Wer --feed-out nutzt, muss --feed-version bei
jeder inhaltlichen Aenderung von Hand erhoehen: CatalogUpdater uebernimmt einen Feed nur,
wenn seine Version hoeher ist als die zuletzt gespeicherte.
"""

from __future__ import annotations

import json
import os
import re
import sys
import unicodedata
from collections import Counter

# --- Zuordnungen -----------------------------------------------------------

FACTIONS = {
    "The Sacred Order": "Heilige Orden",
    "Undead Hordes": "Untote Horden",
    "Barbarians": "Barbaren",
    "Banner Lords": "Banner-Lords",
    "Orcs": "Orks",
    "Dark Elves": "Dunkelelfen",
    "Demonspawn": "Dämonenbrut",
    "Dwarves": "Zwerge",
    "Ogryn Tribes": "Oger-Stämme",
    "Knight Revenant": "Ritter-Wiedergänger",
    "Knights Revenant": "Ritter-Wiedergänger",  # Schreibvariante in den Rohdaten
    "High Elves": "Hochelfen",
    "Lizardmen": "Echsenmenschen",
    "Skinwalkers": "Fellwandler",
    "Shadowkin": "Schattenclans",
}

RARITIES = {
    "common": "COMMON",
    "uncommon": "UNCOMMON",
    "rare": "RARE",
    "epic": "EPIC",
    "legendary": "LEGENDARY",
}

AFFINITIES = {"magic": "MAGIC", "force": "FORCE", "spirit": "SPIRIT", "void": "VOID"}

ROLES = {"attack": "ATTACK", "defense": "DEFENSE", "hp": "HP", "support": "SUPPORT"}

# Effektnamen in eckigen Klammern -> Utility. Schreibvarianten der Quelle sind
# mit aufgefuehrt, damit nichts stillschweigend verloren geht.
BRACKET_EFFECTS = {
    "decrease def": "DECREASE_DEFENSE",
    "decrease atk": "DECREASE_ATTACK",
    "decrease spd": "DECREASE_SPEED",
    "decrease acc": "DECREASE_ACCURACY",
    "decrease c.rate": "DECREASE_CRIT_RATE",
    "weaken": "WEAKEN",
    "heal reduction": "HEAL_REDUCTION",
    "block buffs": "BLOCK_BUFFS",
    "block cooldown skills": "BLOCK_COOLDOWN",
    "block heal": "HEAL_REDUCTION",
    "strengthen": "STRENGTHEN",
    "decrease c.dmg": "DECREASE_CRIT_DAMAGE",
    "block revive": "BLOCK_REVIVE",
    "poison": "POISON",
    "poison sensitivity": "POISON_SENSITIVITY",
    "hp burn": "HP_BURN",
    "bomb": "BOMB",
    "stun": "STUN",
    "freeze": "STUN",
    "sleep": "SLEEP",
    "fear": "FEAR",
    "true fear": "FEAR",
    "provoke": "PROVOKE",
    "continuous heal": "CONTINUOUS_HEAL",
    "continous heal": "CONTINUOUS_HEAL",
    "shield": "SHIELD",
    "block debuffs": "BLOCK_DEBUFFS",
    "block damage": "BLOCK_DAMAGE",
    "unkillable": "UNKILLABLE",
    "veil": "VEIL",
    "perfect veil": "VEIL",
    "ally protection": "ALLY_PROTECTION",
    "leech": "LEECH",
    "revive on death": "REVIVE",
    "increase spd": "INCREASE_SPEED",
    "increase speed": "INCREASE_SPEED",
    "increase atk": "INCREASE_ATTACK",
    "increase def": "INCREASE_DEFENSE",
    "increase c.rate": "INCREASE_CRIT_RATE",
    "increase c. rate": "INCREASE_CRIT_RATE",
    "increase c.rate ": "INCREASE_CRIT_RATE",
    "increased c. rate": "INCREASE_CRIT_RATE",
    "increase c.dmg": "INCREASE_CRIT_DAMAGE",
    "counterattack": "COUNTERATTACK",
    "reflect damage": "REFLECT_DAMAGE",
}

# Wirkungen, die nicht als Effektname markiert sind, sondern aus dem Satzbau
# hervorgehen. Bewusst eng gefasst, damit keine Falschtreffer entstehen.
PHRASE_EFFECTS: list[tuple[str, str]] = [
    (r"\bheals? (?:a |all |the )?(?:target )?all(?:y|ies)\b", "HEAL"),
    (r"\bheals? this champion\b", "HEAL"),
    (r"\brevives? (?:a |all )?(?:dead |destroyed )?all(?:y|ies)\b", "REVIVE"),
    (r"\bresurrects?\b", "REVIVE"),
    (r"\bremoves? (?:\d+ |all )?debuffs?\b", "CLEANSE"),
    (r"\btransfers? (?:\d+ |all )?debuffs?\b", "CLEANSE"),
    (r"\bfills? the turn meter\b", "TURN_METER_BOOST"),
    (r"\bincreases? the turn meter\b", "TURN_METER_BOOST"),
    # "the" fehlt in vielen Texten, und Coldhearts Kernfaehigkeit heisst "depletes".
    (r"\bdecreases? (?:the )?(?:target'?.?s? )?turn meter\b", "TURN_METER_DRAIN"),
    (r"\breduces? (?:the )?turn meter\b", "TURN_METER_DRAIN"),
    (r"\bdepletes? (?:the )?turn meter\b", "TURN_METER_DRAIN"),
    (r"\bremov(?:es|ing) (?:\d+|all|one) (?:random )?buffs?\b", "REMOVE_BUFFS"),
    (r"\bsteal(?:s|ing)? (?:\d+|all|one) (?:random )?buffs?\b", "REMOVE_BUFFS"),
    (r"\bextra turn\b", "EXTRA_TURN"),
    (r"\bignor(?:es|ing) \d+% of (?:the )?(?:target|enem)", "IGNORE_DEFENSE"),
    (r"\bignor(?:es|ing) (?:the )?(?:target'?s? )?def\b", "IGNORE_DEFENSE"),
    (r"\battacks? all enemies\b", "AOE_DAMAGE"),
    (r"\bteams? up with\b", "ALLY_ATTACK"),
    (r"\ball(?:y|ies) to attack\b", "ALLY_ATTACK"),
    (r"\bdecreases? (?:the )?(?:target'?s? )?max(?:imum)? hp\b", "DECREASE_MAX_HP"),
]

# Champions, die die fruehe Kampagne ueberproportional tragen. Bewusst eine kurze,
# handgepflegte Liste - das laesst sich nicht aus Skilltexten ableiten.
EARLY_GAME_CARRIES = {
    "Kael", "Athel", "Galek", "Elhain", "Warmaiden", "Apothecary",
    "Executioner", "Diabolist", "Spirithost",
}

# Aus der Kampagne farmbar, damit praktisch unbegrenzt als Futter verfuegbar.
CAMPAIGN_FARMABLE = {
    "Warmaiden", "Apothecary", "Spirithost", "Armiger", "Chaplain", "Herald",
    "Outlaw Monk", "Sacred Ghoul", "Ghoulish Mage", "Skullcrown",
}


def slugify(name: str) -> str:
    """Stabile, ASCII-basierte Kennung fuer einen Championnamen."""
    plain = unicodedata.normalize("NFKD", name).encode("ascii", "ignore").decode()
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", plain.lower())).strip("-")


def load_lenient(path: str):
    """Manche Rohdateien enthalten nachgestellte Kommas."""
    raw = open(path, encoding="utf-8-sig").read()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return json.loads(re.sub(r",(\s*[\]}])", r"\1", raw))


# Nur diese sechs Felder sind ueber den Bestand hinweg numerisch stimmig (siehe
# Modulkommentar). "crate" und "cdmg" fehlen hier bewusst.
_STAT_FIELDS = {
    "hp": "hp", "atk": "attack", "def": "defense",
    "spd": "speed", "resist": "resistance", "acc": "accuracy",
}


def parse_stats(detail: dict) -> dict[str, int] | None:
    """Liest die Basiswerte, wenn alle sechs Felder eine echte Zahl enthalten.

    Die Quelle liefert entweder den vollstaendigen Satz oder gar keinen - ein
    Mittelding (etwa nur "hp" ohne den Rest) kam bei keinem der 558 gepruesten
    Champions vor. Deshalb alles oder nichts, statt einzelne Felder zu raten.
    """
    raw = detail.get("stats")
    if not raw:
        return None
    out: dict[str, int] = {}
    for source_key, target_key in _STAT_FIELDS.items():
        value = str(raw.get(source_key, "")).replace(",", "").strip()
        if not re.fullmatch(r"\d+", value):
            return None
        out[target_key] = int(value)
    return out


def derive_utilities(skills: list[dict]) -> tuple[set[str], Counter]:
    """Leitet die Utility-Tags aus den Skillbeschreibungen ab."""
    found: set[str] = set()
    unknown: Counter = Counter()

    for skill in skills:
        text = (skill.get("description") or "")
        lowered = text.lower()

        for token in re.findall(r"\[([^\]]+)\]", text):
            key = token.strip().lower().rstrip(".")
            if key in BRACKET_EFFECTS:
                found.add(BRACKET_EFFECTS[key])
            elif len(key) < 30 and " when " not in key and " on the same team" not in key:
                unknown[token.strip()] += 1

        for pattern, utility in PHRASE_EFFECTS:
            if re.search(pattern, lowered):
                found.add(utility)

        # Einzelziel-Burst: hohe Schadensskalierung ohne Flaechenwirkung.
        scaling = skill.get("dmgscaling") or []
        if scaling and "all enemies" not in lowered and re.search(
            r"\bdamage (?:is )?(?:proportional|based on)\b|\bincreases? (?:by )?\d+% (?:for|per)\b", lowered
        ):
            found.add("SINGLE_TARGET_NUKE")

    return found, unknown


def summarise(utilities: set[str], role: str) -> str:
    """Kurze deutsche Einordnung, allein aus den erkannten Wirkungen gebildet."""
    labels = {
        "DECREASE_DEFENSE": "senkt Verteidigung", "DECREASE_ATTACK": "senkt Angriff",
        "WEAKEN": "schwächt", "HEAL": "heilt", "CONTINUOUS_HEAL": "heilt dauerhaft",
        "REVIVE": "belebt wieder", "CLEANSE": "reinigt Debuffs",
        "BLOCK_DEBUFFS": "blockt Debuffs", "UNKILLABLE": "macht unbesiegbar",
        "BLOCK_DAMAGE": "blockt Schaden", "SHIELD": "schildet",
        "TURN_METER_BOOST": "füllt die Zugleiste", "TURN_METER_DRAIN": "leert die Zugleiste",
        "INCREASE_SPEED": "erhöht Tempo", "INCREASE_ATTACK": "erhöht Angriff",
        "INCREASE_DEFENSE": "erhöht Verteidigung", "AOE_DAMAGE": "trifft alle Gegner",
        "STUN": "betäubt", "SLEEP": "schläfert ein", "FEAR": "verängstigt",
        "PROVOKE": "provoziert", "POISON": "vergiftet", "HP_BURN": "entzündet",
        "COUNTERATTACK": "kontert", "ALLY_ATTACK": "löst Verbündeten-Angriffe aus",
        "IGNORE_DEFENSE": "ignoriert Verteidigung", "DECREASE_MAX_HP": "senkt Max-LP",
        "EXTRA_TURN": "nimmt Extra-Züge", "LEECH": "entzieht Leben",
        "BLOCK_REVIVE": "blockt Wiederbelebung", "HEAL_REDUCTION": "mindert Heilung",
    }
    parts = [labels[u] for u in labels if u in utilities][:3]
    if not parts:
        return "Kein Effekt erkannt, der für Teams gebraucht wird — Aufwertungsfutter."
    return parts[0][0].upper() + parts[0][1:] + (
        ", " + ", ".join(parts[1:]) if len(parts) > 1 else ""
    ) + "."


def main() -> int:
    positional = [a for a in sys.argv[1:] if not a.startswith("--")]
    options = dict(
        a[2:].split("=", 1) for a in sys.argv[1:] if a.startswith("--") and "=" in a
    )
    if not positional:
        print(__doc__)
        return 2
    root = positional[0]
    target = positional[1] if len(positional) > 1 else "app/src/main/assets/champions.json"
    feed_out = options.get("feed-out")
    feed_version = int(options.get("feed-version", "1"))
    released = options.get("released", "")

    base = load_lenient(os.path.join(root, "champions-base-info.json"))
    details_dir = os.path.join(root, "champion-details")

    champions = []
    unknown_total: Counter = Counter()
    skipped: list[str] = []
    incomplete = 0

    # Einzelne Namen stehen doppelt in der Quelle, einmal mit Unterstrichen.
    # Der Eintrag mit Skilldaten gewinnt.
    seen: dict[str, int] = {}

    for name, info in sorted(base.items(), key=lambda kv: (" " not in kv[0], kv[0])):
        faction = FACTIONS.get(info.get("faction"))
        rarity = RARITIES.get((info.get("rarity") or "").lower())
        affinity = AFFINITIES.get((info.get("affinity") or "").lower())
        role = ROLES.get((info.get("role") or "").lower())

        if not (faction and rarity and affinity):
            skipped.append(f"{name}: unbekannte Stammdaten {info.get('faction')}/"
                           f"{info.get('rarity')}/{info.get('affinity')}")
            continue
        if role is None:
            # Ein Datensatz traegt 'force' als Rolle; Angriff ist die haeufigste.
            role = "ATTACK"

        detail_path = os.path.join(details_dir, name.replace(" ", "_") + ".json")
        skills: list[dict] = []
        stats: dict[str, int] | None = None
        if os.path.exists(detail_path):
            try:
                detail = load_lenient(detail_path)
                skills = detail.get("skills") or []
                stats = parse_stats(detail)
            except Exception as exc:  # pragma: no cover - defekte Rohdatei
                skipped.append(f"{name}: Detaildatei unlesbar ({exc})")

        utilities, unknown = derive_utilities(skills)
        unknown_total.update(unknown)
        # Ein Teil der Rohdaten enthaelt Skill-Eintraege ohne Beschreibungstext.
        # Solche Platzhalter zaehlen nicht als vorhandene Daten.
        data_complete = any((s.get("description") or "").strip() for s in skills)
        if not data_complete:
            incomplete += 1

        entry = {
            "id": slugify(name),
            "name": name,
            "faction": faction,
            "rarity": rarity,
            "affinity": affinity,
            "role": role,
            "utilities": sorted(utilities),
            "kitSummary": summarise(utilities, role) if data_complete
            else "Für diese Legende liegen noch keine Fähigkeiten vor.",
            "earlyGameCarry": name in EARLY_GAME_CARRIES,
            "campaignFarmable": name in CAMPAIGN_FARMABLE,
            "dataComplete": data_complete,
            "stats": stats,
        }

        champion_id = entry["id"]
        if champion_id in seen:
            previous = champions[seen[champion_id]]
            if data_complete and not previous["dataComplete"]:
                champions[seen[champion_id]] = entry
            continue
        seen[champion_id] = len(champions)
        champions.append(entry)

    ids = Counter(c["id"] for c in champions)
    duplicates = [i for i, n in ids.items() if n > 1]
    if duplicates:
        print(f"WARNUNG: doppelte Kennungen: {duplicates}")

    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(champions, fh, ensure_ascii=False, indent=1)
        fh.write("\n")

    if feed_out:
        feed = {"version": feed_version, "released": released, "champions": champions}
        os.makedirs(os.path.dirname(feed_out) or ".", exist_ok=True)
        with open(feed_out, "w", encoding="utf-8", newline=chr(10)) as fh:
            json.dump(feed, fh, ensure_ascii=False, indent=1)
            fh.write(chr(10))
        print(f"Feed Version {feed_version} geschrieben nach {feed_out}")

    no_utilities = sum(1 for c in champions if not c["utilities"] and c["dataComplete"])
    with_stats = sum(1 for c in champions if c["stats"] is not None)
    print(f"{len(champions)} Legenden geschrieben nach {target}")
    print(f"  ohne Fähigkeitsdaten: {incomplete}")
    print(f"  mit Fähigkeiten, aber ohne erkannte Wirkung: {no_utilities}")
    print(f"  mit Basiswerten: {with_stats}")
    if skipped:
        print(f"  übersprungen: {len(skipped)}")
        for s in skipped[:10]:
            print("    " + s)
    if unknown_total:
        print(f"  nicht zugeordnete Effektnamen: {len(unknown_total)}")
        for eff, count in unknown_total.most_common(15):
            print(f"    {count:4}  {eff}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
