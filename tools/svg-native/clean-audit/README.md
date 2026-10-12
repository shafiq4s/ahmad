# Clean upstream final validation gate

Pinned Hafs/KFQC commit: `b4155f07e4aea087d2c458069acff9f7c7e749f6`.
See [REPORT.md](REPORT.md), [validation.json](validation.json) and the read-only
[compatibility difference report](COMPATIBILITY_DIFF.md).

## Original checkout and commands

`/workspace/quran-svg-upstream-validation` is an independent clean shallow Git
checkout. Cone sparse checkout includes `tools`, `docs`, `LICENSES` and the entire
`mushafs/hafs/kfqc` directory, including all 604 full pages and 118 surah crops.
No project audit tool was copied into it. Its original tools, instructions and
assets match the pinned commit. Both Git status checks are empty.

Read the pinned `tools/README.md` before running. The audit requires NumPy, Pillow,
Brotli, rsvg-convert and vendored qiraat data; upstream does not pin their versions.
The local audit dependencies are separate from Android runtime dependencies.

```bash
cd /workspace/ahmad
python3 tools/svg-native/full-audit/setup_tools.py
python3 tools/svg-native/clean-audit/reproduce.py
PATH=/workspace/.tools/librsvg-audit/usr/bin:$PATH PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=/workspace/.tools/svg-audit-python python3 tools/svg-native/clean-audit/diagnose.py
PYTHONPATH=/workspace/.tools/svg-audit-python python3 tools/svg-native/full-audit/independent.py
```

`reproduce.py` requires the already-created clean checkout at the exact commit.
It runs subset pages 3,48,121,604 before pages 3–604 with all tiers. Each command,
working directory, environment override, exit code, stdout, stderr and JSON is
saved separately. `environment.json` records versions and the original manifest
comparison. `read-instructions.json` records SHA-256/Git blobs of the instructions.

`diagnose.py` is a separate read-only causal experiment. It imports original
functions without replacing them. It does not constitute a corrected official
full audit. `classified-findings.json` records all 698 original findings.

## Generation boundary

Upstream documents direct audit; rebuilding shipped polygons is not required.
An original builder dry-run was additionally performed in the second independent
working tree `/workspace/quran-svg-regeneration-validation`. Its immutable Git
object store can borrow objects from the first checkout, but its working files
are separate. The builder skipped 602 pages before writing because it uses the
same marker parser. Exit 0 means dry-run completed, not geometry validated.
`regeneration-evidence.json` records zero changed files. The complete Git diff is
the empty `regeneration-dry-run.diff.patch`. No write-mode generation was run or
adopted. No source artwork, JSON, Tanzil data or opening override was modified.

## Standalone device validation

Build commands and APK identity are recorded in `validation.json`. Only the five
original sample pages 1,2,48,121,604 are packaged; release remains disabled. No
INTERNET permission. Native AndroidSVG remains 1.4, minSdk 23, targetSdk 36, portrait;
the API <26 software-layer cutoff is unchanged. No new runtime dependency.

The MODE A/B button compares one retained document against at most three:
current plus adjacent available sample pages. Endpoint pages may retain only two.
The small panel shows model/API, actual Canvas HW/SW mode, first cold load,
last parse/render, last warm navigation and session median/P95, cache hits/misses,
retained documents, current/maximum observed PSS and selected canonical AyahId.
PSS samples every three seconds; maximum is observed, not an instantaneous peak.
`—` means a sample has not arrived. Counters reset when changing mode. Cold load
is the first activity content-ready measurement, not a new process launch when
switching modes. Parse time describes the most recent parse; a cache hit performs
no new parse. Rendering excludes compositor presentation. Navigation excludes
post-draw prefetch time, which still consumes CPU.

The app saves local `files/device-validation.json` on pause. No results are sent
over a network. The two-minute comparisons and screenshots require the user's
real modern Android device. Robolectric SDK 23/36 tests are functional checks;
they do not demonstrate device HW rendering quality, latency or PSS.

The original auditor remains failing until an independently reviewed tool fix is
approved and run. Do not integrate the renderer into the main app or remove the
older Open Mushaf/raster prototypes on the strength of this report alone.
