# Integrated Tanzil Quran corpus

## Approved source selection

The user approved complete offline integration after reviewing the cleanup. Only Tanzil official sources are used. The official license was reviewed first; the exact formerly documented files were retrieved again and matched all three approved SHA-256 pins. The approved originals, database, notices and manifest are fixed references; do not rebuild them for UI needs or change them without first explaining a documented reason to the user. Their frozen digests are checked through tools/quran/reference-lock.json. Originals are retained unchanged under `tools/quran/sources`, together with provenance and `Text_License.html`. QuranComplex remains an unused additional reference.

Text: **Tanzil Uthmani 1.1**, released 2021-02-12, Hafs/Medina, UTF-8 `txt-2` (surah|ayah|text). The exact official endpoint and options are:

https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&marks=true&sajdah=true&tatweel=true&agree=true

`marks`, `sajdah`, `tatweel` are enabled; `rub` and `stanween` were omitted (unchecked defaults). Actual attachment filename: `quran-uthmani.txt`. No random variant, generated verses, alternate source or font-specific text substitutions are used.

Official references: https://tanzil.net/download/ ; https://tanzil.net/docs/Uthmani ; https://tanzil.net/docs/quran_metadata ; https://tanzil.net/docs/Text_License ; https://tanzil.net/updates/

## Exact input files

| Source file | Official source | Bytes | Role |
| --- | --- | --- | --- |
| quran-uthmani.txt | Exact text URL above | 1,396,087 | Literal Quran text and original copyright block |
| quran-data.xml | https://tanzil.net/res/text/metadata/quran-data.xml | 77,234 | Metadata 1.0: names, ayah counts, page/juz/quarter boundaries |
| quran-data.js | https://tanzil.net/res/text/metadata/quran-data.js | 25,096 | Independent official boundary cross-check and explicit metadata license header only; not runtime input or packaged JS |

Text + XML source size: 1,473,321 bytes. Original text and JavaScript remain outside APK assets. Reading V1 additionally packages quran-data.xml byte-for-byte to display existing Meccan/Medinan classification without changing SQLite. The original XML SHA-256 still applies; the database, manifest and notices are unchanged.

| File | SHA-256 |
| --- | --- |
| quran-uthmani.txt | `6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f` |
| quran-data.xml | `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a` |
| quran-data.js | `9e9930c592aaa34b5c7a79ff4e99ffdc782263a266090bfe2c49e4ed4bac4ca4` |
| quran-uthmani-v1.db | `8ed100334e8eca42124035f51fffe934d52b20e5224b3a44c1a33b44dba74416` |

`tools/quran/sources/provenance.json` and `app/src/main/assets/source-manifest.json` preserve source/final URLs, retrieval time, byte counts, hashes and actual download options. Corpus identifiers: `tanzil_uthmani_1_1` and `madina_hafs_604`.

## License and attribution

The official text-license page states Creative Commons Attribution 3.0 and additionally requires verbatim distribution: changes to Quran text are not allowed. It requires clear Tanzil Project attribution with a link to `tanzil.net`, and retention of the copyright notice in copies and appropriately in derived files containing substantial text.

The official page says 2007-2021; the actual text file's notice says **Copyright (C) 2007-2026 Tanzil Project**. `TANZIL_NOTICE.txt` preserves the actual downloaded block byte-for-byte, including its original markers/year/terms. The same block is inside the derived SQLite `notices` table. Do not rewrite its year.

XML metadata declares version 1.0, Copyright (C) 2008-2009 Tanzil.info and `license="cc-by"`. The JS header explicitly names Creative Commons Attribution 3.0. `TANZIL_METADATA_NOTICE.txt` preserves both declarations separately and includes official source/license URLs; it too is stored in the database.

Settings/About shows modest Tanzil Project attribution, the link and license summary, with a button to display the full original notices locally. Reading and viewing these notices need no network. Following the link is an explicit external browser action.

## Build-time and runtime integrity

The approved data-stage command `python3 tools/quran/build_corpus.py` verified source hashes, ordered/unique keys, all surah counts, all page/juz/quarter boundaries against official JS, range coverage, foreign keys, SQLite integrity and literal equality of all 6,236 stored verse strings and UTF-8 bytes. It generates the finished 2,170,880-byte database and digest constant before packaging. Android only copies the finished verified file, then opens it read-only; it does not import ayahs.

Android repository tests compare every verse returned by all 114 surah queries to the unchanged original text file, validate every one of the 604 page starts and 30 juz starts against XML, and verify both partitions reproduce the complete corpus exactly once. They also check first/last retrieval, missing references, read-only SQL rejection, the unchanged finished-file digest, and recovery from a damaged local copy. See QURAN_DATA_VALIDATION.md for the approved data-stage report and VALIDATION.md for V1 regression results and API 23/36 limitations.

The XML `hizbs` collection contains 240 **quarter-hizb** starts, confirmed by the official JS name `HizbQaurter`. Whole-hizb starts use every fourth quarter (60), and juz starts every eighth (30); every derived juz start agrees with official juz metadata. The JS leading empty element and `[115,1]` terminator are not Quran entries.

## Exact-text and mapping constraints

No normalization, trim, whitespace collapsing, automatic correction or punctuation/sign replacement is applied to verse content. The builder parses only record delimiters and CRLF framing. The UI binds text as retrieved, placing numbers/references in a separate view.

First-ayah bismillah prefixes remain exactly as supplied; surah 9 has none. Surahs 95 and 97 include the official extra shadda documented in Tanzil's Version 1.0.1 update. Do not remove, prepend, duplicate or visually standardize these prefixes.

Page metadata supplies ayah-based starts, not printed lines, word splits or a page facsimile. All maps are edition-scoped; Ayah has no page field. Future verified line/word/segment records can be added separately without changing canonical identity. No Pakistani/Indo-Pak 16-Line layout or inferred authoritative word IDs are implemented.

No audio, downloads, recitation catalog, backend, advanced search or bookmarks are included. Await the user's review of this stage before adding features.
