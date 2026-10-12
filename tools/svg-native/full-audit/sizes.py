#!/usr/bin/env python3
"""Measure complete corpus bytes and actual deterministic DEFLATE ZIP archives."""
import hashlib
import json
import pathlib
import zipfile

HERE=pathlib.Path(__file__).resolve().parent
ROOT=HERE.parent/'local-fixtures/full-audit-source'
manifest=json.loads((HERE/'manifest.json').read_text())
files=[r for r in manifest['files'] if r['scope']=='full-page']
result={'fullPages':604,'svgCount':604,'jsonCount':604,'archives':[]}
for label,selected in [('svg', [r for r in files if r['path'].endswith('.svg')]),('svg-json',files)]:
    dest=ROOT.parent/f'hafs-604-{label}-deflate9.zip'
    with zipfile.ZipFile(dest,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        for row in selected:
            entry=zipfile.ZipInfo(row['path'],date_time=(1980,1,1,0,0,0))
            entry.compress_type=zipfile.ZIP_DEFLATED
            archive.writestr(entry,(ROOT/row['path']).read_bytes(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    with zipfile.ZipFile(dest) as archive:
        compressed=sum(e.compress_size for e in archive.infolist())
        assert archive.testzip() is None
    result['archives'].append(dict(scope=label,path=str(dest),rawBytes=sum(r['bytes'] for r in selected),
           compressedPayloadBytes=compressed,zipBytes=dest.stat().st_size,zipOverheadBytes=dest.stat().st_size-compressed,
           sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),method='ZIP DEFLATE level 9; original files unchanged'))
result['svgRawBytes']=sum(r['bytes'] for r in files if r['path'].endswith('.svg'))
result['jsonRawBytes']=sum(r['bytes'] for r in files if r['path'].endswith('.json'))
result['officialBrotliBytes']=sum(r['bytes'] for r in manifest['files'] if '/svg-br/' in r['path'])
result['scope']='Only full pages 001..604. No surah crops. ZIP is a reproducible size measurement, not APK/AAB install size.'
(HERE/'sizes.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
