# Roadmap

The old memorization/revision/mistake/teacher/student/Supabase roadmap is cancelled. Do not resume it. Keep work limited to the latest authorized milestone.

## 1. Product cleanup and reading shell — completed and approved

- [x] Retain Kotlin/XML, Arabic/RTL, AppCompat/Lifecycle, API 23, tooling, colors, and icon.
- [x] Remove profile/role persistence, Room/KSP, Supabase configuration, obsolete tests/schema, and the old sync proposal.
- [x] Rename the project/theme/application label for Quran reading.
- [x] Add Quran-index, reader, and settings shells with honest empty states and restored navigation.
- [x] Replace product/architecture/database/docs and retain the historical CHANGELOG with a direction-change entry.
- [x] Document prebuilt read-only SQLite, independent ayah IDs, edition-scoped extensible maps, and deferred audio design.
- [x] Run clean/final debug builds: four API 23/API 36 test executions passed; lint has zero errors/one test-library update notice; signature/dependency/artifact checks passed. Debug APK is 3,576,979 bytes.
- [x] Record the cleanup results for user review in README/CHANGELOG; The user subsequently approved Quran integration.

## 2. Local Quran reading — completed and approved

- [x] Re-read the official license first; recover the exact approved files and match all three source SHA-256 pins.
- [x] Retain unchanged originals, provenance, copyright notices and manifest.
- [x] Generate complete prebuilt SQLite and verify all 6,236 stored texts exactly; no runtime import.
- [x] Implement read-only QuranRepository and independent Ayah/TextVersion/MushafEdition models.
- [x] Integrate the real 114-surah index, surah reading, edition-scoped 604-page navigation and attribution.
- [x] Preserve deferred audio architecture without implementing it.
- [x] Clean build, 14 API 23/36 test executions, lint (zero errors/one test-library update warning), signature/artifact checks passed. Database: 2,170,880 bytes. APK: 4,310,103 bytes; increase: 733,124 bytes. See VALIDATION.md.

The user approved this stage and fixed its corpus/source/hash references. Do not rebuild/change those references for UI work; first explain a documented reason for any future data change.

## 3. Reading Experience V1 — completed and approved

- [x] Separate surah number/name, official metadata classification and ayah count.
- [x] Clean literal surah text, separate reference badges and cached recycled rows.
- [x] Direct page mode with correct 1–604 boundaries, previous/next/input and local juz labels.
- [x] Persist last visible ayah/page/edition/mode and offer resume after restart.
- [x] Persist 20–40sp size and an app-local light/dark theme with existing dependencies.
- [x] Preserve every approved reference hash; no corpus rebuild or schema change.
- [x] Clean build and 26 API 23/36 executions passed; lint zero errors/one test-only update warning; fixed-reference/signature/asset checks passed. APK: 4,345,924 bytes (+35,821). See VALIDATION.md.

## Main SVG integration — current authorized milestone

- [x] Pinned604 full-page SVG/JSON pairs and full-manifest build verification.
- [x] Independent Quran.ws artwork edition and canonical-identity resume migration.
- [x] Native SVG rendering, RTL page navigation, long press and zoom; unchanged opening overrides/API cutoff.
- [x] Bounded3-document cache; startup has no artwork parsing; post-draw neighbor prefetch.
- [x] Integration gate before legacy consumer removal; transferred useful regression coverage.
- [x] Recorded full validation/build/Lint, actual APK/AAB sizes and bounded API23 device measurements in tools/mushaf-svg/REPORT.md.

Stop after the integration report. Selection UI and recitations remain separately gated.

## 4. Recitations — deferred, no implementation authorized

External Abdul Kabir surah MP3 hosting and versioned catalog; streaming and completed offline downloads; later track-revision-specific ayah timings and tap-to-play. Catalog contracts permit future reciters, without implementing more readers now. Choose media/hosting tools when this milestone is authorized.

## 5. Additional Mushaf editions — deferred

Acquire verified, licensed edition data and typography/layout for Indo-Pak/Pakistani 16-Line. Add edition-specific maps; preserve canonical ayah identity and verify numbering correspondence. Do not generate layouts from Medina page starts.

No advertising or AI.
