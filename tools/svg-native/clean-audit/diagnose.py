#!/usr/bin/env python3
"""Read-only parser proof and finding classification; not a patched upstream audit."""
import collections
import concurrent.futures
import inspect
import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET
from unittest import mock

HERE=pathlib.Path(__file__).resolve().parent
CHECKOUT=pathlib.Path('/workspace/quran-svg-upstream-validation')
BASE=CHECKOUT/'mushafs/hafs/kfqc'
sys.path.insert(0,str(CHECKOUT/'tools'))
import polygon_lib as lib
import audit_ayah_polygons as audit

def marker_proof(number):
    text=(BASE/f'svg/{number:03d}.svg').read_text()
    tree=ET.fromstring(text)
    layer=next(g for g in tree.iter() if g.attrib.get('id')=='ayah_markers')
    ornaments=[g for g in layer.iter() if g.attrib.get('class')=='ayah-mark-ornament']
    numeral_anchors=[g for g in layer.iter() if '{https://quranpedia.net}x' in g.attrib]
    # Marker extraction precedes rasterization and must not invoke librsvg at all.
    with mock.patch('subprocess.run',side_effect=AssertionError('No renderer needed for marker parsing')):
        parsed=lib.markers(text)
    return dict(page=number,xmlOrnaments=len(ornaments),xmlNumeralAnchors=len(numeral_anchors),
                auditMarkers=len(parsed),jsonAyahs=len(json.loads((BASE/f'json/{number:03d}.json').read_text())),
                outerMatrixMatches=len(lib._MATRIX.findall(text)),
                offendingGroupStart=re.search(r'<g class="ayah-mark-ornament"[^>]*>',text).group(0),
                markerXPathNamespace='{http://www.w3.org/2000/svg}',
                function='polygon_lib.markers',functionLine=inspect.getsourcelines(lib.markers)[1],
                regexLine=53,matchingLoopLine=158)

def decoration_proof(number):
    text,box,polys=lib.read_page(str(BASE/f'svg/{number:03d}.svg'))
    mask=lib.ink_mask(str(BASE/f'svg/{number:03d}.svg'))
    _,bands=lib.line_grid(mask,[],box)  # Exactly the failed auditor's original grid.
    original={p['key']:[list(r) for r in p['rects']] for p in polys}
    keys=[p['key'] for p in polys]
    before=lib.score(original,bands,set(),mask,[],box,keys)['uncovered_ink']
    rows=json.loads((BASE/f'json/{number:03d}.json').read_text())
    # JSON marker positions are original, hash-verified and independently matched
    # to the source SVG's numeral anchors. No polygons/grid/marker parser change.
    # Only identify the unnumbered bands with the upstream decoration function.
    ends=[audit._band_of(bands,r['y']) for r in rows]
    decor=lib.decoration(bands,ends,[(r['surahNumber'],r['ayahNumber']) for r in rows])
    after=lib.score(original,bands,decor,mask,[],box,keys)['uncovered_ink']
    return dict(page=number,uncoveredWithEmptyDecoration=before,
                uncoveredExcludingSourceIdentifiedDecoration=after,
                decorationBands=[dict(index=i,top=bands[i]['top'],bottom=bands[i]['bot']) for i in sorted(decor)],
                diagnosticOnly=True)

def main():
    details=[marker_proof(n) for n in [48,121,604]]
    # Synthetic non-Quran markup isolates the two assumptions of the old regex.
    body='<path d="M 0 0 L 10 0 L 10 10 L 0 10 Z"/></g>'
    cases={'legacy':'<g transform="translate(1 2) scale(1 1)">',
           'class_only':'<g class="ayah-mark-ornament" transform="translate(1 2) scale(1 1)">',
           'matrix_only':'<g transform="translate(1 2) scale(1 1) matrix(1 0 0 1 0 0)">',
           'both':'<g class="ayah-mark-ornament" transform="translate(1 2) scale(1 1) matrix(1 0 0 1 0 0)">'}
    controls={k:len(lib._MARKER.findall(start+body)) for k,start in cases.items()}
    assert controls==dict(legacy=1,class_only=0,matrix_only=0,both=0)
    (HERE/'marker-proof.json').write_text(json.dumps(dict(pages=details,controlledRegexCases=controls,
        regex=lib._MARKER.pattern,sourceDataModified=False,rendererInvokedForMarkerParsing=False),indent=2)+'\n')
    findings=json.loads((HERE/'upstream-full.json').read_text())['hafs']['findings']
    pages=sorted({r['page'] for r in findings if r['signature']=='UNCOVERED'})
    with concurrent.futures.ProcessPoolExecutor(max_workers=2) as pool:
        evidence=list(pool.map(decoration_proof,pages))
    (HERE/'decoration-proof.json').write_text(json.dumps(evidence,indent=2)+'\n')
    by_page={r['page']:r for r in evidence}
    classified=[]
    for finding in findings:
        category='UNKNOWN';reason='Needs further diagnosis.'
        if finding['signature']=='MARKERCOUNT' and '0 marker rosettes drawn' in finding['message']:
            category='AUDIT_TOOL_COMPATIBILITY_PROBLEM'
            reason='Original regex cannot match classed/normalized ornament groups in this same pinned commit.'
        elif finding['signature']=='UNCOVERED':
            proof=by_page[finding['page']]
            count=int(re.search(r'\d+',finding['message']).group(0))
            assert count==proof['uncoveredWithEmptyDecoration']
            if proof['uncoveredExcludingSourceIdentifiedDecoration']==0:
                category='AUDIT_TOOL_COMPATIBILITY_PROBLEM'
                reason='All flagged ink is in unnumbered decoration bands; unchanged polygons cover all remaining scored ink.'
        classified.append(dict(**finding,classification=category,reason=reason))
    categories=['REAL_CONTENT_OR_GEOMETRY_PROBLEM','AUDIT_TOOL_COMPATIBILITY_PROBLEM','ENVIRONMENT_DEPENDENCY_PROBLEM',
                'EXPECTED_OPENING_PAGE_EXCEPTION','UNKNOWN']
    counts=collections.Counter(r['classification'] for r in classified)
    report=dict(counts={c:counts[c] for c in categories},totalFindings=len(classified),findings=classified,
                sourceToolsUnmodified=True,notACorrectedOfficialAudit=True,
                limitation='The decoration-input diagnostic proves these specific findings; it is not a Unicode transcription or a complete replacement for disabled marker-derived checks.')
    (HERE/'classified-findings.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report['counts'],indent=2))

if __name__=='__main__': main()
