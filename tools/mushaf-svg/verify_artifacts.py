#!/usr/bin/env python3
"""Verify exact original assets inside real APK/AAB outputs and measure their sizes."""
import hashlib
import json
from pathlib import Path
import zipfile

HERE=Path(__file__).resolve().parent
PROJECT=HERE.parents[1]
manifest=json.loads((HERE/'asset-manifest.json').read_text())
baselines=json.loads((HERE/'baseline-sizes.json').read_text())
results=[]
for baseline in baselines:
    file=PROJECT/baseline['path']
    with zipfile.ZipFile(file) as archive:
        prefix='base/assets/mushaf-svg/' if file.suffix=='.aab' else 'assets/mushaf-svg/'
        for suffix in ['svg','json']:
            actual={Path(n).name for n in archive.namelist() if n.startswith(prefix) and Path(n).stem.isdigit() and n.endswith('.'+suffix)}
            assert actual=={f'{i:03d}.{suffix}' for i in range(1,605)}
        expected={prefix+r['filename'] for r in manifest['files']}|{prefix+'source-manifest.json'}
        assert {n for n in archive.namelist() if n.startswith(prefix)}==expected
        payload=0;raw=0
        for row in manifest['files']:
            data=archive.read(prefix+row['filename'])
            assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],row['filename']
            payload+=archive.getinfo(prefix+row['filename']).compress_size;raw+=len(data)
        assert archive.read(prefix+'source-manifest.json')==(HERE/'asset-manifest.json').read_bytes()
        assert not any('image-mushaf/' in n or n.endswith(('.png','.webp','.svg')) and 'assets/' in n and not n.startswith(prefix) for n in archive.namelist())
    results.append(dict(path=baseline['path'],bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest(),
        beforeBytes=baseline['bytes'],increaseBytes=file.stat().st_size-baseline['bytes'],
        mushafCompressedPayloadBytes=payload,mushafRawPayloadBytes=raw,verifiedManifestRecords=len(manifest['files'])))
(HERE/'artifact-sizes.json').write_text(json.dumps(results,indent=2)+'\n')
print(json.dumps(results,indent=2))
