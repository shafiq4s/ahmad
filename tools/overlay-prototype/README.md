# Overlay Validation Prototype

> Historical report only. Main integration removed the obsolete raster/image runtime, its temporary assets and preparation scripts after the SVG gate passed. Old commands below describe that earlier trial and are not current build instructions. Use [main integration instructions](../../README.md) and [report](../mushaf-svg/REPORT.md). Original source manifests and provenance remain for traceability.


This is a local technical experiment, not a new reader release. The opt-in
`overlay-prototype` Android application has a separate application ID and no
release variant. The reader does not depend on it. Its temporary images are
under `overlay-prototype/build/test-fixtures`, which is ignored by Git.

## Run

From the repository root, with the existing Android/JDK environment:

```sh
python3 tools/overlay-prototype/fetch_fixtures.py
./gradlew -PoverlayPrototype=true :overlay-prototype:assembleDebug :overlay-prototype:testDebugUnitTest :overlay-prototype:lintDebug
```

Install `overlay-prototype/build/outputs/apk/debug/overlay-prototype-debug.apk`
only as a local test app. The page selector contains the downloaded sample,
not all 604 pages. Tap a verse to highlight all its segments and display its
canonical surah/ayah identity. The checkbox toggles the diagnostic regions.
The report button writes `files/overlay-validation.json` inside the test app.
The automated report is `results/page-report.json`; debug captures are in
`results/screenshots/`. These captures are Android 16 Robolectric native
graphics renders of the actual Kotlin views, not physical-device captures.

`fetch_fixtures.py` pins Open Mushaf commit
`f4aa7867059ac6250a3f8aa68c64002ca1874b7d`, verifies the coordinate/specification
hashes and records each image's URL, size and SHA-256. It downloads at most 64
images. This sample contains 58 images (4,154,425 bytes), including all 36
pages with different ayah membership from the approved Tanzil page map.

## Geometry

The original arrays are `[surahNumber, ayahNumber, vertical, horizontal]`.
All 6,236 identities match the frozen corpus. Coordinates are already in the
actual sample image's 456x672 pixel space. The upstream 430x720 `specs.json`
describes a different reference container; its offsets/scaling are not used.

The independent Kotlin algorithm follows consecutive verse end markers in
right-to-left reading order. A verse's segments run from the previous marker
to its own marker; it does not assign an arbitrary left-hand strip to
`ayahNumber + 1`. `AyahOrder.next` uses the corpus's official verse counts to
validate sequence and crosses surah boundaries safely, stopping after 114:6.

Markers are snapped to calibrated line bands, with a per-page median vertical
offset. Ordinary pages use a 39.5-pixel pitch; pages 1 and 2 use a 26.3-pixel
pitch and narrower text bounds. Verse end markers are included in their own
verse's region. Titles and unnumbered basmalahs are excluded. Surah 9 has no
basmalah. Some surah titles are on the preceding page, so those pages start
reading one band earlier. All segments are clipped to the image bounds;
out-of-bounds source points and clipping are reported rather than concealed.

`MushafPageFrame` contains an `ImageView` and `MushafTouchOverlayView`. One
`Matrix` fits the image without distortion and is shared with the overlay;
touches are mapped through its inverse. High-density bitmap drawable bounds
are kept in image pixels. Arabic RTL affects the controls, not this matrix.

Overlap diagnostics compare every rectangle pair on a page. Hit testing
returns an identity only if all matching rectangles agree; an ambiguous hit
is rejected. The debug view draws all segments of the selected identity.

## What the checks establish

The report covers all 604 point arrays, including counts, generated region
counts, overlaps, out-of-bounds findings, invalid IDs, first/last IDs and the
unchanged Tanzil first/last IDs. A differing page boundary is not classified
as an error. Zero structural findings do not prove visual accuracy.

Visual endpoint review covers all 36 differing pages and the downloaded
special-case sample. Sixteen manually selected word locations, independent
of generated rectangle centers, are checked in three viewport sizes, both
LTR/RTL layouts and API 23/36. Density alignment is checked separately.

The regions are line segments, not exact word outlines. Some blank space is
included, and narrow gaps between line bands are intentionally inactive.
The remaining 546 page images have not been downloaded or visually checked;
neither their dimensions nor complete visual hit accuracy is certified.
Additional first-line/header cases may require presentation corrections.

## Page-map findings and source limits

All 36 differing pages' printed first and last verses agree with the Open
Mushaf arrays. For example, image 120 ends with 5:77, and image 121 begins
with 5:78. Tanzil's approved page 121 begins with 5:77. Later examples include
image 585 ending with 80:40 and image 586 starting with 80:41, while Tanzil's
page 586 starts with 81:1. These are observed pagination differences, not
missing or invented Quran identities. The exact print edition/year is not
verified, and must not be inferred from the folder name.

No Quran text, corpus bytes, page start or source metadata was modified. The
prototype's image map must not overwrite `madina_hafs_604`. A final image
edition must have its own verified presentation map and provenance.

Open Mushaf's MIT notice is retained in the temporary fixtures. The project
attributes its page images to the King Fahd Complex, but the official asset
license/download provenance remains unverified. Local test permission does
not approve distributing these images in the reader or a public test build.
No upstream app, hook or GPL-licensed coordinate tool was copied into Kotlin.
