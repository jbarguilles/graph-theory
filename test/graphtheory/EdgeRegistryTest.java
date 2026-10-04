package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class EdgeRegistryTest {

    private static Edge edge(Vertex a, Vertex b, boolean directed, int weight) {
        Edge e = new Edge(a, b, directed);
        e.weight = weight;
        return e;
    }

    @Test
    public void verticesWithTheSameName_keepTheirOwnWeights() {
        Vertex x1 = new Vertex("x", 0, 0);
        Vertex x2 = new Vertex("x", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(x1, b, true, 5));
        es.add(edge(x2, b, true, 9));
        EdgeRegistry.rebuild(es);
        assertEquals(5, EdgeRegistry.weightOf(x1, b));
        assertEquals(9, EdgeRegistry.weightOf(x2, b));
    }

    @Test
    public void arrowInsideNames_doesNotCollide() {
        Vertex p = new Vertex("a->", 0, 0);
        Vertex q = new Vertex("b", 0, 0);
        Vertex r = new Vertex("a", 0, 0);
        Vertex s = new Vertex("->b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(p, q, true, 5));
        EdgeRegistry.rebuild(es);
        assertEquals(-1, EdgeRegistry.weightOf(r, s));
    }

    @Test
    public void undirectedEdge_givesBothDirections_butAnEarlierArcKeepsPriority() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(a, b, true, 3));
        es.add(edge(a, b, false, 7));
        EdgeRegistry.rebuild(es);
        assertEquals(3, EdgeRegistry.weightOf(a, b));
        assertEquals(7, EdgeRegistry.weightOf(b, a));
    }
}
