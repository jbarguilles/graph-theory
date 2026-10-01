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

    @Test
    public void mixedOverEdgeCap_isTooLarge_butPureUndirectedIsNot() {
        Vertex[] vs = new Vertex[40];
        for (int i = 0; i < 40; i++) { vs[i] = new Vertex("v" + i, 0, 0); vList.add(vs[i]); }
        for (int i = 0; i < 40; i++) und(vs[i], vs[(i + 1) % 40]);   // 40-edge cycle
        assertFalse(Traversals.eulerTooLarge(eList));
        assertEulerTrail(Traversals.eulerTour(vList, eList));

        arc(vs[0], vs[1]);
        assertTrue(Traversals.eulerTooLarge(eList));
        assertNull(Traversals.eulerTrail(vList, eList));
    }
}
