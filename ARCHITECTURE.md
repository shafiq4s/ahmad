# Architecture

## Main page reader

Native XML -> MainActivity -> MainViewModel and MushafReaderController.
MainViewModel loads canonical metadata from the existing QuranRepository/read-only SQLite, reads the small QuranSvgEdition catalog and maintains navigation via SavedStateHandle/StateFlow. It does not parse artwork. The UI binds Arabic index/settings/page controls; the SVG is the entire visible page, with no reflowed Quran text over it.

MushafReaderController uses an independent foreground loader and raw-file download worker. OnDemandPageAssets resolves bundled, verified disk, then remote data through the existing edition asset contract. Foreground jumps never queue behind optional neighbour downloads. Generation checks reject stale work/callbacks; in-progress reads have bounded timeouts. The page must draw before ±2 disk prefetch begins. Per-page locks coalesce concurrent transfers of the same page. NativeSvgPageLoader retains at most three parsed documents (current/nearest neighbours); memory prefetch reads only files already local and never starts downloads. Leaving the reader evicts parsed documents while keeping raw disk files. Closing stops both workers and the SQLite repository.

NativeSvgPageView directly renders AndroidSVG1.4. One source-to-screen Matrix and its inverse handle drawing, highlighting and hit testing. No RTL reflection. Below API26 the approved software-layer fallback is unchanged; modern APIs keep the default hardware path. The view fits the source presentation box with preserved aspect ratio, supports vector zoom and RTL fling navigation at1x. Long press returns only AyahId; ordinary tap selects without opening a window. Source polygons with multiple components are grouped under one ayah. Selection remains temporary.

QuranSvgPageAdapter is the sole external polygon-format parser. It rejects unsupported syntax, invalid canonical IDs, inconsistent edition/page identities and out-of-bounds coordinates. OpeningPageHitOverrides uses the unchanged local reviewed file only on pages1/2; official SVG/JSON bytes remain intact. Heading and unnumbered basmalah artwork has no canonical ayah identity.

## Separate edition and canonical data

HOME and SEARCH are lightweight native panels in the existing Activity. The starting theme/readiness surface shares the book vector and colors; it adds no delayed activity or dependency. MainViewModel retains pending navigation AyahId in SavedStateHandle, resolves its page through the existing artwork map and consumes it only after the displayed page can select it. NativeSvgPageView.selectAyah is only a validated facade over the original held-selection function; drawing, hit testing, gestures, polygons and highlight methods remain byte-identical. Explicit ayah navigation uses the existing 1x zoom setter so a same-page jump remains visible. Returning from a result restores the search screen/query.

QuranRepository.searchAyahs adds a read-only cursor scan of the fixed 6236-row text version; no search table or canonical mutation exists. QuranSearch computes comparison-only keys. MainViewModel debounces 250ms, runs scans on Dispatchers.IO, checks cancellation per row and returns 50 results plus one look-ahead row for paging. No whole-corpus text index is retained in memory and no SVG is parsed for search/home. Original strings flow to the recycled result TextViews, whose native three-line ellipsis is only a preview. The existing three-document renderer cache and raw download workers are unchanged. Last-read recording coordinates canonical metadata readiness with actual draw completion, including fast bundled pages.

`AyahId(surahNumber, ayahNumber)` and Ayah are unchanged. There is no pageNumber on canonical Ayah. QuranRepository's original Tanzil `madina_hafs_604` map remains frozen. The independent `quran_ws_hafs_kfqc_b4155f0` MushafEdition, MushafPageMap and MushafPageAssetProvider own artwork, polygon geometry and page-first/last/surah-start relationships. The production catalog is derived from the full audited edition-page-map, with a pinned manifest of its own. Tanzil boundaries provide a canonical juz label from the page's first ayah, not a page-layout calculation.

SharedPreferences preserves the last successfully drawn edition/page. Older text-reader positions migrate via canonical ayah into the current artwork edition; a saved Tanzil page number is never reused blindly. SavedStateHandle restores recreation. Existing dark-mode and legacy text-size preferences are preserved; SVG reading uses pinch only rather than reflowing text.

