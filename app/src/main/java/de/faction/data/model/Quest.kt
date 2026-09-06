package de.faction.data.model

enum class QuestCycle(val label: String) {
    DAILY("Täglich"),
    WEEKLY("Wöchentlich"),
    PROGRESS("Fortschritt"),
}

/**
 * Eine wiederkehrende Aufgabe im Spiel. Der Nutzen liegt nicht in der Liste selbst —
 * die steht im Spiel — sondern in der Begründung, warum sich eine Aufgabe lohnt und
 * wie man sie nebenbei erledigt.
 */
data class Quest(
    val id: String,
    val cycle: QuestCycle,
    val title: String,
    val reward: String,
    val why: String,
)

object QuestContent {
    val quests = listOf(
        Quest(
            "daily-login", QuestCycle.DAILY, "Einloggen",
            "Tagesbelohnung",
            "Die Login-Kette läuft über 100 Tage bis zu einem legendären Champion. Ein verpasster Tag setzt sie nicht zurück, verzögert sie aber.",
        ),
        Quest(
            "daily-energy", QuestCycle.DAILY, "Energie ausgeben",
            "Silber und Erfahrung",
            "Am günstigsten nebenbei beim Farmen der Kampagne — dieselbe Energie zählt für Aufgabe, Futter und Turnierpunkte gleichzeitig.",
        ),
        Quest(
            "daily-dungeon", QuestCycle.DAILY, "Dungeons abschließen",
            "Fortschritt und Ausrüstung",
            "Nimm den Dungeon der Wächter, solange du Champions hochlevelst. Er spart mehr Energie als jede andere Quelle.",
        ),
        Quest(
            "daily-clanboss", QuestCycle.DAILY, "Clanboss angreifen",
            "Clan-Truhen",
            "Die verlässlichste Ausrüstungsquelle im Spiel. Wichtiger als hohe Werte ist ein Team, das den Durchlauf übersteht.",
        ),
        Quest(
            "daily-arena", QuestCycle.DAILY, "Arena-Kämpfe bestreiten",
            "Arena-Marken",
            "Auch verlorene Kämpfe zählen für die Aufgabe. Die Marken finanzieren später die großen Arena-Belohnungen.",
        ),
        Quest(
            "weekly-upgrade", QuestCycle.WEEKLY, "Champions aufwerten",
            "Wöchentliche Truhe",
            "Plane Aufwertungen auf die Woche verteilt statt alles an einem Tag — so deckst du Wochen- und Tagesaufgaben zugleich ab.",
        ),
        Quest(
            "weekly-gear", QuestCycle.WEEKLY, "Ausrüstung aufwerten",
            "Wöchentliche Truhe",
            "Werte nur Teile ab Dungeon-Stufe 13 auf. Alles darunter verbrennt Silber, das dir später fehlt.",
        ),
        Quest(
            "progress-arbiter", QuestCycle.PROGRESS, "Fortschrittsmissionen",
            "Schiedsrichterin",
            "Der wichtigste Langzeitpfad im Spiel. Die Belohnung am Ende beschleunigt jedes Team dauerhaft — richte deinen Aufbau danach aus.",
        ),
    )
}
