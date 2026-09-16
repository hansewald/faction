# Quellen: Artefakt-Set-Werte

Stand: 16.09.2026. Betrifft `ArtifactSetContent.kt` (51 Sets, 2- und 4-teilig).

## Warum überhaupt eine eigene Quellenliste

Set-Werte sind Fakten über die Spielmechanik, keine Kreativtexte — insofern derselben
Kategorie zuzurechnen wie die Basiswerte im Champion-Katalog. Anders als dort gibt es
hier aber keine einzelne heruntergeladene Rohdatendatei, sondern zwei unabhängig
abgefragte Web-Quellen, die abgeglichen wurden. Diese Datei hält fest, was geprüft
wurde und wo eine Abweichung auftrat — damit eine spätere Korrektur nachvollziehbar
bleibt, statt sich auf Erinnerung zu verlassen.

## Vorgehen

1. [AyumiLove – Artifact and Accessory Guide](https://ayumilove.net/raid-shadow-legends-artifact-and-accessory-guide/)
   abgefragt: liefert prosaisch gruppierte Listen unter „2-Piece“, „4-Piece“,
   „Variable (1–9 Pieces)“.
2. [RSLTools – Artefakt-Katalog](https://rsltools.fr/en/catalogue/artifacts) abgefragt:
   liefert dieselben Sets als strukturierte Tabelle mit Teilezahl und Bonus.
3. Beide Listen zeilenweise verglichen.

## Gefundene Abweichung

Bei **Retaliation** widersprachen sich die Quellen: AyumiLove nannte 4 Teile,
RSLTools 2 Teile — beide bei identischem Bonus (15 % Gegenangriff-Chance). Eine
gezielte Suche ergab die Erklärung: RSLTools ist aktuell, AyumiLove veraltet — laut
[HellHades](https://hellhades.com/artifact-set-rebalance-raid-shadow-legends/)
gab es ein Rebalancing, bei dem Retaliation von 4 auf 2 Teile gesenkt wurde. Der
HellHades-Artikel selbst ließ sich nicht öffnen (nur Navigationsgerüst, kein Text),
diente also nur als Beleg, *dass* es ein Rebalancing gab, nicht als Quelle für die
Zahl selbst.

**Konsequenz:** Bei Konflikten wurde RSLTools als strukturierte, vermutlich aktuellere
Quelle bevorzugt. `ArtifactSetContent.kt` führt Retaliation entsprechend als 2-Teile-Set.

## Bewusst ausgeschlossen

Die "Variable"-Sets mit 1 oder bis zu 9 Teilen (Bloodshield, Cleansing, Reaction,
Refresh, Revenge, Chronophage, Deflection, Feral, Merciless, Mercurial, Pinpoint,
Protection, Rebirth, Slayer, Stone Skin, Stonecleaver, Supersonic, Swift Parry) sind
**nicht** im Katalog. Beide Quellen nennen für sie nur den Bonus bei voller
Teilezahl, nicht die Zwischenstufen — und diese Zwischenstufen sind der eigentliche
Sinn eines Variable-Sets. Ohne sie wäre der Eintrag unvollständig auf eine Art, die
in der App nicht sichtbar würde.

## Wenn sich das prüfen lässt

Am belastbarsten wäre ein Abgleich gegen offizielle Patch-Notes von Plarium oder
gegen eine dritte, ebenfalls strukturierte Quelle. Beides stand hier nicht zur
Verfügung. Wer das nachholt: `ArtifactSetContent.kt` ist eine flache Liste, jede
Zeile eine Zeile Änderung.
