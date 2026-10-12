# Local reading history and interface maturity

The home screen retains clear resume and reader/index/bookmark/offline-search actions and adds recent readings. Bookmarks show Arabic surah names, saved edition page and optional ayah, newest first, with a 48dp contextual remove action. Selecting a bookmark restores its saved page and canonical selection through the existing reader API.

History uses the existing private SharedPreferences file. At most ten distinct edition/pages are retained, newest first. Revisiting a page updates its position and timestamp; a selected ayah is retained with its surah context. Only successfully displayed pages are recorded, and failed downloads leave the last successful position intact. History items and resume can restore the selected identity; saved pages take precedence for ayahs spanning pages. History dates and times explicitly use Arabic formatting. No account, backend, database schema, new dependency or networking configuration was added.

The existing fast surah-name filter and recycled 114-row index remain, including number, original Arabic name, verse count/classification and first Quran SVG edition page. Settings expose only implemented preferences, cache usage/clear, and entry points to separate native About and Support pages. Back returns from those pages to Settings, and from Settings to its entry screen. Long source/notice text is scrollable. About identifies Android 6+, the actual version, Tanzil, independent Quran.ws, documented KFQC provenance and the existing license scopes. Support contains descriptive text and a quiet coming-soon badge, with no payment action.

Bookmark/history empty states share existing green/ivory rounded tokens. Existing modal, copy/share, reader bars and controls retain their approved visual family. The visible debug-only source-configuration message was removed; unavailable pages show clean Arabic guidance and a guarded retry. Cached/bundled display and downloader behavior are unchanged. Cache clear removes only downloaded edition artwork and preserves seed pages, canonical data, polygons, bookmarks, history and settings.

## Verification

- Full application suite: 122 tests, zero failures/errors/skips, including the existing Quran literal-source/count checks, reading/search/bookmark/cache and touch/render regressions. The unchanged standalone reader's 13 passing regression results were retained by Gradle's up-to-date check: 135 validated tests overall.
- After visual review, only history date formatting, row typography and its caption changed. All six affected native-Canvas UI executions passed again on Robolectric API 23 and 36; no tests were disabled. The initial unattached-row RTL assertion was corrected to inspect the actual attached ListView row, preserving the RTL requirement.
- Debug build, Android device-test APK compilation and Lint succeeded. Final Lint: zero errors, 9 existing warnings, with no new suppressions.
- Runtime scope: API 23 and 36 were exercised using Robolectric. Screenshots are native-Canvas simulated UI captures, not physical-device captures. A software API-23 AVD was attempted; its package-install service did not respond within the bounded timeout, so no new AVD/device runtime pass is claimed. The task-owned emulator was stopped.
- Sixteen frozen production boundaries match their pre-stage SHA-256 locks, including highlight, renderer, polygons adapter/geometry, opening overrides, edition/page map, downloader, cache/controller, repository and dependency/config declarations. All 604 source SVG and 604 JSON plus eleven Tanzil reference files passed the source verifier. Packaged canonical and retained edition assets are byte-exact with their sources.
- APK contains only the unchanged eleven SVG seeds (1–5, 300, 600–604), all 604 official polygon JSON, catalog/manifests/notices/provenance and Tanzil assets. Default source URL remains empty. Memory SVG LRU remains three documents. APK signature verification passed v1/v2; manifest remains minSdk 23 / targetSdk 36, without new permissions.
- Final clean-packaged Debug APK: **6,876,578 bytes** (6.88 MB), an increase of 13,896 bytes from the previous 6,862,682-byte build. SHA-256: `d71eba219b34b005061808eb7754395310d8dbf923ff2a25a246f107ed2f5bae`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Reproduction

Use the existing JDK 21 / SDK 36 / Gradle 8.13 environment:

```sh
./gradlew --continue -PnativeSvgPrototype=true -PreaderUiDeviceTests=true :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :svg-native-prototype:testDebugUnitTest
```

The final history-formatting recheck used `:app:testDebugUnitTest --tests '*ReadingMaturityUiTest' :app:lintDebug :app:assembleDebug`. `full-suite-summary.json` preserves the full-run counts before the filtered recheck. `verify_stage.py` checks frozen boundaries, these passing gates, the final APK subset and exact packaged source bytes. Generated screenshots and APK/build outputs remain ignored and are excluded from Git.

Captures: `screenshots/simulated-api36/01-home.png`, `02-bookmarks.png`, `03-history.png`, `04-settings.png`, `05-about.png`, `06-support.png`, `07-index.png`, `08-reader.png`.
