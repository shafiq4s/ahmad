# Changelog

## Unreleased — Local reading experience

- Unified quiet green/ivory surfaces, shared corner/spacing tokens and compatible system bars. Kept the approved SVG renderer, selection, touch polygons and gestures unchanged.
- Added timestamped last-read state, a surah/page resume card and optional cold-start resume; saved-screen recreation still takes precedence.
- Added local page bookmarks with optional canonical ayah context, a compact list, footer toggle and add/remove in the existing ayah dialog. Copy/share remain text-only; playback and tafsir remain disabled.
- Added surah-name filtering and a single page/juz/surah jump dialog. Juz targets resolve official canonical starts through the artwork edition, never Tanzil page starts.
- Added persisted keep-awake and initial-bar preferences, readable disk-cache size and confirmed clearing of downloaded artwork after cancellation barriers. Seeds, canonical data, geometry, bookmarks and reading state remain intact.
- Added support/about sections, honest future optional-support and privacy placeholders, and independent Quran.ws/KFQC provenance. No payment, audio, hosting or dependency change. See tools/reading-experience/REPORT.md.

## Unreleased — Verified on-demand SVG pages

- Package configurable original seeds (1–5, 300, 600–604), all604 polygons/maps/manifests/licenses and unchanged Tanzil; keep all originals in the source workspace.
- Resolve bundled artwork, verified edition/version disk cache, then an explicit configurable source. Use native HttpURLConnection with timeouts, bounded retry, exact length/SHA-256 and atomic temporary-file commit.
- Download raw neighbours ±2 after the requested page draws on an independent worker; keep parsed SVG memory capped at3 and prioritize distant foreground requests.
- Add INTERNET and a debug-only local HTTP allowance, with empty default base URLs and no production domain. Add Arabic loading/failure/retry inside the page area. No networking library or audio feature.
- Preserve text, source polygons/opening overrides, highlight, modal, gestures, renderer, canonical data, edition/page mapping and navigation. See tools/page-assets/REPORT.md.

## Unreleased — Quiet reader chrome

- Restyled the RTL header with current surah and a separate juz/page caption; added simple native index/settings icons with 48dp touch targets.
- Restyled the footer with Arabic previous/next arrows, current page and a compact native page-entry dialog. Removed production zoom/debug controls; pinch remains unchanged.
- Blank taps animate bar height/alpha over 190ms to release page space; the existing renderer handles viewport resizing. Added a light 110ms page reveal without changing cache or navigation.
- Kept the existing ListView and added artwork-edition first-page labels; restyled existing settings with app version/source and pinch help.
- Preserved renderer, hit testing, highlight, long press, ayah dialog, source polygons/overrides, edition, Tanzil, Gradle dependencies and manifest byte-for-byte. See tools/reader-design/REPORT.md.

## Unreleased — Line selection and centered ayah options

- Replaced ink-hugging patches with straight pale-green row strips, uniform per-page height/gaps and gentle rounded corners. Source polygons, opening hit overrides and artwork remain immutable; no selected-ayah raster analysis remains.
- Ordinary taps select only; platform-timed long presses select and open one centered RTL dialog. Blank taps fade existing reader controls without changing the artwork viewport. Pinch, matrices, RTL fling navigation and cache remain unchanged.
- Replaced the Bottom Sheet with a bounded, scrolling AppCompat dialog, explicit close and light window dim. Retained exact Tanzil text/metadata, artwork-edition page number, text-only copy/share and disabled playback/tafsir. Removed the now-unused Material dependency; added no library.
- Added one hidden, default-off future recitation-on-tap preference, without playback, URLs, audio assets or network permission. See tools/reader-ui/REPORT.md for validation.

## Unreleased — Canonical ayah bottom sheet

- Added one RTL Material bottom sheet with surah/ayah/juz/current artwork-page metadata and verbatim canonical Tanzil text. It initially occupies 48% of the viewport and expands to 88%, with nested text scrolling and visible actions.
- Retained the approved highlight while the sheet is open; Back, outside taps and downward dismissal use its unchanged fade-out. Another long press updates the same sheet. Touch polygons, matrices, highlight appearance, navigation methods, document cache and Quran reference bytes are unchanged.
- Enabled text-only clipboard copy and native Android Sharesheet; play and tafsir are visibly disabled as coming later. Added no audio/tafsir implementation.
- Added Google Material 1.12.0 (Apache-2.0) for standard dragging, nested scrolling and accessibility, with its official license packaged. See tools/ayah-sheet/REPORT.md for tests, scope and native automated captures.

## Unreleased — Ayah selection appearance

