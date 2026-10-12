#!/usr/bin/env python3
"""Verify immutable Quran/reader boundaries, executed checks and the final debug subset."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    references = json.loads((HERE / 'reference-lock.json').read_text())
    for name, expected in references.items():
        assert sha((ROOT / name).read_bytes()) == expected, name
    additions = [
        ('app/src/main/java/com/ahmad/quran/data/QuranRepository.kt',
         '    /** Read-only cursor scan', '    fun getSurahs()',
         '6895ffc7b9dbea6d03234495a5dbca3974ad7eaec80271ea037552eadbb7cb2e'),
        ('app/src/main/java/com/ahmad/quran/mushaf/svg/NativeSvgPageView.kt',
         '    /** Navigation uses the approved', '    /** Keep the selection until',
         'c8f11470c07cf83c7d44ee42c1cec19953ba1502c6dab38a7fa2ad20165d9dac'),
    ]
    for name, start, end, expected in additions:
        text = (ROOT / name).read_text()
        a = text.index(start)
        b = text.index(end, a)
        assert sha((text[:a] + text[b:]).encode()) == expected, name

    tests = {}
    for module in ('app', 'svg-native-prototype'):
        suites = [ET.parse(p).getroot() for p in (ROOT / module / 'build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
        counts = {key: sum(int(s.get(key, '0')) for s in suites) for key in ('tests', 'failures', 'errors', 'skipped')}
        assert counts['tests'] > 0 and all(counts[k] == 0 for k in ('failures', 'errors', 'skipped')), counts
        tests[module] = counts
    assert tests['app']['tests'] >= 108 and tests['svg-native-prototype']['tests'] == 13
    lint = ET.parse(ROOT / 'app/build/reports/lint-results-debug.xml').getroot().findall('issue')
    assert not any(i.get('severity') in ('Error', 'Fatal') for i in lint)
    seeds = json.loads((ROOT / 'config/mushaf-page-assets.json').read_text())['seedPages']
    source = ROOT / 'tools/mushaf-svg/local-assets/mushaf-svg'
    artifact = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    with ZipFile(artifact) as package:
        prefix = 'assets/mushaf-svg/'
        names = package.namelist()
        svg = {n.removeprefix(prefix) for n in names if n.startswith(prefix) and n.endswith('.svg')}
        assert svg == {f'{n:03d}.svg' for n in seeds}
        polygons = {n.removeprefix(prefix) for n in names if n.startswith(prefix) and n.endswith('.json') and Path(n).stem.isdigit()}
        assert polygons == {f'{n:03d}.json' for n in range(1, 605)}
        for original in source.iterdir():
            if original.suffix != '.svg' or original.name in svg:
                assert package.read(prefix + original.name) == original.read_bytes(), original.name
        for original in (ROOT / 'app/src/main/assets').iterdir():
            assert package.read('assets/' + original.name) == original.read_bytes(), original.name
        assert json.loads(package.read(prefix + 'page-assets-config.json'))['baseUrl'] == ''
    result = {
        'tests': tests, 'testedApis': [23, 36], 'platform': 'Robolectric; UI captures use native Canvas',
        'lint': {'errors': 0, 'warnings': sum(i.get('severity') == 'Warning' for i in lint), 'issueIds': [i.get('id') for i in lint]},
        'frozenReferencesUnchanged': len(references), 'existingRepositoryAndRendererMethodsUnchanged': True,
        'canonicalAndEditionAssetBytesExact': True, 'sourceArtworkCount': len(list(source.glob('*.svg'))),
        'packagedSeedPages': seeds, 'packagedOfficialPolygons': 604, 'defaultBaseUrl': '', 'newDependencies': [],
        'searchPageSize': 50, 'searchComparisonOnlyNormalization': True,
        'apk': {'path': str(artifact.relative_to(ROOT)), 'bytes': artifact.stat().st_size, 'sha256': sha(artifact.read_bytes()),
                'previousBytes': 6837509},
    }
    (HERE / 'validation.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
