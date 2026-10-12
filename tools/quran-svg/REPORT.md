# Quran SVG production candidate — five pages only

This is an isolated, opt-in debug candidate, not a final renderer decision or a release asset pack. No Open Mushaf implementation/data is compiled into it. The approved reader and all older prototypes remain available.

## Pinned source and rights

- Repository: https://github.com/quran-ws/quran-svg
- Commit: `b4155f07e4aea087d2c458069acff9f7c7e749f6`, dated 2026-09-15. The selected five SVGs and JSONs are unchanged from stable `v1.1.1` / `3ff27b46cecb0c0519d77531d9c868a579d1f623` (verified with Git diff).
- Retrieved: 2026-10-11 02:21:35 UTC / 06:21:35 Asia/Dubai.
- Edition: `mushafs/hafs/kfqc/`, Hafs an Asim; independent app ID `quran_ws_hafs_kfqc_b4155f0`.
- Original artwork: King Fahd Complex digital Mushaf al-Madinah, 1441H Illustrator archive `1441-AI-hafs.zip`, 466,430,736 bytes, reported SHA-256 `280c5d71ca16aaeeb3a343be1b92c76fa6df71d0671e202c026fde12402d9eef`.
- That archive lineage/digest is the upstream provenance claim, not our independent archive verification. We did not download the full archive. PROVENANCE records structural checks for Hafs, not a completed geometry comparison against its Illustrator masters.
- KFQC artwork has its own free digital/software-use permission reproduced in LICENSE §2 and NOTICE §2.1. The physical commercial printing/import restrictions do not describe Android application distribution. Artwork is not licensed as MIT or as Quran.ws-owned CC BY content.
- Quran.ws polygon metadata is CC BY 4.0, with attribution waived inside products. Republishing data as a standalone dataset retains CC BY attribution obligations. Repository tools/code use MIT.
- Preserve the upstream notices with data distributions. Suggested in-app credit: “Mushaf artwork: King Fahd Complex; SVG and ayah polygons: Quran.ws.” Tanzil attribution remains necessary for our separate text database. No new claim of ownership or modification of Quranic text is made.

`source-lock.json` stores URLs, byte sizes and SHA-256 for every downloaded asset and notice. Copies of LICENSE, NOTICE, PROVENANCE, FORMAT, GETTING-THE-FILES and CC-BY-4.0 are in `upstream-notices/`.

## Files and implementation

Only `svg/{001,002,048,121,604}.svg` and `json/{001,002,048,121,604}.json` were fetched. Original bytes: SVG 1,840,601; JSON 6,264. No full page collection, Quran Engine or SVG Elements was fetched or integrated.

The sources remain in ignored `local-fixtures/sources/`. Inkscape 1.4 renders PNG build inputs; Pillow generates lossless WebP. Decoded PNG and WebP pixels are verified equal. Only the five 1440-pixel WebPs, original five JSONs, our render catalog and legal notices enter the standalone debug APK. Original source SVG remains available locally for auditing; it is not duplicated in that APK.

- `QuranSvgPageAdapter`: strict point-list and absolute M/L/Z subpath parsing; rejects unsupported commands, malformed numeric identities, missing Quran IDs, duplicates and out-of-bounds geometry. External JSON does not enter the region/view contracts. Source x/y markers are used only as independent test anchors, not to reconstruct geometry.
- `AyahPolygonRegion`: edition/page plus canonical surah/ayah identity and all component polygons. Callback returns only AyahId, with no future audio dependency on pages.
- `PolygonPageView`: native Bitmap/Canvas, one matrix for artwork and polygon coordinates, inverse matrix for winding-rule hit tests, native GestureDetector long press, grouped highlighting. Ordinary taps perform no application action. No coordinate reflection or line-height/next-ayah guessing.
- `SvgPrototypeActivity`: portrait-only native page view, previous/next navigation only among the five sample pages, separate small debug label above the page. No Cards or generated Quran text.
- Only one decoded page is retained. The regular 1440 × 2296 ARGB bitmap occupies 13,224,960 bytes (~12.61 MiB), excluding drawing surfaces and temporary decode allocations. Two images can exist briefly during a page change. Corpus queries validate the visible page IDs; the entire Quran is not loaded into RAM.

The opening SVG viewBox is `-53.3109 -198.4777 345 550`; other sampled pages use `0 0 345 550`. Opening pages are fitted to their complete artwork bounds, including headings, using an edition-specific render box. The source files/polygons are unchanged and source coordinates remain intact. There is no reflow of words or lines. The original SVG omits the decorative printed outer frame; this trial follows the supplied SVG artwork.

## Validation and limitations

| Page | Ayah regions | Component polygons | First | Last | Invalid IDs / outside source bounds | Source overlap pairs |
|---|---:|---:|---|---|---|---:|
| 1 | 7 | 7 | 1:1 | 1:7 | 0 / 0 | 3 |
| 2 | 5 | 5 | 2:1 | 2:5 | 0 / 0 | 3 |
| 48 | 1 | 1 | 2:282 | 2:282 | 0 / 0 | 0 |
| 121 | 6 | 15 | 5:78 | 5:83 | 0 / 0 | 0 |
| 604 | 15 | 18 | 112:1 | 114:6 | 0 / 0 | 0 |

The 34 IDs all exist in the unchanged Tanzil database. SVG embedded ayah paths and JSON polygons match coordinate-for-coordinate. Native API 36 captures of all five pages show the original word/line arrangement and correct grouped selection, including all of 2:282 and the final 114:6. This is visual review against the supplied SVG, not an independent scholarly verification of the complete KFQC master archive.