- Added a soft honey backdrop at 15% opacity with a 140ms selection/clear transition; original SVG ink renders above it.
- Tightened presentation-only selection paths with a nominal 2.5dp inset and rounded local ink envelopes. A bounded, temporary selected-ayah analysis reduces the inset around letters, diacritics and verse markers as needed.
- Preserved source polygons, opening hit overrides, touch/zoom matrices, canonical ayah IDs, navigation, document cache and all Quran reference bytes. Added no dependency or feature.
- Validated pages 1, 2, 48, 121 and 604 at 1x/2x/4x; 45 ink-safety cases passed with no measured lost ink. See tools/ayah-highlight/REPORT.md for test scope and native before/after captures.

## 0.5.0 — Main offline SVG reader integration

- Integrated all604 original Hafs/KFQC SVG/JSON pairs pinned at b4155f07e4aea087d2c458069acff9f7c7e749f6, with complete build-verified manifest and independent audited edition map.
- Reused the real-device-approved direct AndroidSVG1.4 renderer and unchanged opening overrides; retained API<26 software fallback, API23 minimum and offline operation.
- Linked surah index, RTL swipes, previous/next/direct pages, zoom and resume to the artwork edition. Migrated legacy Tanzil locations by canonical ayah. Startup parses no SVG; cache retains at most3 neighboring documents with post-draw prefetch.
- Removed obsolete image/rectangle runtime, temporary artwork/coordinates, unused preparation scripts and raster-only modules only after the new integration gate passed. Transferred useful regression tests and preserved source manifests/provenance reports.
- Added Quran.ws/KFQC provenance and separate source/renderer licenses alongside unchanged Tanzil attribution. See tools/mushaf-svg/REPORT.md for final checks, measurements and actual APK/AAB sizes. No additional feature milestone started.

## Unreleased — Direct native SVG and opening hit-testing validation

- Added an isolated opt-in `svg-native-prototype`, using pinned Apache-2.0 AndroidSVG 1.4 only in that module, with five unchanged Hafs/KFQC SVG and JSON pairs. No WebP conversion or main-reader integration.
- Audited all six original opening-spread overlap pairs. Added edition-local, non-overlapping touch overrides with ink-sensitive notches; preserved every original SVG/JSON byte, Quran reference and page start. Pages 3–604 bypass overrides.
- Compared one/three-document bounded LRU caches, parse/render/content-ready timing, sampled heap retention/peaks and actual long presses. Recorded native Android emulator and simulated SDK results separately.
- Identified API 23 hardware-path blur in an actual activity capture; enabled a native software layer below API 26 so zoom redraws the original vectors sharply. Its viewport layer cost is documented separately from SVG-document memory.
- Kept all old Open Mushaf and raster prototypes. Release is disabled; no full corpus of page artwork, sound, tafsir, word selection, Engine or cloud added. See tools/svg-native/REPORT.md and OPENING_OVERRIDES.md.

## Unreleased — Quran SVG production candidate, five pages only

- Pinned stable Hafs/KFQC SVG and official polygon JSON at b4155f07e4aea087d2c458069acff9f7c7e749f6; the selected assets equal release v1.1.1. Preserved source digests, license notices and source-archive provenance.
- Added an isolated opt-in Kotlin/API 23 debug prototype with pre-rendered lossless WebP, independent polygon parsing, shared artwork/touch matrix and canonical ayah long press; no runtime dependency or WebView.
- Kept all Tanzil references, pagination and previous Open Mushaf tools unchanged. Detected and reported six official opening-spread overlap pairs; ambiguous hits fail closed.
- Only pages 1, 2, 48, 121 and 604 are packaged in the candidate. Release remains disabled; no audio, tafsir or full-mushaf integration. See tools/quran-svg/REPORT.md.

## Unreleased — Portrait image trial and image-rights investigation

- Recorded user-reported real-device approval of overlay alignment and canonical ayah hits.
- Restricted only the debug image activity to portrait, including a temporary activity-level API 36 large-screen opt-out; no coordinate/matrix/page-rule change.
- Traced all 604 Hafs image blobs across 69 branch heads to Open Mushaf commit 29445bcaff08ba218f3d5b84a95377951982a79a (2025-01-17, design/hafs), preceding the 2025-03-17 main-path addition. Recorded license, attribution and coordinate-lineage evidence; a separate SVG permission-named file is only a pending request.
- Kept image redistribution and conversion unapproved: the original official image package and its asset-specific rights are not established. No release images, sound, tafsir or dependencies added. See tools/image-provenance/REPORT.md.
- Built the updated temporary portrait APK (5,144,948 bytes); five existing API 23/36 UI executions passed, Lint had zero errors, all 11 Quran reference digests remained unchanged. See tools/image-provenance/portrait-validation.json.

## Unreleased — Edition-scoped image reader device trial

