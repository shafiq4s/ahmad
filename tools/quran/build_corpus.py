"""Build and verify the immutable corpus from pinned official Tanzil artifacts."""
import hashlib
import json
import re
import sqlite3
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCES = ROOT / 'tools/quran/sources'
ASSETS = ROOT / 'app/src/main/assets'
DB_NAME = 'quran-uthmani-v1.db'
TEXT_ID = 'tanzil_uthmani_1_1'
EDITION_ID = 'madina_hafs_604'
PINNED = {
    'quran-uthmani.txt': '6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f',
    'quran-data.xml': '8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a',
    'quran-data.js': '9e9930c592aaa34b5c7a79ff4e99ffdc782263a266090bfe2c49e4ed4bac4ca4',
}

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build():
    for name, digest in PINNED.items():
        assert sha256(SOURCES / name) == digest, 'Unapproved source: ' + name
    raw_text = (SOURCES / 'quran-uthmani.txt').read_bytes()
    records = []
    for framed in raw_text.split(b'\n'):
        # Remove only the CR framing a CRLF line. Never trim or normalize verse bytes.
        row = framed.removesuffix(b'\r')
        if not row or row.startswith(b'#'):
            continue
        surah, ayah, verse = row.split(b'|', 2)
        assert re.fullmatch(rb'[1-9][0-9]*', surah) and re.fullmatch(rb'[1-9][0-9]*', ayah)
        records.append((int(surah), int(ayah), verse.decode('utf-8', errors='strict')))
    metadata = ET.fromstring((SOURCES / 'quran-data.xml').read_bytes())
    surahs = list(metadata.find('suras'))
    expected_keys = [(int(s.attrib['index']), a) for s in surahs for a in range(1, int(s.attrib['ayas']) + 1)]
    assert len(surahs) == 114 and len(records) == 6236
    assert [(s, a) for s, a, _ in records] == expected_keys
    assert {s: len([1 for n, _, _ in records if n == s]) for s in (1, 2, 3, 112, 114)} == {1: 7, 2: 286, 3: 200, 112: 4, 114: 6}
    positions = {(s, a): i for i, (s, a, _) in enumerate(records, 1)}
    javascript = (SOURCES / 'quran-data.js').read_text(encoding='utf-8')
    partitions = {}
    for section, js_name, count in (('pages', 'Page', 604), ('juzs', 'Juz', 30), ('hizbs', 'HizbQaurter', 240)):
        entries = list(metadata.find(section))
        assert [int(e.attrib['index']) for e in entries] == list(range(1, count + 1))
        locations = [(int(e.attrib['sura']), int(e.attrib['aya'])) for e in entries]
        starts = [positions[key] for key in locations]
        assert starts[0] == 1 and all(a < b for a, b in zip(starts, starts[1:]))
        match = re.search(r'QuranData\.' + js_name + r'\s*=\s*\[([\s\S]*?)\];', javascript)
        js_locations = [(int(s), int(a)) for s, a in re.findall(r'\[(\d+),\s*(\d+)\]', match[1])]
        assert js_locations == locations + [(115, 1)]
        partitions[section] = locations
    assert partitions['hizbs'][::8] == partitions['juzs']
    notice = raw_text[raw_text.index(b'# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK'):]
    js_bytes = (SOURCES / 'quran-data.js').read_bytes()
    metadata_notice = js_bytes[:js_bytes.index(b'var QuranData')]
    metadata_notice += (SOURCES / 'quran-data.xml').read_bytes().split(b'\n', 2)[1] + b'\n'
    metadata_notice += b'Source: https://tanzil.net/res/text/metadata/quran-data.xml\n'
    metadata_notice += b'License reference: https://tanzil.net/docs/Text_License\n'
    ASSETS.mkdir(parents=True, exist_ok=True)
    path = ASSETS / DB_NAME
    path.unlink(missing_ok=True)
    db = sqlite3.connect(path)
    db.executescript('''
PRAGMA encoding = 'UTF-8';
PRAGMA page_size = 4096;
PRAGMA journal_mode = DELETE;
PRAGMA foreign_keys = ON;
PRAGMA user_version = 1;
PRAGMA application_id = 1364546126;
CREATE TABLE surahs (surahNumber INTEGER PRIMARY KEY, name TEXT NOT NULL, ayahCount INTEGER NOT NULL);
CREATE TABLE ayahs (surahNumber INTEGER NOT NULL REFERENCES surahs(surahNumber), ayahNumber INTEGER NOT NULL, ordinal INTEGER NOT NULL UNIQUE, PRIMARY KEY(surahNumber, ayahNumber));
CREATE TABLE text_versions (textVersionId TEXT PRIMARY KEY, name TEXT NOT NULL, version TEXT NOT NULL, sourceUrl TEXT NOT NULL, sourceSha256 TEXT NOT NULL);
CREATE TABLE ayah_texts (textVersionId TEXT NOT NULL REFERENCES text_versions(textVersionId), surahNumber INTEGER NOT NULL, ayahNumber INTEGER NOT NULL, text TEXT NOT NULL, PRIMARY KEY(textVersionId, surahNumber, ayahNumber), FOREIGN KEY(surahNumber, ayahNumber) REFERENCES ayahs(surahNumber, ayahNumber));
CREATE TABLE mushaf_editions (editionId TEXT PRIMARY KEY, name TEXT NOT NULL, textVersionId TEXT NOT NULL REFERENCES text_versions(textVersionId), pageCount INTEGER NOT NULL, metadataVersion TEXT NOT NULL, metadataSha256 TEXT NOT NULL);
CREATE TABLE edition_pages (editionId TEXT NOT NULL REFERENCES mushaf_editions(editionId), pageNumber INTEGER NOT NULL, startOrdinal INTEGER NOT NULL, endOrdinal INTEGER NOT NULL, startSurah INTEGER NOT NULL, startAyah INTEGER NOT NULL, PRIMARY KEY(editionId, pageNumber), FOREIGN KEY(startSurah,startAyah) REFERENCES ayahs(surahNumber,ayahNumber));
CREATE TABLE edition_boundaries (editionId TEXT NOT NULL REFERENCES mushaf_editions(editionId), kind TEXT NOT NULL, number INTEGER NOT NULL, startOrdinal INTEGER NOT NULL, endOrdinal INTEGER NOT NULL, startSurah INTEGER NOT NULL, startAyah INTEGER NOT NULL, PRIMARY KEY(editionId,kind,number), FOREIGN KEY(startSurah,startAyah) REFERENCES ayahs(surahNumber,ayahNumber));
CREATE TABLE notices (name TEXT PRIMARY KEY, content TEXT NOT NULL);
''')
    with db:
        db.executemany('INSERT INTO surahs VALUES (?,?,?)', [(int(s.attrib['index']), s.attrib['name'], int(s.attrib['ayas'])) for s in surahs])
        db.executemany('INSERT INTO ayahs VALUES (?,?,?)', [(s, a, i) for i, (s, a, _) in enumerate(records, 1)])
        provenance = json.loads((SOURCES / 'provenance.json').read_text())
        text_url = next(f['source_url'] for f in provenance['files'] if f['filename'] == 'quran-uthmani.txt')
        db.execute('INSERT INTO text_versions VALUES (?,?,?,?,?)', (TEXT_ID, 'Tanzil Uthmani', '1.1', text_url, PINNED['quran-uthmani.txt']))
        db.executemany('INSERT INTO ayah_texts VALUES (?,?,?,?)', [(TEXT_ID, s, a, text) for s, a, text in records])
        db.execute('INSERT INTO mushaf_editions VALUES (?,?,?,?,?,?)', (EDITION_ID, 'مصحف المدينة — رواية حفص', TEXT_ID, 604, '1.0', PINNED['quran-data.xml']))
        def ranges(locations):
            starts = [positions[key] for key in locations]
            return [(i, start, end, s, a) for i, (start, end, (s, a)) in enumerate(zip(starts, starts[1:] + [6237], locations), 1)]
        db.executemany('INSERT INTO edition_pages VALUES (?,?,?,?,?,?)', [(EDITION_ID, *r) for r in ranges(partitions['pages'])])
        for kind, locations in [('JUZ', partitions['juzs']), ('HIZB', partitions['hizbs'][::4]), ('QUARTER', partitions['hizbs'])]:
            db.executemany('INSERT INTO edition_boundaries VALUES (?,?,?,?,?,?,?)', [(EDITION_ID, kind, *r) for r in ranges(locations)])
        db.executemany('INSERT INTO notices VALUES (?,?)', [('TANZIL_NOTICE.txt', notice.decode('utf-8')), ('TANZIL_METADATA_NOTICE.txt', metadata_notice.decode('utf-8'))])
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute('PRAGMA foreign_key_check').fetchall() == []
    stored = db.execute('SELECT surahNumber,ayahNumber,text FROM ayah_texts ORDER BY surahNumber,ayahNumber').fetchall()
    assert stored == records
    assert all(original.encode('utf-8') == saved.encode('utf-8') for (_, _, original), (_, _, saved) in zip(records, stored))
    samples = {1:(1,1),2:(2,1),3:(2,6),22:(2,142),50:(3,1),200:(9,80),400:(29,31),603:(109,1),604:(112,1)}
    for number, ref in samples.items():
        assert db.execute('SELECT startSurah,startAyah FROM edition_pages WHERE pageNumber=?',(number,)).fetchone() == ref
    for table, where, count in [('edition_pages','1=1',604),('edition_boundaries',"kind='JUZ'",30),('edition_boundaries',"kind='HIZB'",60),('edition_boundaries',"kind='QUARTER'",240)]:
        spans=db.execute('SELECT startOrdinal,endOrdinal FROM '+table+' WHERE '+where+' ORDER BY startOrdinal').fetchall()
        assert len(spans)==count and spans[0][0]==1 and spans[-1][1]==6237
        assert all(a[1]==b[0] for a,b in zip(spans,spans[1:])) and sum(end-start for start,end in spans)==6236
    db.execute('VACUUM')
    db.close()
    (ASSETS / 'TANZIL_NOTICE.txt').write_bytes(notice)
    (ASSETS / 'TANZIL_METADATA_NOTICE.txt').write_bytes(metadata_notice)
    manifest = {'schemaVersion':1,'database':{'filename':DB_NAME,'sha256':sha256(path),'bytes':path.stat().st_size},'textVersionId':TEXT_ID,'editionId':EDITION_ID,'sources':provenance['files'],'counts':{'surahs':114,'ayahs':6236,'pages':604,'juzs':30,'hizbs':60,'quarters':240},'downloadOptions':{'quranType':'uthmani','outType':'txt-2','marks':True,'sajdah':True,'tatweel':True,'rub':False,'stanween':False},'verification':{'everyAyahExactUnicodeAndUtf8RoundTrip':True,'foreignKeyCheck':'ok','integrityCheck':'ok','samplePageStarts':{str(k):list(v) for k,v in samples.items()}}}
    (ASSETS / 'source-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    code = 'package com.ahmad.quran.data\n\n// Generated by tools/quran/build_corpus.py from verified official sources.\ninternal object CorpusInfo {\n'
    for name, value in [('FILE_NAME',DB_NAME),('SHA256',sha256(path)),('TEXT_VERSION_ID',TEXT_ID),('EDITION_ID',EDITION_ID)]:
        code += '    const val '+name+' = "'+value+'"\n'
    code += '    const val SCHEMA_VERSION = 1\n}\n'
    (ROOT/'app/src/main/java/com/ahmad/quran/data/CorpusInfo.kt').write_text(code)
    print(json.dumps({'database':manifest['database'],'counts':manifest['counts'],'exact_ayahs_verified':len(stored)},indent=2))


if __name__ == '__main__':
    build()
