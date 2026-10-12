# Architecture

## Main offline page reader

Native XML -> MainActivity -> MainViewModel and MushafReaderController.
MainViewModel loads canonical metadata from the existing QuranRepository/read-only SQLite, reads the small QuranSvgEdition catalog and maintains navigation via SavedStateHandle/StateFlow. It does not parse artwork. The UI binds Arabic index/settings/page controls; the SVG is the entire visible page, with no reflowed Quran text over it.

MushafReaderController owns one background loader worker. It releases the old view before a new load, rejects stale callbacks and loads only the selected page. NativeSvgPageLoader implements the edition asset provider with a standard LRU bounded to3 documents; current, previous and next are pruned on distant jumps. Prefetch runs after the current view draw, checks cancellation between pages and cannot block that first draw. Leaving the reader evicts retained documents; destruction closes the worker/repository.

NativeSvgPageView directly renders AndroidSVG1.4. One source-to-screen Matrix and its inverse handle drawing, highlighting and hit testing. No RTL reflection. Below API26 the approved software-layer fallback is unchanged; modern APIs keep the default hardware path. The view fits the source presentation box with preserved aspect ratio, supports vector zoom and RTL fling navigation at1x. Long press returns only AyahId; ordinary tap does not select. Source polygons with multiple components are grouped under one ayah. Selection remains temporary.

QuranSvgPageAdapter is the sole external polygon-format parser. It rejects unsupported syntax, invalid canonical IDs, inconsistent edition/page identities and out-of-bounds coordinates. OpeningPageHitOverrides uses the unchanged local reviewed file only on pages1/2; official SVG/JSON bytes remain intact. Heading and unnumbered basmalah artwork has no canonical ayah identity.

## Separate edition and canonical data

`AyahId(surahNumber, ayahNumber)` and Ayah are unchanged. There is no pageNumber on canonical Ayah. QuranRepository's original Tanzil `madina_hafs_604` map remains frozen. The independent `quran_ws_hafs_kfqc_b4155f0` MushafEdition, MushafPageMap and MushafPageAssetProvider own artwork, polygon geometry and page-first/last/surah-start relationships. The production catalog is derived from the full audited edition-page-map, with a pinned manifest of its own. Tanzil boundaries provide a canonical juz label from the page's first ayah, not a page-layout calculation.

SharedPreferences preserves the last successfully drawn edition/page. Older text-reader positions migrate via canonical ayah into the current artwork edition; a saved Tanzil page number is never reused blindly. SavedStateHandle restores recreation. Existing dark-mode and legacy text-size preferences are preserved; SVG reading uses pinch/buttons rather than reflowing text.

The approved prebuilt database is atomically copied if needed, hash-verified, opened read-only with query_only and never populated by an initial ayah import. All11 approved artifact hashes are checked before builds. No schema/source text/normalization changes are permitted for UI work.

## Assets and dependencies

Full originals are restored to ignored tools/mushaf-svg/local-assets and packaged via main assets sourceSet. Exactly604 SVG/604 JSON; no surah crops. Committed manifest fields include source URL/repository, pinned commit, stable release, edition, filename/length/SHA-256, notices and archive provenance. Build tasks verify assets, frozen Quran references and the AndroidSVG AAR before packaging. No full-artwork hash scan or SVG preload occurs at runtime startup.

The old image/rectangle runtime and raster-only trials were removed after the integration gate passed. Useful identity/bounds/scaling/heading/stale-touch tests now exercise the production polygon reader. The optional five-page native validation app shares production sources. Original manifests, notices and historical audit/provenance reports remain. The old unmodified upstream audit's marker-format incompatibility is documented in tools/svg-native/clean-audit; user explicitly accepted that diagnosed tool failure as nonblocking.

## Future recitations — design only

Host MP3s and a versioned JSON catalog over HTTPS outside the APK. The catalog lists stable reciter/track IDs, surah numbers, recording revisions, audio URLs, optional digest/length, and available timing URLs/revisions. Publishing a new surah uploads its MP3 and updates the catalog; compatible additions need no APK release. Cache the last valid catalog and expose only available recordings. Hosting provider and concrete client/player dependencies are chosen later.

Only Abdul Kabir is initially planned. Avoid a fixed one-reciter enum in data contracts; a stable reciter ID permits later entries. Do not implement other readers now.

Timing items use independent ayah references and `startMs`/`endMs`, and identify the exact track/audio revision. A replaced MP3 must not silently use old timestamps. Verify numbering/bismillah correspondence before enabling tap-to-play. Playback may later choose a completed downloaded file or the remote stream, with a standard Android media stack assessed for API 23.

No player, catalog fetch, download manager, foreground service, network permission, hosting upload, timing generator, or media dependency is added by this milestone.

## Validation

API23/36 unit checks use Robolectric. Device instrumentation measures actual main-app first draws, bounded cache, canonical hit selection and process PSS on an API23 emulator; those software-emulated timings are distinct from phone performance. User-reported physical modern-device approval covers the shared standalone renderer. Main-app physical-phone measurements require a later user run. Arabic explanations/resources; English code, identifiers, comments and technical documents.
