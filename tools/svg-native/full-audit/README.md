# Full Hafs/KFQC final validation gate

This is an audit and a five-page standalone APK, not main-app integration.
Pinned upstream: `b4155f07e4aea087d2c458069acff9f7c7e749f6`.
Edition: `quran_ws_hafs_kfqc_b4155f0`. Only full pages 001..604 count as pages;
surah crops and other mushafs are excluded.

## Repeatable cloud audit

Use the existing `/workspace/ahmad` checkout. Setup does not change Android runtime
dependencies, system packages, the frozen corpus, networking or source artwork.
`setup_tools.py` verifies official PyPI wheel digests in `tool-lock.json` and the
Debian package SHA-256, installing only under `/workspace/.tools`. Python3.12/x86_64
and existing Pillow/libcairo/librsvg libraries are required. These are audit tools.

```bash
cd /workspace/ahmad
python3 tools/svg-native/full-audit/setup_tools.py
export PYTHONPATH=/workspace/.tools/svg-audit-python
export PATH="/workspace/.tools/librsvg-audit/usr/bin:$PATH"
python3 tools/svg-native/full-audit/download.py
python3 tools/svg-native/full-audit/independent.py
python3 -m unittest discover -s tools/svg-native/full-audit -p 'test_*.py' -v
python3 tools/svg-native/full-audit/sizes.py
```

`download.py` verifies existing manifest hashes without silently re-baselining them.
Source data and deterministic ZIP measurements stay in ignored `../local-fixtures/`,
outside APK asset roots. Manifest scope: 1,208 originals plus 612 audit prerequisites
(604 Brotli files, markers/surah JSON, three Python scripts and three counting files).
The 118 surah crops are absent and not audited or counted.

Unmodified upstream command; preserve its raw output, JSON and exit status:

```bash
python3 tools/svg-native/local-fixtures/full-audit-source/tools/audit_ayah_polygons.py \
  --mushaf hafs --pages 3-604 --tier all --workers 4 --max-detail 100000 \
  --json tools/svg-native/full-audit/upstream-report.json \
  > tools/svg-native/full-audit/upstream-stdout.txt 2>&1
```

Current upstream exit code is 1, not PASS; see `upstream-diagnosis.json` and REPORT.
Original tools are unpatched. Opening pages are intentionally outside its scope.
`independent.py` imports no upstream generator code. It checks artifact hashes,
read-only Tanzil identity/sequence, original-point nonzero SVG fill, GEOS area
intersections and root viewBox limits. SVG numeral and JSON medallion positions use
the pinned documented 8-unit correspondence tolerance; both must hit the same ayah.
Self-crossing/self-touching opening rings use original-point winding, never repair,
snap, buffer, coordinate changes or new overrides.

## Standalone device validation

```bash
export JAVA_HOME=/workspace/.tools/jdk-21
export ANDROID_HOME=/workspace/.android-sdk
export ANDROID_USER_HOME=/workspace/.android-user
export GRADLE_USER_HOME=/workspace/.gradle
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd /workspace/ahmad
python3 tools/svg-native/prepare_fixtures.py
./gradlew -PnativeSvgPrototype=true :svg-native-prototype:assembleDebug \
  :svg-native-prototype:assembleDebugAndroidTest \
  :svg-native-prototype:testDebugUnitTest :svg-native-prototype:lintDebug
adb devices -l
```

APK: `svg-native-prototype/build/outputs/apk/debug/svg-native-prototype-debug.apk`.
Package `com.ahmad.quran.svgnative`, min23/target36, portrait, no INTERNET permission,
release disabled. Exactly five SVG/JSON pairs; no raster page assets, Open Mushaf
data or full-corpus artwork. AndroidSVG1.4/Apache notice and lock are unchanged.
No physical phone is connected: Pending.

Short test on a modern physical phone:

1. Install/open in portrait. Panel should show HW on API>=26.
2. Cache1: visit 1,2,48,121,604 and back for at least two minutes. Test 1x/2x/4x,
   pinch and long press, especially 2:282, page121 5:78 and final 114:6. Inspect
   blur, missing marks, incorrect transforms/touches, unintended clipping,
   flicker and blank pages. Capture the measurement panel.
3. Switch to cache3, repeat the same sequence for two minutes, capture again.
   Background the app to save the raw report.

The panel shows first-content, warm median/P95, parse/draw, observed PSS maximum
and current-request hit/miss counts. First-content is activity/request to first
View draw completion, excluding OS launch and compositor presentation. Cache-hit
parse time is zero. Long-press dispatch excludes the intentional OS timeout. PSS
is sampled every three seconds on the worker; the observed maximum can miss brief
peaks. Device/mode/transition/press/PSS rows save as `files/device-validation.json`.
With ADB, extract after backgrounding:

```bash
adb exec-out run-as com.ahmad.quran.svgnative cat files/device-validation.json \
  > tools/svg-native/full-audit/device-validation.json
```

Cache3 retains current and available neighboring sample documents (only two at
the list ends). Entries are pruned before parsing; prefetch follows first draw;
capacity never exceeds three. Cache1 releases old UI content before parsing.
Mode changes reset the cache. Device measurements must decide if cache3 helps.
Pending long presses are cancelled on page replacement. API<26 software fallback
and API>=26 hardware policy are unchanged. Actual drawing-Canvas hardware state
is recorded separately from hardware-window state. No cutoff or override changed.
