#!/usr/bin/env python3
"""Check the current interaction stage against frozen Quran/rendering and packaged bytes."""
from pathlib import Path
from zipfile import ZipFile
import hashlib,json,re,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent

def digest(data):return hashlib.sha256(data).hexdigest()
def method(source,name):
    match=re.search(r'fun '+name+r'\(',source);assert match,name
    start=source.index('{',match.start());depth=0
    for end in range(start,len(source)):
        if source[end]=='{':depth+=1
        elif source[end]=='}':
            depth-=1
            if depth==0:return source[start:end+1]
    raise AssertionError(name)

def main():
    refs=json.loads((HERE/'reference-lock.json').read_text())
    for name,expected in refs['files'].items():assert digest((ROOT/name).read_bytes())==expected,name
    source=(ROOT/'app/src/main/java/com/ahmad/quran/mushaf/svg/NativeSvgPageView.kt').read_text()
    for name,expected in refs['rendererMethods'].items():assert digest(method(source,name).encode())==expected,name
    assert 'ScaleGestureDetector' not in source and 'GestureDetector' not in source
    assert 'onReaderTap?.invoke()' in source
    tests={}
    for module in ('app','svg-native-prototype'):
        rows=[ET.parse(p).getroot() for p in (ROOT/module/'build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
        count={k:sum(int(r.get(k,'0')) for r in rows) for k in ('tests','failures','errors','skipped')}
        assert count['tests']>0 and all(count[k]==0 for k in ('failures','errors','skipped')),count
        tests[module]=count
    assert tests['app']['tests']>=150 and tests['svg-native-prototype']['tests']==13
    lint={}
    for variant in ('debug','release'):
        rows=ET.parse(ROOT/f'app/build/reports/lint-results-{variant}.xml').getroot().findall('issue')
        assert not any(r.get('severity') in ('Error','Fatal') for r in rows)
        lint[variant]={'errors':0,'warnings':sum(r.get('severity')=='Warning' for r in rows),'ids':[r.get('id') for r in rows]}
    paths={'debug':'app/build/outputs/apk/debug/app-debug.apk','release':'app/build/outputs/apk/release/app-release-unsigned.apk','aab':'app/build/outputs/bundle/release/app-release.aab'}
    original=ROOT/'tools/mushaf-svg/local-assets/mushaf-svg';seeds=json.loads((ROOT/'config/mushaf-page-assets.json').read_text())['seedPages']
    artifacts={}
    for label,name in paths.items():
        p=ROOT/name
        with ZipFile(p) as z:
            prefix=('base/' if label=='aab' else '')+'assets/mushaf-svg/'
            svg={n.removeprefix(prefix) for n in z.namelist() if n.startswith(prefix) and n.endswith('.svg')}
            assert svg=={f'{n:03d}.svg' for n in seeds},(label,svg)
            polygons={n.removeprefix(prefix) for n in z.namelist() if n.startswith(prefix) and n.endswith('.json') and Path(n).stem.isdigit()}
            assert len(polygons)==604
            for source in original.iterdir():
                if source.suffix!='.svg' or source.name in svg:assert z.read(prefix+source.name)==source.read_bytes(),(label,source.name)
            ap=('base/' if label=='aab' else '')+'assets/'
            for source in (ROOT/'app/src/main/assets').iterdir():assert z.read(ap+source.name)==source.read_bytes(),source.name
            assert not any('screenshots/' in n or n.endswith(('.png','.apk','.aab','.keystore')) for n in z.namelist() if '/assets/' in n or n.startswith('assets/'))
            assert json.loads(z.read(prefix+'page-assets-config.json'))['baseUrl']==''
        artifacts[label]={'bytes':p.stat().st_size,'sha256':digest(p.read_bytes()),'path':name,'beforeBytes':refs['beforeSizes'][label],'differenceBytes':p.stat().st_size-refs['beforeSizes'][label]}
    result={'tests':tests,'lint':lint,'frozenFiles':len(refs['files']),'rendererMethodsUnchanged':list(refs['rendererMethods']),'canonicalAndArtworkBytesExactInAllArtifacts':True,'packagedSeedPages':seeds,'polygons':604,'newDependencies':[],'userZoomPanAndMultitouchDisabled':True,'uiLanguages':['ar','en','fa-AF','ps-AF','uz-UZ','tg-TJ','haz-AF'],'runtime':'Robolectric API23/36; native Canvas on Linux desktop, not new hardware measurements','artifacts':artifacts}
    (HERE/'validation.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
if __name__=='__main__':main()
