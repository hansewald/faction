# FACTION

Android-App (Kotlin / Jetpack Compose) für RAID: Shadow Legends. Fünf Reiter — Start,
Legenden, Quests, Anleitung, News — nach dem vorgegebenen UI-Entwurf. Inhaltlicher
Schwerpunkt ist der Aufbau: welche Legende du als nächstes entwickelst, welche Futter
ist, und in welcher Reihenfolge du vorgehst.

## Projekt öffnen

Das Projekt enthält keine `gradle-wrapper.jar` (Binärdatei). Einmalig erzeugen:

```bash
cd raidcompanion && gradle wrapper
```

Alternativ in Android Studio öffnen — es legt den Wrapper selbst an. Danach:

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug
```

Benötigt JDK 17 und Android SDK 35.

## Aufbau

| Paket | Inhalt |
| --- | --- |
| `data.model` | Champion, Kader, Wirkungen (`Utility`), Bereiche (`Area`), Artefakt-Sets (`ArtifactSet`) |
| `data.source` | Importquellen hinter dem Interface `AccountSource` |
| `data.local` | Room-Datenbank für den erfassten Kader |
| `data.repo` | Champion-Katalog und Kader-Repository |
| `domain` | `ScoreEngine` (Kit-Bewertung), `BuildPlanner` (Aufbauplan), `BuildAdvisor` (Empfehlungen) |
| `ui.components` | Wiederverwendete Bausteine: Goldrahmen-Karte, Tags, Schrittmarken |
| `ui.screens` | Start, Legenden, Helden-Details, Artefakte, Quests, Anleitung, News |
| `ui.theme` | FACTION-Palette (nur dunkel), Typografie, Formen |

## Wie die Bewertung funktioniert

Es werden **keine fremden Tier-List-Noten übernommen**. Ein Champion wird
ausschließlich über die Wirkungen bewertet, die sein Kit mitbringt (`Utility`).

1. **`ScoreEngine`** gewichtet jede Wirkung je Spielbereich. Die Gewichte bilden ab,
   was der Bereich mechanisch verlangt — am Clanboss zählen Angriff senken und
   Schwächen, in der Arena Zugleiste und Tempo. Der stärkste Effekt eines Kits zählt
   voll, jeder weitere abgeschwächt, damit breite Kits nicht automatisch gewinnen.
   Seltenheit begrenzt das Ergebnis als Faktor. Herauskommt ein Wert 0–100 je Bereich,
   der nur innerhalb desselben Bereichs vergleichbar ist.

2. **`BuildPlanner`** erzeugt daraus den dreistufigen Aufbauplan der Detailseite —
   Level & Rang, Ausrüstung, Meisterschaften — abhängig von der gewählten Spielphase.
   Wer Debuffs setzt, bekommt Genauigkeit als Pflichtstat; wer heilt, Tempo vor Schaden.

3. **`BuildAdvisor`** übersetzt das in eine Empfehlung. Leitfrage ist nicht "wer ist der
   beste Champion", sondern "was fehlt diesem Kader":
   - **Lücken** werden über die *einsatzbereiten* Champions berechnet (Rang ≥ 5 oder
     Level ≥ 50). Ein Champion, der ungenutzt in der Sammlung liegt, deckt nichts ab —
     und kommt deshalb selbst als Lückenfüller in Frage.
   - Wer als Einziger eine fehlende Kernsäule mitbringt, wird gebaut, auch mit schmalem
     Kit.
   - Ein einzeln vorhandener Epischer oder Legendärer wird **nie** als Futter markiert.
   - Dünn besetzte Fraktionen heben schwächere Champions an, weil die Fraktionskriege
     fünf Champions je Fraktion verlangen.
   - Die Accountphase (Früh/Mittel/Fortgeschritten) wird aus dem Kader abgeleitet und
     verschiebt die Gewichtung Richtung Kampagne-Farmen.

Jede Empfehlung führt ihre Begründung mit, damit nachvollziehbar bleibt, warum ein
Champion oben steht.

## Kaderdaten importieren

`AccountSource` ist die Schnittstelle für alle Importwege:

- **`ScreenshotOcrSource`** — liest Namen und Level per ML Kit aus einem Screenshot der
  Championliste. Kein Zugriff auf den Account.
- **`ToolkitJsonSource`** — importiert einen Export des
  [Raid Toolkits](https://github.com/raid-toolkit/raid-toolkit) (Windows), das die Daten
  aus der laufenden Spielsitzung liest. Die App spricht dabei nie selbst mit
  Plarium-Servern.
- **Lesezeichen** auf jeder Legendenkarte — nimmt sie in "Meine Auswahl" auf.

Jede Quelle liefert **Vorschläge** (`ImportCandidate`), keine fertigen Kadereinträge.
Dazwischen liegt immer der Bestätigungsschritt:

- Sichere Treffer (Namensschärfe ≥ 85 %) sind vorausgewählt, unsichere nicht.
- Zeilen ohne Zuordnung lassen sich von Hand einer Legende zuweisen, statt sie zu
  verwerfen. Rang und Level sind editierbar.
- Erst „Übernehmen" schreibt in den Kader; „Kader ersetzen" ist eine bewusste Option.

Der Zwischenschritt ist kein Komfort, sondern Korrektheit: eine falsch erkannte Legende
zählt als abgedeckte Wirkung, verschiebt die Lückenberechnung und erzeugt damit eine
falsche Aufbau-Empfehlung.

Bewusst **nicht** enthalten ist ein Login mit Plarium-Zugangsdaten. Es gibt keinen
offiziellen Endpunkt dafür, und die
[Nutzungsbedingungen](https://company.plarium.com/en/terms/terms-of-use/) untersagen
sowohl die Weitergabe von Zugangsdaten als auch Drittanbieter-Clients — mit
Account-Sperre als Konsequenz. Sollte Plarium eine offizielle Anmeldung anbieten, ist
sie eine weitere Implementierung von `AccountSource`; der Rest der App bleibt unverändert.

## Datenbasis

`app/src/main/assets/champions.json` enthält **570 Legenden** aus 14 Fraktionen,
erzeugt von `tools/generate_champions.py` aus
[PatPat1567/RaidShadowLegendsData](https://github.com/PatPat1567/RaidShadowLegendsData).

Übernommen werden ausschließlich Fakten: Name, Fraktion, Seltenheit, Affinität, Rolle
und die Frage, **welche Wirkungen ein Kit mitbringt**. Die Skill-Texte selbst werden
nicht mitgeliefert — sie gehören Plarium. Der Generator liest sie nur, um daraus die
`Utility`-Tags abzuleiten, mit denen die `ScoreEngine` arbeitet. Neu erzeugen:

```bash
python tools/generate_champions.py <pfad-zum-datenrepo>
```

**Datenqualität, ehrlich benannt:**

- **90 der 570 Legenden haben keine Fähigkeitsdaten** — die Quelle enthält für sie nur
  Platzhalter. Sie tragen `dataComplete: false`, werden in der Liste als „Daten fehlen"
  markiert und **nie** als Futter empfohlen: ein solches Urteil wäre ein Rat auf Basis
  fehlender Information, nicht auf Basis eines schwachen Kits.
- **Die Quelle hat Fehler.** Stichprobe: Coldheart steht dort als Selten/Dunkelelfen,
  im Spiel ist sie Episch/Hochelfen. Vor einer Veröffentlichung braucht es einen
  Abgleich gegen eine zweite Quelle.
- Bei 10 Legenden mit Skilltext wird keine Wirkung erkannt; ihre Kits bestehen aus
  Effekten, die das Modell bewusst nicht führt.

Die 47 Wirkungen im Modell decken 3,5 Tags je Legende ab.

**Basiswerte** (LP, Angriff, Verteidigung, Tempo, Widerstand, Genauigkeit) stehen für
480 der 570 Legenden im Katalog — bei Rang 6, Stufe 60, ohne Ausrüstung, der in der
Community üblichen Vergleichsbasis. Bewusst **nicht** übernommen sind Krit-Rate und
Krit-Schaden: In der Rohquelle trägt das Feld für die Kritquote bei fast jeder Legende
den Platzhaltertext „RATE“ statt einer Zahl, und der Krit-Schaden denselben Wert 15 —
kein gemessener Wert. Für diese beiden bleibt der Link ins RaidWiki.

## Bildmaterial

**FACTION zeigt die offiziellen Champion-Portraits mit Genehmigung von Plarium.**
Die Genehmigung, die Bestätigung durch das Legal Department und die Auflagen stehen in
[`docs/plarium-genehmigung.md`](docs/plarium-genehmigung.md).

> **Die App muss kostenlos bleiben und darf keinerlei Einnahmen erzielen** — keine
> Werbung, keine Käufe, keine Abos, keine Spenden, keine Affiliate-Links. Jede dieser
> Änderungen beendet die Genehmigung. Das gilt auch für einen einzelnen Spendenlink.

Welches Bild eine Legende zeigt, in dieser Reihenfolge:

1. das Portrait aus dem eigenen Screenshot des Spielers (`PortraitStore`, bleibt auf
   dem Gerät),
2. das mitgelieferte Portrait (`BundledPortraits`, 550 Bilder, 3,7 MB als WebP in
   `assets/portraits`),
3. das selbst gezeichnete Wappen (`ChampionSigil`) — für 20 Legenden, deren
   Quelldatei in der Community-Datenbank beschädigt ist.

Neu erzeugen:

```bash
python tools/generate_portraits.py <pfad-zum-datenrepo>
```

Die Auflagen „Plarium als Rechteinhaber nennen" und „als inoffiziell kennzeichnen"
sind in der App umgesetzt: Quellenangabe im Legenden-Reiter und im Detailkopf, voller
Hinweis im News-Reiter.

## Katalog aktuell halten

Neue Legenden erscheinen laufend. Ein Katalog, der nur beim Erscheinen mitgeliefert
wird, veraltet zwischen zwei Store-Updates — deshalb der Abgleich im News-Reiter.

`CatalogUpdater` lädt einen versionierten Feed:

```json
{ "version": 2, "released": "2026-09-07", "champions": [ … ] }
```

Ist die Version höher als der lokale Stand, wird er übernommen und die Liste sofort
aktualisiert; neue Legenden werden namentlich gemeldet. Eine leere, unlesbare oder
ältere Antwort lässt den bisherigen Bestand unangetastet. `ChampionCatalog` lädt
danach den heruntergeladenen Stand und fällt auf die mitgelieferte Datei zurück, wenn
dieser unlesbar ist — lieber ein älterer Katalog als gar keiner.

Die Feed-Adresse steht in `CatalogUpdater.DEFAULT_FEED_URL` und ist bewusst leer:
sie muss auf einen Bestand zeigen, über den du verfügen darfst.

## Artefakte

`ArtifactSetContent.kt` führt 51 Sets mit zwei oder vier Teilen: Name, Wirkung,
grobe Kategorie. Erreichbar über die Artefakte-Kachel auf dem Start-Reiter.

Die Werte sind gegen zwei unabhängige Quellen geprüft, nicht aus einer heruntergeladenen
Rohdatendatei erzeugt — Vorgehen und eine gefundene Abweichung (Retaliation: 2 oder 4
Teile, je nach Quelle unterschiedlich alt) stehen in
[`docs/artefakt-sets-quellen.md`](docs/artefakt-sets-quellen.md). Bewusst **nicht**
enthalten sind die "Variable"-Sets mit 1 oder bis zu 9 Teilen (Merciless, Slayer, Stone
Skin und ähnliche): für ihre Zwischenstufen fand sich keine belastbare Quelle, und eine
geratene Zahl wäre schlechter als eine fehlende.

Der Aufbauplan einer Legende nennt bereits konkrete Sets statt allgemeiner Begriffe —
`BuildPlanner.gearStep()` löst dafür Set-Kennungen gegen `ArtifactSetContent` auf und
bricht absichtlich hart ab, wenn eine Kennung nicht existiert, statt einen falschen
Namen anzuzeigen. `ArtifactSetContentTest` sichert das zusätzlich ab.

## Noch offen

- **Fähigkeiten im Detail**: Multiplikatoren und Abklingzeiten fehlen im Startdatensatz;
  der Reiter zeigt bisher nur die Wirkungen des Kits. Bis dahin verlinkt die Detailseite
  ins RaidWiki, statt fremde Inhalte selbst auszuliefern.
- **Feed-Adresse eintragen**: `CatalogUpdater.DEFAULT_FEED_URL` ist leer. Sie muss auf
  einen Bestand zeigen, über den du verfügen darfst — etwa eine mit
  `tools/generate_champions.py` erzeugte Datei in deinem eigenen Speicher.
- iOS-Portierung (dann teilen sich beide Plattformen nur die Regeln, nicht den Code)
