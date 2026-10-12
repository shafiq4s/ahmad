# Main-app image reader experiment

> Historical report only. Main integration removed the obsolete raster/image runtime, its temporary assets and preparation scripts after the SVG gate passed. Old commands below describe that earlier trial and are not current build instructions. Use [main integration instructions](../../README.md) and [report](../mushaf-svg/REPORT.md). Original source manifests and provenance remain for traceability.


This phase adds production presentation contracts and an opt-in device trial.
No Quran text, SQLite schema/content, Tanzil PageStart, or source digest was changed.
No audio, tafsir, network, downloads, backend, or dependency was added.

## Edition identity and contracts

The existing `data.MushafEdition` is reused. The temporary image collection has
the independent ID `open_mushaf_hafs_images_test_v1`, not `madina_hafs_604`.
Both reference canonical `(surahNumber, ayahNumber)` IDs and the existing text
version. The image edition is registered by the debug asset collection, never
inserted into or returned by the frozen SQLite edition table.

Main code in `app/src/main/java/com/ahmad/quran/mushaf` provides:

- `MushafPageAssetProvider`: edition identity, available local page numbers,
  and a bounded page stream. Total edition pages and installed pages differ.
- `MushafPageMap`: edition-specific geometry lookup independent of asset storage.
- `AyahRegion`: edition/page/surah/ayah and one or more source-pixel `RectF`s.
  Mutable rectangle references are copied on input and output.
- `MushafPage`: validated IDs, dimensions, ownership, positive finite rectangles,
  bounds and overlaps; ambiguous hits return no ID. Duplicate region IDs are
  rejected; multiple lines of one ayah belong to one region.
- `HafsImagePageMap`: uses our corrected marker-to-line-band algorithm from the
  approved prototype. Pages 1/2, unnumbered basmalah, surah headers, the title
  on the preceding page and Surah 9 rules belong to this image edition layer.
  These rules must not be used for another edition. They never modify `Ayah`.
- `MushafPageFrame` and `MushafTouchOverlayView`: image + transparent touch layer,
  one source-pixel matrix, inverse hit testing, high-density drawable correction,
  and no RTL mirroring. Page dimensions come from the validated page. Debug
  labels are optional and default off in the reusable overlay.

The click callback takes **only `AyahId`**. A future recitation layer must receive
that identity and choose surah timing independently of images/page numbers.
No recitation system is implemented here.

The standalone `overlay-prototype` compiles the same corrected geometry source;
there is no second copy of its mapping algorithm. Its existing validation UI
remains separate.

## Local build and device trial

```sh
python3 tools/image-reader/prepare_fixtures.py
./gradlew -PimageReaderPrototype=true :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set up the JDK/SDK as described in the project README first. The preparation
script reuses existing verified fixtures or downloads only the nine pinned
images when necessary. Source URLs/digests are recorded in the local manifest.
Fixtures and screenshots are ignored by Git and stored outside main assets.
Run preparation before the geometry regression tests, including an unflagged
debug test run; tests read the pinned fixture directory separately from APK assets.

The feature flag defaults to false. Only flagged **debug** builds attach the
temporary fixture directory as assets. Release always has a false flag, a no-op
entry implementation, no debug activity/provider and no temporary image assets,
even when the property is passed. This phase builds only an experimental APK.
Its package ID is `com.ahmad.quran.mushafdebug`; it installs alongside the approved
reader and has separate local preferences. It is signed with the local debug key.

Open the app index and tap **«تجربة المصحف المصوّر — Debug»**. Select pages
1, 2, 7, 48, 121, 187, 531, 585 or 604. The arrows move between the available
samples, not through missing pages. Tap an ayah to highlight all its line
segments and display the canonical surah/ayah. Toggle region labels to inspect
the original page. Return to the index to use the existing textual reader.

Only the current image is decoded; the previous bitmap is released. The small
coordinate metadata is loaded once by this debug screen. Quran text is not
loaded in bulk. Page maps are created/validated on demand.

## Provenance and distribution boundary

Pinned Open Mushaf commit: `f4aa7867059ac6250a3f8aa68c64002ca1874b7d`.

- `aya.json`: `605ffbd1959ce9fea24e0c65b645f6ba0b13dc3f0430deb9f0053f8629c503cd`
- `specs.json`: `af43034d3a2f0ed6865b6014b35ef8c7388c5b4b1e98c992cf0a33c3daa90c43`
- Images: pinned paths `assets/mushaf-data/mushaf-elmadina-hafs-assim/<page>.png`;
  individual hashes are in `fixture-manifest.json`. Nine images total 690,943 bytes.
- MIT notices for Open Mushaf and Ibrahim BIDI's coordinate source are retained
  in local fixtures. The data's reviewed provenance is documented in the prior
  prototype report. No upstream React Native hook or GPL tool implementation
  is copied into the app.

**The Open Mushaf page images remain temporary local testing assets.** Upstream
code/coordinate MIT notices do not settle image redistribution permission.
The official image edition/download provenance and redistribution license still
need approval before any public APK/AAB or full image package. Do not publish
this debug APK, attach these assets to release, or download all 604 images.

The 36 pagination differences are edition differences, not automatically errors.
Page 121 begins at `5:78` in these images; the frozen Tanzil map begins at `5:77`.
The approved structural checks cover all 604 coordinate pages, but visual review
still covers the prior limited image sample, not all pages. Regions are line
segments, not word geometry; future word selection requires separate metadata.

## Regression checks

API 23/36 checks cover all 6,236 IDs and 14,101 line rectangles across 604 maps,
edition ownership, missing/invalid IDs (`112:5`, `114:7`), safe surah transitions,
Quran termination, out-of-bounds/non-finite/empty rectangles, unexpected overlaps,
adjacent ownership, rectangle immutability, page 1/2 basmalah/header rules and
surah title cases. Word positions chosen from actual images are tapped at three
screen sizes in LTR/RTL and xxhdpi, checking the callback and shared matrix.
Flagged integration tests check the index entry, only nine available images,
unavailable-page rejection and the final `114:6` hit. Native Android screenshots
are produced on simulated API 36; they do not claim real device testing.

See `validation.json` for measured artifact sizes and final check results.

Edition-abstraction phase verification (before the portrait patch): experimental APK **5,145,879 bytes**, +799,955 bytes over
the approved reader debug APK (4,345,924 bytes). All 62 executions passed
(51 main-app, 11 standalone prototype) on simulated API 23/36. The additional
default-flag check passed with no entry or image assets. Debug/release lint had
zero errors and two advisory warnings: a newer Robolectric version and optional
Canvas KTX syntax. The standalone prototype retained one test-version warning.
Release compilation/assets/manifest were checked, including with the feature
property supplied; no new release APK was assembled. All 11 frozen references
and the database bundled in the debug APK matched their recorded digests.

## Subsequent device approval and portrait patch

The user reported correct overlay alignment, accurate ayah hits and canonical
IDs on a real Android device and approved the geometry technically. No device
model/OS version was supplied. A later, minimal debug manifest patch requests
portrait only, with an activity-scoped API 36 compatibility property for large
screens. The source-pixel map/matrix and Quran corpus were not changed.
The updated temporary APK is **5,144,948 bytes**. Five existing UI test executions
passed on simulated API 23/36; Lint has zero errors and four advisory warnings
(temporary orientation lock/compatibility opt-out plus the two prior warnings).
See [portrait validation](../image-provenance/portrait-validation.json).

The [separate provenance review](../image-provenance/REPORT.md) identifies the
first image commit and coordinate-data lineage. It does not establish original
image-package provenance or redistribution/conversion permission. The images
remain temporary test assets, excluded from release.
