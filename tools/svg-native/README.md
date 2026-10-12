# Direct SVG validation prototype

The user approved physical modern-device rendering and authorized main integration. The current full reader is documented in [../mushaf-svg/REPORT.md](../mushaf-svg/REPORT.md). This optional standalone app still contains five pages and now shares production polygon/renderer sources. Earlier sections below record the prior validation gates and limitations.

The current final validation gate is in [clean-audit/REPORT.md](clean-audit/REPORT.md)
and [clean-audit/README.md](clean-audit/README.md). It reproduces the unmodified
upstream audit from a clean pinned Git checkout, proves both marker-structure
mismatches and all 96 cascading decoration findings, and adds screenshotable
device/session metrics to the five-page standalone APK. Source data and local
opening overrides remain frozen. The official auditor still exits 1; real modern
device testing is Pending.

The later full-corpus gate is documented in [full-audit/REPORT.md](full-audit/REPORT.md)
and [full-audit/README.md](full-audit/README.md). It supersedes sample-only counts:
all 604 pages are now audit fixtures; the APK still packages only five. The APK
exposes cache1/3 comparison, zoom buttons and local device measurements. Physical
modern-device validation is Pending; the pinned upstream auditor has a diagnosed
marker-parser incompatibility and is not reported as passing.

This is an isolated, debug-only Native Android/Kotlin application, not a main-reader integration. Its only runtime dependency is the expressly authorized `com.caverock:androidsvg-aar:1.4` (Apache-2.0). Min SDK 23, target 36, portrait. No WebView, WebP, Engine, word selection, audio, tafsir, downloads or cloud. Release is disabled.

Original source: `quran-ws/quran-svg`, commit `b4155f07e4aea087d2c458069acff9f7c7e749f6`, selected assets equal stable v1.1.1. Reuses the source lock and provenance from `../quran-svg/source-lock.json`. Only `mushafs/hafs/kfqc/{svg,json}/{001,002,048,121,604}.*` are copied. SHA-256 checks run before asset preparation. Source retrieval timestamps and artwork/polygon/tool licenses remain in that lock and preserved upstream notices. AndroidSVG's license is separate from Quran artwork licensing.

```bash
export JAVA_HOME=/workspace/.tools/jdk-21
export ANDROID_HOME=/workspace/.android-sdk
export ANDROID_USER_HOME=/workspace/.android-user
export GRADLE_USER_HOME=/workspace/.gradle
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd /workspace/ahmad
python3 tools/svg-native/prepare_fixtures.py
./gradlew -PnativeSvgPrototype=true :svg-native-prototype:assembleDebug :svg-native-prototype:assembleDebugAndroidTest :svg-native-prototype:testDebugUnitTest :svg-native-prototype:lintDebug --console=plain
```

Install `svg-native-prototype/build/outputs/apk/debug/svg-native-prototype-debug.apk`. Ordinary tap has no action. Long press highlights every polygon of the selected canonical ayah and updates the small `surah:ayah` label. Pinch supports 1x..4x vector zoom; this trial does not implement production panning/navigation gestures. The activity defaults to one retained SVG document; the benchmark separately compares LRU capacities one and three, prefetching neighboring **sample** pages, not nonexistent intermediate assets.

`NativeSvgPageView` renders AndroidSVG directly into a native Canvas. The identical source-to-screen matrix transforms artwork and selection; the inverse performs polygon hit testing. Source coordinates are never mirrored for RTL. Page 1/2 presentation bounds include the source heading outside the root viewBox; artwork is unchanged. The neutral polygon parser/contracts are reused without importing the earlier raster view or Open Mushaf algorithm. Own `OpeningPageHitOverrides` applies only to this edition's opening pages; see `OPENING_OVERRIDES.md`.

Actual API 23 hardware display initially blurred the tiny internally transformed glyph paths, despite clear software-Canvas captures. The view therefore uses a **software layer below API 26** and redraws the vectors at the current zoom. This is native Android drawing, not a pre-rendered page asset or WebView. The OS layer may require approximately one viewport ARGB buffer (about 7.44 MB for the benchmark viewport), with driver/compositor allocations separate. Do not confuse this attached-view cost with the offscreen SVG-document heap measurements below. Before/after API 23 activity captures and process `dumpsys meminfo` are preserved for this check.

