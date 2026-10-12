#!/usr/bin/env python3
"""Install checksum-pinned official Google emulator packages in the cloud workspace."""
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
SDK = Path('/workspace/.android-sdk')
DESTINATIONS = {
    'emulator': SDK/'emulator',
    'system-images;android-23;default;x86': SDK/'system-images/android-23/default/x86',
    'system-images;android-36;default;x86_64': SDK/'system-images/android-36/default/x86_64',
}
for item in json.loads((ROOT/'android-runtime-packages.json').read_text()):
    destination = DESTINATIONS[item['package']]
    if (destination/'source.properties').exists():
        print(f"Retained installed package: {item['package']}")
        continue
    with tempfile.TemporaryDirectory(dir='/tmp') as staging:
        archive = Path(staging)/'package.zip'
        with urllib.request.urlopen(item['url'], timeout=120) as response, archive.open('wb') as output:
            shutil.copyfileobj(response, output)
        with archive.open('rb') as source:
            digest = hashlib.file_digest(source, 'sha1').hexdigest()
        if archive.stat().st_size != item['bytes'] or digest != item['sha1']:
            raise SystemExit('Official package checksum/size mismatch')
        unpacked = Path(staging)/'unpacked'
        with zipfile.ZipFile(archive) as package:
            for entry in package.infolist():
                target = unpacked/entry.filename
                if not target.resolve().is_relative_to(unpacked.resolve()):
                    raise SystemExit('Unsafe archive path')
                package.extract(entry, unpacked)
                if not entry.is_dir():
                    mode = (entry.external_attr >> 16) & 0o777
                    if mode:
                        target.chmod(mode)
        child, = unpacked.iterdir()
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(child), destination)
        if item['package'] == 'emulator':
            for executable in [destination/'emulator', *destination.glob('qemu/linux-x86_64/qemu-system-*')]:
                executable.chmod(0o755)
    print(f"Installed verified package: {item['package']}")
