#!/usr/bin/env python3
"""Verify the final APK/AAB subset and unchanged original data, then record sizes."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SEEDS = json.loads((ROOT / 'config/mushaf-page-assets.json').read_text())['seedPages']
SOURCE = ROOT / 'tools/mushaf-svg/local-assets/mushaf-svg'
BASELINE = json.loads((ROOT / 'tools/page-assets/baseline.json').read_text())


def main():
    results = {}
    for name in ['app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/release/app-release-unsigned.apk', 'app/build/outputs/bundle/release/app-release.aab']:
        artifact = ROOT / name
        with ZipFile(artifact) as archive:
            prefix = 'base/assets/' if name.endswith('.aab') else 'assets/'
            artwork = prefix + 'mushaf-svg/'
            entries = archive.namelist()
            actual_svg = {n.removeprefix(artwork) for n in entries if n.startswith(artwork) and n.endswith('.svg')}
            assert actual_svg == {f'{n:03d}.svg' for n in SEEDS}, (name, actual_svg)
            assert {n.removeprefix(artwork) for n in entries if n.startswith(artwork) and n.endswith('.json') and Path(n).stem.isdigit()} == {f'{n:03d}.json' for n in range(1, 605)}
            for source in SOURCE.iterdir():
                if source.suffix != '.svg' or source.name in actual_svg:
                    assert archive.read(artwork + source.name) == source.read_bytes(), (name, source.name)
            for source in (ROOT / 'app/src/main/assets').iterdir():
                assert archive.read(prefix + source.name) == source.read_bytes(), (name, source.name)
            config = json.loads(archive.read(artwork + 'page-assets-config.json'))
            assert config['seedPages'] == SEEDS and config['baseUrl'] == ''
            assert config['allowLocalHttp'] == name.endswith('app-debug.apk')
            results[name] = {'bytes': artifact.stat().st_size, 'sha256': hashlib.sha256(artifact.read_bytes()).hexdigest(),
                             'previousBytes': BASELINE[name], 'reductionBytes': BASELINE[name] - artifact.stat().st_size,
                             'reductionPercent': (1 - artifact.stat().st_size / BASELINE[name]) * 100,
                             'svgCount': len(actual_svg), 'officialPolygonCount': 604,
                             'seedCompressedBytes': sum(archive.getinfo(artwork + n).compress_size for n in actual_svg)}
    for name, sha in json.loads((ROOT / 'tools/page-assets/reference-lock.json').read_text()).items():
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == sha, name
    result = {'seedPages': SEEDS, 'seedRawBytes': sum((SOURCE / f'{n:03d}.svg').stat().st_size for n in SEEDS), 'artifacts': results,
              'frozenReadingReferencesUnchanged': True, 'sourceSvgCount': len(list(SOURCE.glob('*.svg')))}
    (ROOT / 'tools/page-assets/package-validation.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
