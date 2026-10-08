package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class StructureTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void simple() {
        assertTrue(Structure.isSimple(vs(a, b), es(arc(a, b), arc(b, a))));
        assertFalse(Structure.isSimple(vs(a, b), es(und(a, b), arc(a, b))));
        assertFalse(Structure.isSimple(vs(a, b), es(arc(a, b), arc(a, b))));
        assertFalse(Structure.isSimple(vs(a), es(und(a, a))));
    }

    @Test
    public void empty_needsAVertex() {
        assertTrue(Structure.isEmpty(vs(a), es()));
        assertFalse(Structure.isEmpty(vs(), es()));
        assertFalse(Structure.isEmpty(vs(a, b), es(arc(a, b))));
    }

    @Test
    public void complete_mixedCanBeComplete_oneArcIsNot() {
        assertTrue(Structure.isComplete(vs(a, b, c), es(und(a, b), arc(a, c), arc(c, a), und(b, c))));
        assertFalse(Structure.isComplete(vs(a, b), es(arc(a, b))));
        assertTrue(Structure.isComplete(vs(a), es()));
        assertFalse(Structure.isComplete(vs(a, b), es(und(a, b), und(a, b))));
    }

    @Test
    public void density_orderedPairs_simpleOnly() {
        assertEquals(1.0, Structure.density(vs(a, b, c), es(und(a, b), arc(a, c), arc(c, a), und(b, c))), 1e-9);
        assertEquals(1.0 / 3, Structure.density(vs(a, b, c), es(und(a, b))), 1e-9);
        assertEquals(1.0 / 6, Structure.density(vs(a, b, c), es(arc(a, b))), 1e-9);
        assertNull(Structure.density(vs(a), es()));
        assertNull(Structure.density(vs(a, b), es(und(a, b), und(a, b))));
    }

    @Test
    public void cyclic_respectsDirection() {
        assertFalse(Structure.isCyclic(vs(a, b, c), es(arc(a, b), arc(b, c), arc(a, c))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(arc(a, b), arc(b, c), arc(c, a))));
        assertTrue(Structure.isCyclic(vs(a, b), es(und(a, b), und(a, b))));
        assertTrue(Structure.isCyclic(vs(a, b), es(und(a, b), arc(b, a))));
        assertTrue(Structure.isCyclic(vs(a), es(arc(a, a))));
        assertFalse(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c), arc(c, a))));
        assertFalse(Structure.isCyclic(vs(a, b), es(arc(a, b), arc(a, b))));
    }

    @Test
    public void forestAndTree_ignoreDirection_countParallelEdges() {
        assertTrue(Structure.isTree(vs(a, b, c), es(arc(a, b), arc(b, c))));
        assertFalse(Structure.isForest(vs(a, b, c), es(arc(a, b), arc(b, c), arc(a, c))));
        assertFalse(Structure.isForest(vs(a, b), es(und(a, b), und(a, b))));
        assertFalse(Structure.isForest(vs(a), es(und(a, a))));
        assertTrue(Structure.isForest(vs(a, b, c), es(und(a, b))));
        assertFalse(Structure.isTree(vs(a, b, c), es(und(a, b))));
    }

    @Test
    public void star() {
        assertTrue(Structure.isStar(vs(a, b, c, d), es(und(a, b), arc(c, a), und(a, d))));
        assertFalse(Structure.isStar(vs(a, b, c, d), es(und(a, b), und(b, c), und(c, d))));
        assertFalse(Structure.isStar(vs(a, b), es(und(a, b))));
    }

    @Test
    public void bipartiteSides_firstVertexOnSideA_loopIsNotBipartite() {
        List<List<Vertex>> s = Structure.bipartiteSides(vs(a, b, c, d), es(und(b, a), arc(c, b)));
        assertEquals(Arrays.asList(a, c, d), s.get(0));
        assertEquals(Arrays.asList(b), s.get(1));
        assertNull(Structure.bipartiteSides(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))));
        assertNull(Structure.bipartiteSides(vs(a), es(und(a, a))));
        assertNotNull(Structure.bipartiteSides(vs(a), es()));
    }

    @Test
    public void completeBipartite_mustBeSimple() {
        assertTrue(Structure.isCompleteBipartite(vs(a, b, c), es(und(a, b), und(a, c))));
        assertFalse(Structure.isCompleteBipartite(vs(a, b, c, d), es(und(a, b), und(a, b), und(a, d), und(c, d))));
        assertFalse(Structure.isCompleteBipartite(vs(a), es()));
    }
}
