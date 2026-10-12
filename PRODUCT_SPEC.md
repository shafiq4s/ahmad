# Product specification

## Purpose and scope

An Arabic-first Android Quran reading and recitation application. The previous memorization/revision/mistake/teacher/student product was cancelled on 2026-10-10. Do not execute its former roadmap.

## Requirements

- Native Kotlin/XML, RTL, readable Arabic, font scaling, accessible navigation, and Android 6.0/API 23 or newer.
- Complete offline Quran reading from the bundled approved corpus. No runtime text API, generated Quran text, normalization, autocorrection, or modification of source letters/signs.
- Begin with one verified Medina/Hafs Mushaf edition using the reviewed Tanzil source.
- Independent ayah identity `(surahNumber, ayahNumber)`; no page field in Ayah. Edition-specific page maps, with later room for verified line/word layout data.
- Future Indo-Pak/Pakistani 16-Line needs its own verified source, license, typography, numbering correspondence, and maps.
- No accounts, Room, Supabase, advertising, or AI in the current product.
- Future Abdul Kabir recordings use the user's original MP3s and supplied permission. Host them outside the APK, supporting streaming and offline download later.
- A remote versioned recitation catalog must allow publishing new surah recordings after app release without shipping a new APK for each surah.
- Future timing metadata maps ayahs to millisecond ranges in a specific recording revision. Support multiple reciter IDs structurally; only Abdul Kabir is initially in scope.

## Current authorized work — Main SVG integration

Use all604 pinned Hafs/KFQC full-page SVG/official JSON pairs, AndroidSVG1.4 and the approved opening touch overrides. The user visually validated the standalone reader on a real modern phone and authorized main integration. Preserve all canonical Tanzil text/database/hash/page-map references. The independent artwork edition owns page assets/geometry/first-last and surah-start navigation. Portrait, offline, RTL page swipes, long press, zoom, resume and an at-most3-document cache. Do not design the final highlight or selection sheet yet.

Validate full structure and distributed parsing, API23/36 behavior, first draws/cache/memory, source hashes, build/Lint and actual Debug/unsigned Release APK/AAB sizes. Remove obsolete image/rectangle/raster consumers only after the replacement gate passes; preserve useful tests and provenance. Stop after the report before selection UI, audio or other features.
