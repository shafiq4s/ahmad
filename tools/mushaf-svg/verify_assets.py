#!/usr/bin/env python3
"""Fail the build if pinned artwork, metadata, overrides or Tanzil bytes change."""
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parents[1]
ASSETS = HERE / 'local-assets/mushaf-svg'


def main():
    manifest = json.loads((HERE / 'asset-manifest.json').read_text())
    assert manifest['commit'] == 'b4155f07e4aea087d2c458069acff9f7c7e749f6'
    assert {p.name for p in ASSETS.iterdir()} == {r['filename'] for r in manifest['files']} | {'source-manifest.json'}, 'Unexpected assets/crops in production folder'
    for suffix in ['svg', 'json']:
        expected = {f'{n:03d}.{suffix}' for n in range(1, 605)}
        actual = {p.name for p in ASSETS.glob(f'*.{suffix}') if p.stem.isdigit()}
        assert actual == expected, f'Missing/extra {suffix} pages; run prepare_assets.py'
    for row in manifest['files']:
        data = (ASSETS / row['filename']).read_bytes()
        assert len(data) == row['bytes'] and hashlib.sha256(data).hexdigest() == row['sha256'], row['filename']
    assert (ASSETS / 'source-manifest.json').read_bytes() == (HERE / 'asset-manifest.json').read_bytes()
    for name, expected in json.loads((PROJECT / 'tools/quran/reference-lock.json').read_text())['sha256'].items():
        assert hashlib.sha256((PROJECT / name).read_bytes()).hexdigest() == expected, name
    print('Verified 604 SVG + 604 JSON, pinned metadata/notices and all 11 Tanzil references.')


if __name__ == '__main__':
    main()
