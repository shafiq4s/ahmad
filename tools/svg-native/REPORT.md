# Direct native SVG validation report

Date: 2026-10-11. Recommendation: **Option A, direct SVG**, as the next production candidate; main integration is still gated on user approval and actual API 36 device/accelerated-VM validation. This is not approval of all 604 pages.

## Source and isolation

Pinned commit: `b4155f07e4aea087d2c458069acff9f7c7e749f6`, selected Hafs/KFQC assets equal stable v1.1.1. Independent edition: `quran_ws_hafs_kfqc_b4155f0`. The approved source lock records original retrieval at 2026-10-11T02:21:35.127490Z, byte counts, SHA-256 and provenance. Native preparation copies the same verified originals, without editing or raster conversion.

Only these source pairs are used:

- `mushafs/hafs/kfqc/svg/001.svg` and `json/001.json`
- `mushafs/hafs/kfqc/svg/002.svg` and `json/002.json`
- `mushafs/hafs/kfqc/svg/048.svg` and `json/048.json`
- `mushafs/hafs/kfqc/svg/121.svg` and `json/121.json`
- `mushafs/hafs/kfqc/svg/604.svg` and `json/604.json`

Preserved LICENSE, NOTICE.md, docs/PROVENANCE.md and CC-BY notice from the pinned source remain alongside the assets. Artwork originates in KFQC's `1441-AI-hafs.zip`, according to the upstream provenance; its archive checksum is recorded upstream, not independently verified in this trial. Artwork terms are separate from Quran.ws polygons (CC-BY 4.0 with stated in-product attribution waiver) and tools (MIT). No new source-rights decision supersedes the user's accepted provenance report.

Opt-in module `svg-native-prototype` uses Android/Kotlin, minSdk 23, target 36, portrait. Release is disabled; the main app has no new runtime dependency or reader integration. It packages five original SVG/JSON pairs, frozen Tanzil assets and notices; no WebP, aya.json, Open Mushaf artwork, Engine, WebView or INTERNET permission. Only neutral polygon contracts/parser are shared from the earlier prototype, not its raster renderer. `OpeningPageHitOverrides` is a separate local adapter. Callbacks return only canonical AyahId, not page/word identity.

## Six local overlap fixes

See [OPENING_OVERRIDES.md](OPENING_OVERRIDES.md) for **each original intersection bounds, chosen split and visual reason**. Page 1's three overlaps arise from small slanted/interline bands and a same-row marker join. Page 2 requires shaped notches to retain the damma of unzila in 2:4, the meem descender of hum in 2:4 and the sila waqf sign in 2:5. Horizontal cuts alone would misassign ink. An additional tiny final-line extension covers the original omitted meem stroke in 1:7.

Original source data are untouched. The non-overlapping local regions highlight whole ayahs, including both polygons of 1:6. Unnumbered page-2 basmalah/headings are unassigned. Pages 3..604 bypass overrides and use their official geometry; the guard is tested across that entire numeric range, without downloading those pages.

Both opening pages now have zero positive-area native path intersection and zero ambiguous half-unit-grid hits. Native 6x source rendering checked 101,503 numbered ink pixels on page 1 and 108,093 on page 2: **zero uncovered ink, zero ambiguous ink**. Existing pinned source-render diagnostics independently found zero gaps/ambiguity. This is pixel-scale validation of this pinned sample, not proof for arbitrary future sources.

## Identity and visual checks