- Added main-app presentation contracts reusing MushafEdition, grouped canonical ayah regions and a validated page map, with a shared native image/touch matrix.
- Reused the corrected prototype geometry as one shared implementation; kept special page margins, basmalah and titles in the image edition layer.
- Added a default-off debug feature flag and nine pinned temporary images under an independent edition ID. Flagged APKs use a separate debug package; release includes no temporary image assets or test provider/activity.
- Added API 23/36 regression and integration checks for IDs, bounds, overlap, safe next ayah, edition isolation and scaled RTL touches.
- Kept all approved Quran reference files, SQLite and Tanzil pagination unchanged. No new dependencies or audio/tafsir/download features. Images remain unapproved for public redistribution; see tools/image-reader/README.md.
- Built only the main-app experimental APK (5,145,879 bytes); 62 passing API 23/36 executions, zero lint errors, 11 unchanged reference digests and zero temporary release images. See tools/image-reader/validation.json.

## Unreleased — Local overlay validation only

- Added an opt-in, separate Kotlin debug application with temporary pinned page samples, canonical verse hit regions, shared image/touch matrix and page diagnostics.
- Preserved all approved Quran sources, SQLite data and Tanzil page starts; the reader includes no prototype code or images.
- Recorded printed pagination differences and native Android debug captures; final image licensing and full visual validation remain pending. See tools/overlay-prototype/README.md.

## 0.4.0 — Reading Experience V1

- Preserved the approved Quran database, original sources, notices and manifest unchanged; added reference-lock regression checks.
- Added official Meccan/Medinan labels from the verbatim approved XML without rebuilding SQLite.
- Improved the surah index, separate verse references, cached recycled rows and textual page navigation with juz labels.
- Added local resume for the last visible ayah/page and persisted text size plus app-local light/dark theme through SharedPreferences.
- Reused native ListView and existing AppCompat DayNight; no added dependencies or network/audio features.
- Clean build, 26 API 23/36 executions, lint (zero errors/one test-only update warning), fixed-reference and APK checks passed. Debug APK: 4,345,924 bytes (+35,821); no physical device/frame-rate verification. See VALIDATION.md.

## 0.3.0 — Complete offline Quran reading

- Bundled a prebuilt read-only SQLite corpus from unchanged, SHA-pinned official Tanzil Uthmani 1.1 and metadata 1.0.
- Added independent ayah identity, text versions and edition-scoped 604-page/30-juz/60-hizb/240-quarter maps.
- Added QuranRepository, real 114-surah index, local surah reading and simple page navigation.
- Included Tanzil attribution and original copyright/license notices in Settings/About and derived packaging.
- Added full source-to-repository text checks, all page/juz coverage checks and API 23/36 offline/navigation/state tests.
- Retained deferred recitation design; added no production dependencies, network permission, audio, downloads, cloud features or other editions.
- Final clean build, 14 passing API 23/36 executions, zero lint errors/one test-library update warning, signature and asset checks passed. Database: 2,170,880 bytes. Debug APK: 4,310,103 bytes (+733,124 over cleanup). See VALIDATION.md; no physical device/emulator verification.

## 0.2.0 — Quran reading product direction

- Replaced the memorization/revision/mistake/teacher/student scope with Quran reading and future recitations.
- Kept Android 6+/Kotlin/XML, Arabic/RTL, toolchain, icon, colors, and historical release notes.
- Removed local profiles/roles, Room/KSP, Supabase configuration, their tests/schema, and the old sync proposal.
- Added index/reader/settings shells with honest absent-data states, Back navigation, and saved screen restoration.
- Renamed the project to QuranReader, theme to Theme.QuranReader, and app label to القرآن الكريم.
- Documented a prebuilt read-only SQLite corpus, independent surah/ayah identity, edition-specific extensible page maps, and deferred externally hosted recitations/timings.
- No Quran corpus/repository, audio implementation, account, advertising, AI, or new production library was added. Quran integration waits for the user's cleanup review.
- Verified clean/final debug builds, four passing API 23/API 36 test executions, zero lint errors and one test-library update notice, and APK signature. Debug APK: 3,576,979 bytes; 19.6% smaller than the previous foundation. No real device/emulator verification.

## 0.1.0 — Phase 0 foundation

- Created a native Kotlin/XML Android app with API 23 minimum support and SDK 36 target.
- Added an Arabic RTL shell, accessible text and tap targets, and four primary navigation entries with secondary Settings.
- Added a local Room profile with explicit name/role saves, retained drafts, transactional initialization, stable UUID, and exported schema.
- Added a validated Supabase publishable-client configuration holder without network or authentication implementation.
- Added focused configuration, local persistence, and Arabic navigation tests.
- Documented the incremental product, database, cloud security, and synchronization design.
