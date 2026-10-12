# Read-only compatibility difference report

Pinned commit: `b4155f07e4aea087d2c458069acff9f7c7e749f6`.
This describes the bug; it is not a source patch or a corrected official audit.
The clean Git checkout, source SVG/JSON and original tools are unchanged.

## Marker structure mismatch

`tools/polygon_lib.py:53-54` assumes this literal opening tag:

```xml
<g transform="translate(x y) scale(sx sy)">
```

The checked-in source has this form (page 121):

```xml
<g class="ayah-mark-ornament" transform="translate(51.900865 84.981966) scale(0.009 -0.009) matrix(0.95233582 0 0 -1.00936986 -738.53642519 498.12402740)">
```

There are two independent mismatches: a class attribute before transform, and an
additional normalization matrix before the closing quote. Controlled non-Quran
markup gives regex counts legacy=1, class-only=0, matrix-only=0, both=0.
`markers()` at line 135 iterates this regex at line 158 and consequently returns
zero markers. `recover_markers()` at line 202 returns immediately for empty input,
so markers.json cannot rescue a completely failed parser.

XML independently counts real ornaments/numeral anchors: page48=1, page121=6,
page604=15. The source uses the usual default SVG namespace and literal `<g>` tags;
namespace handling does not explain the discrepancy. The outer matrix regex
matches. Marker extraction performs no external rendering; the proof disables
subprocess calls and still gets zero markers. NumPy/Pillow/Brotli/librsvg versions
cannot change this literal-regex mismatch.

`tools/replace_hafs_ayah_markers.py:49-72` in the same commit explicitly produces
the classed wrapper, classed ornament and extra normalization transform. This is
an inconsistency between the marker producer and consumer in one pinned version,
not an app modification or a required missing generation step.

## Cascading decoration findings

`audit_ayah_polygons.py:127` gets no markers. The count mismatch at line136 prevents
the reference-building branch at lines174-180. Lines182-184 then set `decor=set()`.
The subsequent score reports unnumbered titles/basmalahs as uncovered verse ink.

The separate diagnostic preserves original polygons, rasterizer, raster scale and
the exact failed-audit line grid. It identifies decorative bands using upstream
`decoration()` and original JSON marker positions, independently matched to SVG
numeral anchors. Only the score's decoration input changes, outside the audit.
All96 original uncovered counts reproduce exactly. After excluding those
unnumbered bands, every page has zero uncovered scored ink. Raw official output
remains FAIL; this is a causal experiment, not a patched full audit.

## Regeneration and future fix boundary

README documents direct audit and separately offers generation; generation is
not a prerequisite for shipped assets. The generator imports the same parser and
exits each page before rendering/writing when counts differ. A dry-run on a second
independent working tree skips602 pages and changes zero files. No write-mode
generation or marker replacement was executed and no generated output adopted.

A future separately approved tool fix must handle the real group structure and
compose all SVG transforms, including normalization. Loosening the regex while
ignoring the matrix would produce incorrect centres/widths. No such implementation
is applied here. Frozen artwork, polygon JSON, Tanzil and opening overrides remain
unchanged. Official marker-derived full audit remains unresolved.
