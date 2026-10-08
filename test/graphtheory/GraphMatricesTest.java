package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import static org.junit.Assert.*;

public class GraphMatricesTest {

    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static Edge w(Edge e, int weight) { e.setWeight(weight); return e; }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void adjacency_countsEdges_rowToColumn() {
        int[][] m = GraphMatrices.adjacency(vs(a, b, c),
                es(und(a, b), und(a, b), arc(b, c), und(a, a), arc(c, c)));
        assertArrayEquals(new int[] {2, 2, 0}, m[0]);   // undirected loop counts 2
        assertArrayEquals(new int[] {2, 0, 1}, m[1]);
        assertArrayEquals(new int[] {0, 0, 1}, m[2]);   // directed loop counts 1
    }

    @Test
    public void adjacency_rowSumIsDegreePlusOutDegree() {
        // a: two undirected to b, loop (2) -> degree 4; arc a->c -> out 1
        int[][] m = GraphMatrices.adjacency(vs(a, b, c), es(und(a, b), und(a, b), und(a, a), arc(a, c)));
        assertEquals(5, m[0][0] + m[0][1] + m[0][2]);
    }

    @Test
    public void distances_countEdges_respectDirection() {
        int[][] d = GraphMatrices.distances(vs(a, b, c), es(w(arc(a, b), 9), und(b, c)));
        assertArrayEquals(new int[] {0, 1, 2}, d[0]);
        assertArrayEquals(new int[] {GraphMatrices.UNREACHABLE, 0, 1}, d[1]);
    }

    @Test
    public void weightedDistances_sumWeights() {
        int[][] d = GraphMatrices.weightedDistances(vs(a, b, c),
                es(w(und(a, b), 5), w(und(a, c), 1), w(und(c, b), 1)));
        assertEquals(2, d[0][1]);
        assertEquals(0, d[1][1]);
    }

    @Test
    public void weightedDistances_unreachable() {
        int[][] d = GraphMatrices.weightedDistances(vs(a, b), es(w(arc(b, a), 3)));
        assertEquals(GraphMatrices.UNREACHABLE, d[0][1]);
        assertEquals(3, d[1][0]);
    }
}
