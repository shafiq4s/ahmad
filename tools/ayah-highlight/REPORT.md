# Ayah highlight appearance validation

The only production changes are `AyahHighlight.kt` and presentation state/drawing in `NativeSvgPageView.kt`. No new dependency, control, bottom sheet, audio or tafsir was added.

The highlight is a honey backdrop (`#C49A45`, 38/255 opacity, approximately 15%) beneath the original SVG ink. It has no outline, nominal 2.5dp erosion, approximately 1.25dp rounded corners and a native 140ms crossfade when selecting, replacing or clearing an ayah. The existing automatic-clear delay is unchanged. All polygons for the selected canonical ayah contribute to the same visual selection.

## Protecting ink without changing touch regions

A temporary selected-ayah crop is rendered from the unchanged SVG at no more than 1024 pixels per edge. Connected local ink envelopes avoid highlighting large blank polygon areas. This is purely visual analysis; it creates no word identities, line-height estimates or touch regions. The temporary bitmap is recycled immediately; only small source-coordinate bounds and a path remain for the active highlight. The document loader/cache is unchanged.

The 2.5dp inset is relaxed locally where it would intersect the protective ink envelope (two analysis pixels plus 0.5 screen dp). The resulting path is clipped to the original polygons. Original letters, diacritics and verse markers render above the highlight, so their opaque ink is neither recolored nor clipped. On zoom, only the visual path is rebuilt to keep the inset/rounding in screen dp.

The safety envelope required local inset reduction in all 15 sampled ayahs: 1:1–1:7, 2:1–2:5, 2:282, 5:78 and 114:6. This is a conservative margin guard, not evidence that every one of these ayahs would lose a visible letter with an unconditional inset.

## Validation

- Debug build passed; main suite: 41 executions, standalone shared-renderer suite: 13 executions; no failures, errors or skipped tests.
- Main Lint: zero errors, five warnings: portrait orientation (two), available Robolectric update, and two native-versus-KTX style suggestions. No dependencies were upgraded or added for style suggestions.
- 45 ink-safety cases: all opening ayahs on pages 1 and 2, long 2:282 on page 48, multipart 5:78 on page 121, and final 114:6 on page 604, each at 1x/2x/4x. Zero measured selected-ink pixels outside the visual highlight; no visual path outside original polygons; visual area never exceeds the original polygon area.
- Long-press captures retained expected AyahIds on all five pages at all three zoom levels. Selection switching, 140ms fade, intermediate fade-out alpha, final clearing, and original opaque ink preservation passed.
- `hitTest`, `screenPoint`, `setZoom`, `onFling` and `onTouchEvent` are byte-identical to the before snapshot. Matrix/inverse calculations are byte-identical; their only added operation rebuilds the visual path afterward. Eight frozen parser/override/loader/controller references remain byte-identical.
- All 604 original SVG + 604 polygon JSON files, pinned notices/metadata and all 11 Tanzil reference digests verified unchanged.

Pixel checks and captures use actual native Skia drawing through Robolectric at simulated SDK36. Existing functional suites run at simulated SDK23 and SDK36. These are not physical-device screenshots, and Robolectric SDK23 cannot supply native SVG pixel validation. No new device test is claimed.

See `validation.json`, `ink-safety-api-36.json`, `reference-lock.json`, and the exact pre-change view snapshot for evidence. PNG files in `screenshots/before` and `screenshots/after` cover pages 1, 2, 48, 121 and 604 at 1x/2x/4x. They are local comparison artifacts, excluded from source packaging and version control.

Useful comparisons:

- Page 1, selected 1:6: [before](screenshots/before/page-1-zoom1.png) / [after](screenshots/after/page-1-zoom1.png).
- Page 48, selected 2:282: [before](screenshots/before/page-48-zoom1.png) / [after](screenshots/after/page-48-zoom1.png).
- Page 121, selected 5:78: [before](screenshots/before/page-121-zoom1.png) / [after](screenshots/after/page-121-zoom1.png).
- Page 604, selected 114:6: [before](screenshots/before/page-604-zoom1.png) / [after](screenshots/after/page-604-zoom1.png).

No bottom-sheet phase has started.
