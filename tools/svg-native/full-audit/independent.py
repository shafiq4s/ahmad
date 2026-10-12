#!/usr/bin/env python3
"""Independent identity, topology and edition pagination audit against read-only Tanzil.

Does not import upstream polygon_lib, rebuild data or repair source geometry. GEOS
intersections test actual area rather than intersecting bounding boxes. Source SVG
path syntax is interpreted independently and only closed straight paths are accepted.
"""
import collections
import csv
import hashlib
import json
import pathlib
import re
import sqlite3
import xml.etree.ElementTree as ET
from shapely.geometry import Polygon, Point, LineString, box
from shapely.ops import unary_union, polygonize
from shapely.validation import explain_validity

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parents[2]
ROOT = HERE.parent / "local-fixtures/full-audit-source"
PREFIX = ROOT / "mushafs/hafs/kfqc"
EDITION = "quran_ws_hafs_kfqc_b4155f0"

def polygon_parts(text):
    if not re.search(r"[MLZmlz]",text):
        assert not re.search(r"[A-Za-z]",text)
        values = [float(x) for x in re.findall(r"[-+]?(?:\d*\.)?\d+(?:[eE][-+]?\d+)?",text)]
        assert len(values)%2==0
        return [[values[i:i+2] for i in range(0,len(values),2)]]
    tokens = re.findall(r"[A-Za-z]|[-+]?(?:\d*\.)?\d+(?:[eE][-+]?\d+)?",text)
    parts, points, i = [], [], 0
    while i < len(tokens):
        command = tokens[i]; i += 1
        assert command in ["M","L","Z"], command
        if command == "Z":
            assert len(points)>=3
            parts.append(points); points=[]
        else:
            if command=="M": assert not points
            assert i+1<len(tokens)
            points.append([float(tokens[i]),float(tokens[i+1])]); i+=2
    assert not points and parts
    return parts

def winding(points, point):
    value=0; x,y=point.x,point.y
    for a,b in zip(points,points[1:]+points[:1]):
        cross=(b[0]-a[0])*(y-a[1])-(x-a[0])*(b[1]-a[1])
        if a[1]<=y<b[1] and cross>0: value+=1
        if b[1]<=y<a[1] and cross<0: value-=1
    return value

def geometry(parts):
    # SVG/Android Path use nonzero fill, including self-touching rings. Interpret
    # faces by winding; never repair, buffer, snap or rewrite the original points.
    edges=[LineString([a,b]) for points in parts for a,b in zip(points,points[1:]+points[:1]) if a!=b]
    faces=list(polygonize(unary_union(edges)))
    result=unary_union([face for face in faces if sum(winding(p,face.representative_point()) for p in parts)!=0])
    assert result.is_valid and result.area>0
    return result

def identity(row):
    assert type(row["surahNumber"]) is int and type(row["ayahNumber"]) is int
    return f'{row["surahNumber"]}:{row["ayahNumber"]}'

def comparisons(regions):
    overlaps, touches = [], []
    for i,(a,ga) in enumerate(regions):
        for b,gb in regions[i+1:]:
            if a==b: continue
            intersect=ga.intersection(gb)
            if intersect.area>0:
                overlaps.append(dict(a=a,b=b,area=intersect.area,bounds=list(intersect.bounds)))
            elif not intersect.is_empty:
                touches.append(dict(a=a,b=b,area=0,kind=intersect.geom_type))
    return overlaps,touches

