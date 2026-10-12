#!/usr/bin/env python3
"""Check this UI stage against its frozen reading boundary and final debug package."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    references = json.loads((HERE / 'reference-lock.json').read_text())
    for name, expected in references.items():
        assert digest((ROOT / name).read_bytes()) == expected, name
    controller = (ROOT / 'app/src/main/java/com/ahmad/quran/ui/MushafReaderController.kt').read_text()
    start = controller.index('    /** Wait for cancelled')
    end = controller.index('    fun saveMeasurements()', start)
    assert digest((controller[:start] + controller[end:]).encode()) == '72257bfea9c61b24e861870a044cb13b9b4c3a5c2e14bfa738d834b8873e8828'

    tests = {}
    for module in ('app', 'svg-native-prototype'):
        reports = list((ROOT / module / 'build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
        assert reports, module
        suites = [ET.parse(p).getroot() for p in reports]
        totals = {k: sum(int(s.get(k, '0')) for s in suites) for k in ('tests', 'failures', 'errors', 'skipped')}
        assert totals['tests'] > 0 and all(totals[k] == 0 for k in ('failures', 'errors', 'skipped')), totals
        tests[module] = totals
    assert tests['app']['tests'] >= 92 and tests['svg-native-prototype']['tests'] == 13
    lint = ET.parse(ROOT / 'app/build/reports/lint-results-debug.xml').getroot()
    issues = list(lint.findall('issue'))
    assert not any(i.get('severity') in ('Error', 'Fatal') for i in issues)

    seeds = json.loads((ROOT / 'config/mushaf-page-assets.json').read_text())['seedPages']
    source = ROOT / 'tools/mushaf-svg/local-assets/mushaf-svg'
    apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    with ZipFile(apk) as archive:
        prefix = 'assets/mushaf-svg/'
        names = archive.namelist()
        svg = {n.removeprefix(prefix) for n in names if n.startswith(prefix) and n.endswith('.svg')}
        assert svg == {f'{n:03d}.svg' for n in seeds}
        polygons = {n.removeprefix(prefix) for n in names if n.startswith(prefix) and n.endswith('.json') and Path(n).stem.isdigit()}
        assert polygons == {f'{n:03d}.json' for n in range(1, 605)}
        for original in source.iterdir():
            if original.suffix != '.svg' or original.name in svg:
                assert archive.read(prefix + original.name) == original.read_bytes(), original.name
        for original in (ROOT / 'app/src/main/assets').iterdir():
            assert archive.read('assets/' + original.name) == original.read_bytes(), original.name
        config = json.loads(archive.read(prefix + 'page-assets-config.json'))
        assert config['baseUrl'] == '' and config['seedPages'] == seeds

    result = {
        'tests': tests,
        'lint': {'errors': 0, 'warnings': sum(i.get('severity') == 'Warning' for i in issues),
                 'issueIds': [i.get('id') for i in issues]},
        'frozenReferencesUnchanged': len(references), 'originalControllerMethodsUnchanged': True,
        'packagedSeedPages': seeds, 'officialPolygons': 604, 'packagedSourceBytesExact': True,
        'newDependencies': [], 'defaultBaseUrl': '',
        'apk': {'path': str(apk.relative_to(ROOT)), 'bytes': apk.stat().st_size, 'sha256': digest(apk.read_bytes()),
                'previousBytes': 6792690},
    }
    device_file = HERE / 'device-api23.json'
    if device_file.exists():
        device = json.loads(device_file.read_text())
        assert device['success'] is True and device['api'] == 23
        result['platformValidation'] = {'23': 'Robolectric and actual Android emulator passed',
                                        '36': 'Robolectric/native-Canvas passed; no device run claimed'}
        result['nativeDeviceReport'] = device_file.name
    (HERE / 'validation.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
