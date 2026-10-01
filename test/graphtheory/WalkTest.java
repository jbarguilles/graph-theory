package graphtheory;

import org.junit.Test;
import java.util.Arrays;
import java.util.Vector;
import static org.junit.Assert.*;

public class WalkTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Vertex c = new Vertex("c", 0, 0);
    private final Vertex d = new Vertex("d", 0, 0);
    private final Vertex e = new Vertex("e", 0, 0);

    private Edge und(Vertex x, Vertex y) { return new Edge(x, y, false); }
    private Edge arc(Vertex x, Vertex y) { return new Edge(x, y, true); }

    @Test
    public void trivialWalk_isTrailAndPath_butNotClosed() {
        Walk w = new Walk(a);
        assertEquals(0, w.length());
        assertSame(a, w.start());
        assertSame(a, w.end());
        assertTrue(w.isTrail());
        assertTrue(w.isPath());
        assertFalse(w.isClosed());
        assertFalse(w.isCircuit());
        assertFalse(w.isCycle());
    }

    @Test
    public void undirectedEdge_traversableBothWays() {
        Edge ab = und(a, b);
        assertTrue(Walk.canTraverse(ab, a));
        assertTrue(Walk.canTraverse(ab, b));
        assertSame(b, Walk.otherEnd(ab, a));
        assertSame(a, Walk.otherEnd(ab, b));
    }

    @Test
    public void arc_traversableOnlyFromSource() {
        Edge ab = arc(a, b);
        assertTrue(Walk.canTraverse(ab, a));
        assertFalse(Walk.canTraverse(ab, b));
    }

    @Test
    public void edgeNotIncident_notTraversable() {
        assertFalse(Walk.canTraverse(und(b, c), a));
    }

    @Test
    public void extend_againstArc_rejectedAndWalkUnchanged() {
        Walk w = new Walk(b);
        assertFalse(w.extend(arc(a, b)));
        assertEquals(0, w.length());
        assertSame(b, w.end());
    }

    @Test
    public void simplePath_isTrailAndPath_notClosed() {
        Walk w = new Walk(a);
        assertTrue(w.extend(und(a, b)));
        assertTrue(w.extend(arc(b, c)));
        assertEquals(2, w.length());
        assertSame(c, w.end());
        assertTrue(w.isTrail());
        assertTrue(w.isPath());
        assertFalse(w.isClosed());
    }

    @Test
    public void sameUndirectedEdgeTwice_closedButNotTrail() {
        Edge ab = und(a, b);
        Walk w = new Walk(a);
        w.extend(ab);
        w.extend(ab);
        assertTrue(w.isClosed());
        assertFalse(w.isTrail());
        assertFalse(w.isCircuit());
        assertFalse(w.isCycle());
    }

    @Test
    public void outOnUndirectedBackOnArc_isCycleOfLengthTwo() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        w.extend(arc(b, a));
        assertEquals(2, w.length());
        assertTrue(w.isCircuit());
        assertTrue(w.isCycle());
        assertFalse(w.isPath());
    }

    @Test
    public void selfLoop_isCycleOfLengthOne() {
        Walk w = new Walk(a);
        assertTrue(w.extend(und(a, a)));
        assertEquals(1, w.length());
        assertSame(a, w.end());
        assertTrue(w.isCycle());
        assertFalse(w.isPath());
    }

    @Test
    public void directedSelfLoop_isCycleOfLengthOne() {
        Walk w = new Walk(a);
        assertTrue(w.extend(arc(a, a)));
        assertTrue(w.isCycle());
    }

    @Test
    public void triangle_isCycle() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        w.extend(und(b, c));
        w.extend(und(c, a));
        assertTrue(w.isCycle());
    }

    @Test
    public void figureEight_isCircuitButNotCycle() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        w.extend(und(b, c));
        w.extend(und(c, a));
        w.extend(und(a, d));
        w.extend(und(d, e));
        w.extend(und(e, a));
        assertTrue(w.isCircuit());
        assertFalse(w.isCycle());
    }

    @Test
    public void revisitingVertexWithNewEdges_isTrailNotPath() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        w.extend(und(b, c));
        w.extend(und(c, a));
        w.extend(und(a, d));
        assertTrue(w.isTrail());
        assertFalse(w.isPath());
        assertFalse(w.isClosed());
    }

    @Test
    public void edgesBetween_returnsParallelEdges_excludesReversedArc() {
        Edge undAB = und(a, b);
        Edge arcAB = arc(a, b);
        Edge arcBA = arc(b, a);
        Edge bc = und(b, c);
        Vector<Edge> eList = new Vector<Edge>(Arrays.asList(undAB, arcAB, arcBA, bc));

        Vector<Edge> result = Walk.edgesBetween(a, b, eList);
        assertEquals(2, result.size());
        assertTrue(result.contains(undAB));
        assertTrue(result.contains(arcAB));
    }

    @Test
    public void undo_removesLastStep_falseWhenTrivial() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        assertTrue(w.undo());
        assertEquals(0, w.length());
        assertSame(a, w.end());
        assertFalse(w.undo());
    }

    @Test
    public void copy_isIndependent() {
        Walk w = new Walk(a);
        w.extend(und(a, b));
        Walk copy = w.copy();
        w.undo();
        assertEquals(1, copy.length());
        assertSame(b, copy.end());
    }

    @Test
    public void stepsUsing_listsOneBasedSteps() {
        Edge ab = und(a, b);
        Edge bc = und(b, c);
        Walk w = new Walk(a);
        w.extend(ab);
        w.extend(bc);
        w.extend(bc);
        assertEquals(Arrays.asList(1), w.stepsUsing(ab));
        assertEquals(Arrays.asList(2, 3), w.stepsUsing(bc));
        assertTrue(w.uses(bc));
        assertTrue(w.visits(c));
        assertFalse(w.visits(d));
    }

    @Test
    public void toString_writesUndirectedAsSetInTravelOrder_andArcsAsOrderedPair() {
        Walk w = new Walk(b);
        w.extend(und(a, b));   // stored as {a,b} but travelled b -> a
        w.extend(arc(a, c));
        assertEquals("b -{b,a}-> a -(a,c)-> c", w.toString());
    }

    @Test
    public void twoOppositeArcs_isCycleOfLengthTwo() {
        Walk w = new Walk(a);
        assertTrue(w.extend(arc(a, b)));
        assertTrue(w.extend(arc(b, a)));
        assertEquals(2, w.length());
        assertTrue(w.isCycle());
    }

    @Test
    public void twoParallelUndirectedEdges_isCycleOfLengthTwo() {
        Walk w = new Walk(a);
        assertTrue(w.extend(und(a, b)));
        assertTrue(w.extend(und(a, b)));
        assertEquals(2, w.length());
        assertSame(a, w.end());
        assertTrue(w.isTrail());
        assertTrue(w.isCycle());
    }

    @Test
    public void selfLoopTwice_notTrailNotCycle() {
        Edge loop = und(a, a);
        Walk w = new Walk(a);
        assertTrue(w.extend(loop));
        assertTrue(w.extend(loop));
        assertTrue(w.isClosed());
        assertFalse(w.isTrail());
        assertFalse(w.isCycle());
    }

    @Test
    public void edgeLabel_formats() {
        assertEquals("{a,b}", Walk.edgeLabel(und(a, b), a));
        assertEquals("{b,a}", Walk.edgeLabel(und(a, b), b));
        assertEquals("(a,b)", Walk.edgeLabel(arc(a, b), a));
    }
}
