# Main integration — offline Medina/Hafs SVG reader

User authorized main integration after approving the shared standalone renderer,
long press, selection and zoom on a real modern Android phone. This report covers
main app0.5.0; no subsequent feature milestone or distribution was started.

1. **604 pages integrated:** yes, the main reader now uses full-page Direct SVG.
2. **604 SVG +604 JSON:** yes; original full pages only, no surah crops. All1216
   manifest records, plus the manifest itself, were verified inside Debug APK,
   unsigned Release APK and Release AAB. `verify_artifacts.py` reproduces this.
3. **minSdk:** unchanged23; compile/target36. Manifest evidence is preserved.
4. **INTERNET:** absent from both actual APK manifests. The existing AndroidX
   private dynamic-receiver permission is unrelated to network access.
5. **Tanzil unchanged:** all11 approved references, SQLite and source text hashes
   match. Database remains2,170,880 bytes, SHA-256
   `8ed100334e8eca42124035f51fffe934d52b20e5224b3a44c1a33b44dba74416`.
   Canonical Ayah/AyahId are unchanged; there is no page field on Ayah.
6. **Independent edition map:** `quran_ws_hafs_kfqc_b4155f0`, derived from the full
   audited edition map. Page121 starts5:78; Tanzil still starts5:77. The36-page
   layout difference is preserved; Tanzil PageStart is never used for SVG layout.
7. **RTL swipe:** rightward fling advances, leftward fling returns. Artwork and
   hit coordinates are not reflected. Swipe at1x; pinch/buttons provide1x–4x zoom.
8. **Surah index:** all114 Arabic names/numbers/counts and official classifications.
   Selecting a surah opens its first artwork page, including other surahs/verses
   on that whole page. All114 starts checked against official per-page IDs.
9. **Long press:** returns canonical AyahId; all component polygons are highlighted.
   Normal tap does not select. Temporary highlight/debug reference retained; no
   final selection sheet or highlight redesign.
10. **Opening overrides:** original local override bytes unchanged, pages1/2 only.
    All1:1–1:7 and2:1–2:5 tested. Reviewed ink notches, headings, unnumbered
    basmalah, scaling/zoom and stale pending-gesture regressions were transferred
    to the main production reader. Native ink coverage remains covered by the
    retained standalone suite.
11. **Pages3–604:** official polygons directly, no estimation, rectangular source
    map or next-ayah guessing. Identity/sequence/bounds pass over the entire
    corpus; independent exact geometry reports zero positive-area overlaps.
12. **Cache:** standard LRU, max3 current/adjacent documents. Distant jumps prune
    unrelated documents. Prefetch starts after first draw and checks cancellation
    between pages. Leaving the reader evicts documents; destruction closes its
    worker/repository. Zero SVG parsing at the index, proven on the API23 device.
13. **Rendering cutoff:** unchanged; native software view layer for API<26,
    default hardware rendering on API>=26. AndroidSVG1.4 only, approved AAR202,395
    bytes, SHA-256 `02a5b08a2b35d2d58eb2eaca9d84ac00fb341da725fdbd653ea3ed130437e95a`.
    Its Apache2.0 license/copyright and metadata ship separately from artwork.
14. **Tests:**38 main executions across Robolectric SDK23/36,13 shared standalone
    executions and4 independent-auditor regressions passed, with zero skipped or
    failed cases. Full structural audit:604/604,114 surahs,6236 canonical IDs,
    zero invalid/missing IDs/unexplained duplicates/final positive overlaps;
    final114:6. Distributed SVG parse sampling covers18 pages from1 to604.
    Actual Google API23 emulator instrumentation passes reference-page draws,
    bounded cache and long press114:6. Modern physical-device approval is the
    user's earlier standalone test; main-app physical-phone timings are not
    claimed. The diagnosed unmodified upstream tool failure remains preserved
    in clean-audit and was explicitly accepted as nonblocking by the user.
15. **Lint:** Debug and Release each0 errors/4 warnings: two portrait-orientation
    notices, newer test-only Robolectric availability, and Canvas KTX suggestion.
    The shared renderer keeps its approved native matrix code. Android16 may
    ignore orientation restrictions on larger displays; the existing activity
    compatibility property is retained.
16. **Debug APK:**139,177,279 bytes (132.73MiB),
    `app/build/outputs/apk/debug/app-debug.apk`.
    SHA-256 `3fb3f221973add8582945ebe04bafd0067f05095ff25b288fe5f04b5ade9026f`.
