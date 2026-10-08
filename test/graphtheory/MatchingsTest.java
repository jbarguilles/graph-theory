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

    private static boolean joins(List<Edge> m, Vertex x, Vertex y) {
        for (Edge e : m) {
            if ((e.vertex1 == x && e.vertex2 == y) || (e.vertex1 == y && e.vertex2 == x)) return true;
        }
        return false;
    }
}
