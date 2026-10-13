# Fitted reader interaction / visual polish

## Final behavior

Every short tap inside the page toggles the existing upper/lower reader chrome, including Quran text; it never creates selection or opens options. Android touch slop cancels taps/holds after movement. A 60dp predominantly horizontal one-finger swipe navigates in existing book order: right advances, left returns, in every interface language. Small/subthreshold swipes and vertical drags do not become clicks. Additional pointers invalidate the touch sequence and cannot navigate, zoom or pan.

A stationary 500ms hold selects the original canonical ayah and hides reader bars. Releasing before 900ms keeps selection with no popup. Continuing to 900ms opens the one existing centered options dialog, for the ID captured through the original hit test. Raw screen coordinates distinguish finger motion from the View repositioning that happens when the chrome collapses. Page replacement, cancellation, movement, extra pointers, detach and activity pause cancel timers. Closing the modal retains the approved fade/selection clear and does not automatically restore the bars. Another tap restores them. Chrome animation remains 190ms; highlight 140ms and geometry/green remain exact.

There is no production ScaleGestureDetector, pinch/pan path or user zoom control. Every displayed page starts at its automatic fit. The existing low-level programmatic scale/matrix helper remains **only for geometry validation and the separate historical debug prototype**; production input never writes scale/focus. Keeping that helper avoids changing the approved SVG render implementation or sacrificing prior geometry/scaling regressions. The main reader never exposes it. Accessibility/keyboard activation uses performClick to toggle reader controls; the page is focusable/clickable.

Top header minimum height is 72dp, translated titles can wrap to two lines, and long home buttons use content height with 56dp minima. Icon 48dp targets/colors/design tokens are retained. Page margin is 4dp outside the unchanged white SVG View; aspect ratio/renderBox/native inverse stay unchanged. Loading cards use 20dp padding/margins matching sections. No new theme, decoration, audio, tafsir, payment, account, network host or library is added. Seven locales remain supported including provisional Hazaragi; only two gesture/accessibility messages per locale changed, replacing obsolete zoom instructions.

## Verification

Full build: Debug APK, Android-test APK, unsigned Release APK and Release AAB passed. App: 150 passed; shared native prototype: 13 passed. No failures/errors/skips. Twenty added API 23/36 test executions cover time boundaries, early release, repeated popup prevention, movement/swipes/cancel, raw-coordinate relayout, ignored pinch, click accessibility, real main-modal/chrome/dismiss flow, all seven locales, and 15 source page/viewport combinations per API. Existing corpus/read-only/search/copy/share/bookmark/history/cache/pagination regressions remain enabled. Search `الحمد لله` retains 23 results.

Lint debug: 0 errors/11 warnings; release: 0 errors/11 warnings. Existing orientation/API-attribute/layout/style/update advisories remain; no lint suppressions or dependency/build flags were added.

Runtime checks are **Robolectric API 23/36 with native Canvas on a Linux desktop**, not a new physical device or emulator run. Native animation shadows can advance the test clock, so early-release tests use the actual absolute uptime deadline rather than assuming accumulated idle calls equal elapsed hold time. Real-device review remains needed for reading comfort on small phones, touch timing/drag feel, device SVG draw/PSS and system-inset behavior. There are no claims of Android latency improvements from desktop numbers.

All protected canonical/repository/edition/override/polygon/download/cache/settings/locale/modal references match their recorded SHA256. Eight renderer/matrix/hit/highlight method bodies are identical. All 11 packaged SVG seeds and 604 JSON/source/catalog/license files are exact, as are Tanzil assets in debug/release/AAB. Default base URL stays empty. No screenshots, fixture captures or experimental page archives enter assets. Release already used R8/minification/resource shrinking and retains them; there was no accidental test-asset packaging to remove. Output packaging is refreshed without deleting source assets or unrelated workspace backups.

## Desktop observations

| Simulated API | Samples | Median direct draw ms | Median hit test µs | Max retained documents |
| --- | ---: | ---: | ---: | ---: |
| 23 | 15 | 9.96 | 24.16 | 2 |
| 36 | 15 | 21.05 | 24.08 | 2 |

Draw/hit figures are desktop observations, not end-to-end touchscreen latency, Android GPU performance or phone memory. Renderer/parser/cache architecture is unchanged. Removing two gesture recognizers removes their per-event processing; no unmeasured speedup percentage is claimed. Only the existing bounded raw disk prefetch and three-document parsing cache are used; home does not parse the corpus or 604 SVGs.

## Actual artifact sizes

| Variant | Before bytes | After bytes | Difference |
| --- | ---: | ---: | ---: |
| debug | 6,938,261 | 6,939,140 | +879 |
| release | 3,318,346 | 3,318,930 | +584 |
| aab | 3,906,183 | 3,906,349 | +166 |

Sizes use the current seven-language build as baseline, not the historical 141.8MB full-SVG prototype. APK/AAB contents remain 11 seed SVGs + all 604 polygon JSON + unchanged canonical data. Release APK/AAB are unsigned, with no publishing/signing/store changes. Current hashes/paths and byte differences are in validation.json. Binary outputs and native screenshots stay ignored; only source/tests/small reports are committed.

Native Canvas screenshots at `screenshots/simulated-api36/`: reader-bars-visible, reader-bars-hidden, ayah-highlighted (all 15 lines of 2:282), ayah-modal (long scrollable canonical text), home, settings. They are local test captures, not device screenshots. Historical measurement/audit changes and the handoff/source-backup folders are excluded from this commit.
