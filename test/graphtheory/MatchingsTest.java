package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatchingsTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    private static Vector<Vertex> prefs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    @Test
    public void maximal_greedyInEdgeOrder_skipsLoops_usesArcs() {
        Edge bc = arc(b, c);
        assertEquals(Arrays.asList(bc), Matchings.maximal(vs(a, b, c, d), es(und(a, a), bc, und(a, b), und(c, d))));
    }

    @Test
    public void maximum_beatsGreedyOnAPath() {
        // a-b-c-d: greedy taking b-c first gets 1; maximum is 2 (a-b, c-d)
        List<Edge> m = Matchings.maximum(vs(a, b, c, d), es(und(b, c), und(a, b), arc(d, c)));
        assertEquals(2, m.size());
    }

    @Test
    public void maximum_oddCycleLeavesOneOut() {
        assertEquals(1, Matchings.maximum(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))).size());
    }

    @Test
    public void maximum_tooLarge_isNull() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i <= Matchings.MAXIMUM_VERTEX_CAP; i++) many.add(v("v" + i));
        assertNull(Matchings.maximum(many, es()));
    }

    @Test
    public void stable_proposingSideGetsItsBest() {
        // Sides {a, c} and {b, d}; everyone adjacent across.
        List<Edge> edges = es(und(a, b), und(a, d), und(c, b), und(c, d));
        // Each side's first choices conflict, so the proposing side decides the result.
        a.preferences = prefs(b, d);
        c.preferences = prefs(d, b);
        b.preferences = prefs(c, a);
        d.preferences = prefs(a, c);
        List<Edge> fromA = Matchings.stable(vs(a, b, c, d), edges, a);
        assertTrue(joins(fromA, a, b) && joins(fromA, c, d));
        List<Edge> fromB = Matchings.stable(vs(a, b, c, d), edges, b);
        assertTrue(joins(fromB, c, b) && joins(fromB, a, d));
    }

    @Test
    public void stable_needsBipartiteAndLists() {
        List<Edge> edges = es(und(a, b));
        assertNull(Matchings.stable(vs(a, b), edges, a));   // no lists
        a.preferences = prefs(b);
        b.preferences = prefs(a);
        assertEquals(1, Matchings.stable(vs(a, b), edges, null).size());
        assertNull(Matchings.stable(vs(a, b, c), es(und(a, b), und(b, c), und(c, a)), a));
    }

    @Test
    public void maximum_isAValidMatching() {
        List<Edge> edges = es(und(a, b), und(b, c), und(c, d), und(a, a), arc(d, a));
        List<Edge> m = Matchings.maximum(vs(a, b, c, d), edges);
        java.util.Set<Vertex> seen = new java.util.HashSet<Vertex>();
        for (Edge e : m) {
            assertTrue(edges.contains(e));
            assertNotSame(e.vertex1, e.vertex2);
            assertTrue(seen.add(e.vertex1));
            assertTrue(seen.add(e.vertex2));
        }
        assertEquals(2, m.size());
    }

    @Test
    public void maximum_selfLoopAndParallelEdges() {
        List<Edge> m = Matchings.maximum(vs(a, b), es(und(a, a), und(a, b), arc(b, a), und(a, b)));
        assertEquals(1, m.size());
        assertNotSame(m.get(0).vertex1, m.get(0).vertex2);
    }

    @Test
    public void maximum_emptyGraph() {
        assertTrue(Matchings.maximum(vs(), es()).isEmpty());
    }

    @Test
    public void matchings_skipEdgesOutsideTheVertexList() {
        assertEquals(1, Matchings.maximum(vs(a, b), es(und(a, c), und(a, b))).size());
        assertEquals(1, Matchings.maximal(vs(a, b), es(und(a, c), und(a, b))).size());
    }

    @Test
    public void stable_starMatchesExactlyOneLeaf() {
        List<Edge> edges = es(und(a, b), und(a, c));
        a.preferences = prefs(c, b);
        b.preferences = prefs(a);
        c.preferences = prefs(a);
        List<Edge> m = Matchings.stable(vs(a, b, c), edges, a);
        assertEquals(1, m.size());
        assertTrue(joins(m, a, c));
        m = Matchings.stable(vs(a, b, c), edges, b);
        assertEquals(1, m.size());
        assertTrue(joins(m, a, c));
    }

    @Test
    public void stable_nullOrForeignProposerMeansSideA() {
        List<Edge> edges = es(und(a, b), und(a, d), und(c, b), und(c, d));
        a.preferences = prefs(b, d);
        c.preferences = prefs(d, b);
        b.preferences = prefs(c, a);
        d.preferences = prefs(a, c);
        for (Vertex p : new Vertex[] {null, v("outsider")}) {
            List<Edge> m = Matchings.stable(vs(a, b, c, d), edges, p);
            assertTrue(joins(m, a, b) && joins(m, c, d));
        }
    }

    @Test
    public void stable_selfLoopIsNull() {
        a.preferences = prefs(b);
        b.preferences = prefs(a);
        assertNull(Matchings.stable(vs(a, b), es(und(a, b), und(a, a)), a));
    }

    private static boolean joins(List<Edge> m, Vertex x, Vertex y) {
        for (Edge e : m) {
            if ((e.vertex1 == x && e.vertex2 == y) || (e.vertex1 == y && e.vertex2 == x)) return true;
        }
        return false;
    }
}
