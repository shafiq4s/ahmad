#!/usr/bin/env python3
"""Run unmodified upstream tools from a clean pinned Git checkout; save raw evidence."""
import datetime
import hashlib
import json
import os
import pathlib
import platform
import shlex
import subprocess
import sys

HERE=pathlib.Path(__file__).resolve().parent
PROJECT=HERE.parents[2]
CHECKOUT=pathlib.Path('/workspace/quran-svg-upstream-validation')
COMMIT='b4155f07e4aea087d2c458069acff9f7c7e749f6'
ENV=dict(os.environ, PYTHONDONTWRITEBYTECODE='1', PYTHONPATH='/workspace/.tools/svg-audit-python')
ENV['PATH']='/workspace/.tools/librsvg-audit/usr/bin:'+ENV['PATH']

def git(*args):
    return subprocess.check_output(['git','-C',str(CHECKOUT),*args],text=True).strip()

def run(label,arguments,checkout=CHECKOUT):
    command=[sys.executable,*arguments]
    with (HERE/f'{label}.stdout.txt').open('w') as stdout, (HERE/f'{label}.stderr.txt').open('w') as stderr:
        result=subprocess.run(command,cwd=checkout,env=ENV,stdout=stdout,stderr=stderr)
    evidence=dict(command=command,exactCommand=shlex.join(command),cwd=str(checkout),exitCode=result.returncode,
                  environmentOverrides={k:ENV[k] for k in ['PYTHONDONTWRITEBYTECODE','PYTHONPATH']},
                  pathPrefix='/workspace/.tools/librsvg-audit/usr/bin')
    (HERE/f'{label}.command.json').write_text(json.dumps(evidence,indent=2)+'\n')
    (HERE/f'{label}.exit-code.txt').write_text(str(result.returncode)+'\n')
    print(f'{label}: exit {result.returncode}',flush=True)
    return evidence

def main():
    assert git('rev-parse','HEAD')==COMMIT
    assert git('status','--porcelain')=='', 'Upstream working tree must be clean.'
    manifest=json.loads((HERE.parent/'full-audit/manifest.json').read_text())
    compared=[]
    for row in manifest['files']:
        content=(CHECKOUT/row['path']).read_bytes()
        assert len(content)==row['bytes'] and hashlib.sha256(content).hexdigest()==row['sha256'],row['path']
        compared.append(row['path'])
    versions=json.loads(subprocess.check_output([sys.executable,'-c',
        'import sys,numpy,PIL,brotli,json; print(json.dumps(dict(python=sys.version,numpy=numpy.__version__,Pillow=PIL.__version__,brotli=brotli.__version__)))'],env=ENV,text=True))
    versions.update(os=pathlib.Path('/etc/os-release').read_text(),kernel=platform.release(),pythonExecutable=sys.executable,
                    rsvgConvert=subprocess.check_output(['rsvg-convert','--version'],env=ENV,text=True).strip(),
                    librsvgPackage=subprocess.check_output(['dpkg-query','-W','-f=${Version}','librsvg2-2'],text=True).strip(),
                    otherAuditDependencies='Python standard library; vendored qiraat data. No fontTools needed for audit/build_ayah_polygons.',
                    commit=COMMIT,checkout=str(CHECKOUT),sparsePaths=git('sparse-checkout','list').splitlines(),
                    gitStatusBefore=git('status','--porcelain'),matchedExistingManifestFiles=len(compared),
                    checkedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
    (HERE/'environment.json').write_text(json.dumps(versions,indent=2)+'\n')
    docs=[]
    for path in ['README.md','tools/README.md','docs/FORMAT.md','docs/GETTING-THE-FILES.md','docs/PROVENANCE.md']:
        data=(CHECKOUT/path).read_bytes()
        docs.append(dict(path=path,sha256=hashlib.sha256(data).hexdigest(),gitBlob=git('rev-parse',f'HEAD:{path}')))
    (HERE/'read-instructions.json').write_text(json.dumps(docs,indent=2)+'\n')
    base=['tools/audit_ayah_polygons.py','--mushaf','hafs','--tier','all','--workers','4','--max-detail','100000']
    run('upstream-subset',base+['--pages','3,48,121,604','--json',str(HERE/'upstream-subset.json')])
    run('upstream-full',base+['--pages','3-604','--json',str(HERE/'upstream-full.json')])
    versions['gitStatusAfter']=git('status','--porcelain')
    assert versions['gitStatusAfter']==''
    (HERE/'environment.json').write_text(json.dumps(versions,indent=2)+'\n')

if __name__=='__main__': main()
