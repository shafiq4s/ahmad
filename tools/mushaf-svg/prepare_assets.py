#!/usr/bin/env python3
"""Prepare byte-identical pinned full pages for offline APK/AAB packaging."""
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parents[1]
ASSETS = HERE / 'local-assets/mushaf-svg'


def main():
    manifest = json.loads((HERE / 'asset-manifest.json').read_text())
    existing = json.loads((PROJECT / 'tools/svg-native/full-audit/manifest.json').read_text())
    cache = Path(existing['root'])
    ASSETS.mkdir(parents=True, exist_ok=True)
    for row in manifest['files']:
        destination = ASSETS / row['filename']
        if destination.exists() and hashlib.sha256(destination.read_bytes()).hexdigest() == row['sha256']:
            continue
        if row.get('sourcePath'):
            source = cache / row['sourcePath']
            data = source.read_bytes() if source.exists() else urllib.request.urlopen(row['url'], timeout=60).read()
        else:
            data = (PROJECT / row['projectPath']).read_bytes()
        assert len(data) == row['bytes'] and hashlib.sha256(data).hexdigest() == row['sha256'], row['filename']
        destination.write_bytes(data)
    shutil.copyfile(HERE / 'asset-manifest.json', ASSETS / 'source-manifest.json')
    print('Prepared 604 original SVG/JSON pairs, edition catalog, overrides and notices.')


if __name__ == '__main__':
    main()
