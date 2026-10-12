#!/usr/bin/env python3
"""Diagnostic coverage against existing original-SVG PNG captures; not app raster assets."""
import json
import re
from pathlib import Path
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent
REFERENCE = ROOT.parent / 'quran-svg' / 'local-fixtures'


def inside(points, x, y):
    winding = np.zeros(x.shape, dtype=np.int16)
    for a, b in zip(points, points[1:] + points[:1]):
        cross = (b[0]-a[0])*(y-a[1])-(x-a[0])*(b[1]-a[1])
        winding += ((a[1] <= y) & (b[1] > y) & (cross > 0)).astype(np.int16)
        winding -= ((a[1] > y) & (b[1] <= y) & (cross < 0)).astype(np.int16)
    return winding != 0


def main():
    overrides = json.loads((ROOT/'opening-hit-overrides.json').read_text())
    catalog = json.loads((REFERENCE/'assets/svg-mushaf/pages.json').read_text())
    for number in (1, 2):
        image = np.array(Image.open(REFERENCE/f'{number:03d}-1440.png').convert('L'))
        left, top, width, height = catalog['pages'][number-1]['renderBox']
        x, y = np.meshgrid(left+(np.arange(image.shape[1])+.5)*width/image.shape[1],
                          top+(np.arange(image.shape[0])+.5)*height/image.shape[0])
        original = json.loads((REFERENCE/f'sources/mushafs/hafs/kfqc/json/{number:03d}.json').read_text())
        local = {r['ayahNumber']: r.get('polygons', [r.get('points')])
                 for r in overrides['pages'][number-1]['regions']}
        source_masks = []
        local_masks = []
        for row in original:
            values = list(map(float, re.findall(r'[-+]?\d+(?:\.\d+)?', row['polygon'])))
            points = [values[i:i+2] for i in range(0, len(values), 2)]
            source_masks.append(inside(points, x, y))
            pieces = local.get(row['ayahNumber'], [points])
            local_masks.append(np.logical_or.reduce([inside(piece, x, y) for piece in pieces]))
        count = np.sum(local_masks, axis=0)
        # Page 2's unnumbered basmalah belongs to neither 2:1 nor the heading hit layer.
        body = y >= (14 if number == 1 else 46.08)
        ink = (image < 160) & body
        gaps = ink & (count == 0)
        labels, _ = ndimage.label(gaps)
        objects = ndimage.find_objects(labels)
        largest = []
        for label, obj in enumerate(objects, 1):
            size = int(np.sum(labels[obj] == label))
            if size >= 10:
                ys, xs = obj
                largest.append(dict(pixels=size, bounds=[float(x[ys.start,xs.start]),float(y[ys.start,xs.start]),
                    float(x[ys.stop-1,xs.stop-1]),float(y[ys.stop-1,xs.stop-1])]))
        print(json.dumps(dict(page=number, ambiguousPixels=int(np.sum(count>1)),inkPixels=int(ink.sum()),
            missingInkPixels=int(gaps.sum()),largeGapComponents=sorted(largest,key=lambda a:-a['pixels'])[:16]),indent=2))
        for i, first in enumerate(source_masks):
            for j in range(i+1,len(source_masks)):
                overlap = first & source_masks[j]
                if overlap.any():
                    print('Original overlap',number,i+1,j+1,'ink pixels',int(np.sum(overlap&ink)))


if __name__ == '__main__':
    main()
