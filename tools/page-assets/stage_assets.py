#!/usr/bin/env python3
"""Stage a packaging subset; leave all pinned source files untouched."""
import argparse
import json
import shutil
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--destination', type=Path, required=True)
    parser.add_argument('--base-url', default='')
    parser.add_argument('--allow-local-http', action='store_true')
    args = parser.parse_args()
    seeds = json.loads((ROOT / 'config/mushaf-page-assets.json').read_text())['seedPages']
    assert seeds and len(set(seeds)) == len(seeds) and all(type(n) is int and 1 <= n <= 604 for n in seeds)
    base = args.base_url.rstrip('/')
    if base:
        url = urlparse(base)
        assert not url.username and not url.password and not url.query and not url.fragment
        assert url.scheme == 'https' or (args.allow_local_http and url.scheme == 'http' and url.hostname in ['localhost', '127.0.0.1', '10.0.2.2'])
        assert url.netloc
    source = ROOT / 'tools/mushaf-svg/local-assets/mushaf-svg'
    destination = args.destination / 'mushaf-svg'
    destination.mkdir(parents=True, exist_ok=True)
    names = {p.name for p in source.iterdir() if p.suffix != '.svg' or int(p.stem) in seeds}
    for old in destination.iterdir():
        if old.name not in names | {'page-assets-config.json'}:
            old.unlink()
    for name in names:
        target = destination / name
        if not target.exists() or target.read_bytes() != (source / name).read_bytes():
            shutil.copyfile(source / name, target)
    (destination / 'page-assets-config.json').write_text(json.dumps({'seedPages': seeds, 'baseUrl': base, 'allowLocalHttp': args.allow_local_http}, indent=2) + '\n')
    print(f'Staged {len(seeds)} original SVG seeds, all 604 JSON and unchanged metadata/notices. Seed bytes: {sum((source / f"{n:03d}.svg").stat().st_size for n in seeds)}')

if __name__ == '__main__':
    main()
