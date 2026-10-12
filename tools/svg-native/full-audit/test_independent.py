"""Regression checks for the independent auditor's topology and strict parser."""
import unittest
from shapely.geometry import Point
from independent import comparisons, geometry, identity, polygon_parts

class IndependentAuditTests(unittest.TestCase):
    def test_real_overlap_is_distinct_from_shared_edge(self):
        a=geometry(polygon_parts('M 0 0 L 10 0 L 10 10 L 0 10 Z'))
        b=geometry(polygon_parts('M 10 0 L 20 0 L 20 10 L 10 10 Z'))
        overlap,touch=comparisons([('1:1',a),('1:2',b)])
        self.assertEqual([],overlap)
        self.assertEqual(0,touch[0]['area'])
        c=geometry(polygon_parts('M 9 0 L 20 0 L 20 10 L 9 10 Z'))
        overlap,touch=comparisons([('1:1',a),('1:2',c)])
        self.assertEqual(10,overlap[0]['area'])
        self.assertEqual([],touch)

    def test_self_touching_path_uses_nonzero_fill_without_coordinate_changes(self):
        points=[[10,0],[0,0],[0,10],[20,10],[20,20],[10,20]]
        before=[p[:] for p in points]
        result=geometry([points])
        self.assertEqual(200,result.area)
        self.assertTrue(result.covers(Point(5,5)))
        self.assertTrue(result.covers(Point(15,15)))
        self.assertFalse(result.covers(Point(15,5)))
        self.assertEqual(before,points)

    def test_compound_nonzero_path_preserves_an_oppositely_wound_hole(self):
        outer=[[0,0],[10,0],[10,10],[0,10]]
        inner=[[2,2],[2,8],[8,8],[8,2]]
        filled=geometry([outer,inner])
        self.assertEqual(64,filled.area)
        self.assertFalse(filled.covers(Point(5,5)))

    def test_external_formats_are_strict(self):
        with self.assertRaises(AssertionError): polygon_parts('M 0 0 C 1 1 2 2 3 3 Z')
        with self.assertRaises(AssertionError): polygon_parts('M 0 0 L 10 0 L 10 10')
        with self.assertRaises(AssertionError): identity({'surahNumber':True,'ayahNumber':1})

if __name__=='__main__': unittest.main()
