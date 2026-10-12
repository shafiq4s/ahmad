# Five-page Quran SVG candidate

> Historical report only. Main integration removed the obsolete raster/image runtime, its temporary assets and preparation scripts after the SVG gate passed. Old commands below describe that earlier trial and are not current build instructions. Use [main integration instructions](../../README.md) and [report](../mushaf-svg/REPORT.md). Original source manifests and provenance remain for traceability.


Read [REPORT.md](REPORT.md) and [source-lock.json](source-lock.json) before using these fixtures. They are separate from the reader's production assets and the old Open Mushaf trial.

Prerequisites: project Android/JDK/Gradle setup, Inkscape 1.4, Python 3 with Pillow/WebP support. These are preparation tools, not Android dependencies.

```sh
python3 tools/quran-svg/prepare_fixtures.py
./gradlew -PquranSvgPrototype=true :svg-prototype:assembleDebug :svg-prototype:testDebugUnitTest :svg-prototype:lintDebug
```

Sources are fetched only by the explicit preparation command, at the fixed SHA and verified against the recorded hashes. Build/test/app runtime do not fetch page content. Only pages 1, 2, 48, 121 and 604 are permitted.

Install `svg-prototype/build/outputs/apk/debug/svg-prototype-debug.apk` as a standalone device trial. It has no Internet permission. Long press highlights a complete ayah and displays its canonical ID above the artwork. Ordinary taps have no application action. Navigation steps through only the five fixture pages, in portrait mode. There is no release build.

Native API 36 captures are in the ignored `screenshots/` directory. API 23 is logic-tested; Robolectric cannot produce native raster graphics on that API, so it is not a substitute for an Android 6 device trial. Per-page validation and source overlap findings are in `validation-api-36.json`; `validation-api-23.json` reports API 23 identity/bounds results with its graphics limitation.