17. **Release outputs:** unsigned APK104,916,870 bytes (100.06MiB),
    `app/build/outputs/apk/release/app-release-unsigned.apk`; AAB105,553,635 bytes
    (100.66MiB), `app/build/outputs/bundle/release/app-release.aab`.
    No production signing, upload or publishing. SHA-256 values in validation.json.
18. **Actual growth:** baseline was built immediately before integration with the
    same toolchain. Debug4,380,621 ->139,177,279 (+134,796,658 bytes);
    Release APK1,323,859 ->104,916,870 (+103,593,011);
    AAB1,820,820 ->105,553,635 (+103,732,815). These are whole-build deltas, including
    renderer/code cleanup. Actual compressed mushaf payload including catalog,
    overrides/licenses is134,076,393 bytes in Debug and103,292,386 in Release/AAB,
    excluding ZIP entry overhead and the manifest's own compressed payload.
    Original SVG raw372,827,329 +JSON1,151,611 bytes are unchanged. ZIP estimates
    from the earlier audit were not used as final application sizes.
19. **Measurements:** final Debug APK SHA matches the instrumented candidate.
    Same API23 AVD, software CPU emulation, process-cold first-window median from
    three trials: baseline4,208ms; final4,199ms; -9ms is within noise, not a speedup
    claim. Final index metadata readiness4,872ms. First mushaf page request to
    actual View draw1,604ms (SVG parse353ms, render314ms). Eight process-warm
    transitions including distant misses: median2,707ms; two cache-hit transitions:
    median461ms. Retained documents max3. Four completed post-prefetch PSS samples:
    maximum44,838KiB (43.79MiB), not a continuous/instantaneous peak. Every API23
    draw uses the approved software layer. Timings include scheduling/queued
    loading, exclude compositor presentation and are not physical-phone values.
    Debug keeps at most128 local metric rows; Release collects/saves no debug
    metrics or PSS. Startup metadata is small; no604-document parsing/hash sweep.
20. **Removed old consumers:** debug image entry/activity/provider and build flag;
    temporary PNG/aya fixtures; old marker/line-band estimation; rectangle page
    implementation and MushafTouchOverlayView; overlay-prototype and unused
    raster-only svg-prototype; obsolete preparation/validation/conversion tools
    and old text-list presentation. Removed-paths.json records49 old paths.
    Gate tests passed before removal. Useful source/geometry/heading/zoom/gesture
    regressions were transferred rather than discarded.
21. **Remaining old references:** zero in runtime/build consumers. Historical
    mentions remain in CHANGELOG and retained provenance/validation reports,
    license evidence and source manifests. Whole-repo search results are in
    remaining-historical-references.txt. They do not contribute production assets,
    code, attribution UI or dependencies. No Open Mushaf assets in APK/AAB.
22. **Retained older files and reasons:** frozen canonical data/repository/models;
    generic edition/map/asset interfaces; official source locks/licenses/archive
    provenance; opening rationale/overrides; original upstream audit diagnosis and
    independent audit tools; optional five-page direct-native prototype sharing
    production code; legacy preference keys for safe migration. A recoverable
    pre-cleanup source/APK archive is outside the project at
    `/workspace/task-backups/main-integration-before`, not an application asset.
23. **Next selection UI phase:** reader infrastructure is complete and ready for
    separate user review/authorization. No selection sheet, final highlight,
    audio, tafsir or other feature started. Main APK physical-phone review is
    recommended; shared modern renderer approval and simulated main checks are
    stated separately.

Source: independent Quran.ws technical project, pinned commit
`b4155f07e4aea087d2c458069acff9f7c7e749f6`, stable asset releasev1.1.1, Hafs/KFQC.
Upstream reports artwork provenance as KFQC Hafs1441,
`1441-AI-hafs.zip`, SHA-256
`280c5d71ca16aaeeb3a343be1b92c76fa6df71d0671e202c026fde12402d9eef`.
We did not download that original archive directly from the Complex and do not
claim independent verification of it. Quran.ws is not presented as a government
entity. Original LICENSE/NOTICE/PROVENANCE, CC-BY4.0 contribution terms and
AndroidSVG's separate notice are packaged; Tanzil attribution is retained.

**MAIN_INTEGRATION_COMPLETE**