ReadingPreferences also stores a last-read timestamp, keep-awake/automatic-resume/initial-bars options and a small JSON list of local page bookmarks. ReadingBookmark owns edition/page plus optional canonical ayah context; canonical Ayah is unchanged. Malformed or invalid bookmark rows are rejected against the official surah counts. Automatic resume applies only to a fresh launch and never overwrites a restored screen. The index filters only surah names, without touching ayah text. Thirty official canonical juz starts resolve through QuranSvgEdition.pageForAyah; Tanzil page starts do not drive artwork jumps.

DownloadedPageCache is a settings-only file-management helper scoped to the current artwork edition directory. Before clearing, the controller invalidates reader work and queues foreground/download barriers so cancelled writers cannot repopulate the cache after deletion. Disk-size calculation and deletion run off the main thread. Canonical database/assets, seed pages, polygon JSON and SharedPreferences live elsewhere and are never deleted. The existing transfer, verification, prioritization and bounded-memory methods remain unchanged.

Support/about reuse the current lightweight settings screen. Support is explanatory text only, with no provider, payment state or network call. About separates independent Quran.ws from its documented upstream KFQC source and retains source notices/licenses. A privacy preparation placeholder has no URL. Shared visual dimensions and system-bar handling apply to host UI; none alter artwork geometry, highlight or touch matrices.

The approved prebuilt database is atomically copied if needed, hash-verified, opened read-only with query_only and never populated by an initial ayah import. All11 approved artifact hashes are checked before builds. No schema/source text/normalization changes are permitted for UI work.

## Assets and dependencies

Full originals remain under ignored tools/mushaf-svg/local-assets. Variant-generated staging packages only the seed SVG list from config/mushaf-page-assets.json and all604 JSON/map/source manifests/hashes/licenses. Originals are never deleted or rewritten. The downloader takes an explicit configurable base URL, with no upstream URL fallback. Release defaults to no endpoint and requires HTTPS if configured; debug can use a loopback/emulator HTTP test host. INTERNET is the only new normal permission. HttpURLConnection, SHA-256, FileOutputStream and same-directory atomic rename cover downloading without an added networking dependency.

Disk cache is namespaced by the pinned edition/version. Every cache read verifies expected length and SHA-256. Downloads have 5s connect/read timeouts, a 30s total transfer deadline, a two-attempt ceiling, size limits and cancellation checks; only a verified completed file gains the final .svg name. Orphan .part files are never opened. Runtime artwork startup parses no SVG or whole-corpus hashes. A page-area loading/error/retry overlay is host UI and changes none of the view's drawing/gesture/matrix/highlight code.

The old image/rectangle runtime and raster-only trials were removed after the integration gate passed. Useful identity/bounds/scaling/heading/stale-touch tests now exercise the production polygon reader. The optional five-page native validation app shares production sources. Original manifests, notices and historical audit/provenance reports remain. The old unmodified upstream audit's marker-format incompatibility is documented in tools/svg-native/clean-audit; user explicitly accepted that diagnosed tool failure as nonblocking.

## Future recitations — design only

Host MP3s and a versioned JSON catalog over HTTPS outside the APK. The catalog lists stable reciter/track IDs, surah numbers, recording revisions, audio URLs, optional digest/length, and available timing URLs/revisions. Publishing a new surah uploads its MP3 and updates the catalog; compatible additions need no APK release. Cache the last valid catalog and expose only available recordings. Hosting provider and concrete client/player dependencies are chosen later.

Only Abdul Kabir is initially planned. Avoid a fixed one-reciter enum in data contracts; a stable reciter ID permits later entries. Do not implement other readers now.

Timing items use independent ayah references and `startMs`/`endMs`, and identify the exact track/audio revision. A replaced MP3 must not silently use old timestamps. Verify numbering/bismillah correspondence before enabling tap-to-play. Playback may later choose a completed downloaded file or the remote stream, with a standard Android media stack assessed for API 23.

No audio player/catalog, download manager, foreground service, hosting upload, timing generator or media dependency is implemented. The page-asset HTTP client is independent of the deferred audio design.

## Validation

API23/36 unit checks use Robolectric. Device instrumentation measures actual main-app first draws, bounded cache, canonical hit selection and process PSS on an API23 emulator; those software-emulated timings are distinct from phone performance. User-reported physical modern-device approval covers the shared standalone renderer. Main-app physical-phone measurements require a later user run. Arabic explanations/resources; English code, identifiers, comments and technical documents.
