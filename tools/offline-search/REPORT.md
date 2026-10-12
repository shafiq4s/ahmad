# Home, offline Quran search and ayah navigation

This stage changes native UX and adds read-only canonical search. No Quran data, original SVG/JSON, opening overrides, edition map, downloader, parsed-document cache, ayah modal or persisted bookmark/reading preference format changes. Twelve whole-file boundary hashes match reference-lock.json. verify_stage.py also strips only the added repository search method and the validated renderer selection facade and compares the remaining files to their original hashes. Existing drawing, highlight, matrices, touch handling and repository methods are unchanged. Builds verify all 1208 source assets and eleven approved Tanzil references; SQLite is not rebuilt.

## Home and branding

Launch opens a compact warm RTL home screen with the existing book vector, named last-surah/page resume and open-mushaf, index, bookmarks and search entry points. Resume is disabled with a short explanatory empty state when there is no saved position. The original recycled surah index remains a separate panel, including its name-only filter. Optional automatic resume and saved-screen recreation are preserved.

The starting theme shows the same icon and ivory background, with native API 31 splash attributes. A named in-activity readiness surface disappears as soon as the initial local metadata is ready or reports a failure; no timer, extra activity, animated logo or new dependency is used. Existing reader bar geometry/style and 190ms collapse remain. The header shows surah/juz, and the footer owns the page number, removing repeated page information. Controls keep 48dp or larger targets, and panel/input labels support accessibility. Original SVG text sizing is untouched.

## Search and comparison keys

QuranRepository.searchAyahs streams the fixed text-version cursor in surah/ayah order on Dispatchers.IO. MainViewModel debounces changes by 250ms, cancels obsolete jobs and checks cancellation for each row. Results use batches of 50 with one look-ahead row and a load-more action; no normalized corpus/index or whole Quran string list is retained at startup. Search never reads a page SVG and performs no network request.

Normalization applies only to temporary comparison strings: Unicode combining marks, tatweel and small elongation waw/yeh are removed; alif variants fold to alif, alif maqsura to yeh, and whitespace collapses. Additional source comparison forms expand dagger alif and try the traditional waw/dagger-alif carrier as alif, supporting common queries such as العالمين and الصلاة. These spelling alternatives broaden matching; they do not change canonical storage. The query is a literal substring/phrase rather than SQL LIKE or a semantic query. Every returned Ayah retains cursor.getString(text) unchanged. Result TextViews receive the entire original string and preview up to three lines using Android's ellipsis; no edited or normalized excerpt is displayed.

The required الحمد لله query returns 23 canonical results, beginning with 1:2. Source-equality assertions compare returned and displayed strings to the repository. Cursor cancellation closes safely, leaving the read-only database usable. Existing full-source verification still checks all 6236 canonical ayahs.

## Ayah navigation and loading

Both result clicks and the existing surah-mode jump's new ayah field validate the identity against current canonical counts, then resolve its page through QuranSvgEdition.pageForAyah. No Tanzil page start is used. Targets 1:1, 2:282, 5:78 and 114:6 open pages 1, 48, 121 and 604. One pending AyahId is saved until the exact requested page has drawn. The selection facade validates membership in that page and calls the original held-selection function, highlighting every original component together without dispatching a synthetic touch or opening a modal. Explicit same-page navigation uses the existing 1x setter so the selected ayah remains visible. A target survives a missing-page failure/retry, but ordinary navigation or leaving the reader clears a stale pending target. Back from a result restores the search/query.

The direct-jump card has a labeled numeric field with per-surah bounds, clear validation and native scrolling when a keyboard reduces available space. Editing clears an old error. Ordinary page/juz modes remain. Last-read recording now also handles a fast seed draw that precedes canonical metadata readiness: saving still requires a successfully drawn matching page, and failed targets do not overwrite a prior position.

Bundled/cache/remote resolution, source hashing, foreground priority, post-draw raw prefetch and the three-document LRU remain byte-identical. Loading stays inside the page area. A failed-state guard disables/ignores repeated retry clicks while loading. Default base URL remains empty. Debug describes the unconfigured test source cleanly; release retains the general Arabic availability message. Bookmarks, last-read, no-results and zero-download-cache states use short Arabic messages. No hosting configuration or network implementation is added.

## Verification and delivery

```sh
./gradlew --continue -PnativeSvgPrototype=true \
  :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest \
  :app:lintDebug :svg-native-prototype:testDebugUnitTest
python3 tools/offline-search/verify_stage.py
```

All 121 executions pass: 108 app and 13 shared-renderer, zero failures/errors/skips. Sixteen added executions exercise SDK 23 and 36: offline/common-script queries, original result text, literal/no-results behavior, ordered pagination, cancellation, rapid replacement/load-more coalescing, canonical bounds and edition-specific page targets, home/result/Back flows, full long-ayah selection, same-page selection, missing-target retry and reading/bookmark regressions. Existing corpus/source, zoom/pinch/swipe, modal/copy/share, cache and download-integrity suites pass. Instrumentation sources also compile; this stage's runtime platform evidence and captures are Robolectric/native Canvas, not a new emulator/phone run.

Lint, exact final APK size/digest and packaged membership are recorded in validation.json. Build-tools verifies debug v1/v2 signatures and minSdk23/target36. The package still contains only seeds 1–5, 300, 600–604 plus all 604 official JSON and unchanged corpus/catalog/notices. Six final SDK36 captures in screenshots/simulated-api36 show home/resume, search, no results, direct ayah jump, selected 2:282 and unconfigured page availability. Temporary captures and build outputs are ignored by Git.

Artifact: app/build/outputs/apk/debug/app-debug.apk. Non-seed pages not already cached still need the existing configurable server; no server is selected in this stage. No audio, tafsir, Cloudflare, payment, subscriptions or further feature follows delivery.

The final signed debug APK is 6,862,682 bytes (6.86 decimal MB), SHA-256 335859a7b331ac50fba1d47929424dcc171bdb43b18295f2da5c975a2943e684, an increase of 25,173 bytes from the previous stage. Lint has zero errors and nine nonblocking existing compatibility/layout/tool suggestions; no rule is suppressed. No new dependency or network permission is added.
