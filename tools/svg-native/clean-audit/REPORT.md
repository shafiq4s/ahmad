# Final Validation Gate — clean upstream reproduction

Date: 2026-10-11 (UTC evidence timestamps). Edition: Hafs / KFQC.
Pinned original commit: `b4155f07e4aea087d2c458069acff9f7c7e749f6`.
Scope: read-only source audit and five-page standalone device-validation APK.
No main-app integration. No generated source outputs adopted.

## 1. Did the clean original upstream audit pass?

No. The independent original Git checkout is clean before and after execution.
Original subset audit exits 1: four MARKERCOUNT and one UNCOVERED finding.
Original all-tier pages 3–604 audit exits 1: 602 MARKERCOUNT + 96 UNCOVERED = 698.
All 118 surah crops in the selected edition were also available to the tool;
they introduce no additional findings. Full stderr is empty. The finding set
exactly matches the earlier audit on the hash-verified downloaded fixtures.
This reproduces a failure; it does not turn the original audit into a pass.

## 2. Exact command

Working directory: `/workspace/quran-svg-upstream-validation`.

```bash
export PYTHONDONTWRITEBYTECODE=1
export PYTHONPATH=/workspace/.tools/svg-audit-python
export PATH="/workspace/.tools/librsvg-audit/usr/bin:$PATH"
/opt/codex/runtimes/codex-primary-runtime/dependencies/python/bin/python3 tools/audit_ayah_polygons.py --mushaf hafs --tier all --workers 4 --max-detail 100000 --pages 3-604 --json /workspace/ahmad/tools/svg-native/clean-audit/upstream-full.json
```

The first command used `--pages 3,48,121,604` and `upstream-subset.json`.
Exact argument arrays, environment overrides, stdout, stderr and exit codes are
preserved in the two `upstream-*.command.json` and associated files.

## 3. Dependency versions

| Component | Actual version |
| --- | --- |
| OS | Debian GNU/Linux 13.6 (trixie) |
| Kernel | 6.18.44 |
| Python | 3.12.14 |
| NumPy | 2.5.4 |
| Pillow | 12.3.0 |
| Brotli | 1.1.0 |
| rsvg-convert | 2.60.0 |
| System librsvg package | 2.60.0+dfsg-1 |
| Other audit inputs | Python standard library; same-commit vendored qiraat data |
| Existing Android renderer | AndroidSVG AAR 1.4, Apache-2.0, unchanged |

Upstream README specifies these audit libraries without version pins. fontTools
belongs to the optional marker replacement tool, which was not run; it is not
required by this audit or polygon builder. See `environment.json` and the original
instruction blob hashes in `read-instructions.json`. No Android dependency added.

## 4. Root cause of the 602 marker failures

Original `tools/polygon_lib.py:53–54` expects exactly a group starting
`<g transform="translate(...) scale(...)">`. Checked-in markers instead have
`class="ayah-mark-ornament"` before `transform`, and an extra `matrix(...)` in
the transform list. Each difference independently breaks the literal regex.
`markers()` begins at line 135 and iterates that regex at line 158. It returns
zero. `recover_markers()` at line 202 returns immediately when input is empty.

| Page | Real SVG ornament groups | SVG numeral anchors / JSON ayahs | Original parser |
| --- | ---: | ---: | ---: |
| 48 | 1 | 1 / 1 | 0 |
| 121 | 6 | 6 / 6 | 0 |
| 604 | 15 | 15 / 15 | 0 |

Independent namespace-aware XML counting finds the actual markers. Normal SVG
namespace and outer matrix parsing work. The marker extraction invokes no
renderer; disabling subprocess calls does not affect the failure. This therefore
is not caused by a librsvg output change or library version. The same-commit
`replace_hafs_ayah_markers.py:49–72` explicitly emits the classed ornament and
normalization matrix: producer and consumer disagree in this pinned source.
No missing prerequisite or evidence of app-modified source was found.

## 5. Cause of the 96 dependent findings

`audit_ayah_polygons.py:127` receives zero markers; line 136 reports the mismatch.
The reference-building branch at lines 174–180 is consequently not run, and lines
182–184 use an empty decoration set. Unnumbered surah headers/basmalahs are then
scored as uncovered ayah ink.

The external diagnostic reproduced every original uncovered-pixel count exactly
using unchanged source polygons, renderer, scale and failed-audit line grid.
Using original JSON marker positions solely to identify decorative bands with
the original `decoration()` function leaves zero uncovered scored ink on all
96 pages. `decoration-proof.json` records each before/after result. This is a
read-only causal experiment, not a replacement full audit or original tool fix.

## 6. Is regeneration required?

No such prerequisite exists in the pinned README; direct audit is documented.
The builder imports the same incompatible parser and cannot repair this cause.
The original builder dry-run was additionally run in a separate independent
working tree at the same commit, preserving the sole original checkout.

