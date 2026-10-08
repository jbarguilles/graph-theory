package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PropertiesReportTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static Edge w(Edge e, int weight) { e.setWeight(weight); return e; }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void weightedOnlyWhenSomeWeightIsNotOne() {
        assertNull(new PropertiesReport(vs(a, b), es(und(a, b)), null).weightedDistances);
        assertNotNull(new PropertiesReport(vs(a, b), es(w(und(a, b), 4)), null).weightedDistances);
    }

    @Test
    public void strongConnectivityOnlyWithArcs() {
        assertNull(new PropertiesReport(vs(a, b), es(und(a, b)), null).stronglyConnected);
        assertEquals(Boolean.FALSE, new PropertiesReport(vs(a, b), es(arc(a, b)), null).stronglyConnected);
    }

    @Test
    public void perfectFollowsMaximum() {
        PropertiesReport r = new PropertiesReport(vs(a, b, c), es(und(a, b), und(b, c)), null);
        assertEquals(1, r.maximum.size());
        assertEquals(Boolean.FALSE, r.perfect);
    }

    @Test
    public void proposerOutsideGraph_fallsBackToSideA() {
        PropertiesReport r = new PropertiesReport(vs(a, b), es(und(a, b)), v("zz"));
        assertSame(a, r.proposer);
    }

    @Test
    public void emptyGraph_doesNotThrow() {
        PropertiesReport r = new PropertiesReport(vs(), es(), null);
        assertEquals(0, r.vertexCut.size);
        assertTrue(r.blocks.isEmpty());
    }

    @Test(timeout = 1000)
    public void twentyVerticesSixtyEdges_isFast() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i < 20; i++) many.add(v("v" + i));
        List<Edge> edges = new Vector<Edge>();
        java.util.Random rnd = new java.util.Random(7);
        while (edges.size() < 60) {
            Vertex x = many.get(rnd.nextInt(20)), y = many.get(rnd.nextInt(20));
            edges.add(rnd.nextBoolean() ? und(x, y) : arc(x, y));
        }
        new PropertiesReport(many, edges, null);
    }
}
