package de.faction.data.model

enum class MasteryTree(val label: String) {
    OFFENSE("Offensive"),
    DEFENSE("Defensive"),
    SUPPORT("Unterstützung"),
}

/**
 * Eine Meisterschaft im Baum.
 *
 * [name] bleibt der Name aus dem Spiel — danach sucht man im Meisterschaftsfenster,
 * eine Übersetzung würde das Wiederfinden nur erschweren. Die Erklärung daneben ist
 * unsere eigene, knappe Fassung der Wirkung.
 */
data class Mastery(
    val tree: MasteryTree,
    /** 1..6. Tier 1 hat zwei Meisterschaften, alle weiteren je vier. */
    val tier: Int,
    val name: String,
    val effect: String,
)

/**
 * Der vollständige Meisterschaftsbaum: drei Bäume zu je 22 Meisterschaften.
 *
 * Reine Stammdaten, keine Bewertung — welche davon empfohlen werden, entscheidet
 * [de.faction.domain.MasteryPlanner] aus dem Kit des Champions.
 */
object Masteries {

    val all: List<Mastery> = listOf(
        // ---- Offensive ----
        Mastery(MasteryTree.OFFENSE, 1, "Blade Disciple", "+75 Angriff"),
        Mastery(MasteryTree.OFFENSE, 1, "Deadly Precision", "+5 % KritQuote"),

        Mastery(MasteryTree.OFFENSE, 2, "Heart of Glory", "Mehr Schaden bei vollen LP"),
        Mastery(MasteryTree.OFFENSE, 2, "Keen Strike", "+10 % KritSchaden"),
        Mastery(MasteryTree.OFFENSE, 2, "Shield Breaker", "+25 % Schaden gegen Schilde"),
        Mastery(MasteryTree.OFFENSE, 2, "Grim Resolve", "Mehr Schaden unter 50 % LP"),

        Mastery(MasteryTree.OFFENSE, 3, "Single Out", "+8 % Schaden gegen Ziele unter 40 % LP"),
        Mastery(MasteryTree.OFFENSE, 3, "Life Drinker", "Heilung aus Schaden unter 50 % LP"),
        Mastery(MasteryTree.OFFENSE, 3, "Whirlwind of Death", "Tempo je erschlagenem Gegner"),
        Mastery(MasteryTree.OFFENSE, 3, "Ruthless Ambush", "Mehr Schaden beim ersten Treffer je Gegner"),

        Mastery(MasteryTree.OFFENSE, 4, "Bring It Down", "Mehr Schaden gegen Ziele mit hohen LP"),
        Mastery(MasteryTree.OFFENSE, 4, "Wrath of the Slain", "Mehr Schaden je gefallenem Verbündeten"),
        Mastery(MasteryTree.OFFENSE, 4, "Cycle of Violence", "Abklingzeit sinkt bei hohem Schaden"),
        Mastery(MasteryTree.OFFENSE, 4, "Opportunist", "Mehr Schaden gegen kontrollierte Ziele"),

        Mastery(MasteryTree.OFFENSE, 5, "Methodical", "Schaden steigt bei wiederholtem Standardangriff"),
        Mastery(MasteryTree.OFFENSE, 5, "Kill Streak", "Schaden staffelt je erschlagenem Gegner"),
        Mastery(MasteryTree.OFFENSE, 5, "Blood Shield", "Schild bei jedem Erschlagen"),
        Mastery(MasteryTree.OFFENSE, 5, "Stoked to Fury", "Mehr Schaden je Schwächung auf sich selbst"),

        Mastery(MasteryTree.OFFENSE, 6, "Warmaster", "Zusatzschaden aus den Max-LP des Gegners"),
        Mastery(MasteryTree.OFFENSE, 6, "Helmsmasher", "Chance, Verteidigung zu durchdringen"),
        Mastery(MasteryTree.OFFENSE, 6, "Giant Slayer", "Zusatzschaden bei mehrfach treffenden Fähigkeiten"),
        Mastery(MasteryTree.OFFENSE, 6, "Flawless Execution", "+20 % KritSchaden"),

        // ---- Defensive ----
        Mastery(MasteryTree.DEFENSE, 1, "Tough Skin", "+75 Verteidigung"),
        Mastery(MasteryTree.DEFENSE, 1, "Defiant", "+10 Widerstand"),

        Mastery(MasteryTree.DEFENSE, 2, "Blastproof", "Weniger Flächenschaden"),
        Mastery(MasteryTree.DEFENSE, 2, "Rejuvenation", "Erhaltene Heilung und Schilde wirken stärker"),
        Mastery(MasteryTree.DEFENSE, 2, "Mighty Endurance", "Weniger Schaden während Kontrolle"),
        Mastery(MasteryTree.DEFENSE, 2, "Improved Parry", "Weniger Schaden durch kritische Treffer"),

        Mastery(MasteryTree.DEFENSE, 3, "Shadow Heal", "Heilung, wenn Gegner sich heilen"),
        Mastery(MasteryTree.DEFENSE, 3, "Resurgent", "Entfernt eine Schwächung bei hohem Schaden"),
        Mastery(MasteryTree.DEFENSE, 3, "Bloodthirst", "Heilung beim Erschlagen"),
        Mastery(MasteryTree.DEFENSE, 3, "Wisdom of Battle", "Chance auf Debuff-Block nach Kontrolle"),

        Mastery(MasteryTree.DEFENSE, 4, "Solidarity", "Widerstand für Verbündete je Stärkung"),
        Mastery(MasteryTree.DEFENSE, 4, "Delay Death", "Schadensminderung, gestaffelt je Gegner"),
        Mastery(MasteryTree.DEFENSE, 4, "Harvest Despair", "Lebensentzug beim Setzen von Kontrolle"),
        Mastery(MasteryTree.DEFENSE, 4, "Stubbornness", "Widerstand staffelt je aktiver Schwächung"),

        Mastery(MasteryTree.DEFENSE, 5, "Selfless Defender", "Fängt den ersten Treffer auf Verbündete ab"),
        Mastery(MasteryTree.DEFENSE, 5, "Cycle of Revenge", "Zugleiste, wenn Verbündete kritisch getroffen werden"),
        Mastery(MasteryTree.DEFENSE, 5, "Retribution", "Gegenangriff bei hohem erlittenem Schaden"),
        Mastery(MasteryTree.DEFENSE, 5, "Deterrence", "Gegenangriff, wenn Verbündete kontrolliert werden"),

        Mastery(MasteryTree.DEFENSE, 6, "Iron Skin", "+200 Verteidigung"),
        Mastery(MasteryTree.DEFENSE, 6, "Bulwark", "Schadensminderung für das ganze Team"),
        Mastery(MasteryTree.DEFENSE, 6, "Fearsome Presence", "Kontrolle greift häufiger"),
        Mastery(MasteryTree.DEFENSE, 6, "Unshakeable", "+50 Widerstand"),

        // ---- Unterstützung ----
        Mastery(MasteryTree.SUPPORT, 1, "Steadfast", "+810 Max-LP"),
        Mastery(MasteryTree.SUPPORT, 1, "Pinpoint Accuracy", "+10 Genauigkeit"),

        Mastery(MasteryTree.SUPPORT, 2, "Lay on Hands", "+5 % Heilwirkung"),
        Mastery(MasteryTree.SUPPORT, 2, "Shieldbearer", "+5 % Schildwert"),
        Mastery(MasteryTree.SUPPORT, 2, "Exalt in Death", "Heilung beim ersten Erschlagen je Runde"),
        Mastery(MasteryTree.SUPPORT, 2, "Charged Focus", "Genauigkeit, solange nichts abklingt"),

        Mastery(MasteryTree.SUPPORT, 3, "Healing Savior", "Stärkere Heilung für Verbündete mit wenig LP"),
        Mastery(MasteryTree.SUPPORT, 3, "Rapid Response", "Zugleiste, wenn Stärkungen enden"),
        Mastery(MasteryTree.SUPPORT, 3, "Swarm Smiter", "Genauigkeit je lebendem Gegner"),
        Mastery(MasteryTree.SUPPORT, 3, "Arcane Celerity", "Zugleiste, wenn Schwächungen enden"),

        Mastery(MasteryTree.SUPPORT, 4, "Merciful Aid", "Stärkere Heilung für geschwächte Verbündete"),
        Mastery(MasteryTree.SUPPORT, 4, "Cycle of Magic", "Chance, eine Abklingzeit zu verkürzen"),
        Mastery(MasteryTree.SUPPORT, 4, "Lore of Steel", "+15 % auf Boni einfacher Artefaktsets"),
        Mastery(MasteryTree.SUPPORT, 4, "Evil Eye", "Zugleiste des Ziels beim ersten Standardangriff"),

        Mastery(MasteryTree.SUPPORT, 5, "Lasting Gifts", "Chance auf längere Stärkungen"),
        Mastery(MasteryTree.SUPPORT, 5, "Spirit Haste", "Tempo je gefallenem Verbündeten"),
        Mastery(MasteryTree.SUPPORT, 5, "Sniper", "Schwächungen greifen häufiger"),
        Mastery(MasteryTree.SUPPORT, 5, "Master Hexer", "Chance auf längere Schwächungen"),

        Mastery(MasteryTree.SUPPORT, 6, "Elixir of Life", "+3000 Max-LP"),
        Mastery(MasteryTree.SUPPORT, 6, "Timely Intervention", "Zugleiste, wenn ein Verbündeter unter 25 % LP fällt"),
        Mastery(MasteryTree.SUPPORT, 6, "Oppressor", "Schnellere Zugleiste je aktiver Schwächung"),
        Mastery(MasteryTree.SUPPORT, 6, "Eagle Eye", "+50 Genauigkeit"),
    )

    fun tree(tree: MasteryTree): List<Mastery> = all.filter { it.tree == tree }

    /** Die Meisterschaften einer Stufe, in der Reihenfolge des Spielfensters. */
    fun tier(tree: MasteryTree, tier: Int): List<Mastery> =
        all.filter { it.tree == tree && it.tier == tier }

    /** Ein Baum hat sechs Stufen. */
    val tiers: IntRange = 1..6
}
