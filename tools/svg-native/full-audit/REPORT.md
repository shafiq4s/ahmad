# Quran.ws final validation gate report

Date: 2026-10-11. Retrieved at 2026-10-11T10:02:57.117603+00:00. Full audit plus standalone five-page APK; no main integration.

Pinned source: quran-ws/quran-svg commit `b4155f07e4aea087d2c458069acff9f7c7e749f6`, Hafs/KFQC, edition `quran_ws_hafs_kfqc_b4155f0`. Existing provenance, source/license and AndroidSVG locks are unchanged.

1. **Files:** 604/604 full SVGs and 604/604 polygon JSONs, original bytes. [Manifest](manifest.json) records hashes, counts and URLs; all rechecks match. All ten sample hashes and eleven Tanzil reference hashes match. Surah crops are excluded.

2. **Raw SVG size:** 372,827,329 bytes (355.56 MiB). JSON adds 1,151,611 bytes; combined 373,978,940 bytes.

3. **Actual compressed size:** SVG-only DEFLATE9 ZIP 95,050,763 bytes (90.65 MiB); compressed payload 94,969,805, ZIP overhead 80,958. SVG+JSON ZIP 95,409,502 bytes (90.99 MiB). Official Brotli SVG total 63,254,130 bytes. Full measurements, not APK/AAB installation estimates; [sizes/hash evidence](sizes.json).

4. **Unmodified upstream audit: FAIL, exit1.** All tiers requested on3–604. [Raw stdout/stderr](upstream-stdout.txt) and [JSON](upstream-report.json) retain698 findings:602 MARKERCOUNT and96 UNCOVERED. Identity and full-page files have no findings. The pinned marker regex expects an unadorned group with translate+scale; source ornaments now have a class and translate+scale+normalization matrix, so it sees zero medallions. Failed reference generation leaves decoration empty; all96 uncovered pages coincide exactly with96 surah-start pages, counting unnumbered headings/basmalahs as gaps. See [diagnosis](upstream-diagnosis.json). This diagnosis does not convert FAIL to PASS. Marker-derived boundary checks could not effectively run. No upstream tool was patched. Cropped variants are absent/outside full-page scope.

5. **Independent audit: PASS.** No upstream generator code imported. Read-only identity/sequence, artifact hashes, original-point SVG nonzero fill, exact GEOS intersection area, root viewBox bounds and marker hit ownership checked. Both JSON medallion and SVG numeral positions hit the same ayah,6236 cases. Distinct anchors use pinned documented8-unit correspondence tolerance; observed maximum5.344539 units. No snapping or repair; [full results](independent-report.json).

6. **Canonical identities:**6236 across114 surahs, all present; no phantom, missing, out-of-range ID. Page121 starts5:78; page604 ends114:6.

7. **Duplicates/continuations:**6236 appearances, no repeated identities, continuation seams or unexplained duplicates in this pinned Hafs edition. Adjacent last/first repeated IDs would be separately reported as continuations, not new canonical verses.

8. **Overlap:** zero positive-area overlaps on3–604 and zero after unchanged opening overrides across604. 8,315 different-ayah pairs share edges/points with area0, not failures. Original openings have6 positive intersections and2 self-crossing rings. Nonzero fill interprets the local self-touching2:2 ring without changing its points.

9. **Opening pages: PASS, unchanged overrides.**1:1..7 and2:1..5. Zero positive ambiguity, uncovered or double-assigned numbered ink. Native6x:101503/108093 numbered ink pixels;7338/18015 unnumbered heading/basmalah pixels, all unassigned. All12 markers match. Independent existing source-render diagnostics pass. Override hash remains `f9cdec09bb5abae526d5ce59aaa2fff9eaa5e3f26107f02c69352622dd258b22`; [OPENING_OVERRIDES.md](../OPENING_OVERRIDES.md) retained unchanged.

10. **Independent map:** all604 physical first/last IDs and continuation fields in [JSON](edition-page-map.json) and [CSV](edition-page-map.csv). Tanzil database/PageStart untouched.

11. **Pagination:** 25 starts and 25 completed ends differ; their union contains 36 affected pages. Fair end compares the last completed SVG ayah to Tanzil `endOrdinal-1`. No continuation exists here. This is edition pagination, with no missing or extra canonical verse. The earlier Open Mushaf figure concerned a different edition and comparison; do not reuse it as Quran.ws's start-difference count. Complete differences:

| Page | Quran.ws first | Tanzil first | Quran.ws last completed | Tanzil last assigned | Start differs | End differs |
| --- | --- | --- | --- | --- | --- | --- |
| 120 | 5:71 | 5:71 | 5:77 | 5:76 | False | True |
| 121 | 5:78 | 5:77 | 5:83 | 5:82 | True | True |
| 122 | 5:84 | 5:83 | 5:90 | 5:89 | True | True |
| 123 | 5:91 | 5:90 | 5:95 | 5:95 | True | False |
| 144 | 6:125 | 6:125 | 6:130 | 6:131 | False | True |
| 145 | 6:131 | 6:132 | 6:137 | 6:137 | True | False |
| 531 | 54:50 | 54:50 | 55:18 | 55:16 | False | True |
| 532 | 55:19 | 55:17 | 55:41 | 55:40 | True | True |
| 533 | 55:42 | 55:41 | 55:69 | 55:67 | True | True |
| 534 | 55:70 | 55:68 | 56:16 | 56:16 | True | False |
| 564 | 67:27 | 67:27 | 68:16 | 68:15 | False | True |
| 565 | 68:17 | 68:16 | 68:42 | 68:42 | True | False |
| 567 | 69:9 | 69:9 | 69:35 | 69:34 | False | True |
| 568 | 69:36 | 69:35 | 70:10 | 70:10 | True | False |
| 569 | 70:11 | 70:11 | 70:40 | 70:39 | False | True |
| 570 | 70:41 | 70:40 | 71:10 | 71:10 | True | False |
| 575 | 73:20 | 73:20 | 74:18 | 74:17 | False | True |
| 576 | 74:19 | 74:18 | 74:47 | 74:47 | True | False |
| 583 | 78:31 | 78:31 | 79:16 | 79:15 | False | True |
| 584 | 79:17 | 79:16 | 79:46 | 79:46 | True | False |
| 585 | 80:1 | 80:1 | 80:40 | 80:42 | False | True |
| 586 | 80:41 | 81:1 | 81:29 | 81:29 | True | False |
| 587 | 82:1 | 82:1 | 83:4 | 83:6 | False | True |
| 588 | 83:5 | 83:7 | 83:33 | 83:34 | True | True |
| 589 | 83:34 | 83:35 | 84:24 | 84:25 | True | True |
| 590 | 84:25 | 85:1 | 85:22 | 85:22 | True | False |
| 591 | 86:1 | 86:1 | 87:10 | 87:15 | False | True |
| 592 | 87:11 | 87:16 | 88:22 | 88:26 | True | True |
| 593 | 88:23 | 89:1 | 89:22 | 89:23 | True | True |
| 594 | 89:23 | 89:24 | 90:18 | 90:20 | True | True |
| 595 | 90:19 | 91:1 | 92:9 | 92:14 | True | True |
| 596 | 92:10 | 92:15 | 94:2 | 94:8 | True | True |
| 597 | 94:3 | 95:1 | 96:12 | 96:19 | True | True |
| 598 | 96:13 | 97:1 | 98:5 | 98:7 | True | True |
| 599 | 98:6 | 98:8 | 100:5 | 100:9 | True | True |
| 600 | 100:6 | 100:10 | 102:8 | 102:8 | True | False |

12. **Text/identity:** no detected identity/sequence defect; frozen Tanzil text/database/source artifacts remain byte-identical. SVG outlines do not expose lossless Unicode verse text; no claim of visually transcribing/proofreading all604 pages. Hashes certify pinned bytes; this audit certifies identity/geometry.

13. **Physical modern device: Pending.** No connected phone via ADB. Standalone APK ready; short manual steps in [README](README.md). No emulator substituted for this gate.

14. **Device Android/API/model:** unavailable; no invented data. Fresh local tests coverSDK23/36. Prior Android6 VM evidence retained; updated APK not physically tested this gate.

15. **Hardware:** Pending on modern phone. Policy unchanged: software View belowAPI26 for previously observed Android6 glyph blur, default hardwareAPI26+. Actual Canvas state recorded separately from window state. No cutoff change.

16. **Cold/warm:** Pending. APK records activity/request-to-first-draw, warm median/P95, parse/draw; excludes OS launch/compositor presentation. Hit parse=0. No host/emulator values used as real-device measurements.

17. **PSS:** Pending. Whole-process PSS sampled every3 seconds; maximum observed sample can miss brief peaks. No instantaneous-peak or cache-only memory claim.

18. **Cache1 vs3:** Pending on device. User toggle; current+available sample neighbors pruned before loading, old UI released, prefetch after draw, at most3 documents. Regression covers edges/capacity. Emulator benefit alone does not justify production choice.

19. **Zoom:** local native-Skia1x/2x/4x captures/tests pass at small/medium/large sizes. Real hardware sharpness, glyphs, clipping/flicker Pending. Same matrix/inverse; no RTL reflection.

20. **Long press:** local GestureDetector identity/scaling pass, including2:282,5:78,114:6. Ordinary tap does not select; all selected ayah polygons highlight together. Pending press cancelled on page replacement. Real modern-device precision/latency Pending; measured dispatch excludes OS timeout.

21. **Option A:** still recommended as a candidate: unchanged direct SVG, official polygons and opening-only overrides; Native Android/Kotlin, AndroidSVG1.4 only. Three-document cache remains provisional pending real measurements.

22. **Main integration:** not ready. Need compatible verified marker-derived full audit and real modern-device quality/performance/PSS/cache evidence. No integration or full-page release performed.

23. **Removal after successful approved integration:** old overlay-prototype, Open Mushaf images/aya fixtures and preparation/validation tools, old main debug image activity/provider, marker-estimated maps and old MushafTouchOverlayView. Later remove raster svg-prototype after moving neutral polygons/parser and useful tests. Retain frozen Quran data/models/repository, edition contracts, regression coverage, source hashes/provenance/licenses, opening rationale and CHANGELOG. Nothing deleted now.

**Build/tests:** debug and androidTest APKs built. 82 Android unit executions passed:main51, old overlay11, old SVG9, native11; zero failures/errors/skips. Unchanged suites reused Gradle outputs; native ran fresh onSDK23/36. Four audit regressions passed. Lint main0 errors/4 warnings; native0 errors/3 warnings. Upstream failure separately reported above.

**Standalone APK:** 2,453,941 bytes; SHA256 `7a0fc70531703e7dbf0566bc46a2b95c33b6da78dd29ae149b1873561e0adcb6`. Only001,002,048,121,604 original SVG/JSON; no604-page/raster/Open Mushaf assets. Min23/target36, portrait, release disabled, no INTERNET permission. AndroidSVG AAR SHA256 `02a5b08a2b35d2d58eb2eaca9d84ac00fb341da725fdbd653ea3ed130437e95a` and Apache2 notice remain locked. Cloud audit tools are not Android dependencies.

**Verdict: NOT_READY_FOR_MAIN_INTEGRATION.** Reasons: pinned upstream marker-parser incompatibility leaves a failed official audit; physical modern-device validation is Pending. Stop at report; no extra features or integration.