def main():
    manifest=json.loads((HERE/'manifest.json').read_text())
    assert not manifest['failures'] and manifest['commit']=='b4155f07e4aea087d2c458069acff9f7c7e749f6'
    for row in manifest['files']:
        data=(ROOT/row['path']).read_bytes()
        assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],row['path']
    db=PROJECT/'app/src/main/assets/quran-uthmani-v1.db'
    before=hashlib.sha256(db.read_bytes()).hexdigest()
    assert before=='8ed100334e8eca42124035f51fffe934d52b20e5224b3a44c1a33b44dba74416'
    connection=sqlite3.connect(f'file:{db}?mode=ro',uri=True)
    connection.execute('PRAGMA query_only=ON')
    ids={f'{s}:{a}':o for s,a,o in connection.execute('SELECT surahNumber,ayahNumber,ordinal FROM ayahs')}
    ordinal_ids={v:k for k,v in ids.items()}
    assert len(ids)==6236
    counts=dict(connection.execute('SELECT surahNumber,ayahCount FROM surahs'))
    assert len(counts)==114
    surahs=json.loads((PREFIX/'json/surah.json').read_text())
    assert {int(s['number']):int(s['ayahCount']) for s in surahs}==counts
    overrides=json.loads((HERE.parent/'opening-hit-overrides.json').read_text())
    override_hash=hashlib.sha256((HERE.parent/'opening-hit-overrides.json').read_bytes()).hexdigest()
    override_map={p['pageNumber']:{identity(r):r.get('polygons',[r.get('points')]) for r in p['regions']} for p in overrides['pages']}
    failures=[]; source_overlaps=[]; source_invalid=[]; final_overlaps=[]; touches=[]; pages=[]; occurrences=collections.defaultdict(list)
    previous=None; previous_page=None; appearances=0; source_polygon_count=0; final_polygon_count=0
    source_marker_checks=0; maximum_marker_delta=0.0
    for number in range(1,605):
        svg=PREFIX/f'svg/{number:03d}.svg'
        source=ET.parse(svg).getroot()
        vx,vy,w,h=map(float,source.attrib['viewBox'].split())
        bounds=box(vx,vy,vx+w,vy+h)
        rows=json.loads((PREFIX/f'json/{number:03d}.json').read_text())
        regions=[]; originals=[]; keys=[]; marker_results=[]
        metadata=[Point(float(g.attrib['{https://quranpedia.net}x']),float(g.attrib['{https://quranpedia.net}y']))
                  for g in source.iter() if '{https://quranpedia.net}x' in g.attrib]
        if len(metadata)!=len(rows): failures.append(dict(page=number,kind='svg-marker-count',actual=len(metadata),expected=len(rows)))
        remaining=list(metadata)
        for row in rows:
            key=identity(row); keys.append(key); occurrences[key].append(number); appearances+=1
            if key not in ids:
                failures.append(dict(page=number,kind='invalid-identity',id=key)); continue
            ordinal=ids[key]
            if previous is not None and not (ordinal==previous+1 or (ordinal==previous and previous_page==number-1 and len(keys)==1)):
                failures.append(dict(page=number,kind='sequence',id=key,previous=ordinal_ids.get(previous)))
            previous,previous_page=ordinal,number
            parts=polygon_parts(row['polygon']); original=geometry(parts); source_polygon_count+=len(parts)
            invalid=[explain_validity(Polygon(p)) for p in parts if not Polygon(p).is_valid]
            if invalid:
                source_invalid.append(dict(page=number,id=key,reason=invalid))
                if number not in [1,2]: failures.append(dict(page=number,kind='invalid-source-topology',id=key))
            replacements=override_map.get(number,{}).get(key,parts)
            local=geometry(replacements); final_polygon_count+=len(replacements)
            if not bounds.covers(original): failures.append(dict(page=number,kind='source-out-of-bounds',id=key,bounds=list(original.bounds)))
            if not bounds.covers(local): failures.append(dict(page=number,kind='final-out-of-bounds',id=key,bounds=list(local.bounds)))
            originals.append((key,original));regions.append((key,local))
            marker=Point(row['x'],row['y'])
            nearest=min(remaining,key=marker.distance) if remaining else None
            delta=marker.distance(nearest) if nearest else None
            if nearest:
                remaining.remove(nearest)
                maximum_marker_delta=max(maximum_marker_delta,delta)
            # The SVG attribute anchors the numeral; JSON anchors the medallion.
            # Use the pinned auditor's documented 8-unit correspondence tolerance,
            # not byte equality between geometrically different anchor points.
            if delta is None or delta>8.0:
                failures.append(dict(page=number,kind='svg-json-marker-position',id=key,distance=delta))
            marker_results.append(dict(id=key,x=row['x'],y=row['y'],svgX=nearest.x if nearest else None,
                                      svgY=nearest.y if nearest else None,svgDelta=delta,expectedCovered=local.covers(marker)))
            source_marker_checks+=1
        assert len(set(keys))==len(keys),f'duplicate within page {number}'
        a,_=comparisons(originals)
        b,t= comparisons(regions)
        source_overlaps += [dict(page=number,**r) for r in a]
        final_overlaps += [dict(page=number,**r) for r in b]
        touches += [dict(page=number,**r) for r in t]
        for marker in marker_results:
            hits=[k for k,g in regions if g.covers(Point(marker['x'],marker['y']))]
            marker['hits']=hits
            if hits!=[marker['id']]: failures.append(dict(page=number,kind='marker-hit-identity',**marker))
            if marker['svgX'] is not None:
                svg_hits=[k for k,g in regions if g.covers(Point(marker['svgX'],marker['svgY']))]
                marker['svgNumeralHits']=svg_hits
                if svg_hits!=[marker['id']]: failures.append(dict(page=number,kind='svg-numeral-hit-identity',**marker))
        pages.append(dict(pageNumber=number,editionId=EDITION,viewBox=[vx,vy,w,h],firstAyahId=keys[0],lastAyahId=keys[-1],
                          ayahIds=keys,regionCount=len(regions),sourcePolygonCount=sum(len(polygon_parts(r['polygon'])) for r in rows),
                          markerResults=marker_results))
    missing=sorted(set(ids)-set(occurrences),key=ids.get)
    extras=sorted(set(occurrences)-set(ids))
    duplicates={k:v for k,v in occurrences.items() if len(v)>1}
    unexplained={k:v for k,v in duplicates.items() if not all(v[i]+1==v[i+1] and pages[v[i]-1]['lastAyahId']==k and pages[v[i+1]-1]['firstAyahId']==k for i in range(len(v)-1))}
    for i,page in enumerate(pages):
        page['continuationFromPrevious']=page['firstAyahId'] if i>0 and pages[i-1]['lastAyahId']==page['firstAyahId'] else None
        page['continuationToNext']=page['lastAyahId'] if i<603 and pages[i+1]['firstAyahId']==page['lastAyahId'] else None
        completed=page['ayahIds'][:-1] if page['continuationToNext'] else page['ayahIds']
        page['lastCompletedAyahId']=completed[-1] if completed else None
    tanzil=list(connection.execute('SELECT pageNumber,startOrdinal,endOrdinal FROM edition_pages ORDER BY pageNumber'))
    assert len(tanzil)==604
    differences=[]
    for number,start,end in tanzil:
        page=pages[number-1]
        a,b=ordinal_ids[start],ordinal_ids[end-1]
        page['tanzilFirstAyahId']=a;page['tanzilLastAssignedAyahId']=b
        page['startDiffers']=page['firstAyahId']!=a
        # Compare completed verses to Tanzil's exclusive-ordinal assignment endpoint.
        page['endDiffers']=page['lastCompletedAyahId']!=b
        if page['startDiffers'] or page['endDiffers']:
            differences.append({k:page[k] for k in ['pageNumber','firstAyahId','lastAyahId','lastCompletedAyahId',
                              'continuationFromPrevious','continuationToNext','tanzilFirstAyahId','tanzilLastAssignedAyahId','startDiffers','endDiffers']})
    assert pages[120]['firstAyahId']=='5:78' and pages[603]['lastAyahId']=='114:6'
    assert pages[0]['ayahIds']==[f'1:{n}' for n in range(1,8)]
    assert pages[1]['ayahIds']==[f'2:{n}' for n in range(1,6)]
    # These probes belong to unnumbered headings/basmalah and must remain unassigned.
    unassigned=[]
    for n,point,label in [(1,(110,-40),'surah-heading'),(2,(110,-40),'surah-heading'),(2,(110,30),'unnumbered-basmalah')]:
        rows=json.loads((PREFIX/f'json/{n:03d}.json').read_text())
        matches=[identity(r) for r in rows if geometry(override_map.get(n,{}).get(identity(r),polygon_parts(r['polygon']))).covers(Point(*point))]
        unassigned.append(dict(page=n,point=point,kind=label,hits=matches))
        if matches: failures.append(dict(page=n,kind='unnumbered-content-assigned',hits=matches))
    connection.close()
    assert hashlib.sha256(db.read_bytes()).hexdigest()==before
    report=dict(commit=manifest['commit'],editionId=EDITION,canonicalAyahIds=len(set(occurrences)&set(ids)),appearances=appearances,
                sourcePolygonCount=source_polygon_count,finalPolygonCount=final_polygon_count,missing=missing,invalidIds=extras,
                continuations=duplicates,continuationSeams=appearances-len(occurrences),unexplainedDuplicates=unexplained,
                failures=failures,sourceInvalidTopology=source_invalid,sourcePositiveAreaOverlaps=source_overlaps,finalPositiveAreaOverlaps=final_overlaps,
                zeroAreaTouchingPairs=touches,startDifferenceCount=sum(p['startDiffers'] for p in pages),
                completedEndDifferenceCount=sum(p['endDiffers'] for p in pages),paginationDifferences=differences,
                unassignedOpeningProbes=unassigned,openingOverrideSha256=override_hash,tanzilSha256=before,
                sourceMarkerChecks=source_marker_checks,maximumSvgJsonMarkerDistance=maximum_marker_delta,
                markerCorrespondenceTolerance=8.0,
                pageCount=len(pages),svgCount=len(list((PREFIX/'svg').glob('[0-9][0-9][0-9].svg'))),
                jsonCount=len(list((PREFIX/'json').glob('[0-9][0-9][0-9].json'))),
                comparisonDefinition='Physical first/last include partial verses. Fair endpoint compares last verse completed on SVG page to Tanzil endOrdinal-1. Each adjacent-page repeated ID is a continuation, not a new canonical verse.',
                textLimitation='SVG outlines have no lossless Unicode verse text: this audit validates identity/sequence/artifact hashes, not a 604-page visual transcription. Frozen Tanzil text remains unchanged.',
                geometryPolicy='Exact nonzero SVG fill: self-crossing/self-touching opening rings are interpreted by polygonized faces and original-point winding. No coordinates, overrides or source artifacts are changed; no make_valid/buffer/snap.')
    report['passed']=not any([missing,extras,unexplained,failures,final_overlaps])
    (HERE/'independent-report.json').write_text(json.dumps(report,indent=2)+'\n')
    (HERE/'edition-page-map.json').write_text(json.dumps(dict(editionId=EDITION,pages=pages),indent=2)+'\n')
    columns=['pageNumber','firstAyahId','lastAyahId','lastCompletedAyahId','continuationFromPrevious','continuationToNext',
             'tanzilFirstAyahId','tanzilLastAssignedAyahId','startDiffers','endDiffers']
    with (HERE/'edition-page-map.csv').open('w') as output:
        writer=csv.DictWriter(output,fieldnames=columns);writer.writeheader()
        writer.writerows({k:p[k] for k in columns} for p in pages)
    print(json.dumps({k:report[k] for k in ['passed','pageCount','svgCount','jsonCount','canonicalAyahIds','appearances','continuationSeams',
                                        'startDifferenceCount','completedEndDifferenceCount','failures','finalPositiveAreaOverlaps']},indent=2))
    raise SystemExit(0 if report['passed'] else 1)

if __name__=='__main__':
    main()
