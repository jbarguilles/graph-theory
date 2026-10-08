package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConnectivityTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void components_ignoreDirection_inVertexOrder() {
        List<List<Vertex>> cs = Connectivity.components(vs(a, b, c, d), es(arc(b, a), und(d, d)));
        assertEquals(Arrays.asList(Arrays.asList(a, b), Arrays.asList(c), Arrays.asList(d)), cs);
        assertFalse(Connectivity.isConnected(vs(a, b, c, d), es(arc(b, a))));
        assertFalse(Connectivity.isConnected(vs(), es()));
        assertTrue(Connectivity.isConnected(vs(a), es()));
    }

    @Test
    public void stronglyConnected_respectsDirection_singleVertexIsStrong() {
        assertTrue(Connectivity.isStronglyConnected(vs(a), es()));
        assertFalse(Connectivity.isStronglyConnected(vs(a, b), es(arc(a, b))));
        assertTrue(Connectivity.isStronglyConnected(vs(a, b, c), es(arc(a, b), und(b, c), arc(c, a))));
    }

    @Test
    public void edgeCut_parallelEdgesCountSeparately() {
        Connectivity.Cut<Edge> cut = Connectivity.minimumEdgeCut(vs(a, b), es(und(a, b), und(a, b)));
        assertEquals(2, cut.size);
        assertEquals(2, cut.members.size());
    }

    @Test
    public void edgeCut_pathHasBridge_arcsIgnoreDirection() {
        Edge bc = arc(c, b);
        Connectivity.Cut<Edge> cut = Connectivity.minimumEdgeCut(vs(a, b, c),
                es(und(a, b), und(a, b), bc));
        assertEquals(1, cut.size);
        assertEquals(Arrays.asList(bc), cut.members);
    }

    @Test
    public void edgeCut_disconnectedOrTiny_isZeroAndEmpty() {
        assertEquals(0, Connectivity.minimumEdgeCut(vs(a, b), es()).size);
        assertEquals(0, Connectivity.minimumEdgeCut(vs(a), es()).size);
        assertTrue(Connectivity.minimumEdgeCut(vs(a, b), es()).members.isEmpty());
    }

    @Test
    public void vertexCut_path_isTheMiddle() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c), es(und(a, b), arc(c, b)));
        assertEquals(1, cut.size);
        assertEquals(Arrays.asList(b), cut.members);
    }

    @Test
    public void vertexCut_cycleOfFour_isTwoOppositeVertices() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c, d),
                es(und(a, b), und(b, c), und(c, d), und(d, a)));
        assertEquals(2, cut.size);
        assertEquals(2, cut.members.size());
        assertTrue(cut.members.equals(Arrays.asList(a, c)) || cut.members.equals(Arrays.asList(b, d)));
    }

    @Test
    public void vertexCut_k2_isOne_withNoCut() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b), es(und(a, b)));
        assertEquals(1, cut.size);
        assertTrue(cut.members.isEmpty());
    }

    @Test
    public void stronglyConnected_emptyGraph_isFalse() {
        assertFalse(Connectivity.isStronglyConnected(vs(), es()));
    }

    @Test
    public void singleVertex_hasZeroCuts() {
        assertEquals(0, Connectivity.minimumVertexCut(vs(a), es()).size);
        assertEquals(0, Connectivity.minimumEdgeCut(vs(a), es()).size);
    }

    @Test
    public void edgeCut_cycleOfFour_isTwoEdgesThatDisconnect() {
        List<Edge> edges = es(und(a, b), und(b, c), und(c, d), und(d, a));
        Connectivity.Cut<Edge> cut = Connectivity.minimumEdgeCut(vs(a, b, c, d), edges);
        assertEquals(2, cut.size);
        assertEquals(2, cut.members.size());
        List<Edge> rest = new Vector<Edge>(edges);
        rest.removeAll(cut.members);
        assertFalse(Connectivity.isConnected(vs(a, b, c, d), rest));
    }

    @Test
    public void onlySelfLoops_isNotConnected() {
        assertFalse(Connectivity.isConnected(vs(a, b), es(und(a, a), und(b, b))));
    }

    @Test
    public void cuts_skipEdgesOutsideTheVertexList() {
        Edge stray = und(a, d);
        assertEquals(1, Connectivity.minimumEdgeCut(vs(a, b), es(und(a, b), stray)).size);
        assertEquals(1, Connectivity.minimumVertexCut(vs(a, b), es(und(a, b), stray)).size);
    }

    @Test
    public void vertexCut_complete_isNMinusOne_withNoCut() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c, d),
                es(und(a, b), und(a, c), und(a, d), arc(b, c), und(b, d), und(c, d), und(c, c)));
        assertEquals(3, cut.size);
        assertTrue(cut.members.isEmpty());
    }

    @Test
    public void vertexCut_disconnected_isZero() {
        assertEquals(0, Connectivity.minimumVertexCut(vs(a, b, c), es(und(a, b))).size);
    }
}
