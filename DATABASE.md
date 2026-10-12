# Quran database

## Prebuilt reference corpus

`app/src/main/assets/quran-uthmani-v1.db`, SQLite user_version 1, UTF-8, 4,096-byte pages. Built from the approved original text/XML before packaging. Android copies bytes to private no-backup storage and opens read-only; it never imports verse records at first launch. The runtime verifies the finished-file SHA-256 on every open. Legacy profile databases are neither opened nor erased.

| Table | Key | Stored fields / purpose |
| --- | --- | --- |
| surahs | surahNumber | Official Arabic name and ayahCount |
| ayahs | surahNumber + ayahNumber | Canonical identity, unique corpus ordering ordinal; no page |
| text_versions | textVersionId | Name, version, official source URL and SHA-256 |
| ayah_texts | textVersionId + surahNumber + ayahNumber | Literal source text; FK to canonical identity |
| mushaf_editions | editionId | Name, textVersionId, pageCount, metadataVersion and metadataSha256 |
| edition_pages | editionId + pageNumber | startOrdinal, exclusive endOrdinal, startSurah/startAyah |
| edition_boundaries | editionId + kind + number | Same range/start references; JUZ, HIZB, QUARTER |
| notices | name | Verbatim text copyright block and metadata license declarations |

One TextVersion (`tanzil_uthmani_1_1`) and one edition (`madina_hafs_604`) are currently installed. There are 114 surahs, 6,236 ayahs/texts, 604 pages, 30 juz, 60 hizbs and 240 quarters. Whole-hizb starts are derived from every fourth official quarter start; juz starts match every eighth. Ordinals support ordered range reads without becoming ayah identity or a global page assignment.

PKs and the unique ordinal index support the bounded queries. Boundaries use an exclusive end (last end = 6,237). All partition ranges cover the corpus continuously without gaps/duplicates. There are no writable user tables, preferences, audio records, line/word maps or cloud schema.

The generator preserves every text character, space, combining mark and literal first-ayah bismillah prefix. Only record delimiters/CRLF framing are parsed. It compares all stored verse strings and their UTF-8 bytes against the unchanged source. `PRAGMA integrity_check` and `foreign_key_check` must pass. Source manifest and required copyright notices are both bundled and accessible in Settings/About.

## Reading V1: schema unchanged

The exact approved database, original sources, notices, manifest and generated digest constants are fixed. The UI does not rebuild or migrate them. Meccan/Medinan metadata is read from a verbatim copy of the approved XML; juz labels query the existing edition_boundaries table.

A separate SharedPreferences file `reading` stores edition ID, SURAH/PAGE mode, surah, visible ayah, page, textSizeSp (20–40) and darkMode. There are no user tables or settings writes inside the reference corpus. Saving a resume anchor deduplicates equal locations and uses asynchronous native preferences writes.

## Future richer page maps

Page maps belong to an edition. The same ayah may map to different pages, or multiple page segments, in different editions. A future `EditionLine` child can reference `(editionId, pageNumber, lineNumber)`; separate verified word/segment mappings can refer to ayahs and source-specific word positions. This extension adds mapping records without changing Ayah identity. It is design only; no line/word tables, fabricated indexes, or Pakistani layout are created now.

## Future user data and recitation metadata

Basic reading settings/last location can use SharedPreferences when needed, with edition-scoped locations. Future catalog/timing/download storage will be selected at the audio milestone; there is no user-account database or cloud-sync model now. Timing references a track revision and canonical ayah, never a global page number.
