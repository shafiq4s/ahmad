#!/usr/bin/env python3
"""Retrieve only pinned full Hafs pages and the upstream auditor's prerequisites."""
import concurrent.futures
import datetime
import hashlib
import json
import pathlib
import shutil
import urllib.request

COMMIT = "b4155f07e4aea087d2c458069acff9f7c7e749f6"
HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent / "local-fixtures" / "full-audit-source"
PROJECT = HERE.parents[2]
PREFIX = "mushafs/hafs/kfqc"
BASE = f"https://raw.githubusercontent.com/quran-ws/quran-svg/{COMMIT}/"
MANIFEST = HERE / "manifest.json"
EXISTING = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else None
EXPECTED = {r['path']:r for r in EXISTING['files']} if EXISTING else {}

def retrieve(path):
    dest = ROOT / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.exists():
        previous = PROJECT / "tools/quran-svg/local-fixtures/sources" / path
        if previous.exists():
            shutil.copyfile(previous, dest)
        else:
            with urllib.request.urlopen(BASE + path, timeout=90) as response:
                content = response.read()
            temp = dest.with_suffix(dest.suffix + ".part")
            temp.write_bytes(content)
            temp.replace(dest)
    content = dest.read_bytes()
    record = dict(path=path, url=BASE+path, bytes=len(content), sha256=hashlib.sha256(content).hexdigest())
    if path in EXPECTED:
        assert record['sha256']==EXPECTED[path]['sha256'] and record['bytes']==EXPECTED[path]['bytes'],path
    return record

def main():
    full = [f"{PREFIX}/{kind}/{n:03d}.{ext}" for n in range(1,605)
            for kind,ext in [("svg","svg"),("json","json")]]
    auxiliary = [f"{PREFIX}/svg-br/{n:03d}.svg.br" for n in range(1,605)]
    auxiliary += [f"{PREFIX}/json/{name}.json" for name in ["surah","markers"]]
    auxiliary += [f"tools/{name}.py" for name in ["audit_ayah_polygons","polygon_lib","qiraat_map"]]
    auxiliary += [f"tools/data/qiraat-ayah-map/{name}.json" for name in
                  ["counting-systems","qiraat","book-boundary-primitives"]]
    records, failures = [], []
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        futures = {pool.submit(retrieve,path):path for path in full+auxiliary}
        for i, future in enumerate(concurrent.futures.as_completed(futures),1):
            try:
                records.append(future.result())
            except Exception as error:
                failures.append(dict(path=futures[future], error=str(error)))
            if i % 100 == 0:
                print(f"retrieved {i}/{len(futures)}; failures {len(failures)}",flush=True)
    full_set = set(full)
    for record in records:
        record["scope"] = "full-page" if record["path"] in full_set else "audit-only"
    manifest = dict(commit=COMMIT, retrievedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                    root=str(ROOT), fullPages=604, files=sorted(records,key=lambda r:r["path"]),
                    failures=failures, excluded="All surah-crop variants; other mushafs; app packaging")
    # Existing approved files are immutable anchors for this larger retrieval.
    lock = json.loads((PROJECT / "tools/quran-svg/source-lock.json").read_text())
    anchors = {row["path"]:row["sha256"] for row in lock["files"]}
    for row in records:
        if row["path"] in anchors:
            assert row["sha256"] == anchors[row["path"]], row["path"]
    if not EXISTING:
        MANIFEST.write_text(json.dumps(manifest,indent=2)+"\n")
    # A subsequent verification must never silently re-baseline approved hashes.
    print(json.dumps(dict(files=len(records),failures=failures)),flush=True)
    if failures:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
