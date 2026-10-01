package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class TraversalsTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Vertex c = new Vertex("c", 0, 0);
    private final Vertex d = new Vertex("d", 0, 0);

    private final Vector<Vertex> vList = new Vector<Vertex>();
    private final Vector<Edge> eList = new Vector<Edge>();

    private void vertices(Vertex... vs) { for (Vertex v : vs) vList.add(v); }
    private Edge und(Vertex x, Vertex y) { Edge e = new Edge(x, y, false); eList.add(e); return e; }
    private Edge arc(Vertex x, Vertex y) { Edge e = new Edge(x, y, true);  eList.add(e); return e; }

    /** w is a trail that uses every edge of eList exactly once. */
    private void assertEulerTrail(Walk w) {
        assertNotNull(w);
        assertTrue(w.isTrail());
        assertEquals(eList.size(), w.length());
        for (Edge e : eList) assertTrue(w.uses(e));
    }

    // ---------- Euler ----------

    @Test
    public void noVertices_noEulerTrailOrTour() {
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void edgeless_trivialWalkIsEulerTrail_butNotTour() {
        vertices(a, b);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertNotNull(w);
        assertEquals(0, w.length());
        assertSame(a, w.start());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void undirectedTriangle_hasEulerTour() {
        vertices(a, b, c);
        und(a, b); und(b, c); und(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
        assertEulerTrail(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void undirectedPath_trailStartsAtFirstOddVertex_noTour() {
        vertices(a, b, c);
        und(a, b); und(b, c);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(a, w.start());
        assertSame(c, w.end());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void star_fourOddVertices_noEulerTrail() {
        vertices(d, a, b, c);
        und(d, a); und(d, b); und(d, c);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void twoSeparateEdges_notConnected_noEulerTrail() {
        vertices(a, b, c, d);
        und(a, b); und(c, d);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void isolatedVertex_doesNotBlockEulerTour() {
        vertices(d, a, b, c);
        und(a, b); und(b, c); und(c, a);
        assertEulerTrail(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void undirectedSelfLoop_isEulerTourOfLengthOne() {
        vertices(a);
        und(a, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void directedCycle_hasEulerTour() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); arc(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void directedPath_trailStartsAtSource_noTour() {
        vertices(c, b, a);
        arc(a, b); arc(b, c);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(a, w.start());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void twoArcsIntoSameVertex_noEulerTrail() {
        // Undirected degrees are 0, so the old check wrongly said "Yes".
        vertices(a, b, c);
        arc(a, b); arc(c, b);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void mixedTriangleWithExtraArc_hasTrail_noTour() {
        vertices(a, b, c);
        und(a, b); und(b, c); und(c, a); arc(a, b);
        assertEulerTrail(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void mixedCycleFollowingArcs_hasEulerTour() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); und(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void mixedEvenDegrees_butArcsTrapAtB_noTrailOrTour() {
        // Every vertex has even total degree, but b has no way out.
        vertices(a, b, c);
        arc(a, b); arc(c, b); und(a, c);
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test(timeout = 2000)
    public void windmillWithTwoTrappingArcs_noEulerTrail_fast() {
        // 9 triangles sharing hub h, plus arcs h->x twice: every vertex has
        // even edge-ends, but x can't be left. Backtracking took minutes here.
        Vertex h = new Vertex("h", 0, 0), x = new Vertex("x", 0, 0);
        vList.add(h); vList.add(x);
        for (int i = 0; i < 9; i++) {
            Vertex p = new Vertex("p" + i, 0, 0), q = new Vertex("q" + i, 0, 0);
            vList.add(p); vList.add(q);
            und(h, p); und(p, q); und(q, h);
        }
        arc(h, x); arc(h, x);
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test(timeout = 2000)
    public void k7MinusEdgePlusTwoArcs_noEulerTour_fast() {
        Vertex[] k = new Vertex[7];
        for (int i = 0; i < 7; i++) { k[i] = new Vertex("k" + i, 0, 0); vList.add(k[i]); }
        for (int i = 0; i < 7; i++)
            for (int j = i + 1; j < 7; j++)
                if (!(i == 0 && j == 1)) und(k[i], k[j]);
        Vertex x = new Vertex("x", 0, 0);
        vList.add(x);
        arc(x, k[0]); arc(x, k[1]);
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test(timeout = 2000)
    public void largeMixedCycle_hasEulerTour_noCap() {
        Vertex[] vs = new Vertex[60];
        for (int i = 0; i < 60; i++) { vs[i] = new Vertex("v" + i, 0, 0); vList.add(vs[i]); }
        for (int i = 0; i < 60; i++) {
            if (i % 2 == 0) arc(vs[i], vs[(i + 1) % 60]); else und(vs[i], vs[(i + 1) % 60]);
        }
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void mixedTrail_mustStartAtSecondOddVertex() {
        // Odd vertices are a (first in vList) and c; the arc forces c -> b -> a.
        vertices(a, b, c);
        arc(c, b); und(b, a);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(c, w.start());
        assertSame(a, w.end());
    }

    @Test
    public void mixedUndirectedEdgesMustBeOrientedConsistently() {
        // Square with arcs (a,b), (c,d) and undirected {b,c}, {d,a}.
        // The tour a->b->c->d->a needs {b,c} as b->c and {d,a} as d->a.
        vertices(a, b, c, d);
        arc(a, b); und(b, c); arc(c, d); und(d, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void parallelUndirectedEdges_tourOfLengthTwo() {
        vertices(a, b);
        und(a, b); und(a, b);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void parallelArcsSameWay_noTour_trailNeedsBalance() {
        vertices(a, b);
        arc(a, b); arc(a, b);
        assertNull(Traversals.eulerTour(vList, eList));
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void directedSelfLoop_isEulerTourOfLengthOne() {
        vertices(a);
        arc(a, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void balancedDirectedGraph_eulerTrailIsClosed() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); arc(c, a);
        assertTrue(Traversals.eulerTrail(vList, eList).isCircuit());
    }

    @Test
    public void mixedWithSelfLoops_hasEulerTour() {
        vertices(a, b);
        arc(a, b); und(b, a); und(a, a); arc(b, b);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }
}
