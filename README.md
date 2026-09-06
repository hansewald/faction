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

## Noch offen

- **Bildmaterial**: Champion- und Kapitelbilder sind derzeit Farbverläufe nach
  Seltenheit. Die Artworks gehören Plarium und brauchen eine geklärte Quelle.
- **Fähigkeiten im Detail**: Multiplikatoren und Abklingzeiten fehlen im Startdatensatz;
  der Reiter zeigt bisher nur die Wirkungen des Kits.
- Bestätigungsschritt für OCR-Treffer, bevor sie in den Kader wandern
- Sync des Champion-Katalogs statt reiner Seed-Datei
- Artefakt-Sets als eigener Bereich
- iOS-Portierung (dann teilen sich beide Plattformen nur die Regeln, nicht den Code)
