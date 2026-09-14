#!/usr/bin/env python3
"""Packt die Champion-Portraits als WebP in die App.

Quelle: images/avatar aus https://github.com/PatPat1567/RaidShadowLegendsData.
Die Rechte an den Bildern liegen bei Plarium; ihre Nutzung in FACTION ist durch die
Genehmigung in docs/plarium-genehmigung.md gedeckt - unter der Auflage, dass die App
kostenlos bleibt und keinerlei Einnahmen erzielt.

Die Dateien heissen nach der Champion-Kennung aus champions.json, damit die App sie
ohne Zuordnungstabelle findet.

Aufruf:
    python tools/generate_portraits.py <pfad-zum-datenrepo>
"""

from __future__ import annotations

import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from generate_champions import slugify  # noqa: E402  gleiche Kennungen wie im Katalog

CATALOG = "app/src/main/assets/champions.json"
TARGET = "app/src/main/assets/portraits"
QUALITY = 80


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    source = os.path.join(sys.argv[1], "images", "avatar")

    with open(CATALOG, encoding="utf-8") as fh:
        ids = {c["id"] for c in json.load(fh)}

    os.makedirs(TARGET, exist_ok=True)
    for stale in os.listdir(TARGET):
        os.remove(os.path.join(TARGET, stale))

    written, total, skipped, broken = 0, 0, [], []
    for name in sorted(os.listdir(source)):
        champion_id = slugify(os.path.splitext(name)[0])
        if champion_id not in ids:
            skipped.append(name)
            continue
        target = os.path.join(TARGET, champion_id + ".webp")
        try:
            with Image.open(os.path.join(source, name)) as image:
                # Die Quellen sind Paletten-PNGs; WebP verlangt echte Farbkanaele.
                image.convert("RGB").save(target, "WEBP", quality=QUALITY, method=6)
        except OSError as exc:
            # Einzelne Quelldateien sind abgeschnitten. Lieber das Wappen als ein
            # halbes Bild - die App faellt fuer fehlende Portraits darauf zurueck.
            if os.path.exists(target):
                os.remove(target)
            broken.append(f"{name} ({exc})")
            continue
        written += 1
        total += os.path.getsize(target)

    missing = sorted(ids - {os.path.splitext(f)[0] for f in os.listdir(TARGET)})
    print(f"{written} Portraits geschrieben, {total / 1024 / 1024:.1f} MB")
    if missing:
        print(f"  ohne Portrait: {len(missing)}: {missing[:10]}")
    if broken:
        print(f"  defekte Quelldateien: {len(broken)}")
        for entry in broken:
            print("    " + entry)
    if skipped:
        print(f"  nicht im Katalog: {len(skipped)}: {skipped[:10]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
