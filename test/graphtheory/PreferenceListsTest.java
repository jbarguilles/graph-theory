package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreferenceListsTest {

    private static Vertex v(String name) { return new Vertex(name, 0, 0); }
    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }
    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }
    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }
    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void neighbours_ignoreDirectionAndSelfAndRepeats_inFirstEdgeOrder() {
        List<Edge> edges = es(arc(c, a), und(a, b), und(a, b), und(a, a), arc(a, c));
        assertEquals(Arrays.asList(c, b), PreferenceLists.neighboursOf(a, edges));
    }

    @Test
    public void sync_newNeighbourJoinsTheEnd() {
        a.preferences = new Vector<Vertex>(Arrays.asList(c, b));
        PreferenceLists.sync(vs(a, b, c, d), es(und(a, b), und(a, c), und(a, d)));
        assertEquals(Arrays.asList(c, b, d), a.preferences);
    }

    @Test
    public void sync_lostNeighbourLeaves_andNoListStaysNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(c, b));
        PreferenceLists.sync(vs(a, b, c), es(und(a, b)));
        assertEquals(Arrays.asList(b), a.preferences);
        assertNull(b.preferences);
    }

    @Test
    public void sync_isIdempotent() {
        a.preferences = new Vector<Vertex>(Arrays.asList(c, b));
        List<Vertex> all = vs(a, b, c, d);
        List<Edge> edges = es(und(a, b), und(a, c), und(a, d));
        PreferenceLists.sync(all, edges);
        List<Vertex> once = new Vector<Vertex>(a.preferences);
        PreferenceLists.sync(all, edges);
        assertEquals(once, a.preferences);
        assertEquals(Arrays.asList(c, b, d), a.preferences);
    }

    @Test
    public void sync_tidiesMessyList_duplicatesNonNeighboursAndArcIn() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b, b, d));
        PreferenceLists.sync(vs(a, b, c, d), es(und(a, b), arc(c, a)));
        assertEquals(Arrays.asList(b, c), a.preferences);
    }

    @Test
    public void sync_noNeighboursLeft_meansNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        PreferenceLists.sync(vs(a, b), es());
        assertNull(a.preferences);
    }

    @Test
    public void missing_listsVerticesWithNeighboursButNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        assertEquals(Arrays.asList(b), PreferenceLists.missing(vs(a, b, d), es(und(a, b))));
    }

    @Test
    public void anyLists() {
        assertFalse(PreferenceLists.any(vs(a, b)));
        b.preferences = new Vector<Vertex>(Arrays.asList(a));
        assertTrue(PreferenceLists.any(vs(a, b)));
    }
}
