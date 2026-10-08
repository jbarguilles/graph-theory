package graphtheory;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeShapesTest {

    private static final double EPS = 1e-9;

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 100, 0);
    private final Vertex c = new Vertex("c", 0, 100);

    private static EdgeShapes shapes(Edge... es) {
        return EdgeShapes.of(Arrays.asList(es));
    }

    @Test
    public void singleEdge_isStraight() {
        Edge e = new Edge(a, b, false);
        double[] cv = shapes(e).curve(e);
        assertEquals(50, cv[2], EPS);
        assertEquals(0, cv[3], EPS);
    }

    @Test
    public void singleEdge_endpointsOnVertexOutlines() {
        Edge e = new Edge(a, b, true);
        double[] cv = shapes(e).curve(e);
        assertEquals(Vertex.RADIUS, cv[0], EPS);
        assertEquals(100 - Vertex.RADIUS, cv[4], EPS);
    }

    @Test
    public void twoParallel_fanSymmetrically() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        assertEquals(-EdgeShapes.FAN_SPACING / 2, s.curve(e1)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING / 2, s.curve(e2)[3], EPS);
    }

    @Test
    public void threeParallel_middleIsStraight() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, true);
        Edge e3 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2, e3);
        assertEquals(-EdgeShapes.FAN_SPACING, s.curve(e1)[3], EPS);
        assertEquals(0, s.curve(e2)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING, s.curve(e3)[3], EPS);
    }

    @Test
    public void mixedDirections_shareOneFan_withPairPerpendicular() {
        Edge undirected = new Edge(a, b, false);
        Edge reversedArc = new Edge(b, a, true);
        EdgeShapes s = shapes(undirected, reversedArc);
        // The reversed arc still bends to +y: the perpendicular belongs to the pair, not the edge.
        assertEquals(-EdgeShapes.FAN_SPACING / 2, s.curve(undirected)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING / 2, s.curve(reversedArc)[3], EPS);
    }

    @Test
    public void reversedArc_startsAtItsOwnSource() {
        Edge arc = new Edge(b, a, true);
        double[] cv = shapes(arc).curve(arc);
        assertEquals(100 - Vertex.RADIUS, cv[0], EPS);
        assertEquals(Vertex.RADIUS, cv[4], EPS);
    }

    @Test
    public void curvedEdge_endpointsStillOnOutlines() {
        Edge e1 = new Edge(a, b, true);
        Edge e2 = new Edge(a, b, true);
        double[] cv = shapes(e1, e2).curve(e2);
        assertEquals(Vertex.RADIUS, Math.hypot(cv[0] - a.location.x, cv[1] - a.location.y), EPS);
        assertEquals(Vertex.RADIUS, Math.hypot(cv[4] - b.location.x, cv[5] - b.location.y), EPS);
    }

    @Test
    public void differentPairs_doNotFan() {
        Edge ab = new Edge(a, b, false);
        Edge ac = new Edge(a, c, false);
        EdgeShapes s = shapes(ab, ac);
        assertEquals(0, s.curve(ab)[3], EPS);
        assertEquals(0, s.curve(ac)[2], EPS);
    }

    @Test
    public void parallelMidpoints_fartherApartThanTwoTolerances() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        double[] m1 = EdgeShapes.at(s.curve(e1), 0.5);
        double[] m2 = EdgeShapes.at(s.curve(e2), 0.5);
        assertTrue(Math.hypot(m1[0] - m2[0], m1[1] - m2[1]) > 2 * EdgeShapes.TOLERANCE);
    }

    @Test
    public void nearest_picksTheParallelEdgeClickedOn() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        double[] m2 = EdgeShapes.at(s.curve(e2), 0.5);
        assertSame(e2, s.nearest(m2[0], m2[1]));
        double[] m1 = EdgeShapes.at(s.curve(e1), 0.5);
        assertSame(e1, s.nearest(m1[0], m1[1]));
    }

    @Test
    public void nearest_farFromEverything_null() {
        Edge e = new Edge(a, b, false);
        assertNull(shapes(e).nearest(50, 60));
    }

    @Test
    public void edgeNotInList_isStraight() {
        Edge listed = new Edge(a, b, false);
        Edge other = new Edge(a, b, false);
        assertEquals(0, shapes(listed).curve(other)[3], EPS);
    }

    @Test
    public void firstLoop_sitsAboveVertex() {
        Vertex v = new Vertex("v", 100, 100);
        Edge loop = new Edge(v, v, false);
        double[] l = shapes(loop).loop(loop);
        assertEquals(100, l[0], EPS);
        assertEquals(EdgeShapes.LOOP_RADIUS, l[2], EPS);
        // lowest point is just inside the vertex outline
        assertEquals(100 - Vertex.RADIUS + 4, l[1] + l[2], EPS);
    }

    @Test
    public void loops_nestWithTopsFourteenApart() {
        Vertex v = new Vertex("v", 100, 100);
        Edge l1 = new Edge(v, v, false);
        Edge l2 = new Edge(v, v, true);
        EdgeShapes s = shapes(l1, l2);
        double top1 = s.loop(l1)[1] - s.loop(l1)[2];
        double top2 = s.loop(l2)[1] - s.loop(l2)[2];
        assertEquals(14, top1 - top2, EPS);
    }

    @Test
    public void loops_onDifferentVertices_countSeparately() {
        Vertex v = new Vertex("v", 100, 100);
        Vertex w = new Vertex("w", 300, 100);
        Edge lv = new Edge(v, v, false);
        Edge lw = new Edge(w, w, false);
        EdgeShapes s = shapes(lv, lw);
        assertEquals(EdgeShapes.LOOP_RADIUS, s.loop(lw)[2], EPS);
    }

    @Test
    public void nearest_picksTheNestedLoopClickedOn() {
        Vertex v = new Vertex("v", 100, 100);
        Edge l1 = new Edge(v, v, false);
        Edge l2 = new Edge(v, v, false);
        EdgeShapes s = shapes(l1, l2);
        double[] o = s.loop(l2);
        assertSame(l2, s.nearest(o[0], o[1] - o[2]));
        double[] i = s.loop(l1);
        assertSame(l1, s.nearest(i[0], i[1] - i[2]));
    }

    @Test
    public void loopAndEdge_sameList_bothLaidOut() {
        Edge ab = new Edge(a, b, false);
        Edge loop = new Edge(a, a, false);
        EdgeShapes s = shapes(ab, loop);
        assertEquals(0, s.curve(ab)[3], EPS);
        assertEquals(EdgeShapes.LOOP_RADIUS, s.loop(loop)[2], EPS);
    }

    @Test
    public void nearest_longEdge_hitBetweenSamples() {
        Vertex far = new Vertex("z", 1000, 0);
        Edge e = new Edge(a, far, false);
        // Samples along a 1000px edge are ~30px apart; a click on the line between two must still hit.
        assertSame(e, shapes(e).nearest(33, 0));
        assertSame(e, shapes(e).nearest(33, 5));
    }
}
