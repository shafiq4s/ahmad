# Quran Reader

Arabic-first native Kotlin/XML Android Quran reader, **القرآن الكريم**. Android 6/API 23 minimum; target/compile API 36. Canonical Quran text/metadata are offline. Medina/Hafs SVG seed pages are bundled; downloaded pages remain readable offline in disk cache. INTERNET is used only for configured page downloads. No accounts, advertising, AI or audio.

## Main integration: Direct SVG (0.5.0)

The main reader packages 11 original full-page SVG seeds (1–5, 300, 600–604) and all 604 official polygon JSON files from Quran.ws, pinned at `b4155f07e4aea087d2c458069acff9f7c7e749f6`, stable asset release v1.1.1, `mushafs/hafs/kfqc/`. No surah crops or raster page assets. AndroidSVG 1.4 renders directly to the native Canvas. Below API26 the approved software view layer preserves sharp transformed glyphs; API26+ retains hardware rendering. Portrait is requested, with the existing Android16 compatibility property.

The index lists all 114 Arabic surahs, their numbers/counts and original metadata classification. Selecting a surah opens its first page in this artwork edition, including preceding verses if the surah begins mid-page. Page controls support 1–604, previous/next/direct entry. Rightward swipes advance RTL book order; leftward swipes return. Swipes are disabled while zoomed; pinch alone uses the approved matrix. Production zoom/debug buttons are removed. Ordinary tap selects a canonical `AyahId` without opening anything. Long press uses Android’s default gesture timeout and opens a centered ayah-options dialog. Blank taps collapse/restore the upper/lower bars over 190ms, releasing more viewport space. The existing renderer recalculates its fit-center matrix/inverse for the new viewport without changing source geometry or aspect ratio.

The artwork edition `quran_ws_hafs_kfqc_b4155f0` has its own audited page map. It is independent of the frozen Tanzil database and `madina_hafs_604` map; their 36-page pagination difference remains intentional. Current Quran.ws page121 starts 5:78; Tanzil starts 5:77. No text is bound over the artwork, generated or normalized. Opening-page touch overrides remain byte-identical, scoped to pages1/2 only. Pages3–604 use official polygons directly.

Startup loads the small catalog and 114-surah metadata, parsing zero SVG documents. The reader lazily loads SVG through bundled assets, verified disk cache, then a configured remote source. One foreground worker loads/parses the requested page; a separate worker downloads raw neighbours ±2 only after its first draw. The parsed-document LRU remains bounded to three; only local nearest neighbours are parsed. Leaving the reader clears this memory cache and preserves disk files. SharedPreferences retains the last successfully drawn edition/page; old Tanzil reader locations migrate by canonical ayah identity. App-local light/dark settings remain, with the artwork kept readable on white. Historical text-size preferences are retained for compatibility; vector reading uses zoom.

## Build and verification

Use JDK21, Android SDK36/Build Tools35.0.0 and existing Gradle8.13. Assets stay under ignored `tools/mushaf-svg/local-assets`; the committed complete manifest and deterministic preparation script restore only pinned originals. On a fresh checkout, prepare assets before Gradle:

```sh
python3 tools/mushaf-svg/prepare_assets.py
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

Preparation reuses verified local sources or downloads the exact manifest URLs. This restores development sources. Runtime downloads use only an explicitly configured base URL, with no fallback to the source repository. Every build verifies 1208 originals, local catalog/overrides/notices, all11 frozen Tanzil references and the AndroidSVG AAR SHA-256. Release APK/AAB are built locally unsigned; no distribution or store upload is performed.

See [main integration evidence](tools/mushaf-svg/REPORT.md), [asset manifest](tools/mushaf-svg/asset-manifest.json), [architecture](ARCHITECTURE.md) and [fixed Quran sources](QURAN_DATA.md). Historical stage reports remain evidence for earlier decisions, not current build instructions.

## On-demand page configuration

Edit [seed list](config/mushaf-page-assets.json) to change bundled pages. Gradle stages a subset into build/generated/pageAssets for each variant; all original workspace files remain intact. The pinned complete source manifest, all polygons, page map, notices and Tanzil assets remain packaged. Start a clean package when comparing sizes with an old full-corpus APK, because incremental ZIP packaging can retain unused space.

The default base URL is empty in debug and release; non-seed pages require a configured server or existing valid cache. Set the Gradle property `quranPageBaseUrl` when a host is chosen; the request path is `<base>/<editionId>/<three-digit-page>.svg`. Release accepts HTTPS only. Debug additionally allows HTTP solely on localhost/127.0.0.1/10.0.2.2. No domain, Cloudflare service or external temporary source is configured.

For the development-only five-page server:

```sh
python3 tools/page-assets/local_server.py --port 8773
./gradlew -PquranPageBaseUrl=http://127.0.0.1:8773 :app:assembleDebug
adb reverse tcp:8773 tcp:8773
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The server serves only 119–123 from the unchanged pinned originals. It is not packaged. Remote failures show a quiet Arabic page-area message and retry action. Each transfer has connect/read and total deadlines, a maximum of two attempts, a same-directory temporary file, exact length/SHA-256 verification, and an atomic rename. Existing cache files are reverified before use; invalid files are discarded. Cache storage is `cacheDir/mushaf-pages/quran_ws_hafs_kfqc_b4155f0/`. Android may reclaim this ordinary disk cache; retained files remain usable without a connection. See [verification](tools/page-assets/REPORT.md).

