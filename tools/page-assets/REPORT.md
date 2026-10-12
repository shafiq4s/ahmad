# Verified on-demand page assets

The main APK/AAB now retains eleven unchanged SVG seeds: 1–5, 300 and 600–604. Change only config/mushaf-page-assets.json to adjust that list. All604 official polygon JSON, complete edition page map, source manifest/hashes, licenses/NOTICE/provenance and original Tanzil assets remain packaged. All604 original SVG remain in the source workspace. Variant-generated staging changes packaging only; verify_assets.py still verifies all1208 originals and11 approved Tanzil references.

## Storage and scheduling

OnDemandPageAssets supplies the existing edition asset contract in this order: bundled, validated disk, configured remote. Native HttpURLConnection avoids a new networking dependency. Reads/downloads run on workers. Each transfer uses 5s connect/read timeouts, a 30s transfer deadline, a two-attempt ceiling, a pinned size limit, a temporary same-directory file, exact length/SHA-256 verification and atomic rename. Existing cache files are verified before use; invalid files are deleted and partial/orphan .part files are never opened. Cache is cacheDir/mushaf-pages/quran_ws_hafs_kfqc_b4155f0/. Ordinary Android cache storage may be reclaimed by the OS.

One foreground worker loads/parses the requested page. A separate worker downloads only raw neighbours page-2/page-1/page+1/page+2, clamped to1–604, after the requested page's first actual view draw. Foreground jumps do not queue behind this optional worker; stale generation checks cancel old work. Per-page locks coalesce simultaneous transfers. Memory prefetch parses only nearest locally available neighbours, never starts a download, and retains at most three parsed SVG documents. Disk cache lifetime is independent of view/controller lifetime.

The host shows quiet Arabic loading/error/retry inside the page area. Failed requests remain stable until explicit retry, without an automatic state-flow retry loop. The original view, polygons, opening overrides, highlight, modal, gestures, edition/map, navigation ViewModel and preferences match all11 frozen reading hashes. All source and canonical bytes remain unchanged; no corpus rebuild occurs.

## Endpoint and network policy

INTERNET is the only new normal permission. Both final default base URLs are empty: no production endpoint or public temporary service is used. Configure the Gradle property quranPageBaseUrl to change hosting, without changing the downloader. Requests append /<editionId>/<three-digit-page>.svg. Release requires HTTPS. Debug permits HTTP only on localhost,127.0.0.1 or10.0.2.2, with a debug-only manifest allowance. No Cloudflare or audio work is implemented. Non-seed, uncached pages require a configured server; default packages deliberately show a page failure/retry message for them.

For local validation, tools/page-assets/local_server.py serves only119–123 from pinned originals and verifies them before sending. The server is not part of the application. Use adb reverse tcp:8773 tcp:8773 with the local debug build shown in README. The preserved app/build/outputs/local-validation/app-debug-local.apk is a temporary loopback-configured validation build, not the default package.

## Verification

The clean debug/release APK and AAB builds pass. All91 regression executions pass (78 app,13 shared-renderer), with zero failures/errors/skips on SDK23/36. Debug and release Lint each have zero errors and the same nine previous warnings. These include fixed portrait restrictions, newer test dependency, deliberate compact-modal weight, root background overdraw, unused legacy label, bar animation wrappers and the optional KTX suggestion. No new library or lint suppression was added.

Coverage includes bundled1/300/604 with no requested-page network,604 bounds excluding605/606, foreground121 before first draw/prefetch, exact ordered raw prefetch119/120/122/123, five cached files with only three parsed documents, a distant400 request while old119 transfer is blocked, offline fresh-provider reuse, same-page/coalesced requests, wrong SHA at unchanged length, truncation, bounded stalled-body timeout, retry recovery, corrupt cache deletion, orphan/other-edition isolation, empty/unsafe endpoints and Arabic failure/retry recovery. Existing canonical IDs/text, hit testing, zoom, long press, modal, clipboard/share and navigation tests remain active. Rendering regressions read pinned workspace artwork into test-only disk cache; no extra fixture SVG is packaged.

Actual API23 emulator instrumentation also passes: bundled1, requested121 download/hash/cache/render, all four raw neighbours with exact hashes, parser memory capped at3, then reopen121 after clearing document memory and stopping the host HTTP server. It subsequently renders bundled300/604 and confirms last ID114:6. The server received exactly121,119,120,122,123, all200;121 is requested only once. See device-api23.json, cache-ready-api23.json and local-server-validation.json. The compositor capture screenshots/device-api23-offline-121.png shows the original page121 rendered from disk after server shutdown; it was visually reviewed without a blocking system/application dialog. The loopback server and ADB reverse were removed after the test. Software-emulator timing is not presented as phone performance.

## Sizes and artifacts

| Artifact | Previous stored build (bytes) | New (bytes) | Decrease |
| --- | ---: | ---: | ---: |
| Debug APK | 141,791,033 | 6,792,690 | 134,998,343 / 95.21% |
| Release APK, unsigned | 104,916,870 | 3,200,690 | 101,716,180 / 96.95% |
| Release AAB | 105,553,635 | 3,822,352 | 101,731,283 / 96.38% |

The prior debug baseline is the immediately approved reader UI build; prior release/AAB are the existing earlier integration artifacts, recorded before this task. Seed SVG total4,963,718 raw bytes; packaged compressed bytes are1,719,085 in debug and1,320,888 in release/AAB. Sizes are archive sizes, not installed storage or Play-delivered per-device splits.

The final Debug APK is app/build/outputs/apk/debug/app-debug.apk (6.79 decimal MB), SHA-256 a90b450afe19cf151a64272535944301fc90bbe9e326b3137916ba3951ac3e52. Build-tools verifies its signature; minSdk23/target36 are unchanged. Release APK is unsigned because no release signing configuration was requested. verify_packages.py checks exact seed membership,604 JSON and byte equality for every retained asset in all three archives, empty final endpoints, unchanged reading references and recorded artifact hashes. See package-validation.json and test-validation.json. Final default debug was recreated after the local test and is byte-identical to the clean default build, with no embedded loopback URL.

Incremental APK updates can retain unused ZIP space when hundreds of entries are removed. A clean package was used for the size comparison; no source artwork was removed or rewritten. Stop after this gate; no audio or hosting deployment follows.
