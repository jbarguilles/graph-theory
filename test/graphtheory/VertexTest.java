package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class VertexTest {

    @Test
    public void degree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.degree());
    }

    @Test
    public void degree_twoUndirectedNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.undirectedNeighbors.add(new Vertex("a", 0, 0));
        v.undirectedNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(2, v.degree());
    }

    @Test
    public void degree_selfLoop_countsAsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.undirectedNeighbors.add(v);
        assertEquals(2, v.degree());
    }

    @Test
    public void degree_severalSelfLoops_eachCountsAsTwo() throws GraphFile.FormatException {
        GraphFile.Data d = GraphFile.read("graph-theory 1\nvertex a 0 0\nvertex b 10 0\n"
                + "edge a a\nedge a a\nedge a b\n");
        Vertex a = d.vertices.get(0);
        assertEquals(5, a.degree());
        assertEquals(1, d.vertices.get(1).degree());
    }

    @Test
    public void degree_ignoresDirectedNeighbors() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(new Vertex("a", 0, 0));
        v.outNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(0, v.degree());
    }

    @Test
    public void inDegree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.inDegree());
    }

    @Test
    public void inDegree_twoInNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(new Vertex("a", 0, 0));
        v.inNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(2, v.inDegree());
    }

    @Test
    public void inDegree_directedSelfLoop_countsAsOne() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(v);
        assertEquals(1, v.inDegree());
    }

    @Test
    public void outDegree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.outDegree());
    }

    @Test
    public void outDegree_twoOutNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.outNeighbors.add(new Vertex("a", 0, 0));
        v.outNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(2, v.outDegree());
    }

    @Test
    public void isIsolated_noNeighbors_returnsTrue() {
        Vertex v = new Vertex("v", 0, 0);
        assertTrue(v.isIsolated());
    }

    @Test
    public void isIsolated_hasUndirectedNeighbor_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.undirectedNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }

    @Test
    public void isIsolated_hasInNeighborOnly_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }

    @Test
    public void isIsolated_hasOutNeighborOnly_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.outNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }

    @Test
    public void hasIntersection_insideRadius_true() {
        Vertex v = new Vertex("v", 100, 100);
        assertTrue(v.hasIntersection(100 + Vertex.RADIUS, 100));
    }

    @Test
    public void hasIntersection_justOutsideRadius_false() {
        Vertex v = new Vertex("v", 100, 100);
        assertFalse(v.hasIntersection(100 + Vertex.RADIUS + 1, 100));
    }

    @Test
    public void radius_is18() {
        assertEquals(18, Vertex.RADIUS);
    }
}
