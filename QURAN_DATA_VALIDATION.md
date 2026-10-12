# Offline Quran validation — 0.3.0

Verified on 2026-10-10. The final clean command passed:

```sh
./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

## Quran integrity (separate from UI/build results)

| Requirement | Actual result | Status |
| --- | --- | --- |
| Surahs | 114; every name/count/number agrees with official XML | Pass |
| Numbered ayahs | 6,236, complete ordered unique surah/ayah keys | Pass |
| Al-Fatiha / Al-Baqarah / Aal-Imran / Al-Ikhlas / An-Nas | 7 / 286 / 200 / 4 / 6 | Pass |
| Medina/Hafs pages | 604; all starts agree with official XML | Pass |
| Juz | 30; all starts agree with official XML | Pass |
| Whole hizbs / quarters | 60 / 240; whole hizbs use every fourth official quarter | Pass |
| Page and juz coverage | Each complete partition equals all 6,236 source ayahs, once in order | Pass |
| First / last ayah retrieval | 1:1 / 114:6; first/last surahs present | Pass |
| Literal text | All 6,236 SQLite and Android-retrieved strings and UTF-8 bytes match unchanged source | Pass |
| SQLite integrity / foreign keys | integrity_check = ok; foreign_key_check empty | Pass |
| Read-only corpus | Read-only open, SQL writes rejected, finished-file digest unchanged | Pass |
| Damaged private copy | Replaced atomically from verified bundled database; no verse import | Pass |
| XML/JS boundaries | All 604 page / 30 juz / 240 quarter starts agree | Pass |
| Reproducibility | Rebuilding produces the same database SHA-256 | Pass |
| Copyright notice | Original downloaded block is byte-identical in the bundled notice; retained inside DB | Pass |

Page-start samples (`surah:ayah`): 1 -> 1:1; 2 -> 2:1; 3 -> 2:6; 22 -> 2:142; 50 -> 3:1; 200 -> 9:80; 400 -> 29:31; 603 -> 109:1; 604 -> 112:1. Page 604 ends at 114:6. Tests verify all starts, exceeding the required samples.

No normalization, trimming of verse values, generated replacement text or automatic correction is performed. First-ayah bismillah prefixes and every combining mark/sign remain as supplied.

## Android/build results and practical limits

- 14 test executions passed: eight repository and six UI executions, seven on Android 6/API 23 and seven on Android 16/API 36. Zero failures, errors or skips.
- API 23/36 Robolectric tests cover exact repository reads, read-only SQL rejection, copy recovery, the full real index, actual surah reading, page navigation/bounds, attribution, Arabic/RTL, recreation and saved-state restoration, 320dp screen width and fontScale 2.
- Reading tests set airplane mode and verify the app has no Internet permission. APK manifest inspection independently confirms no Internet permission; there is no runtime text API/client. Airplane-mode testing is simulated, not a real radio/device test.
- Lint: zero errors, one warning (`NewerVersionAvailable`, test-only Robolectric 4.17 available; tested version 4.16.1 retained).
- APK signature verification passed. minSdk 23, targetSdk 36, versionCode 3/versionName 0.3.0 and Arabic label verified from the packaged APK.
- Packaged corpus, manifest and notices match local assets exactly; no duplicate original text/XML/JS, audio assets or production data/network dependency was added.
- No physical Android device or Android emulator is available/verified in this workspace. Actual Quran glyph rendering and physical-device behavior remain a manual check; JVM tests do not certify those.

## Sizes (debug APK, like-for-like baseline)

| Artifact | Bytes |
| --- | ---: |
| Finished SQLite database alone | 2,170,880 |
| Compressed database entry inside APK | 721,430 |
| Final debug APK | 4,310,103 |
| Previous cleanup debug APK baseline | 3,576,979 |
| Total APK increase (data, notices and reader implementation) | 733,124 |

The APK increase is about 20.5%; the final debug APK is about 4.31 MB (decimal). The database's standalone size is not its compressed APK contribution. First use also copies the finished 2,170,880-byte file into private storage. This is not a signed production release size.

Database SHA-256: `8ed100334e8eca42124035f51fffe934d52b20e5224b3a44c1a33b44dba74416`.

APK SHA-256: `afa37f4d4e6408179a5e3638106f4e0b0c9f0c3c520bbb3ac0049b61533bd9a2`.

Exact source filenames, official URLs, options and SHA-256 pins are in [QURAN_DATA.md](QURAN_DATA.md), [source-manifest.json](app/src/main/assets/source-manifest.json) and [validation.json](tools/quran/validation.json). The Quran text comes from quran-uthmani.txt and metadata from quran-data.xml; quran-data.js only cross-checks metadata and provides its original license header.

Stop for the user's review. Audio, downloads, Abdul Kabir integration, advanced search, bookmarks, 16-Line editions and cloud backends remain unimplemented.
