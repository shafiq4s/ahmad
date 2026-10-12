# Hafs page-image provenance and redistribution review

Review date: 2026-10-11. Scope: the 604 PNGs used by the approved temporary image
edition, not Quran text, future audio, another image set, or a new release.

**Decision: image redistribution is NOT established. Keep these images out of
public APK/AAB builds and final downloadable packages.** Open Mushaf's general
MIT declaration is evidence of its software license, not sufficient evidence
that its author can relicense third-party page images attributed to the King
Fahd Complex. Free-of-charge distribution and attribution alone do not establish
image rights. No complete image collection was downloaded or integrated.

## 1. Original source: claimed versus demonstrated

The pinned Open Mushaf About screen says `صفحات المصحف من` and links to the
[King Fahd Complex developer platform](https://qurancomplex.gov.sa/techquran/dev/).
It also describes the images as `صور عالية الجودة من مجمع الملك فهد لطباعة المصحف الشريف`.
This is the publisher attribution made by Open Mushaf; it does not identify an
archive, file name, archive digest, print edition/year, download date or license.

The attribution is already present in this 2024-09-29 commit, before Hafs images were added:
[b5d95e3ee193f6950764724a92b116488ab407fb](https://github.com/adelpro/open-mushaf-native/blob/b5d95e3ee193f6950764724a92b116488ab407fb/app/%28tabs%29/%28more%29/about.tsx).
That older attribution cannot by itself prove where the later Hafs bytes came
from. The current pinned [About screen](https://github.com/adelpro/open-mushaf-native/blob/f4aa7867059ac6250a3f8aa68c64002ca1874b7d/app/%28tabs%29/%28more%29/about.tsx)
retains it, and labels Mushaf Mauri/aQuran as JSON database sources.

The **actual original digital image package and its rights holder's terms have
not been independently identified**. Direct download from the Complex versus
copying via another project is unresolved. The folder name does not establish
the exact print edition, including given the previously observed 36 pagination
differences from Tanzil. Do not substitute a license for a font, smart-device
text database, or another official digital package for the license of these PNGs.

## 2. First introduction into Open Mushaf

All 604 currently pinned image blobs first appear in the published branch histories in:

- Commit: **`29445bcaff08ba218f3d5b84a95377951982a79a`**.
- Date: **2025-01-17 14:41:21 +01:00**, equivalent to **13:41:21 UTC**.
- Author: `adelpro`.
- Message: **`test(assets): added hafs pages to design repo`**.
- Path: `design/hafs/1.png` through `604.png`.
- [Commit](https://github.com/adelpro/open-mushaf-native/commit/29445bcaff08ba218f3d5b84a95377951982a79a).

The fetched ancestry of all **69 advertised branch heads** was scanned using raw
tree changes with renames disabled, and the current blobs were searched across
earlier paths. Every one of the 604 blobs first appears in this January commit.
The images were moved to `assets/mushaf-data/mushaf-elmadina-hafs-assim/` in
[d637a25898f4fd72bab6dac505cb138ca5a2f266](https://github.com/adelpro/open-mushaf-native/commit/d637a25898f4fd72bab6dac505cb138ca5a2f266)
on **2025-03-06**, with message `feat(hafs): move hafs image files to mushaf data`.
In the **main** branch ancestry, their first addition is instead
[b11a3ab816443852f34620ed69b01d12ab2b6ba9](https://github.com/adelpro/open-mushaf-native/commit/b11a3ab816443852f34620ed69b01d12ab2b6ba9),
on **2025-03-17**, `test: adding multiple mashaf`. A main-only investigation would
therefore miss the January introduction in `design/hafs`. All 604 blob IDs match
the images in pinned main revision `f4aa7867059ac6250a3f8aa68c64002ca1874b7d`.
Deleted branches and unreachable objects were not examined.

The January introducing commit adds the 604 design PNGs without an original
download URL/archive or separate image license. The March main addition also
adds Hafs metadata and application map changes without supplying those rights.
The repository's MIT license was already present, added on 2024-09-21 in
`44e3029a1b4ce6d59db1b9cba6b1e5aab4239418`.

`evidence/image-first-occurrences.json` contains all 604 current/first paths,
first repository and main commits, and Git SHA-1 blob identifiers.
`image-first-occurrences-main.json` preserves the narrower main-only trace;
`open-branch-scope.json` records the 69 examined branch heads. These are Git content identifiers,
not SHA-256 file digests. No PNG blobs were fetched to inspect Git history.
The nine existing local PNGs retain their prior SHA-256 records in the fixture
manifest; no new image bytes were placed in the app.

## 3. Other projects and coordinate lineage

### Zizwar/mushaf-mauri

The entire 605-entry coordinate array (empty entry 0, 604 pages, 6,236 tuples)
inside Open Mushaf `aya.json` is identical to
[`src/data/madina/coordinateMadina.js` at bddd9097](https://github.com/Zizwar/mushaf-mauri/blob/bddd9097e9dd1573425369b11169e7f8291825d1/src/data/madina/coordinateMadina.js).
That exact source blob, `719503c0e852e4a29cbfb22df9a655d0437ea428`, is present
from **2021-10-01 19:17:22 UTC**, in commit
`bddd9097e9dd1573425369b11169e7f8291825d1` (`stylos layer ayat warsh madina`).
The same coordinates persist in current reorganized paths. This is strong
content-based evidence for the coordinate lineage, consistent with attribution.

Its [root LICENSE at that same revision](https://github.com/Zizwar/mushaf-mauri/blob/bddd9097e9dd1573425369b11169e7f8291825d1/LICENSE)
is MIT, `Copyright (c) 2021 Ibrahim BIDI`; it predates the matching coordinate
revision. No different license was found along that coordinate file's directory
ancestry. Preserve this notice and the Open Mushaf MIT notice when distributing
the derived coordinate JSON. The notices are already retained in test fixtures.

Mauri's [archived README](https://github.com/Zizwar/mushaf-mauri/blob/c4316e433238d3b3b68e254c9252132888c5b3f8/_archive/README.md)
links to `outils-mushaf-mauri` and `mushaf.ma`. Its image API references a separate
`hafsTajweed` collection hosted at `www.mushaf.ma`, alongside Muhammadi pages.
Those statements do not establish that Open Mushaf's Hafs images came from it.

### Zizwar/outils-mushaf-mauri

The coordinate-tool repository's [root LICENSE](https://github.com/Zizwar/outils-mushaf-mauri/blob/6fd129d23a077c254f59fb6fd0a54d8c9409c764/LICENSE)
is GPL-3.0. Its README describes coordinate-generation tools. That tool license
is separate from the MIT-licensed coordinate array in Mushaf Mauri. No tool code
was copied into our Kotlin implementation; the overlay uses our corrected,
independent algorithm. Neither this GPL declaration nor the data's MIT notice
proves the page-image license.

### forabi/aQuran

The [README](https://github.com/forabi/aQuran/blob/cb7b0bfbe4b7e653010283e52b46a519d48a2bf0/README.en.md)
describes a text-based Quran application, explicitly contrasting it with apps
that display page pictures. [`bower.json`](https://github.com/forabi/aQuran/blob/cb7b0bfbe4b7e653010283e52b46a519d48a2bf0/bower.json)
declares `"license": "MIT"`; a root full license notice is not present in that
revision. There is no demonstrated connection to these 604 image files. No
aQuran text or code was imported; its declarations cannot authorize the images.

### Image comparisons: precise limit

The 604 Open Mushaf Git blob IDs were compared with PNG additions/modifications
in the fetched histories of Mauri (default `expo55` plus `main`), aQuran and the
coordinate tools. **Zero identical blobs** were found. This rules out an exact
byte-for-byte match in those examined histories, **not** an indirect source,
resized/reencoded copy, or files hosted outside their Git repositories. All
branches of those repositories were not exhaustively fetched.

The nine existing PNGs contain `Creation Time: 10/29/11` and
`Software: Adobe Fireworks CS5`. Their dimensions are 456 x 672. No embedded
copyright/license/source URL appears in the examined text chunks. This timestamp
is an unverified metadata string, not evidence of a publication date, authorship,
an official download, or permission. No inference about the print year is made.

## 4. Three distinct licensing conclusions

An additional file named
[`docs/licenses/quran-svg-permission.eml`](https://github.com/adelpro/open-mushaf-native/blob/576f45a4c6df9f04ea6fd382d142bc2433755e5c/docs/licenses/quran-svg-permission.eml)
was found in other branch history. **It is not a permission grant.** It says
`Date: PENDING`, `PLACEHOLDER FILE` and asks the quranpedia team for permission
for a planned SVG migration. Its companion SHA file explicitly blocks that SVG
revision from production until written permission is received. The file concerns
different quranpedia SVG assets, not the existing 604 PNGs; it establishes rights
for neither collection. A filename containing "permission" must not be treated
as evidence of authorization. Only that one addition appears in the examined
history for this correspondence path; no later actual reply was found there.

| Material | Evidence | Redistribution/modification conclusion |
| --- | --- | --- |
| Open Mushaf software | Root MIT, copyright `[2024] adelpro`; README states MIT | MIT permits software use, modification and distribution with the copyright and full license notice retained. It does not require publishing our source. It is not an independently verified image rights grant. |
| Coordinate data | All tuples match Mauri's 2021 array under its existing root MIT; Open Mushaf declares MIT for its derived repository | The documented data path supports reuse/format conversion with Ibrahim BIDI and Open Mushaf notices retained. Do not copy GPL tool code or conflate these notices with image permission. |
| Hafs page PNGs | About-screen attribution to the Complex; root MIT declaration only; no asset-specific grant or original package located | **Permission to redistribute in APK/AAB is unproven. Permission to transform into WebP or another format is unproven. Not approved for release.** |

The same image decision applies to a free app, embedded/offline copies, a
separate downloadable package and externally hosted distribution. A local
testing authorization from the user does not establish public distribution
rights from the image rights holder.

## 5. Conversion and attribution

Technically, lossless WebP can preserve every decoded pixel; this does not
resolve whether a rights holder permits format conversion. No conversion was
performed. Lossy conversion is not approved either. A code license allowing
software modification does not authorize modifications of separately owned
image assets.

Confirmed notice obligations for reused MIT material:

- Retain **the complete license text**, not just an About-screen name/link.
- Retain `Copyright (c) [2024] adelpro` for applicable Open Mushaf material.
- Retain `Copyright (c) 2021 Ibrahim BIDI` for the coordinate data lineage.
- An in-app Third-party Notices entry can expose bundled notices. It does not
  replace permission for the images. No production attribution UI was added here.

**Exact mandatory image attribution cannot be determined without its license.**
Once the actual image package is identified and licensed, use the rights holder's
required wording, edition/version, source link and any required conversion
disclosure. Do not label these images MIT merely because the surrounding code
is MIT, or present the claimed Complex attribution as independent provenance.
Tanzil attribution remains unchanged and applies to our text, not these images.

## 6. What would establish release eligibility

Before using this collection for release, obtain an identifiable original
download/archive and a license accompanying it, or written permission from its
rights holder that identifies this exact edition/collection and expressly
covers redistribution inside Android APK/AAB and, if desired, lossless format
conversion/separate package hosting. Preserve original files and hashes and
verify correspondence to the current collection/map. An upstream author's
download link alone would establish a lead, not substitute for the license.
No messages to maintainers or the Complex were sent during this review.

## 7. Retrieval limitations and reproducible evidence

During this review the official Complex root and developer pages (Arabic,
English and www variants) returned **HTTP 503**. This prevented retrieval of an
official archive or its license; it does not prove that such permission does
not exist. The ITQAN article, GitHub REST API and KSU endpoints were blocked by
the environment's proxy with **403** tunnel responses. GitHub Git transport,
raw files and the GitHub history page worked, so the commit conclusions are
based on actual Git trees/history, not guessed from GitHub HTML or API failure.

Evidence is in `evidence/`: pinned text/license/About files with source URLs,
commit IDs and SHA-256 digests in `evidence-manifest.json`; all 604 first-intro
records; coordinate comparison; image-match scope; sampled PNG metadata and
retrieval outcomes. Git metadata was examined in temporary bare, filtered
repositories without checking out the upstream apps or downloading the page
collection. No source was integrated as app implementation.

## 8. Small UI correction and approved technical status

The user reported accurate alignment, hits and canonical ayah IDs on a real
Android device and approved `MushafTouchOverlayView`/`AyahRegion` technically.
The device model/OS version were not supplied, and this report does not claim
that we repeated that physical-device test.

Only the debug image activity manifest was changed to portrait. Its temporary
API-36 restricted-resizability opt-out also covers the platform's large-screen
orientation behavior; older Android versions ignore that manifest property.
The opt-out is scoped to this test activity, not the main reader/application.
The source-pixel matrix, coordinate map, page rules, Quran database and Tanzil
PageStart are unchanged. Orientation remains subject to platform/device window
policies; this is not a new landscape/zoom implementation.

Build and Lint results and the updated temporary APK are recorded in
`portrait-validation.json`. No new dependency, sound, tafsir, download feature,
complete image collection, release APK/AAB, or release image integration was added.
