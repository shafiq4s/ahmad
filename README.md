# Quran Reader

Arabic-first native Kotlin/XML Android Quran reader, **القرآن الكريم**. Android 6/API 23 minimum; target/compile API 36. Complete offline Medina/Hafs page reading. No INTERNET permission, accounts, advertising or AI.

## Main integration: Direct SVG (0.5.0)

The main reader packages exactly 604 full-page SVG and 604 official polygon JSON files from Quran.ws, pinned at `b4155f07e4aea087d2c458069acff9f7c7e749f6`, stable asset release v1.1.1, `mushafs/hafs/kfqc/`. No surah crops or raster page assets. AndroidSVG 1.4 renders directly to the native Canvas. Below API26 the approved software view layer preserves sharp transformed glyphs; API26+ retains hardware rendering. Portrait is requested, with the existing Android16 compatibility property.

The index lists all 114 Arabic surahs, their numbers/counts and original metadata classification. Selecting a surah opens its first page in this artwork edition, including preceding verses if the surah begins mid-page. Page controls support 1–604, previous/next/direct entry. Rightward swipes advance RTL book order; leftward swipes return. Swipes are disabled while zoomed; pinch and 1x/2x/4x buttons reuse the approved matrix. Ordinary tap does not select; long press returns canonical `AyahId` and temporarily highlights all its polygons.

The artwork edition `quran_ws_hafs_kfqc_b4155f0` has its own audited page map. It is independent of the frozen Tanzil database and `madina_hafs_604` map; their 36-page pagination difference remains intentional. Current Quran.ws page121 starts 5:78; Tanzil starts 5:77. No text is bound over the artwork, generated or normalized. Opening-page touch overrides remain byte-identical, scoped to pages1/2 only. Pages3–604 use official polygons directly.

Startup loads the small catalog and 114-surah metadata, parsing zero SVG documents. The reader lazily starts one loader worker. Its LRU holds at most three neighboring documents, with prefetch only after the current page's first draw. Leaving the reader clears the cache. SharedPreferences retains the last successfully drawn edition/page; old Tanzil reader locations migrate by canonical ayah identity. App-local light/dark settings remain, with the artwork kept readable on white. Historical text-size preferences are retained for compatibility; vector reading uses zoom.

## Build and verification

Use JDK21, Android SDK36/Build Tools35.0.0 and existing Gradle8.13. All 604 original SVG/JSON pairs and required notices are committed under `tools/mushaf-svg/local-assets`. A fresh checkout includes the complete offline corpus; verify it before Gradle:

```sh
python3 tools/mushaf-svg/verify_assets.py
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

If assets are missing, `python3 tools/mushaf-svg/prepare_assets.py` restores only pinned originals from verified local sources or exact manifest URLs. This is development-only network access; the application never downloads pages. Every build verifies 1208 originals, local catalog/overrides/notices, all11 frozen Tanzil references and the AndroidSVG AAR SHA-256. Release APK/AAB are built locally unsigned; no distribution or store upload is performed.

See [main integration evidence](tools/mushaf-svg/REPORT.md), [asset manifest](tools/mushaf-svg/asset-manifest.json), [architecture](ARCHITECTURE.md) and [fixed Quran sources](QURAN_DATA.md). Historical stage reports remain evidence for earlier decisions, not current build instructions.

## Provenance and licenses

Quran.ws is the pinned technical source, an independent project rather than a government entity. Its upstream provenance records KFQC Hafs1441 artwork from `1441-AI-hafs.zip`, SHA-256 `280c5d71ca16aaeeb3a343be1b92c76fa6df71d0671e202c026fde12402d9eef`. This archive digest is reported by upstream; we have not independently downloaded the original archive from the Complex. Preserve the publisher's digital-use terms, Quran.ws LICENSE/NOTICE/PROVENANCE and CC-BY4.0 polygon terms. AndroidSVG has its separate Apache2.0 notice and pinned AAR digest. Tanzil attribution and original notices remain unchanged. The About screen includes both sources.

The obsolete Open Mushaf image/rectangle runtime, temporary page assets and raster-only prototype were removed only after the new reader passed its gate. Historical provenance reports and source manifests remain for traceability. The optional `-PnativeSvgPrototype=true` five-page direct-SVG validation app reuses the main renderer without packaging the full corpus.

## Deferred work

No audio, tafsir, translation, notes, word selection, final selection sheet/highlight, downloads or additional editions are implemented. Stop after the integration report. The future recitation design in ARCHITECTURE.md remains design only.
