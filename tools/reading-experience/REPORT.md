# Local reading experience

This stage adds only native reading UX and local state. No production dependency, manifest, page endpoint, seed list, audio/payment/hosting architecture or Quran source changes. The twelve frozen references in reference-lock.json match exactly, including the whole renderer, line highlight, polygons/adapter, opening overrides, edition/map, loader/downloader, canonical repository, Gradle file and manifest. verify_stage.py also removes only the newly added cache-management method and checks the remaining controller against its original hash. All 604 original SVG/JSON pairs, packaged notices/catalog and eleven approved Tanzil artifacts verify unchanged. The corpus is not rebuilt.

## Interface and state

Warm ivory surfaces, quiet green text/icons, restrained support-badge gold and shared 12/16/24dp corner tokens keep the index, bookmarks, existing ayah modal, jump dialog and settings consistent. Header/footer navigation, 190ms chrome collapse and 110ms page reveal remain. Compatible status/navigation-bar colors and icon contrast follow the persisted light/dark theme; Android23 keeps a dark navigation surface because dark navigation icons require API26. There are no production zoom/debug controls.

SharedPreferences stores the last successfully drawn artwork page, edition, first-ayah surah context and timestamp. Failed/unavailable page requests never overwrite this position. The home resume action shows the original Arabic surah name and page. Optional cold-start resume does not override SavedStateHandle screen restoration. Defaults are manual resume, bars shown and keep-awake off.

ReadingBookmark is a small local edition/page record with optional canonical surah/ayah context and a creation timestamp. One record exists per page/edition; newest records appear first. Footer and existing modal share add/remove behavior, with a separate lightweight native ListView for jump/removal. Bookmark actions are enabled only after the requested page actually displays, preventing stale-page selection during a transition. Invalid canonical IDs, out-of-range pages, other editions and malformed rows cannot become bookmark targets. Canonical Ayah and SQLite remain unchanged.

The index reuses its recycled ListView and adds name-only filtering. Folding applies only to surah-name/query comparison, never canonical text. Leaving the index hides the search keyboard. One existing centered jump dialog switches among page 1–604, juz 1–30 and surah 1–114 using native controls. Official canonical juz starts resolve through QuranSvgEdition.pageForAyah. Juz 7 begins at 5:82 on artwork page 121; that page starts at 5:78, so its header correctly labels the initial juz 6. Tanzil PageStart is never used for artwork navigation.

Settings persist keep-awake only on the reader screen, optional automatic resume and initial chrome visibility. Disk size is calculated on an IO coroutine and formatted in the Arabic locale. Confirmed cache clearing invalidates pending reader work and waits behind both workers before removing only cacheDir/mushaf-pages/current-edition. The temporary clear state prevents reader re-entry until completion. Seeds, all geometry, SQLite, unrelated caches, bookmarks and last-read preferences are preserved. Transfer/hash/priority/memory methods remain unchanged.

Support and about reuse sections in the existing settings ScrollView, avoiding additional screen infrastructure. Support explains free use and future optional maintenance support with a non-interactive coming-soon badge. About shows actual app/version, Tanzil, independent Quran.ws and its documented upstream KFQC Hafs1441 provenance, plus existing notices/licenses. Privacy is explicitly under preparation with no fabricated URL. Unconfigured unavailable pages use a short Arabic availability message; ordinary configured failures retain retry. No endpoint, payment option or new network functionality is added.

The centered ayah modal keeps its approved text, copy/share, disabled playback/tafsir and bounded long-text scrolling. Only an add/remove bookmark action and shared corner styling are added. Selection and hit testing remain immutable.

## Validation

The final build, test and lint command succeeds:

```sh
./gradlew --continue -PnativeSvgPrototype=true -PreaderUiDeviceTests=true \
  :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest \
  :app:lintDebug :svg-native-prototype:testDebugUnitTest
python3 tools/reading-experience/verify_stage.py
```

All 105 executions pass: 92 app and 13 shared-renderer, zero failures/errors/skips. Robolectric exercises API23 and36. Fourteen added executions cover persisted options/timestamps, unique bookmark replacement/removal, invalid IDs/corrupt rows, all 30 edition-resolved juz targets, resume/search/bookmark/modal/jump flows, hidden entry bars/keep-awake, restoration precedence and confirmed cache clearing without loss of state/assets. Existing canonical equality, five key IDs, full corpus, edition boundaries, copy/share/long-text modal, gesture/zoom/hit-testing and on-demand integrity/offline/memory regressions pass.

Lint has zero errors and 15 nonblocking warnings, recorded individually in validation.json. They concern portrait compatibility, test-library update, compact modal weight, background overdraw, unused UI tokens/legacy strings, intentional chrome wrappers, settings hierarchy size, search autofill hint, a local bookmark label concatenation and an optional existing KTX suggestion. No rule is suppressed or dependency updated to hide a warning. API36 device-level execution is not claimed; SDK36 evidence is Robolectric/native-Canvas simulation.

Five final SDK36 native-Canvas captures are in screenshots/simulated-api36: index/resume, bookmarks, reader, multi-mode jump, and the full settings/support/about content. They are automated offscreen simulation captures, not phone screenshots. Full settings content is rendered beyond the normal scrolling viewport for review. These temporary PNGs are ignored by Git.

The signed Debug APK is 6,837,509 bytes (6.84 decimal MB /6.52MiB), only 44,819 bytes larger than the approved 6,792,690-byte on-demand build. SHA-256: 72a6d4ba1af8516ce341351cb791f24d9f515c69dc4a28fad7e5ff739420ca0a. Build-tools verifies v1/v2 signatures; minSdk23 and target36 remain. verify_stage.py confirms exactly eleven unchanged seed SVG, all 604 official polygon JSON, byte-exact canonical/source assets and an empty default base URL. Artifact:app/build/outputs/apk/debug/app-debug.apk. A non-seed uncached page still needs a configured page server; this stage does not configure one.

The native test-only readingExperience entry reuses the existing ReaderUiInstrumentation runner. It expects pinned 121.svg in the app's test-only disk cache, uses no server and verifies real Android bookmark/modal/name-search/juz/settings/cache behavior. It deliberately clears that isolated test cache at the end. Device evidence, when executed, is stored separately from simulation and unit-test counts.

The final native Android23 run succeeds (device-api23.json): timestamped resume, name filtering, page-list bookmark jump, actual platform long press and exact 5:78 text/ID in the bookmark-enabled modal, juz7 navigation to artwork page121, persisted options, reader-only keep-awake and hidden entry bars. Confirmed clearing leaves zero downloaded bytes while preserving bookmarks, last-read state, seed001.svg, official121.json and all6236 canonical ayahs. The installed base.apk matches the final APK's SHA-256 exactly. Five actual UiAutomation compositor captures are in screenshots/device-api23 and were visually reviewed without a blocking system/app window. The first native attempt was inconclusive for clearing because a known software-emulator Process-system boot dialog intercepted the confirmation; the test harness now acknowledges only known boot dialogs with WAIT and validates the actual cache-confirmation message before acting. No application code changed for that retry. CPU-only emulation is not a phone-performance benchmark.

The local Git checkout also contained previously approved reader/modal/on-demand milestones not yet recorded in origin/main. The stage commit records those required foundations together with this UX stage so the current application remains buildable. The frozen hashes above establish the unchanged boundary relative to the actual starting application. No original Quran source asset changes, generated packages, temporary captures, SDK files or credentials are staged; no push is part of this task.

Stop after this stage. Audio, tafsir, Cloudflare, domains, billing, advertising, accounts and sync remain unimplemented.
