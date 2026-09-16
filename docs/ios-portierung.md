# iOS-Portierung: Stand und Optionen

**Nicht umgesetzt — und auf dieser Maschine auch nicht überprüfbar umsetzbar.**
Diese Datei hält fest, warum, und was die eigentliche Arbeit wäre, damit eine
spätere Entscheidung nicht bei null anfängt.

## Warum hier nichts Neues dazukam

Dieses Projekt wird durchgehend nach einem festen Grundsatz entwickelt: nichts
gilt als fertig, das nicht kompiliert und dessen Tests nicht liefen (siehe die
Commit-Historie — jede Änderung nennt Testanzahl und Build-Ergebnis). Für iOS
bräuchte es Xcode und einen Swift-Compiler; beides läuft nur unter macOS. Diese
Maschine ist ein Windows-Rechner ohne Zugriff auf einen Mac. Blind Swift-Code zu
schreiben, den niemand kompiliert hat, wäre genau die Art unüberprüfter Arbeit,
die in diesem Projekt sonst konsequent vermieden wird — siehe etwa die
Krit-Rate/Krit-Schaden-Entscheidung im Champion-Katalog oder die
Artefakt-Set-Quellenprüfung: lieber eine Lücke offen lassen als eine ungeprüfte
Behauptung ausliefern.

## Die eigentliche Frage: wie viel teilen sich beide Plattformen

Nicht *ob* iOS möglich ist, sondern *wie viel vom bestehenden Code wiederverwendbar
ist*. Drei Wege, absteigend nach Wiederverwendung:

### 1. Kotlin Multiplatform (KMP) — die meiste Wiederverwendung

`data.model`, `domain` (ScoreEngine, BuildAdvisor, BuildPlanner, MasteryPlanner)
und ein Großteil von `data.repo` enthalten keine Android-spezifischen Typen — sie
sind reines Kotlin. Als KMP-Modul kompiliert dieser Code sowohl zu einer
Android- als auch zu einer iOS-Bibliothek (über Kotlin/Native). Übrig bliebe pro
Plattform nur die Oberfläche: Jetpack Compose bleibt für Android, SwiftUI käme
für iOS neu dazu. Was nicht mitkäme: `data.local` (Room ist Android-spezifisch,
bräuchte ein `expect`/`actual`-Paar mit einem iOS-seitigen Ersatz, etwa SQLDelight
oder Core Data direkt) und `data.source` (ML Kit für die OCR ist ebenfalls
Android-gebunden — iOS hätte hier die Wahl zwischen Apples eigener Vision-API
mit neuem Erkennungscode oder einem plattformübergreifenden OCR-Paket).

Aufwand grob: die drei genannten Pakete in ein `commonMain`-Sourceset ziehen,
Gradle auf ein KMP-Setup umstellen, dann für iOS neu bauen: Oberfläche,
Room-Ersatz, OCR-Ersatz, Portrait-Speicher (`PortraitStore`/`BundledPortraits`),
Bildschirme.

### 2. Komplette SwiftUI-Neufassung — keine Wiederverwendung, volle Kontrolle

Die *Regeln* (welcher Champion sich lohnt, welches Set wozu passt) noch einmal in
Swift nachbauen, mit denselben Daten (`champions.json`, `ArtifactSetContent`
ließen sich 1:1 als JSON mitgeben, da sie ohnehin nur Daten sind, kein
Android-Code). Mehr Aufwand als KMP, aber kein Kotlin/Native-Setup nötig und
volle Freiheit bei der iOS-Oberfläche.

### 3. Cross-Platform-Framework (Flutter, React Native) — Neuanfang

Würde beide Oberflächen ersetzen, auch die bestehende Android-App. Nur sinnvoll,
wenn ohnehin ein Neuanfang gewollt ist — verwirft sonst investierte Arbeit ohne
Not.

## Empfehlung, unverbindlich

Kotlin Multiplatform, weil `domain` bereits sauber von Android getrennt ist —
das war beim Bau nicht der Anlass, zahlt sich hier aber aus. Der größte
Einzelposten wäre der Room-Ersatz für `data.local`, nicht die Geschäftslogik.

## Voraussetzung für den nächsten Schritt

Ein Mac mit Xcode, oder zumindest ein CI-Dienst, der iOS-Builds ausführen kann
(z. B. GitHub Actions mit `macos-latest`-Runnern) — dafür wiederum bräuchte es
ein Repository auf GitHub, siehe die offene Feed-Adresse in der README.