## 7. What changes after the generation check?

Command: `python3 tools/build_ayah_polygons.py --mushaf hafs --pages 3-604 --workers 4 --dry-run`.
Exact interpreter/environment are in `regeneration-dry-run.command.json`.
Exit 0; 0 pages written, 0 crops, 602 skipped, 0 errors. The mismatch return at
`build_ayah_polygons.py:96–100` occurs before rendering or writing.
Zero changed files, all source hashes unchanged, clean Git status, empty complete
diff `regeneration-dry-run.diff.patch`. No write-mode generation or marker
replacement was performed; no generated artwork or metadata was adopted.
Exit 0 here does not mean the 602 skipped pages passed validation.

## 8. Real problems in Quran SVG pages 3–604?

None detected in the tested identity/sequence/geometry checks or among these
698 findings. Independent validation finds no invalid or missing canonical IDs,
unexplained duplicates, or positive-area overlaps on pages 3–604. Existing local
opening-page overrides are untouched. The original marker-dependent full checks
remain blocked, and SVG outlines cannot provide a lossless Unicode transcription
to compare with Tanzil. This report does not certify all artwork text visually.

## 9. Finding classifications

| Category | Count |
| --- | ---: |
| REAL_CONTENT_OR_GEOMETRY_PROBLEM | 0 |
| AUDIT_TOOL_COMPATIBILITY_PROBLEM | 698 |
| ENVIRONMENT_DEPENDENCY_PROBLEM | 0 |
| EXPECTED_OPENING_PAGE_EXCEPTION | 0 |
| UNKNOWN | 0 |

Every raw finding has an individual explanation in `classified-findings.json`.
Opening exceptions are zero here because pages 1/2 are outside the original
3–604 scope. These counts classify the findings, not every conceivable defect.

## 10. Independent cross-check and frozen references

Independent validation passed again: 604 SVG + 604 JSON; all 6236 canonical IDs;
zero invalid IDs, missing IDs, unexplained duplicates or final positive overlaps.
All 1820 existing source-manifest records match the actual pinned Git checkout
and both independent working trees. All 11 Tanzil reference hashes and current
opening override SHA-256 match their approved values. No source normalization.
All four independent-auditor regression tests pass.
See `independent-crosscheck.json` and `validation.json` for hashes/evidence.

The editions remain distinct: 25 starts and 25 completed ends differ from Tanzil,
affecting 36 pages in their union. Quran SVG page 121 starts 5:78; Tanzil starts
5:77. These are mapping differences, not rewritten text or invalid identities.

## 11. Modern-device APK

Path: `/workspace/ahmad/svg-native-prototype/build/outputs/apk/debug/svg-native-prototype-debug.apk`.
Size: **2,454,513 bytes** (2.34 MiB).
SHA-256: `3bd09b658ef89407ce28810af73f210f77c55a15bd7918fbc66aa48f379e7e59`.
Original pages 1,2,48,121,604 only; packaged SVG/JSON and override hashes verified.
No permissions, including no INTERNET. MinSdk 23, targetSdk 36, portrait.
MODE A retains current only; MODE B at most three available sample documents.
The small panel includes every requested field; definitions are in README.md.
No rendering cutoff, AndroidSVG version or source/coordinate change.

Build, androidTest APK compilation, 13 native unit-test executions (Robolectric
SDK23/36), and Lint pass. Lint: 0 errors, 3 warnings: two portrait-orientation
warnings and one newer Robolectric release notice. Android 16 may disregard
orientation restrictions on large displays. Hardware behavior still requires
device testing. No physical device is attached; androidTest was compiled, not
executed on a modern phone. No device timings/PSS results are invented.

## 12. Short user device checklist

1. Open page 1 and long-press several ayahs.
2. Repeat on page 2.
3. Page 48: long-press 2:282 near top, middle and bottom.
4. Repeat on page 121.
5. Page 604: long-press 114:6.
6. Zoom to 2x and 4x.
7. Navigate for two minutes in MODE A.
8. Capture the metrics screenshot.
9. Navigate for two minutes in MODE B.
10. Capture the metrics screenshot.

## 13. Pending and audit-only verdict

Pending: user's real modern-device screenshots/results for HW drawing, scaling,
long press, cold/warm timings, retained documents and PSS; separately reviewed
upstream parser fix and rerun of the original marker-dependent full audit.
Only the compatibility difference report is provided; no fix is applied.
Main integration and removal of old prototypes remain unauthorized at this gate.
Stop after report; no audio, tafsir or additional feature work.

**UPSTREAM_AUDIT_NOT_RESOLVED** — causes are proven, but the unmodified original
audit still exits 1 and its full marker-dependent checks have not passed.
