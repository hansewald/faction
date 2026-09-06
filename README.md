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
| `data.model` | Champion, Kader, Wirkungen (`Utility`), Bereiche (`Area`) |
| `data.source` | Importquellen hinter dem Interface `AccountSource` |
| `data.local` | Room-Datenbank für den erfassten Kader |
| `data.repo` | Champion-Katalog und Kader-Repository |
| `domain` | `ScoreEngine` (Kit-Bewertung), `BuildPlanner` (Aufbauplan), `BuildAdvisor` (Empfehlungen) |
| `ui.components` | Wiederverwendete Bausteine: Goldrahmen-Karte, Tags, Schrittmarken |
| `ui.screens` | Start, Legenden, Helden-Details, Quests, Anleitung, News |
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

Bewusst **nicht** enthalten ist ein Login mit Plarium-Zugangsdaten. Es gibt keinen
offiziellen Endpunkt dafür, und die
[Nutzungsbedingungen](https://company.plarium.com/en/terms/terms-of-use/) untersagen
sowohl die Weitergabe von Zugangsdaten als auch Drittanbieter-Clients — mit
Account-Sperre als Konsequenz. Sollte Plarium eine offizielle Anmeldung anbieten, ist
sie eine weitere Implementierung von `AccountSource`; der Rest der App bleibt unverändert.

## Datenbasis

`app/src/main/assets/champions_seed.json` enthält 30 Champions als Startdatensatz. Die
Felder `utilities` und `kitSummary` sind eigene Einordnungen der Kits, keine übernommenen
Bewertungen. Für den vollständigen Champion-Bestand bieten sich als Sync-Quelle an:

- [Goctionni/raid-data](https://goctionni.github.io/raid-data/)
- [PatPat1567/RaidShadowLegendsData](https://github.com/PatPat1567/RaidShadowLegendsData)

Vor der Übernahme jeweils die Lizenz der Quelle prüfen.

## Noch offen

- **Bildmaterial**: Champion- und Kapitelbilder sind derzeit Farbverläufe nach
  Seltenheit. Die Artworks gehören Plarium und brauchen eine geklärte Quelle.
- **Fähigkeiten im Detail**: Multiplikatoren und Abklingzeiten fehlen im Startdatensatz;
  der Reiter zeigt bisher nur die Wirkungen des Kits.
- Bestätigungsschritt für OCR-Treffer, bevor sie in den Kader wandern
- Sync des Champion-Katalogs statt reiner Seed-Datei
- Artefakt-Sets als eigener Bereich
- iOS-Portierung (dann teilen sich beide Plattformen nur die Regeln, nicht den Code)