- All 34 original sample AyahIds are present in frozen Tanzil SQLite; no invalid IDs or out-of-bounds polygons.
- Page 121 starts **5:78**, visually confirmed in the SVG. Existing Tanzil PageStart remains **5:77**. This is an independent edition-map difference, not an invalid ID or automatic Quran-text error.
- Page 48 contains the single long ayah **2:282**; page 604 ends **114:6**, with surahs 112, 113 and 114.
- All ten original SVG/JSON SHA-256 values and all eleven approved Tanzil reference hashes match.
- The five native-Skia captures preserve the source line/word order and page boundaries; no reflow or textual reconstruction. Vector captures at 4x zoom remain sharp.
- 34 markers plus eight reviewed boundary/ink points were tested with native GestureDetector at three sizes (320x480, 412x732, 600x960) and zoom 1x/2x/4x on simulated API 23/36. Long press returns the canonical identity; ordinary tap produces no selection callback. Same matrix/inverse is used for drawing and touches; no coordinate RTL reflection. Some mathematical zoom probes are off the viewport; this is transform validation, not a production panning test.
- Actual API 23 instrumentation painted all five pages and verified real Handler-based long presses on 1:7, 2:5, 2:282, 5:83 and 114:6. An attached activity capture and adb long press also confirmed full 1:7 selection.

### Actual API 23 display correction

A real API 23 activity capture initially exposed hardware path-cache blur in the source's tiny transformed glyph paths. Offscreen software captures alone had missed it. The view now requests a native software layer below API 26. The same vectors redraw at the current zoom; no pre-rendered page asset is created. Corrected attached activity capture is sharp. The OS viewport layer can cost about 7.44 MB at 1080x1722, plus compositor/driver allocations. Attached one-page activity PSS snapshot: **16,455 KiB (16.07 MiB)**; this is not a peak or comparable to the heap sums below.

## Dependency

Only runtime addition: `com.caverock:androidsvg-aar:1.4`, pinned, Apache-2.0. Copyright 2013 Paul LeBeau, Cave Rock Software Ltd. AAR **202,395 bytes**; embedded classes.jar **222,137 bytes**. AAR SHA-256 `02a5b08a2b35d2d58eb2eaca9d84ac00fb341da725fdbd653ea3ed130437e95a` verified against Gradle's actual artifact. License copy bundled and recorded in `androidsvg-lock.json`. AAR size is not an APK increment. No AndroidX instrumentation library was added.

## Primary measurements: actual Android 6 VM

Software CPU emulation, x86, no KVM; offscreen native Canvas 1080x1722. These are not physical-phone frame-rate estimates. Cold content timing includes repository/loader setup and first draw, not launcher/compositor startup. Host load affects absolute values: observed cold results across repeats range **0.99–1.81 seconds**. Final run:

| Metric | One document | Three-document LRU + neighbor prefetch |
| --- | ---: | ---: |
| Warm transition median | 671.81 ms | 134.26 ms |
| Warm transition P95 | 849.05 ms | 324.26 ms |
| Render median | 130.70 ms | 129.28 ms |
| Retained heap delta | 714,112 bytes (0.68 MiB) | 1,512,976 bytes (1.44 MiB) |
| Observed heap peak, including baseline | 19,344,328 bytes (18.45 MiB) | 21,128,968 bytes (20.15 MiB) |
| Observed peak above baseline | 5.76 MiB | 7.59 MiB |

- First content: **1812.59 ms**; first-page SVG parse **491.49 ms**; first render **157.59 ms**.
- Long press DOWN-to-callback **601.80–602.70 ms**: includes Android 6's 500ms long-press timeout plus 100ms tap timeout.
- Actual selection dispatch **0.398–0.796 ms**; standalone polygon lookup median **0.144 ms**.
- Heap scope is Runtime used managed heap + Debug native malloc allocation. The **7,439,040-byte destination bitmap is already in the baseline**. Observed peak samples every 5ms; it excludes an attached view's layer, compositor/GPU, mapped code and other PSS categories. It is not an absolute process peak. Separate PSS snapshot above covers an attached page-1 activity; heap trials use page 48 and neighbors 2/121.
- Three-page prefetch runs after content draw and is excluded from current-page content-ready time, but still costs CPU. Measurements use the five-page sample sequence, not a complete navigation corpus. Prototype UI defaults to one document; the benchmark exercises both policies. Production candidate recommendation is background parse/prefetch plus at most three retained documents, pending device validation. Three retained documents cost about **0.76 MiB** extra in this trial, while reducing measured median switching about **5.0x**. Do not preload the Quran or all pages.

