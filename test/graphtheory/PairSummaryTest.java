package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PairSummaryTest {

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);
    private final Vertex m = new Vertex("m", 0, 0);

    private static Edge und(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, false);
        e.setWeight(w);
        return e;
    }

    private static Edge arc(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, true);
        e.setWeight(w);
        return e;
    }

    private static Vector<Edge> edges(Edge... es) {
        return new Vector<Edge>(Arrays.asList(es));
    }

    @Test
    public void undirectedEdge_isAdjacentAndReachable() {
        PairSummary s = new PairSummary(u, v, edges(und(u, v, 1)));
        assertTrue(s.adjacent);
        assertTrue(s.reachable());
        assertEquals(1, s.distance());
    }

    @Test
    public void arcTheOtherWay_isNeitherAdjacentNorReachable() {
        PairSummary s = new PairSummary(u, v, edges(arc(v, u, 1)));
        assertFalse(s.adjacent);
        assertFalse(s.reachable());
        assertEquals(-1, s.distance());
        assertEquals(-1, s.weightedDistance());
        assertTrue(s.paths.isEmpty());
    }

    @Test
    public void parallelEdges_areSeparatePaths() {
        PairSummary s = new PairSummary(u, v, edges(und(u, v, 1), arc(u, v, 1)));
        assertEquals(2, s.paths.size());
    }

    @Test
    public void weighted_distanceCountsEdges_weightedDistanceSumsWeights() {
        Edge direct = und(u, v, 10);
        PairSummary s = new PairSummary(u, v, edges(direct, und(u, m, 1), und(m, v, 1)));
        assertTrue(s.weighted);
        assertEquals(1, s.distance());
        assertEquals(2, s.weightedDistance());
    }

    @Test
    public void weighted_pathsSortedLightestFirst_andTaggedSeparately() {
        Edge direct = und(u, v, 10);
        PairSummary s = new PairSummary(u, v, edges(direct, und(u, m, 1), und(m, v, 1)));
        Walk lightest = s.paths.get(0);
        Walk shortest = s.paths.get(1);
        assertEquals(2, lightest.length());
        assertTrue(s.isLightest(lightest));
        assertFalse(s.isGeodesic(lightest));
        assertTrue(s.isGeodesic(shortest));
        assertFalse(s.isLightest(shortest));
    }

    @Test
    public void unweighted_pathsSortedShortestFirst_distancesAgree() {
        PairSummary s = new PairSummary(u, v, edges(und(u, m, 1), und(m, v, 1), und(u, v, 1)));
        assertFalse(s.weighted);
        assertEquals(1, s.paths.get(0).length());
        assertEquals(2, s.paths.get(1).length());
        assertEquals(s.distance(), s.weightedDistance());
    }
}
