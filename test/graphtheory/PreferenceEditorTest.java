package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreferenceEditorTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void onlyVerticesWithNeighbours_areListed() {
        PreferenceEditor ed = new PreferenceEditor(vs(a, b, c, d), es(und(a, b), und(a, c)));
        assertEquals(Arrays.asList(a, b, c), ed.vertices());
    }

    @Test
    public void create_reorder_apply() {
        List<Vertex> all = vs(a, b, c);
        PreferenceEditor ed = new PreferenceEditor(all, es(und(a, b), und(a, c)));
        assertNull(ed.listOf(a));
        ed.create(a);
        assertEquals(Arrays.asList(b, c), ed.listOf(a));
        assertEquals(0, ed.moveUp(a, 1));
        assertEquals(Arrays.asList(c, b), ed.listOf(a));
        assertTrue(ed.changed());
        assertNull(a.preferences);   // nothing touches the graph before apply
        ed.apply();
        assertEquals(Arrays.asList(c, b), a.preferences);
    }

    @Test
    public void clear_andNoChange() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        PreferenceEditor ed = new PreferenceEditor(vs(a, b), es(und(a, b)));
        assertFalse(ed.changed());
        assertEquals(0, ed.moveUp(a, 0));
        assertFalse(ed.changed());
        ed.clear(a);
        assertTrue(ed.changed());
        ed.apply();
        assertNull(a.preferences);
    }
}
