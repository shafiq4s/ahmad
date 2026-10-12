# Main offline Quran SVG assets

Use the pinned complete manifest; do not regenerate/rebaseline Quran artwork or
canonical data. Prepare with `python3 tools/mushaf-svg/prepare_assets.py`, then
build the main application normally. `verify_assets.py` is a Gradle preBuild
requirement. It verifies every page, local edition catalog, opening override,
license notice and all11 Tanzil reference hashes.

Exactly604 full-page SVG and604 per-page JSON originals from
`mushafs/hafs/kfqc/`, commit `b4155f07e4aea087d2c458069acff9f7c7e749f6`, stable
assets v1.1.1. No surah crops. Committed local-assets is included in main assets,
not in the optional five-page validation APK. A fresh checkout includes these files; restoration uses the pinned
manifest URLs if original local cache files are unavailable; no application
network capability is added.

`pages.json` is our small runtime catalog derived from the audited edition map,
with original first/last ayah and surah-start identities. Original JSON/SVG remain
byte-identical. The opening presentation boxes and hit overrides are unchanged.
The asset manifest records every filename/byte count/SHA-256, source/provenance,
notices and the original archive digest reported by upstream. Source artwork,
Quran.ws polygons/tools, AndroidSVG and Tanzil have distinct license notices.

`integration-gate.json` records successful checks before old consumer removal;
`removed-paths.json` records the removed paths. A recoverable source/APK snapshot
was preserved outside the checkout at `/workspace/task-backups/main-integration-before`.
Historical source manifests and license reports remain in the repository, without
runtime assets or consumers. Regression tests now exercise the production reader.

Run actual Android instrumentation after installing Debug and androidTest APKs:

```sh
adb shell am instrument -w com.ahmad.quran.test/com.ahmad.quran.MushafInstrumentation
adb exec-out run-as com.ahmad.quran cat files/main-integration-device.json
```

Require `success=true`, not only shell exit0. The instrumentation uses the actual
main Activity/View and verifies zero SVG parses at the index, first draw of
reference pages, cache<=3 and canonical long press. Debug-only local measurement
files include timing/render mode and post-prefetch process PSS. Debug retains at most128 rows; Release collects no debug samples. They are not sent
anywhere; Release saves no debug measurements. Values from a software-emulated
API23 device must not be represented as physical-phone timings. Modern-device
approval was supplied by the user for the shared standalone renderer; new main
application phone measurements remain a separate validation item.

See REPORT.md and validation.json for actual final sizes, tests, limitations and
verdict. Do not start the selection sheet, final highlight, audio or other feature
milestone after the report without user authorization.
