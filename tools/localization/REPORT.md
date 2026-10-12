# Seven interface languages — validation

## Delivered interface

Arabic (`ar`), English (`en`), Afghan Dari (`fa-AF`), Afghan Pashto (`ps-AF`), Uzbek Latin (`uz-UZ`), Tajik Cyrillic (`tg-TJ`) and Afghan Hazaragi (`haz-AF`) each contain all 116 translatable interface messages (812 resource entries). Six language-neutral formats are shared. Existing AppCompat and AndroidX Core are reused; no dependency or permission was added.

Settings → Language offers System plus seven native language names. System picks the first supported device locale and falls back to Arabic. Bare Persian/`fa-AF` selects Afghan Dari; explicitly Iranian Persian does not. Android 13+ uses platform per-app locales and `locales_config`; changes from native Android settings are honored. Older Android versions save the System/manual choice separately and apply the resolved language through AppCompat. The same choice is a no-op and the framework owns necessary activity recreation.

Arabic, Dari, Pashto and Hazaragi use RTL; English, Uzbek and Tajik use LTR. UI rows/dialogs/arrows follow this policy. Unchanged Arabic surah names are isolated with existing AndroidX BidiFormatter only inside translated UI labels. Canonical Quran text remains an independent RTL view; its content, Copy and Share are exact. Mushaf pages, touch coordinates and swipe/page order never mirror.

AAPT successfully compiled `values-b+haz+AF`, APK metadata lists `haz-AF`, and API 23/36 runtime resource lookup selects its own Hazaragi text. No substitute language tag is used. Only calendar formatting borrows `fa-AF` conventions because CLDR Hazaragi date data is incomplete. See [translation review](../../TRANSLATION_REVIEW.md): regional wording is provisional, especially the complete Hazaragi draft, and needs native-speaker editorial review.

## Executed checks

Command: `./gradlew --continue -PnativeSvgPrototype=true -PreaderUiDeviceTests=true :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :svg-native-prototype:testDebugUnitTest`.

- Application: 130 passed, zero failures/errors/skips.
- Shared native SVG prototype: 13 passed (unchanged task/results reused by Gradle).
- Eight additional executions: two policy/resource tests and three native UI methods on each API (23 and 36).
- System → Arabic → English → Dari → Pashto → Uzbek → Tajik → Hazaragi → System preserves reading settings, bookmarks, history, last location and all cached SVG hashes.
- Real resource selection, native-language picker, UI layout direction, lists, About/Support/Back, regional Persian policy and Android 13+ platform-language changes verified.
- English/Hazaragi reader and dialogs: original Quran text, Copy/Share payloads and AyahId `5:78` unchanged at 1x/2x/4x. Existing full Quran/geometry/navigation/search/history/bookmark regressions also pass.
- Home parses zero SVG documents. Parsed-document retention remains capped at three.
- Lint: zero errors, 11 warnings. Existing warnings retained; additional advisory warnings concern the API-33 locale-config attribute and the standard SharedPreferences edit idiom. No suppressions added.

Runtime checks and screenshots use **Robolectric API 23/36 with native Canvas**, not an attached emulator or physical device. Android-test APK is built but was not executed on hardware in this environment. Native Android settings and regional/font behavior should also receive a physical-device review before public release.

## Immutable boundaries and package

`verify_stage.py` confirms 16 frozen source/configuration files byte-for-byte unchanged, including rendering, hit-testing, polygons/overrides/highlight, models, edition/page map, canonical repository and download/cache architecture. All original Tanzil and edition assets are exact in the APK. All 604 official polygon files remain bundled; only existing seed SVG pages 1–5, 300 and 600–604 are packaged. Base URL stays empty.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — **6,938,261 bytes** (6.94 MB; 6.62 MiB).
Previous APK: 6,876,578 bytes; difference: +61,683 bytes.
SHA-256: `6b9eda79e2ba2b5f2923b2e98ee6084a16dd1b2748264a09c0e563c0e5703e96`.

APK and screenshots are local ignored delivery artifacts, not Git content. Seven 720×1280 native Canvas screenshots are at `tools/localization/screenshots/simulated-api36/`, named with the seven locale tags. They show the same home state and unchanged Arabic surah name inside each translated interface. Unrelated pre-existing SVG audit outputs and measurement changes are excluded from this commit.
