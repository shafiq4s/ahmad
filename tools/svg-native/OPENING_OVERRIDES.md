# Opening spread hit-testing audit

Scope: Hafs/KFQC Quran SVG commit `b4155f07e4aea087d2c458069acff9f7c7e749f6`, edition `quran_ws_hafs_kfqc_b4155f0`. Original SVG and polygon JSON are byte-identical to the pinned sources. `opening-hit-overrides.json` changes only local selection geometry; it never changes the artwork, Quran text, canonical identity or database pagination.

Coordinates below are **source SVG units**, before the common display matrix. Bounds are `[left, top, right, bottom]` of the original polygon intersection. A bounding box can enclose several disconnected strips; it is not itself the intersection. Native Skia path intersections and the earlier pinned source-render captures were both inspected.

| Page | Ayah A / B | Original intersection bounds | Local separation | Visual ownership |
| --- | --- | --- | --- | --- |
| 1 | 1:5 / 1:6 | [56.04, 130.02159, 229.52, 131.26] | Shared row boundary y=130.39; same-row split x=56.04. Ayah 6 has two independent polygons. | RTL: 5 ends at its marker on row 4; 6 starts at the left of that row, then continues at the right of row 5. The thin original intersection contains no numbered ink in the inspected source render. |
| 1 | 1:5 / 1:7 | [78.59325, 130.39, 113.7, 130.59236] | Flatten ayah 5's bottom to y=130.39. | Ayah 5 belongs to the row above, 7 to the left portion of the next row. The slanted source edge crosses empty interline space. |
| 1 | 1:6 / 1:7 | [100.22854, 130.39, 204.99, 160.65] | Same-row split x=114.20; next-row join y=160.65. | Ayah 6 includes its end marker on the right, 7 starts to its left and continues below. Both pieces of 6 highlight together. |
| 2 | 2:2 / 2:3 | [33.75, 72.23, 172.36, 72.32384] | Shared boundary y=72.23 and same-row split x=172.36; remove the small source edge spike/slant. | Row 1 belongs to 2; its continuation ends on row 2's right at marker 2. Ayah 3 starts to its left. No ink in the original intersection. |
| 2 | 2:3 / 2:4 | [113.95, 133.63, 228.16, 133.88] | Baseline y=133.63, plus a complementary notch x=172..178, y=130..133.63 assigned to 4. | The damma above **unzila** on the lower row belongs to ayah 4 even where it reaches into ayah 3's original band. A simple horizontal split would misassign it. |
| 2 | 2:4 / 2:5 | [37.73, 158.53, 200.01, 160.15] | Baseline y=158.53. Give x=64.3..66.8, y=158.53..162.3 to 4; give x=107.5..114.1, y=156.75..158.53 to 5. Cut their complements from the other polygon. | The meem descender of **hum** belongs to upper-row ayah 4. The **sila** waqf sign and lower-row diacritics belong to ayah 5. These cross the horizontal gap in opposite directions. |

Additional ink-coverage correction: ayah 1:7's final-row left edge is 72.70 rather than 73.48. The original edge cut the meem stroke of **alayhim**, around x=72.98..73.44, y=201.94..208.56. This small extension cannot intersect any other ayah at that row. It is a local geometry fix, not an artwork edit.

The unnumbered basmalah on page 2 and surah headings remain unassigned. Page 1's numbered basmalah belongs to 1:1. Boundaries shared by two polygons have no positive intersection area; the winding test uses half-open ownership and is tested on representative joins.

Validation:

- Exact native path intersections: zero positive-area overlap among all opening-page ayah pairs.
- Half-unit source grid: no ambiguous hit on either opening page.
- Independently rendered native SVG at 6x source scale: page 1 **101,503** numbered ink pixels; page 2 **108,093**; zero uncovered or multiply assigned ink pixels.
- Existing pinned source-render diagnostic: zero uncovered numbered ink and zero ambiguous pixels. Reference counts differ because viewport, rasterizer and threshold differ.
- All original marker identities, reviewed notches and multiline cases pass at three display sizes and zoom 1x/2x/4x on simulated API 23 and 36.
- Every page number 3..604 is checked to return the original page object without any override. Only the five approved pages are downloaded; this guard test does not claim a full-corpus geometry audit.

These checks establish ownership for the reviewed sample and render scales, not a mathematical guarantee for every infinitesimal SVG curve or every future upstream revision. Changing the pinned source requires a fresh review.