## Android runtime measurements

Official Google emulator/system images are locked by byte count and official manifest SHA-1 in `android-runtime-packages.json`; installation helper is idempotent and checks any downloaded archive before extraction:

```bash
python3 tools/svg-native/install_android_runtime.py
export ANDROID_AVD_HOME=/workspace/.android-user/avd
# Run in separate terminals, using the retained manually configured AVDs.
$ANDROID_HOME/emulator/emulator -avd svg-api23 -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot -accel off -gpu swiftshader -feature -Vulkan -no-metrics
$ANDROID_HOME/emulator/emulator -avd svg-api36 -port 5556 -no-window -no-audio -no-boot-anim -no-snapshot -accel off -gpu swiftshader -feature -Vulkan -no-metrics
```

Use hardware acceleration instead where available; this cloud host has no `/dev/kvm`. First-boot dex verification on API 36 is slow. Wait for `adb -s emulator-5556 shell getprop sys.boot_completed` to return 1 before recording timings. The AVDs, SDK and tool caches remain in the cloud workspace; they do not belong in the APK.

**Actual API 36 VM limitation:** the official default x86_64 revision 2 image (`BE2A.250530.026.D1`) repeatedly lost `system_server` to Watchdog/GC stalls with software CPU emulation, before our instrumentation ran. A boot-completed property alone was insufficient: the package service disappeared. A VM-local collector fallback was attempted; ART rejected it with `foreground_collector_type_ == kCollectorTypeCMC`. Collector settings were restored. Do not apply the rejected fallback or publish measurements from this unstable VM. API 36 functional/scaling/gesture coverage and native-Skia captures passed under Robolectric; those are distinct from Android device/VM performance. Actual API 36 instrumentation/performance remains unverified and needs a stable accelerated VM or device. See `android-36-runtime-diagnosis.txt`.

```bash
adb -s emulator-5554 install -r svg-native-prototype/build/outputs/apk/debug/svg-native-prototype-debug.apk
adb -s emulator-5554 install -r svg-native-prototype/build/outputs/apk/androidTest/debug/svg-native-prototype-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w com.ahmad.quran.svgnative.test/com.ahmad.quran.svgnative.NativeSvgInstrumentation
adb -s emulator-5554 exec-out run-as com.ahmad.quran.svgnative cat files/measurements-android.json > tools/svg-native/measurements-android-api-23.json
```

Repeat on port 5556 for API 36. Require `success=true` from instrumentation; process exit alone is not proof. Native captures and selected-ayah captures are in the app's private files directory, retrievable with `adb exec-out run-as ... cat`. Local captures are ignored by Git. Robolectric results label API 23's non-native drawing; actual Android emulator results are primary for performance and memory.

Measurements are to first **drawn content** in a 1080x1722 offscreen native Canvas, not launcher-to-display startup or compositor presentation. Cold timing includes repository/loader setup, page load and first SVG draw. Warm switching uses a warmed process and 15 specified transitions. Three-page prefetch work occurs after drawing and is excluded from current-page content-ready time; it still consumes CPU. Separate parse/render and cache-hit rows remain in JSON.

Memory is used managed heap plus native malloc allocation, not process PSS or total GPU/driver memory. A 7,439,040-byte benchmark destination bitmap is already in the baseline; the actual direct SVG view does not retain a page bitmap. Peak is sampled every 5ms and is an observed estimate, not an absolute process peak. Retention is after repeated GC for cache 1 versus 3 around page 48 with sample neighbors 2 and 121. Host-JVM memory cleanup can yield negative deltas; do not interpret those as Android device measurements.

Raster comparison uses only a temporary in-memory PNG encoding/decoding of the native page 604 capture at the same viewport. It creates no WebP or distributable raster assets. It is a bounded comparison, not a benchmark of an optimized raster production reader or all 604 pages.

The main app, frozen Quran data and all old prototypes remain intact. Do not integrate the chosen renderer or remove old trials until the user approves the report.
