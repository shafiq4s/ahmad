# Quiet reader interface

This stage changes host presentation only. The original SVG view, hit testing, highlight geometry/animation, long press, source adapter/polygons, opening overrides, page edition/map, loader/cache, ayah-options implementation/layout, manifest and dependencies match all 12 hashes in reference-lock.json. All 1208 Quran.ws originals and 11 approved Tanzil references also verify unchanged. MainViewModel differs only by one read-only accessor for the existing artwork-edition first-page map.

## Interface

The warm RTL header has a prominent current surah title, a separate juz/page caption and simple index/settings icons with 48dp targets. The quiet bottom bar has previous/next arrows matching the existing Arabic book order, current page out of 604 and one page-entry action. Entry uses a compact centered AppCompat dialog with the existing modal card/fade style; validation remains 1–604. Production zoom/debug buttons are removed; pinch and swipe handling are unchanged.

A blank tap uses a 190ms native animator on both bar heights and alpha. At completion the bars are GONE, letting the existing fit-center view use more space. Reversal starts from the current fraction. Restoration leaves the whole page below/above the bars without covering artwork. The existing onSizeChanged updates the matrix/inverse for the new viewport; there is no aspect-ratio change or source-coordinate alteration. Page reveal uses only a short 110ms host-view alpha animation, with no extra load wait or cache changes.

The original ListView/recycling remains. Rows show the original Arabic name/number/ayah count/classification, a subtle divider and the first page of the current Quran SVG edition. Settings keep the implemented local dark switch and source/license notices; sections add pinch guidance, actual BuildConfig version and a concise source summary. The current centered ayah modal already shares the quiet palette and rounded-corner family and is unchanged, including exact canonical text-only Copy/Share.

## Verification

The final debug APK and instrumentation APK build successfully. All 67 executions pass: 54 app and 13 shared-renderer, with no failures/errors/skips. Robolectric exercises API23 and API36. Existing regression covers the five canonical IDs (1:1, 1:7, 2:282, 5:78 and 114:6), original text/edition page, short/long modal scrolling, Back/outside/close, clipboard/Share payloads, single/long/blank gestures, 1x/2x/4x hit testing, pinch, RTL swipe and cache bounds. Four new executions verify all 114 edition first-page labels, actual RTL rows, page121 metadata, removed zoom resources, minimum touch targets, no bar overlap, partial/reversed height animation, enlarged viewport with unchanged aspect ratio and ID, restored viewport, and no additional SVG parsing caused by bar toggles.

Lint has zero errors and nine warnings: portrait restrictions (2), newer test-library version, optional KTX suggestion, intentional wrap-content/weight for the unchanged ayah modal, root background overdraw, legacy unused page/juz label, and two bar wrappers retained to clip their full-height contents during collapse. No lint suppression or dependency update was introduced.

The signed debug APK is 141,791,033 bytes (141.79 MB / 135.22 MiB), SHA-256 957cd4fdd74904b8ef7144f160a1df3f32790620180dd7eb3e5be1ef6976c68f. Android build-tools verifies v1/v2 signatures; the manifest remains minSdk23/target36 with no INTERNET permission. The APK is app/build/outputs/apk/debug/app-debug.apk.

SDK36 native-Canvas Robolectric captures are in screenshots/simulated-api36/. They are simulation evidence, not physical-device captures. API36 software-emulator boot did not finish after more than 20 minutes; package-manager service was unavailable, so that install attempt is not reported as a passed device run. The final current APK installs successfully on the API23 emulator. The native harness passes bar hide/restore, page-entry dialog and settings, then all five canonical cases, exact text/clipboard, real system Back that retains the reader, and native scrolling of 2:282. The five PNGs in screenshots/device-api23/ are actual UiAutomation compositor captures, visually reviewed with no app/system dialog covering the interface. See device-api23.json, canonical-device-api23.json and validation.json. Software CPU emulation is not used as a physical-device performance benchmark.

Run the Android UI harness without adding a dependency or changing Gradle:

```sh
./gradlew -PnativeSvgPrototype=true -PreaderUiDeviceTests=true :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :svg-native-prototype:testDebugUnitTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e designScreenshots true com.ahmad.quran.test/com.ahmad.quran.ReaderUiInstrumentation
```

Retrieve app-private files/reader-design-device.json, reader-ui-device.json and design-*.png with adb exec-out run-as. Temporary screenshots are ignored. No audio, networking, Cloudflare or new feature follows this stage.
