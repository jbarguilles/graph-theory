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
}
