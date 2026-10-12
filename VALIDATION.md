# Reading Experience V1 validation — 0.4.0

Final clean build/test/lint passed on 2026-10-10:

```sh
./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

## Fixed Quran references

The SQLite database, original sources, original copyright notices, source manifest and CorpusInfo remain byte-identical. All 11 locked reference files matched their SHA-256 values before/after implementation and inside the test suite. The corpus builder was not run; no data/schema migration or normalization was introduced. The database still contains 114 surahs, 6,236 literal numbered ayahs and 604 edition-scoped pages and occupies 2,170,880 bytes.

Database SHA-256 remains `8ed100334e8eca42124035f51fffe934d52b20e5224b3a44c1a33b44dba74416`. The existing full-text, surah-count, page/juz-boundary, read-only and corruption-recovery tests passed again. See [QURAN_DATA_VALIDATION.md](QURAN_DATA_VALIDATION.md) for the approved data-stage report and [reference-lock.json](tools/quran/reference-lock.json) for frozen reference hashes.

The approved quran-data.xml is additionally packaged verbatim (77,234 bytes; 15,618 compressed APK bytes) to read Meccan/Medinan classification. It still matches `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a`; no new source or regenerated Quran data was used. Page juz labels query the existing read-only edition_boundaries table.

## Test results

26 executions passed: 12 UI, eight QuranRepository, six preferences/metadata/reference-lock executions. Thirteen run on Android 6/API 23 and thirteen on Android 16/API 36 through Robolectric 4.16.1. Zero failures, errors or skips.

Verified:

- All original corpus integrity and exact-source checks, including every verse's UTF-8 equality and all page/juz starts.
- Index number/name/count and official Meccan/Medinan labels; every classification agrees with original XML.
- Baqarah loads exactly 286 verses, with a bounded visible child count and reused row views. Scrolling to ayah 140 persists that anchor, restores it after activity recreation and resumes it after a fresh activity launch from the index.
- Rapid direct page changes through 604, 22, 400, 50, 200, 3, 600, 2, 1, 604 and 400 retain the latest selection, page 400, starting at 29:31, juz 20. The selected page/edition resumes after a fresh launch. Canceled loads cannot replace the latest result.
- Previous/next bounds and invalid page inputs; correct page/juz labels based on local edition metadata.
- Persisted font size and app-local dark mode after recreation/relaunch, native night colors, original verse strings after resizing, and size limits 20–40sp.
- Arabic/RTL, 320dp small-screen configuration and fontScale 2, with positive index/reader viewports; Back and saved Settings state.
- Airplane mode is enabled in the reading simulation. The app/package have no Internet permission or text API.

Only the selected surah/page text is loaded, with cached 114-surah metadata and recycled native ListView rows. These checks demonstrate bounded loading, row reuse and selection correctness; they are not physical-device frame-rate or heap benchmarks. Device-specific Quran glyph rendering and actual scroll smoothness remain manual device checks. No physical device/emulator screenshots are claimed.

## Build, lint and APK

- Clean debug build and all tests passed.
- Lint: zero errors, one warning about newer test-only Robolectric 4.17; retained tested 4.16.1.
- APK signature, minSdk 23, targetSdk 36, versionCode 4/versionName 0.4.0 and no Internet permission verified.
- Bundled database, unchanged manifest/notices and original XML match their local files. No MP3, JavaScript or duplicate original Quran text file is packaged.
- **No dependency was added.** Native SharedPreferences/ListView and existing AppCompat DayNight/Core KTX/Lifecycle are reused. No separate font download/package was added; the native serif/Arabic fallback is used.

| Artifact | Bytes |
| --- | ---: |
| V1 debug APK | 4,345,924 |
| Approved offline-reader baseline | 4,310,103 |
| V1 increase | 35,821 |
| Unchanged Quran SQLite database | 2,170,880 |

APK SHA-256: `ab9fe476eaf8a3d7f66afbd6bb076d323560f0c696df9b841d9581cbd42aa802`.

Machine-readable evidence: [reading-v1-validation.json](tools/reading-v1-validation.json).

## Screen walkthrough

The index shows the app heading, the edition name, a page-reading entry and a resume entry/summary when a last location exists. Each recycled row has a separate number badge, official Arabic surah name and type/count below it.

Surah mode shows the surah name and ayah count above a full-height scrolling verse list. A separate small badge identifies each surah/ayah; the original Quran text is rendered in a spacious 28sp native serif/Arabic fallback by default, with extra line spacing and font padding. Page controls are hidden in surah mode to preserve reading space.

Page mode shows the page title, page/count/juz label, previous/next buttons and numeric page input with Go. It displays only the ayahs in the approved page range, with surah labels so mixed-surah pages remain clear. It is a textual list, not a facsimile.

Settings offers smaller/larger text (20–40sp) and a dark-mode switch, followed by the retained About/Tanzil attribution and original notices. Settings and last location remain local through SharedPreferences.

Stop here for user review. Audio, downloads, search, bookmarks, tafsir, translations, 16-Line editions and cloud backends remain unimplemented.
