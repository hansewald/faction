package de.faction.data.model

/** Ein Kapitel des Anfänger-Guides. */
data class GuideChapter(
    val id: String,
    val title: String,
    val teaser: String,
    val paragraphs: List<String>,
)

/**
 * Der Anfänger-Guide liegt bewusst im Code statt auf einem Server: die Grundlagen
 * ändern sich selten, und die App bleibt damit ohne Netz vollständig nutzbar.
 */
object GuideContent {
    val chapters = listOf(
        GuideChapter(
            "start", "Die ersten Schritte",
            "Worauf es in den ersten Tagen wirklich ankommt.",
            listOf(
                "Ziel der ersten Wochen ist ein Team, das die Kampagne auf Brutal 12-3 im Auto-Modus durchspielt. Alles andere ergibt sich daraus: Futter, Silber, Kampagne-Champions.",
                "Wähle einen Starter mit Flächenschaden und baue ihn zuerst auf. Ein einziger starker Champion bringt dich weiter als vier halbfertige.",
                "Verfüttere nichts, was du nur einmal besitzt und was Selten oder besser ist, bevor du seine Fähigkeiten gelesen hast.",
            ),
        ),
        GuideChapter(
            "rank", "Level und Rang",
            "Warum Rang vor Level kommt.",
            listOf(
                "Futter sollte auf Level 1 bleiben. Erfahrung, die im Futter steckt, geht beim Aufwerten verloren.",
                "Für Rang 5 und 6 lohnt gleichfarbiges Futter: es bringt mehr Fortschritt pro eingesetztem Champion.",
                "Steigere erst den Rang, dann das Level. Ein 5-Sterne-Champion auf Level 1 ist besser aufzubauen als ein 4-Sterne auf Level 40.",
            ),
        ),
        GuideChapter(
            "gear", "Ausrüstung",
            "Sets, Hauptwerte und die Falle der Nebenwerte.",
            listOf(
                "Tempo ist der wichtigste Wert im Spiel. Ein Champion, der öfter dran ist, wirkt seine Fähigkeiten öfter — das schlägt fast jeden Schadensbonus.",
                "Für Champions, die Debuffs setzen, ist Genauigkeit die zweite Pflichtstat. Ohne sie treffen die Debuffs nicht, und das ganze Kit läuft ins Leere.",
                "Werte keine 4-Sterne-Ausrüstung aus niedrigen Dungeon-Stufen auf. Erst ab Stufe 13 bis 15 lohnt sich der Silberaufwand.",
            ),
        ),
        GuideChapter(
            "masteries", "Meisterschaften",
            "Welchen Pfad du für welche Rolle wählst.",
            listOf(
                "Meisterschaften kosten Schriftrollen aus dem Meisterschaftsturm. Setze sie erst ein, wenn ein Champion dauerhaft in deinem Team bleibt.",
                "Für Schadensträger gegen den Clanboss lohnt der Pfad über Anhaltender Schaden. Für Support zählen Debuff-Genauigkeit und Überleben.",
                "Ein Zurücksetzen kostet echte Ressourcen — lieber einmal überlegen als zweimal setzen.",
            ),
        ),
        GuideChapter(
            "campaign", "Kampagne und Farmen",
            "Wie du deine Energie am besten einsetzt.",
            listOf(
                "Energie ist deine knappste Ressource. Verbrenne sie nicht in Stufen, die du nicht sicher schaffst — wiederholtes Scheitern ist die teuerste Art zu spielen.",
                "Brutal 12-3 ist der Standard zum Futterfarmen, weil dort viele Champions pro Durchlauf fallen.",
                "Sammle in Events und Turnieren gezielt: dieselbe Energie zählt oft für mehrere Belohnungen gleichzeitig.",
            ),
        ),
        GuideChapter(
            "dungeons", "Dungeons",
            "Die Reihenfolge, in der du sie angehst.",
            listOf(
                "Der Dungeon der Wächter liefert die Erfahrung, die dir das Hochleveln erspart. Er hat Vorrang vor allem anderen.",
                "Danach folgen die Ausrüstungs-Dungeons. Bleibe auf einer Stufe, bis du sie zuverlässig im Auto-Modus schaffst, bevor du weitergehst.",
                "Der Fluch-Turm der jeweiligen Affinität liefert die Potionen zum Aufsteigen — ohne sie bleiben deine Champions in ihrem Rang stecken.",
            ),
        ),
        GuideChapter(
            "clanboss", "Clanboss",
            "Das erste Team, das dauerhaft Ressourcen liefert.",
            listOf(
                "Der Clanboss ist die verlässlichste Quelle für Ausrüstung und Schlüssel. Ein funktionierendes Team dort ist mehr wert als jeder einzelne starke Champion.",
                "Ein Grundgerüst braucht: Angriff senken, Verteidigung senken, Debuffs blocken und eine Heilung. Der Rest ist Schaden.",
                "Wichtiger als hohe Werte ist, dass das Team einen kompletten Durchlauf übersteht. Ein Team, das stirbt, bringt nichts.",
            ),
        ),
        GuideChapter(
            "safety", "Account und Sicherheit",
            "Was du niemals tun solltest.",
            listOf(
                "Gib deine Zugangsdaten in keinem Drittanbieter-Tool ein. Plarium untersagt das ausdrücklich, und die Konsequenz ist die dauerhafte Sperre deines Accounts.",
                "Verknüpfe deinen Fortschritt mit einem Plarium-Konto, damit er beim Gerätewechsel nicht verloren geht.",
                "Account-Kauf und -Verkauf sind ebenfalls untersagt — beide Seiten riskieren die Sperre.",
            ),
        ),
    )
}