## API 36 result and limitations

Robolectric API 36 with native Skia passed functional rendering, geometry, long press, sizes and zoom. Host-JVM example: cold **364.85 ms**, one/three document warm medians **40.75/22.35 ms**. These are host-JVM values, not Android 16 CPU/memory performance; never compare them numerically with the API 23 VM as if they were two phones.

Full official API 36 VM instrumentation **did not complete**. Before app tests, default x86_64 revision 2 (`BE2A.250530.026.D1`) repeatedly lost system_server to Watchdog while ClassLinker waited on HeapTrim/GC for ~70 seconds under TCG. A fallback attempt was rejected by ART's CMC-only heap check; default properties were restored. See `android-36-runtime-diagnosis.txt`. No actual API 36 performance/long-press/attached-hardware rendering claim is made. A stable accelerated API 36 VM or real device is still required before rollout. No current-stage physical phone was available.

## SVG versus raster, measured comparison and recommendation

PNG baseline uses a temporary **in-memory** capture of page 604 at the same viewport; no distributable raster/WebP is produced. Median PNG decode **223.63 ms**, bitmap draw **3.95 ms**, decoded bitmap **7,439,040 bytes (7.09 MiB)**. Raster cached painting is much faster than SVG path drawing in this software VM. It is not a full optimized raster-reader benchmark, and total raster PSS was not measured.

**Recommend Option A: direct SVG + original polygons + opening-only touch overrides.** It preserves original geometry/line arrangement and sharp vector zoom. Retention of three SVG documents is acceptable in the measured trial (1.44 MiB delta), the native Android 6 fallback works visually, and neighbor caching substantially improves switching. The viewport layer still costs memory: do not claim SVG always uses less total RAM than raster. The API 36 runtime limitation must be resolved with device testing before production integration/release. The existing one-page UI and three-page benchmark support evaluating that decision without main-app changes.

Five SVGs total **1,840,601 raw bytes / 654,183 ZIP bytes**; official JSON **6,264 raw bytes**. Historical previous-stage full-mushaf estimates (not newly generated/downloaded): approximately **92–120 MB ZIP/Deflate SVG** versus **150–200 MB lossless raster at width 1440**; PNG may be much larger. These remain sample extrapolations, not exact 604-page sizes. Upstream directory totals include 722 SVGs (604 full pages + 118 crops), not only our 604-page selection. Quality-preserving zoom needs higher raster resolution, increasing memory and download/storage size. No additional 599 pages were downloaded.

## Build, regression and review gate

Build/debug instrumentation APK and Lint pass. **80 passing test executions, zero failures/errors/skips** with the legacy image flag enabled: main 51, old overlay 11, old SVG trial 9, new native SVG 9. Gradle reused unchanged legacy outputs when up-to-date; these are aggregate suite executions, not 80 independent new tests or all emulator tests. Main Lint zero errors/four warnings; native zero errors/three warnings (portrait/API36 advisory plus existing test-library version notice).

Native trial APK **2,446,098 bytes**, debug-only; hash and source/reference evidence in `validation.json`. No full-artwork release or main UI integration, source change, old-prototype deletion, audio, tafsir, word selection, bottom sheet or cloud.

After final replacement approval, removable candidates are old `overlay-prototype`, aya/PNG fixtures/preparation scripts, the old main-app debug image activity/provider, marker-estimated geometry/page maps and `MushafTouchOverlayView` rectangle implementation once no consumers remain. The previous raster-only `svg-prototype` may then be removed after moving neutral parser/contracts and needed regressions. Retain frozen Quran data/models/repository, independent edition contracts, useful regression tests, source hashes, provenance/license reports and CHANGELOG. Nothing was deleted now.

Cloud onboarding saved only updated **start_skill** instructions as a reviewable draft; install script, repositories, network domains and secrets were preserved. Activation requires reviewing/saving the draft and publishing the environment, not publishing the Android app. Fresh-task restoration and actual API 36 VM readiness were not verified. Trial emulators were stopped; SDK/AVDs remain reusable.
