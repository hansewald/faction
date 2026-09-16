package de.faction.data.model

/**
 * Die 51 Artefakt-Sets mit zwei oder vier Teilen — die Sets, die in der Praxis
 * tatsächlich zusammengestellt werden. Bewusst ausgeschlossen sind die selteneren
 * "Variable"-Sets mit 1 oder bis zu 9 Teilen (etwa Merciless, Slayer, Stone Skin):
 * für ihre Zwischenstufen zwischen 1 und 9 Teilen gibt es keine belastbar geprüfte
 * Quelle, und eine geratene Zahl wäre hier schlechter als eine fehlende.
 *
 * Werte geprüft gegen zwei unabhängige Quellen (siehe `docs/artefakt-sets-quellen.md`
 * für Datum und Belege) — Plarium ändert Set-Werte gelegentlich in Patches, ohne dass
 * das rückwirkend in dieser Datei sichtbar wird.
 */
object ArtifactSetContent {
    val sets: List<ArtifactSet> = listOf(
    ArtifactSet("life", "Leben", 2, "Erhöht die maximalen LP um 15 %.", SetKind.STAT),
    ArtifactSet("offense", "Angriff", 2, "Erhöht den Angriff um 15 %.", SetKind.STAT),
    ArtifactSet("defense", "Verteidigung", 2, "Erhöht die Verteidigung um 15 %.", SetKind.STAT),
    ArtifactSet("critical-rate", "Kritische Rate", 2, "Erhöht die Krit. Rate um 12 %.", SetKind.STAT),
    ArtifactSet("critical-damage", "Kritischer Schaden", 2, "Erhöht den Krit. Schaden um 20 %.", SetKind.STAT),
    ArtifactSet("accuracy", "Genauigkeit", 2, "Erhöht die Genauigkeit um 40 Punkte.", SetKind.STAT),
    ArtifactSet("speed", "Tempo", 2, "Erhöht das Tempo um 12 %.", SetKind.STAT),
    ArtifactSet("resistance", "Widerstand", 2, "Erhöht den Widerstand um 40 Punkte.", SetKind.STAT),
    ArtifactSet("resilience", "Zähigkeit", 2, "Erhöht LP und Verteidigung um je 10 %.", SetKind.HYBRID),
    ArtifactSet("fortitude", "Standhaftigkeit", 2, "Erhöht die Verteidigung um 10 % und den Widerstand um 40 Punkte.", SetKind.HYBRID),
    ArtifactSet("righteous", "Rechtschaffenheit", 2, "Erhöht das Tempo um 10 % und den Widerstand um 40 Punkte.", SetKind.HYBRID),
    ArtifactSet("perception", "Wahrnehmung", 2, "Erhöht die Genauigkeit um 40 Punkte und das Tempo um 5 %.", SetKind.HYBRID),
    ArtifactSet("fatal", "Tödlich", 2, "Erhöht den Angriff um 15 % und die Krit. Rate um 5 %.", SetKind.HYBRID),
    ArtifactSet("cruel", "Grausam", 2, "Erhöht den Angriff um 15 % und ignoriert 5 % der gegnerischen Verteidigung.", SetKind.HYBRID),
    ArtifactSet("killstroke", "Todesstoß", 2, "Erhöht den Krit. Schaden um 20 % und das Tempo um 5 %.", SetKind.HYBRID),
    ArtifactSet("impulse", "Impuls", 2, "Erhöht das Tempo um 12 %. 12 % Chance, die Abklingzeit einer zufälligen Fähigkeit um 1 Zug zu senken.", SetKind.HYBRID),
    ArtifactSet("zeal", "Eifer", 2, "Erhöht den Krit. Schaden um 20 % und steigert den Schaden zusätzlich, je weniger LP der Gegner noch hat.", SetKind.HYBRID),
    ArtifactSet("immortal", "Unsterblich", 2, "Erhöht die LP um 15 % und heilt den Träger jeden Zug um 3 % der max. LP.", SetKind.DEFENSIVE),
    ArtifactSet("frostbite", "Frostbiss", 2, "15 % Chance, ein Einfrieren abzuwehren, und 10 % Chance, den Angreifer dabei selbst einzufrieren.", SetKind.DEFENSIVE),
    ArtifactSet("defiant", "Trotz", 2, "Erhöht die Verteidigung um 10 % und senkt erlittenen Flächenschaden um 15 %.", SetKind.DEFENSIVE),
    ArtifactSet("retaliation", "Vergeltung", 2, "15 % Chance auf einen Gegenangriff, wenn der Träger angegriffen wird.", SetKind.OFFENSIVE),
    ArtifactSet("divine-life", "Göttliches Leben", 2, "Erhöht die LP um 15 % und legt zu Rundenbeginn ein Schild über 15 % der max. LP für 3 Züge.", SetKind.HYBRID),
    ArtifactSet("divine-offense", "Göttlicher Angriff", 2, "Erhöht den Angriff um 15 % und legt zu Rundenbeginn ein Schild über 15 % der max. LP für 3 Züge.", SetKind.HYBRID),
    ArtifactSet("divine-critical-rate", "Göttliche Krit. Rate", 2, "Erhöht die Krit. Rate um 12 % und legt zu Rundenbeginn ein Schild über 15 % der max. LP für 3 Züge.", SetKind.HYBRID),
    ArtifactSet("divine-speed", "Göttliches Tempo", 2, "Erhöht das Tempo um 12 % und legt zu Rundenbeginn ein Schild über 15 % der max. LP für 3 Züge.", SetKind.HYBRID),
    ArtifactSet("lifesteal", "Lebensraub", 4, "Heilt den Träger um 30 % des angerichteten Schadens.", SetKind.OFFENSIVE),
    ArtifactSet("savage", "Wild", 4, "Jeder Angriff ignoriert 25 % der gegnerischen Verteidigung.", SetKind.OFFENSIVE),
    ArtifactSet("instinct", "Instinkt", 4, "Erhöht das Tempo um 12 % und ignoriert 20 % der gegnerischen Verteidigung.", SetKind.HYBRID),
    ArtifactSet("lethal", "Tödliche Klinge", 4, "Ignoriert 25 % der gegnerischen Verteidigung und erhöht die Krit. Rate um 10 %.", SetKind.OFFENSIVE),
    ArtifactSet("affinitybreaker", "Affinitätsbrecher", 4, "Erhöht den Krit. Schaden um 30 %. 50 % Chance, einen schwachen Treffer in einen kritischen zu verwandeln.", SetKind.OFFENSIVE),
    ArtifactSet("bloodthirst", "Blutdurst", 4, "Erhöht die Krit. Rate um 12 % und heilt den Träger um 30 % des angerichteten Schadens.", SetKind.HYBRID),
    ArtifactSet("fury", "Raserei", 4, "Erhöht den angerichteten Schaden, je weniger LP der Träger noch hat — bis zu 50 % unter 51 % LP.", SetKind.OFFENSIVE),
    ArtifactSet("destroy", "Vernichtung", 4, "Senkt die maximalen LP des Ziels um einen Teil des angerichteten Schadens.", SetKind.OFFENSIVE),
    ArtifactSet("stun", "Betäubung", 4, "18 % Chance, den Gegner beim Angriff für 1 Zug zu betäuben.", SetKind.CONTROL),
    ArtifactSet("frost", "Frost", 4, "20 % Chance, bei einem erlittenen Angriff den Angreifer für 1 Zug einzufrieren.", SetKind.CONTROL),
    ArtifactSet("daze", "Schlaf", 4, "25 % Chance, den Gegner beim Angriff für 1 Zug einzuschläfern.", SetKind.CONTROL),
    ArtifactSet("provoke", "Provokation", 4, "30 % Chance, den Gegner beim Angriff für 1 Zug zu provozieren.", SetKind.CONTROL),
    ArtifactSet("cursed", "Verflucht", 4, "50 % Chance, dem Gegner beim Angriff für 2 Züge einen Fluch-Debuff aufzuerlegen.", SetKind.CONTROL),
    ArtifactSet("toxic", "Toxisch", 4, "75 % Chance, dem Gegner beim Angriff für 2 Züge 2,5 % Gift aufzuerlegen.", SetKind.CONTROL),
    ArtifactSet("reflex", "Reflex", 4, "40 % Chance, die Abklingzeit einer Fähigkeit um 1 Zug zu senken.", SetKind.UTILITY),
    ArtifactSet("relentless", "Unerbittlich", 4, "18 % Chance auf einen sofortigen Extra-Zug nach dem eigenen Zug.", SetKind.UTILITY),
    ArtifactSet("frenzy", "Raserei der Wut", 4, "Füllt die eigene Zugleiste um 5 % für jeden erlittenen Debuff.", SetKind.UTILITY),
    ArtifactSet("avenging", "Vergeltungsschlag", 4, "45 % Chance auf einen Gegenangriff, sobald der Gegner einen Debuff auf den Träger setzt.", SetKind.OFFENSIVE),
    ArtifactSet("shield", "Schild", 4, "Legt zu Rundenbeginn ein Schild über 30 % der max. LP für 3 Züge auf alle Verbündeten.", SetKind.DEFENSIVE),
    ArtifactSet("regeneration", "Regeneration", 4, "Heilt den Träger zu Rundenbeginn um 15 % der max. LP.", SetKind.DEFENSIVE),
    ArtifactSet("curing", "Heilkunst", 4, "Erhöht die Stärke der eigenen Heilungen um 20 %.", SetKind.UTILITY),
    ArtifactSet("guardian", "Wächter", 4, "Schildet Verbündete um 10 % des erlittenen Schadens und heilt sie zusätzlich jeden Zug.", SetKind.DEFENSIVE),
    ArtifactSet("bolster", "Rückhalt", 4, "Legt zu Rundenbeginn ein Schild über 30 % der max. LP auf einen Verbündeten und heilt den Träger jeden Zug um 10 %.", SetKind.DEFENSIVE),
    ArtifactSet("stalwart", "Unbeugsam", 4, "Senkt erlittenen Flächenschaden um 30 %.", SetKind.DEFENSIVE),
    ArtifactSet("immunity", "Immunität", 4, "Legt zu Rundenbeginn für 2 Züge Debuff-Schutz auf den Träger.", SetKind.DEFENSIVE),
    ArtifactSet("untouchable", "Unantastbar", 4, "Legt zu Rundenbeginn für 2 Züge Debuff-Schutz auf den Träger und erhöht den Widerstand um 40 Punkte.", SetKind.DEFENSIVE),
    )
}
