# Canonical ayah bottom sheet V1

One persistent RTL sheet uses Google Material 1.12.0 `BottomSheetBehavior`. A small handle, 24dp upper corners, the existing day/night palette, fixed header/actions and a nested scroll viewport keep the content simple. Initial visible height is 48% of the available window; expansion is bounded to 88%. Bottom padding tracks the offscreen portion of the expanded panel so actions remain visible in both states. The uncovered reader remains available for another long press. There are no stacked dialogs.

## Data and selection

- Identity: the existing `NativeSvgPageView.onAyahLongPress` canonical `AyahId`.
- Text: `QuranRepository.getAyah`, default frozen Tanzil text version, assigned verbatim. No SVG text extraction, normalization, shaping edits, trimming or number suffixes.
- Surah name: the existing surah list originally loaded by `QuranRepository.getSurahs`.
- Juz: existing canonical `getJuzForAyah(DEFAULT_EDITION, surahNumber, ayahNumber)` boundaries. This does not query Tanzil page starts.
- Page/edition: the actual displayed view's `geometry.pageNumber` / `geometry.editionId`, not a Tanzil page lookup. Quran SVG page121 starts 5:78; the frozen Tanzil page121 starts 5:77. The distinction remains unchanged.

Selection lifetime is the only change to the native renderer: the activity holds its selection while the sheet is open and invokes its existing clear/fade when dismissed. The approved visual geometry, gold color, 140ms fade and drawing order are unchanged. The legacy inline debug ID label remains hidden so opening a sheet does not resize the underlying artwork viewport.

Another long press clears stale sheet content/actions until its background canonical read completes. Cancellation and page/ID checks reject stale results. Back is handled by the sheet before the existing reader callback. Outside ordinary taps cancel the underlying click and dismiss; longer gestures still reach the existing reader. Changing/leaving a page closes the old sheet without changing navigation or cache logic.

Copy uses `ClipData.newPlainText` with only canonical text as the item payload. Share uses `ACTION_SEND`, MIME `text/plain`, and exactly `EXTRA_TEXT`, wrapped in the native Android chooser. Play/tafsir have no listeners and are disabled with visible “coming later” labels.

## Verification

The complete run passed: 45 main and 13 shared-renderer executions, no failures/errors/skips. Debug build passed; Lint has zero errors and five warnings (existing portrait/API36 orientation warnings, available Robolectric update, two native-versus-KTX style suggestions). After the real API23 test exposed a system Toast inflation issue, the copy confirmation and new load-error Toast were changed to use application context. All four sheet executions, debug build and Lint passed again; the native API23 run then passed all five cases. See `validation.json`.

Canonical identity, exact text, expected juz, artwork edition/page, disabled actions, clipboard payload and sharesheet payload are checked on simulated SDK23 and SDK36 for:

| Ayah | Artwork page | Juz |
| --- | --- | --- |
| 1:1 | 1 | 1 |
| 1:7 | 1 | 1 |
| 2:282 | 48 | 3 |
| 5:78 | 121 | 6 |
| 114:6 | 604 | 30 |

The tests also check retained highlight beyond the old automatic-clear timeout, real window-dispatched selection of another exposed ayah, one unchanged sheet instance, ordinary outside-tap dismissal without accidental navigation, downward-handle gesture dismissal, and Back leaving the reader open. Long 2:282 text scrolls at initial and expanded height with actions fully visible; scrolling preserves the text and clipboard/share payload. RTL and explicit text direction are asserted.

All 604 SVG + 604 JSON originals/notices and 11 Tanzil reference digests verify unchanged. Seven frozen geometry/parser/override/loader/controller references verify unchanged. Renderer method comparisons confirm byte-identical hit testing, point conversion, zoom, matrix/inverse, Long Press, fling, visual preparation, draw and fade logic. Only selection retention/clearing was added.

Robolectric exercises functional behavior on SDK23 and SDK36; its native paragraph metrics are asserted on SDK36. Its SDK23 native paragraph metrics/SVG pixels are unsupported, and SDK36 offscreen captures intermittently omit already-visible controls because of stale simulated render state. Those captures are not the visual evidence used here.

A separate native instrumentation run on an actual API23 Android emulator passed all five long presses, canonical text comparisons, displayed edition/page identity, clipboard copy and Back/highlight dismissal checks. It also verified that 2:282 scrolls at both initial and expanded height and reaches a positive scroll offset. Android compositor screenshots show the real handle, header, canonical text and visible action row. This is an emulator run, not a physical-device claim or an API36 emulator claim. Software CPU emulation without KVM is too slow for meaningful performance timing. See `device-api23.json`.

The API23 copy-confirmation failure was caused by inflating the platform Toast layout through the Activity’s AppCompat inflater. Using application context avoids that inflater; the final native five-case copy run passed without a crash. No change to Quran text or selection geometry was needed.

Local screenshots (ignored and not release assets):

- `screenshots/device-api23-short.png` — 1:1, initial sheet.
- `screenshots/device-api23-long-collapsed.png` — 2:282, initial sheet.
- `screenshots/device-api23-long-expanded.png` — 2:282, expanded sheet.

Run the dedicated device test without changing the normal instrumentation runner:

```sh
./gradlew -PayahSheetDeviceTests=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.ahmad.quran.test/com.ahmad.quran.AyahSheetInstrumentation
```

The device report and PNGs are written under the app’s private `files/` directory and can be retrieved with `adb exec-out run-as com.ahmad.quran cat files/<name>`.

## Dependency

Google Material 1.12.0 is the only added direct dependency. Its official AAR is 2,323,473 bytes (approximately 2.22 MiB); this is the library archive size, not its final APK contribution. The resolved artifact matches the official SHA256 recorded in `material-lock.json`. It supports API21+, retaining the app's API23 minimum. The standard behavior supplies drag/settle, nested scrolling and accessibility rather than a custom gesture engine. The official Apache-2.0 license is bundled as `app/src/main/assets/MATERIAL-APACHE-2.0.txt`.

No audio, tafsir, downloads, cloud, bookmarks, source rebuilding or subsequent feature was implemented. This stage stops after the report.