Scaling tests cover 320×480, 412×732 and 600×960 pixel viewports under RTL. Marker anchors and every component polygon are long-pressed: 240 successful selections per API, 480 across API 23 and 36, plus the five native capture selections. Ordinary taps do not select. Unknown 112:5/114:7, malformed paths, bounds violations, disconnected-subpath false positives and ambiguous overlapping hits have regression coverage.

**Opening-spread source overlaps remain:** 1:5/1:6, 1:5/1:7, 1:6/1:7; 2:2/2:3, 2:3/2:4, 2:4/2:5. Native path intersection gives approximate areas 11.21, 3.55, 16.09, 6.50, 28.55 and 262.89 square SVG units, respectively. These are thin edge areas, not the solid rectangle implied by their enclosing bounds. At a multiply-covered point the callback deliberately returns nothing. Geometry is not trimmed or reassigned. These six pairs are pinned as known source findings; a new overlap fails regression. A corrected upstream revision or explicitly approved edition correction is needed to guarantee selection at every boundary point.

Official polygons eliminate our old coordinate/line reconstruction and align directly with their artwork. This makes the source relationship more reliable; it is not evidence of higher numerical hit accuracy than the physically approved old overlay at every point. They still represent ayah line bands, not individual word outlines, and the opening spread has the above defects.

Tanzil page 121 starts at 5:77; this SVG edition starts at 5:78. The other four sample page memberships agree. All 11 frozen Quran references retain their approved SHA-256. No text comparison can be claimed between vector outlines and Unicode text byte-for-byte; we verified identities, visible content and membership, without changing the database. We have not audited all 604 SVG pages for pagination differences or polygon defects.

API 23 compatibility, local repository, adapter, coordinate transformations, native gesture scheduling and callbacks pass Robolectric. **Robolectric does not support native graphics rendering below API 26**; API 23 screenshots would be blank test artifacts and are not supplied. API 36 visual captures use native Skia. No physical device or emulator was connected here; Android 6 device rendering/performance and Android 16 physical-device behavior remain unverified. WebP decoding and Canvas/GestureDetector APIs used are available before API 23.

Build and Lint results and artifact digest are recorded in `validation.json`. Existing reader tests and old prototype tests remain passing. No Release variant exists for this candidate, and the reader's production assets are untouched.

## Direct SVG versus pre-rendered images

| Aspect | A: direct native SVG | B: generated lossless WebP |
|---|---|---|
| Quality | Vector resolution at any scale | Exact raster pixels at chosen resolution; upscaling loses sharpness |
| Dependencies | Native SVG parser/renderer needed | Android Bitmap/Canvas only; conversion tools run before packaging |
| Android 6 | Renderer must be separately validated | Platform WebP supported; app logic tested, physical rendering pending |
| Performance | Parses/draws many paths; not measured in this trial | Decode and bitmap draw; predictable page memory, no SVG parsing at runtime |
| Memory | Depends on path/DOM/Picture cache and surface; not measured | 12.61 MiB/page at 1440×2296, plus surfaces and transient decode |
| Full 604-page artwork estimate | ~325 MB raw by dense-page sample; ~92–120 MB ZIP/Deflate | ~150–200 MB at width 1440; ~100–140 MB at 1080; ~240–310 MB at 2160 |

These are **estimates**, not a measured complete package. Dense pages 48 and 121 give 604-page mean extrapolations of 91.9 MB Deflate SVG, 119.4 MB WebP at 1080, 166.2 MB at 1440, and 260.1 MB at 2160. The sample is small and not representative of every page; allow additional metadata and app code. `size-profiles.json` contains all five pages at three widths and their actual PNG/WebP/SVG sizes. Lossless PNG at 1440 extrapolates to ~545 MB, so PNG is not recommended for packaging.

Upstream GETTING-THE-FILES reports ~414 MB SVG, 72 MB Brotli SVG and 3.7 MB JSON for the Hafs directory. **The pinned tree has 722 SVGs, not just 604:** 604 numbered full pages plus 118 surah-cropped variants. Those directory sizes must not be presented as exact sizes of our 604-page selection. Brotli would also require decompression support; Android ZIP/Deflate does not attain its quoted size automatically. No extra pages were downloaded to obtain these tree counts.

No new runtime dependency was added. If direct SVG is later approved, the reviewed alternative is `com.caverock:androidsvg-aar:1.4`: Apache 2.0, 202,395-byte AAR (~197.65 KiB), `classes.jar` 222,137 bytes, advertised minSdk 8. Artifact SHA-256: `02a5b08a2b35d2d58eb2eaca9d84ac00fb341da725fdbd653ea3ed130437e95a`. The AAR size is not a measured APK increment. It offers native Canvas rendering without WebView; it has not been added or benchmarked/render-validated in our application. Its old release requires verification before adoption.

**Recommendation for this application:** pre-rendered lossless WebP plus original official polygons, retaining SVG as the auditable master. It avoids a runtime library and path-parsing complexity on Android 6 and makes memory easier to bound. Start with 1440 width for the next device evaluation; choose the final resolution/package strategy only after the user approves this report, the known source overlaps are resolved and Android 6 device rendering is checked. Do not call the five-page candidate a complete production-approved edition.

## Retained old implementation and future deletion

Nothing was removed from Open Mushaf. After final approval and replacement of its main-app debug entry, removable items include `overlay-prototype/`, the old aya/PNG fixtures and prepare scripts, `tools/image-reader` trial assets/provider/activity, and the old marker-based `OverlayGeometry` / `HafsImagePageMap` / rectangle touch view if there are no remaining consumers. Replace their tests with the polygon regression suite before deleting them. Retain the source/license investigation reports, changelog, canonical Quran models/repository and useful edition-independent presentation contracts. No deletion or full-page integration is authorized by this trial report.
