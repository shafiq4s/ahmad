# Line selection and centered ayah options

The pinned Hafs/KFQC SVG reader now draws one pale-green rounded strip per selected typeset row. It samples horizontal winding spans of the existing ayah polygons at each row center, unites all same-row pieces by their left/right extent, and draws a constant-height rectangle. It never intersects the backdrop with letter outlines or builds word-sized patches.

For this artwork's regular fifteen-row layout, centers start at 25.25 SVG units with a 36-unit pitch; the opening spread uses seven centers starting at 32.5 with a 28.33-unit pitch. These visually verified presentation constants are not source metadata, page mapping or touch overrides. Height is 80% of the pitch, leaving a uniform 20% inter-row gap. Corners are gently rounded at up to 3dp, tint is #7BB796 at 46/255 alpha, and the existing 140ms transition is retained. The original artwork renders above the backdrop. No bitmap/ink analysis remains.

## Interaction and modal

- Ordinary ayah taps select and retain highlight without opening a window or playing anything. Unused double-tap detection is disabled so consecutive ordinary taps are handled individually.
- Long presses use the platform GestureDetector default timeout, select the same canonical ID and open one centered AppCompat dialog.
- A tap for which the unchanged hit test returns no ayah toggles the existing upper/lower reader controls over 140ms. Controls become INVISIBLE rather than GONE, preserving viewport size, zoom focus and touch transforms. Whitespace inside a source polygon still belongs to that ayah, as before.
- The modal is a rounded RTL card with an explicit close button, light 0.22 window dim and 140ms enter/exit fades. Its width is at most 480dp/90% of the window; height is bounded to 80% of available height. Long text scrolls while header and actions remain visible.
- Close, outside tap or system Back dismisses the dialog and clears highlight with the same fade. One dialog instance is reused, and stale asynchronous text loads are rejected.

Text, surah name and canonical juz come from the existing frozen Tanzil repository. Page and edition come from the currently displayed Quran SVG geometry. No text is extracted from SVG, trimmed, normalized or appended to. Copy and native Android Sharesheet contain only canonical text. Play and tafsir are disabled, visibly marked as coming later, with no click listeners.

The old Bottom Sheet/resources and Google Material dependency were removed. No new dependency was added. A hidden, default-off `recitationOnAyahTap` SharedPreferences value round-trips locally but has no runtime action or visible option. There is no player, audio URL/file, networking code or INTERNET permission.

## Verification

The complete debug build, 50 app test executions and 13 shared-renderer executions pass, with no failures/errors/skips. Lint reports zero errors and five warnings. Four are existing orientation/update/KTX notices; the new InefficientWeight notice is deliberate: wrap_content keeps a short dialog compact, while weight shrinks a long paragraph to leave the header/actions visible. Using 0dp would remove the natural short-text height in this bounded wrap-content dialog. Test SDKs are 23 and 36. Coverage includes all 604 pages' visual strips, constant row height/gaps, same-row multipart merging, immutable source geometry, artwork-independent touch regions, opaque original ink above the green backdrop, transition/switch/clear, real ordinary/long gestures, stable coordinates when hiding controls, 1x/2x/4x, RTL swipes, disabled swipe when zoomed, two-pointer pinch, one modal instance, close/outside/Back, and canonical text-only clipboard/chooser payloads.

The five canonical cases are 1:1 / page1 / juz1, 1:7 / page1 / juz1, 2:282 / page48 / juz3, 5:78 / page121 / juz6, and 114:6 / page604 / juz30. Page121 artwork starts 5:78; frozen Tanzil page121 starts 5:77. The sources remain distinct.

All 1208 original SVG/JSON files and 11 Tanzil references verify unchanged. Eight pinned source/geometry/edition/cache/manifest references match `reference-lock.json`. Method comparison confirms byte-identical hit testing, screen-point conversion, zoom/matrix/inverse and RTL fling logic. Source assets, opening overrides, page maps, AndroidSVG 1.4 and API23 minimum are unchanged.

SDK36 simulation captures are stored locally under `screenshots/simulated-api36/`. The final native API23 emulator run passed all five canonical cases, exact clipboard payloads, real injected system Back without leaving the reader, and scrolling of 2:282. See `device-api23.json` and `validation.json`. The three final PNGs under `screenshots/device-api23/` are Android UiAutomation compositor captures, not simulated or generated artwork. The emulator was stopped after the test; evidence bytes were retrieved read-only from a raw copy of its saved userdata.

API23 and modern Android close outside taps on different event phases, so the regression sends the complete DOWN/UP sequence. Native key injection uses current timestamps; zero-time keys are rejected as stale by Android. Known system/Launcher boot ANR dialogs from slow software emulation were acknowledged with their WAIT buttons in the test harness. No application ANR is hidden, and no production code was changed for these harness fixes. Software CPU emulation without KVM is not a meaningful physical-device performance measurement.

Run the dedicated Android test with:

```sh
./gradlew -PreaderUiDeviceTests=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.ahmad.quran.test/com.ahmad.quran.ReaderUiInstrumentation
```

The report and compositor PNGs are written under the app's private `files/` directory. Retrieve them with `adb exec-out run-as com.ahmad.quran cat files/<name>`. Temporary screenshots are ignored and are not release assets. Historical ink-hugging and Bottom Sheet reports document superseded stages, not the current UI.
