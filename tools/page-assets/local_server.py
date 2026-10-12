#!/usr/bin/env python3
"""Development-only pinned page server. Serves five originals without changing them."""
import argparse
import hashlib
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EDITION = 'quran_ws_hafs_kfqc_b4155f0'
PAGES = {119, 120, 121, 122, 123}
MANIFEST = json.loads((ROOT / 'tools/mushaf-svg/asset-manifest.json').read_text())
PINNED = {r['filename']: r for r in MANIFEST['files']}

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        stem = self.path.removeprefix(f'/{EDITION}/').removesuffix('.svg')
        if self.path != f'/{EDITION}/{stem}.svg' or not stem.isdigit() or int(stem) not in PAGES or stem != f'{int(stem):03d}':
            self.send_error(404)
            return
        filename = f'{int(stem):03d}.svg'
        data = (ROOT / 'tools/mushaf-svg/local-assets/mushaf-svg' / filename).read_bytes()
        assert len(data) == PINNED[filename]['bytes'] and hashlib.sha256(data).hexdigest() == PINNED[filename]['sha256']
        self.send_response(200)
        self.send_header('Content-Type', 'image/svg+xml')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=8773)
    parser.add_argument('--bind', default='127.0.0.1')
    args = parser.parse_args()
    print(f'Local-only edition pages {sorted(PAGES)} on {args.bind}:{args.port}', flush=True)
    ThreadingHTTPServer((args.bind, args.port), Handler).serve_forever()
