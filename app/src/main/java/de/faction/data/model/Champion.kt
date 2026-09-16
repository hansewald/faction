package de.faction.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Rarity(val stars: Int, val label: String) {
    COMMON(1, "Gewöhnlich"),
    UNCOMMON(2, "Ungewöhnlich"),
    RARE(3, "Selten"),
    EPIC(4, "Episch"),
    LEGENDARY(5, "Legendär");
}

enum class Affinity(val label: String) {
    MAGIC("Magie"), FORCE("Kraft"), SPIRIT("Geist"), VOID("Leere");
}

enum class Role(val label: String) {
    ATTACK("Angriff"), DEFENSE("Verteidigung"), HP("LP"), SUPPORT("Unterstützung");
}

/**
 * Wirkungen, die ein Champion über sein Skill-Kit einbringt. Die Bewertung in
 * [de.faction.domain.ScoreEngine] arbeitet ausschließlich auf diesen Tags,
 * nicht auf fremden Tier-List-Noten.
 */
enum class Utility(val label: String) {
    // Schwächungen
    DECREASE_DEFENSE("Verteidigung senken"),
    DECREASE_ATTACK("Angriff senken"),
    DECREASE_SPEED("Tempo senken"),
    DECREASE_ACCURACY("Genauigkeit senken"),
    DECREASE_CRIT_RATE("KritQuote senken"),
    DECREASE_CRIT_DAMAGE("KritSchaden senken"),
    WEAKEN("Schwächen"),
    HEAL_REDUCTION("Heilungsminderung"),
    BLOCK_BUFFS("Buffs blocken"),
    REMOVE_BUFFS("Buffs entfernen"),
    BLOCK_COOLDOWN("Fähigkeiten sperren"),
    BLOCK_REVIVE("Wiederbelebung blocken"),
    DECREASE_MAX_HP("Max-LP senken"),

    // Schaden über Zeit
    POISON("Gift"),
    POISON_SENSITIVITY("Giftempfindlichkeit"),
    HP_BURN("LP-Verbrennung"),
    BOMB("Bombe"),

    // Kontrolle
    STUN("Betäuben / Einfrieren"),
    SLEEP("Schlaf"),
    FEAR("Furcht"),
    PROVOKE("Provozieren"),
    TURN_METER_DRAIN("Zugleiste leeren"),

    // Erhalt
    HEAL("Heilen"),
    CONTINUOUS_HEAL("Dauerheilung"),
    REVIVE("Wiederbeleben"),
    SHIELD("Schild"),
    CLEANSE("Debuffs entfernen"),
    BLOCK_DEBUFFS("Debuffs blocken"),
    BLOCK_DAMAGE("Schaden blocken"),
    UNKILLABLE("Unbesiegbar"),
    VEIL("Schleier"),
    ALLY_PROTECTION("Verbündeten-Schutz"),
    STRENGTHEN("Stärkung"),
    LEECH("Lebensentzug"),

    // Verstärkung
    INCREASE_SPEED("Tempo erhöhen"),
    INCREASE_ATTACK("Angriff erhöhen"),
    INCREASE_DEFENSE("Verteidigung erhöhen"),
    INCREASE_CRIT_RATE("KritQuote erhöhen"),
    INCREASE_CRIT_DAMAGE("KritSchaden erhöhen"),
    TURN_METER_BOOST("Zugleiste füllen"),

    // Schaden und Zugfolge
    EXTRA_TURN("Extra-Zug"),
    ALLY_ATTACK("Verbündeten-Angriff"),
    COUNTERATTACK("Gegenangriff"),
    REFLECT_DAMAGE("Schaden zurückwerfen"),
    IGNORE_DEFENSE("Verteidigung ignorieren"),
    AOE_DAMAGE("Flächenschaden"),
    SINGLE_TARGET_NUKE("Einzelziel-Burst"),
}

enum class Area(val label: String) {
    CLAN_BOSS("Clanboss"),
    ARENA("Arena"),
    DUNGEONS("Dungeons"),
    DOOM_TOWER("Turm des Schicksals"),
    FACTION_WARS("Fraktionskriege"),
    CAMPAIGN("Kampagne / Farmen");
}

/**
 * Die sechs verlässlichen Basiswerte bei Rang 6, Stufe 60, ohne Ausrüstung — die in
 * der Community übliche Vergleichsbasis. Krit-Rate und Krit-Schaden fehlen bewusst:
 * in der Rohdatenquelle ist das Feld für die Kritquote bei praktisch jedem Champion
 * der Platzhaltertext „RATE“ statt einer Zahl, und der Krit-Schaden trägt bei den
 * meisten Champions denselben Wert 15 — kein gemessener Wert, sondern ein
 * Formularfeld, das beim Auslesen nicht befüllt wurde. Lieber ganz weglassen als
 * eine erfundene Zahl anzeigen.
 */
@Serializable
data class ChampionStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val resistance: Int,
    val accuracy: Int,
)

/** Stammdaten eines Champions — unabhängig davon, ob der Spieler ihn besitzt. */
@Serializable
data class Champion(
    val id: String,
    val name: String,
    val faction: String,
    val rarity: Rarity,
    val affinity: Affinity,
    val role: Role,
    @SerialName("utilities") val utilities: Set<Utility> = emptySet(),
    /** Kurze, eigene Einordnung des Kits in einem Satz. */
    val kitSummary: String = "",
    /** Champions, die früh im Spiel überproportional viel Wert liefern. */
    val earlyGameCarry: Boolean = false,
    /** Aus der Kampagne farmbar — damit als Futter praktisch unbegrenzt verfügbar. */
    val campaignFarmable: Boolean = false,
    /**
     * Falsch, wenn für diese Legende keine Fähigkeiten vorliegen. Ohne Kit lässt sich
     * nichts bewerten — solche Einträge dürfen nie als Futter empfohlen werden.
     */
    val dataComplete: Boolean = true,
    /** `null`, wenn die Quelle für diese Legende keine verlässlichen Werte lieferte. */
    val stats: ChampionStats? = null,
) {
    /**
     * Adresse der Legende im RaidWiki, zum Nachschlagen von Werten und Fähigkeiten.
     *
     * Die App verlinkt dorthin, statt fremde Inhalte selbst auszuliefern: was dort
     * steht, gehört den Betreibern und Plarium. Der Namensteil folgt deren Muster —
     * Kleinschreibung, Leerzeichen als Bindestrich, Sonderzeichen entfallen.
     */
    val wikiUrl: String
        get() {
            val slug = name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
            return "https://raidwiki.com/champion/" + slug
        }
}
