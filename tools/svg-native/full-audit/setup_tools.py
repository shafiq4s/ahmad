#!/usr/bin/env python3
"""Restore checksum-pinned audit-only tools without changing system packages."""
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import urllib.request

HERE=pathlib.Path(__file__).resolve().parent
LOCK=json.loads((HERE/'tool-lock.json').read_text())
TOOLS=pathlib.Path('/workspace/.tools')
WHEELS=TOOLS/'svg-audit-wheels'
TARGET=TOOLS/'svg-audit-python'

def verified_download(row,destination):
    destination.parent.mkdir(parents=True,exist_ok=True)
    if not destination.exists():
        with urllib.request.urlopen(row['url'],timeout=90) as response:
            data=response.read()
        assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256']
        destination.write_bytes(data)
    data=destination.read_bytes()
    assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],destination
    return destination

for wheel in LOCK['wheels']:
    verified_download(wheel,WHEELS/wheel['filename'])
requirement=TOOLS/'svg-audit-requirements.txt'
requirement.write_text(''.join(f"{r['name']}=={r['version']} --hash=sha256:{r['sha256']}\n" for r in LOCK['wheels']))
environment=dict(os.environ,PYTHONPATH=str(TARGET))
check='import numpy,shapely,brotli; assert [numpy.__version__,shapely.__version__,brotli.__version__]==["2.5.4","2.1.2","1.1.0"]'
if subprocess.run([sys.executable,'-c',check],env=environment,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
    subprocess.run([sys.executable,'-m','pip','install','--no-index','--find-links',str(WHEELS),'--no-deps',
                    '--require-hashes','--upgrade','--target',str(TARGET),'-r',str(requirement)],check=True)
subprocess.run([sys.executable,'-c',check],env=environment,check=True)
package=verified_download(LOCK['rsvg'],TOOLS/'librsvg2-bin.deb')
binary=TOOLS/'librsvg-audit/usr/bin/rsvg-convert'
if not binary.exists(): subprocess.run(['dpkg-deb','-x',str(package),str(TOOLS/'librsvg-audit')],check=True)
subprocess.run([str(binary),'--version'],check=True)
print('Audit tools ready; use PYTHONPATH=/workspace/.tools/svg-audit-python and prepend /workspace/.tools/librsvg-audit/usr/bin to PATH.')