## Provenance and licenses

Quran.ws is the pinned technical source, an independent project rather than a government entity. Its upstream provenance records KFQC Hafs1441 artwork from `1441-AI-hafs.zip`, SHA-256 `280c5d71ca16aaeeb3a343be1b92c76fa6df71d0671e202c026fde12402d9eef`. This archive digest is reported by upstream; we have not independently downloaded the original archive from the Complex. Preserve the publisher's digital-use terms, Quran.ws LICENSE/NOTICE/PROVENANCE and CC-BY4.0 polygon terms. AndroidSVG has its separate Apache2.0 notice and pinned AAR digest. Tanzil attribution and original notices remain unchanged. The About screen includes both sources.

The obsolete Open Mushaf image/rectangle runtime, temporary page assets and raster-only prototype were removed only after the new reader passed its gate. Historical provenance reports and source manifests remain for traceability. The optional `-PnativeSvgPrototype=true` five-page direct-SVG validation app reuses the main renderer without packaging the full corpus.

## Reader interface

The RTL header separates the current artwork-page surah name from its juz/page caption, with 48dp index/settings icon targets. A quiet bottom bar holds previous/next icons, the current page out of 604 and a bounded native page-entry dialog. Page changes use a short 110ms view-alpha transition without changing loading, cache or navigation. Warm surfaces, restrained green, flat rounded controls and subtle separators match the existing ayah dialog.

The existing 114-surah ListView shows its original name/number/count/classification plus the first page from the Quran SVG edition. Settings retain the implemented local dark switch, add pinch guidance and show the actual build version and existing source/licenses. No placeholder audio controls or new libraries are introduced. See [UI validation](tools/reader-design/REPORT.md).

## Local reading experience

The home index offers a named-surah/page resume card, a page-bookmark list and surah-name-only filtering. SharedPreferences stores the last successfully drawn artwork page, surah context and timestamp. Optional cold-start resume does not override saved-screen restoration. Page bookmarks are unique per edition/page and can retain a selected canonical ayah; footer and ayah-dialog toggles share the same local state. Removing bookmarks changes no Quran records.

The existing compact jump dialog switches between page 1–604, juz 1–30 and surah selection. Official Tanzil juz starts are canonical IDs resolved through the current Quran SVG edition map: juz 7 starts at 5:82 on artwork page121, which itself begins at 5:78. This keeps canonical part membership separate from page layouts.

Settings persist keep-awake only while reading, automatic resume and initial reader-bar visibility. Disk size is calculated off the UI thread. Confirmed clearing waits for cancelled page/prefetch writers and removes only the downloaded current-edition cache; bundled seeds, JSON, SQLite, bookmarks and last-read state remain. The support and about sections explain free use, future optional support, actual version, independent Quran.ws/upstream KFQC provenance and current licenses. Privacy is an explicit preparation placeholder without a fabricated URL. No support payment is implemented. Unconfigured unavailable pages show a clean Arabic availability message. See [current-stage validation](tools/reading-experience/REPORT.md).

## Ayah details

Selection uses pale green, rounded, straight line strips, one per occupied typeset row, rather than word/ink envelopes. Same-row polygon pieces are visually united; original touch polygons remain unchanged. Every selected row fades together over 140ms, with a uniform height and gap within the page. Opening-spread and regular-page row rhythms are presentation-only constants for the pinned KFQC artwork.

Long press opens one centered RTL AppCompat dialog with a light dim, rounded card and explicit close button. Its canonical text, surah name and juz come from the existing Tanzil repository; its page comes from the displayed Quran SVG edition. Long text scrolls within a window bounded to 80% of available height. Close, outside tap or Back dismisses it and clears selection with the same fade. Copy and Android Sharesheet send only the original ayah text. An add/remove bookmark action saves the artwork page and selected canonical ID without dismissing the modal. Play and tafsir are disabled and marked as coming later. The obsolete Bottom Sheet and its Material dependency were removed; no new library was added. See [validation](tools/reader-ui/REPORT.md).

A hidden, default-off `recitationOnAyahTap` SharedPreferences preference records the future “recitation on ayah touch” option. It currently has no effect, visible switch, player, audio URL or audio file. Existing INTERNET permission is exclusively for configured page assets.

## Deferred work

No audio, tafsir, translation, notes, word selection, media downloads or additional editions are implemented. Stop after the current stage report. The future recitation design in ARCHITECTURE.md remains design only.
