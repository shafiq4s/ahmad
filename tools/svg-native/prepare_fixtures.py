#!/usr/bin/env python3
"""Copy exactly five pinned originals for native SVG. No raster conversion or source edits."""
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
PREVIOUS = ROOT.parent/'quran-svg'
LOCK = json.loads((PREVIOUS/'source-lock.json').read_text())
ASSETS = ROOT/'local-fixtures/assets/native-svg'
ASSETS.mkdir(parents=True, exist_ok=True)
OPENING_BOXES = {1: [8.09, -60.5067, 221.68, 275.457], 2: [11.46, -60.7597, 217.06, 276.96]}

for item in LOCK['files']:
    if not item['path'].startswith('mushafs/'):
        continue
    source = PREVIOUS/'local-fixtures/sources'/item['path']
    data = source.read_bytes() if source.exists() else urllib.request.urlopen(item['url'], timeout=60).read()
    assert len(data) == item['bytes'] and hashlib.sha256(data).hexdigest() == item['sha256']
    (ASSETS/Path(item['path']).name).write_bytes(data)
pages = []
for number in (1, 2, 48, 121, 604):
    box = list(map(float, ET.parse(ASSETS/f'{number:03d}.svg').getroot().attrib['viewBox'].split()))
    pages.append(dict(pageNumber=number, viewBox=box, renderBox=OPENING_BOXES.get(number, box)))
(ASSETS/'pages.json').write_text(json.dumps(dict(editionId=LOCK['editionId'], commit=LOCK['commit'], pages=pages), indent=2)+'\n')
shutil.copyfile(ROOT/'opening-hit-overrides.json', ASSETS/'opening-hit-overrides.json')
shutil.copyfile(ROOT/'ANDROIDSVG-APACHE-2.0.txt', ASSETS/'ANDROIDSVG-APACHE-2.0.txt')
for name in ('LICENSE', 'NOTICE.md', 'docs/PROVENANCE.md', 'LICENSES/CC-BY-4.0.txt'):
    shutil.copyfile(PREVIOUS/'upstream-notices'/name, ASSETS/Path(name).name)
print('Prepared five original SVG/JSON pairs; no raster assets.')
