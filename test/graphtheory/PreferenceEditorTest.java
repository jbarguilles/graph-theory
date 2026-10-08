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

    @Test
    public void createThenClear_isNoChange() {
        PreferenceEditor ed = new PreferenceEditor(vs(a, b), es(und(a, b)));
        ed.create(a);
        ed.clear(a);
        assertFalse(ed.changed());
    }

    @Test
    public void moveUpThenDown_isNoChange() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b, c));
        PreferenceEditor ed = new PreferenceEditor(vs(a, b, c), es(und(a, b), und(a, c)));
        assertEquals(0, ed.moveUp(a, 1));
        assertEquals(1, ed.moveDown(a, 0));
        assertFalse(ed.changed());
    }

    @Test
    public void noSelection_movesNothing() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b, c));
        PreferenceEditor ed = new PreferenceEditor(vs(a, b, c), es(und(a, b), und(a, c)));
        assertEquals(-1, ed.moveUp(a, -1));
        assertEquals(-1, ed.moveDown(a, -1));
        assertFalse(ed.changed());
    }

    @Test
    public void moveDown_movesEntryDown() {
        PreferenceEditor ed = new PreferenceEditor(vs(a, b, c), es(und(a, b), und(a, c)));
        ed.create(a);
        assertEquals(1, ed.moveDown(a, 0));
        assertEquals(Arrays.asList(c, b), ed.listOf(a));
        assertEquals(1, ed.moveDown(a, 1));
    }

    @Test
    public void create_skipsSelfAndListsParallelNeighbourOnce() {
        PreferenceEditor ed = new PreferenceEditor(vs(a, b), es(und(a, a), und(a, b), und(b, a)));
        ed.create(a);
        assertEquals(Arrays.asList(b), ed.listOf(a));
    }
}
