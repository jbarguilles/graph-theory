package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class VertexNamesTest {

    private static Vector<Vertex> named(String... names) {
        Vector<Vertex> vs = new Vector<Vertex>();
        for (String n : names) vs.add(new Vertex(n, 0, 0));
        return vs;
    }

    @Test
    public void isValid_acceptsShortLabels() {
        for (String n : new String[] {"a", "v1", "u_2", "ABCD", "0", "_"}) {
            assertTrue(n, VertexNames.isValid(n));
        }
    }

    @Test
    public void isValid_rejectsEverythingElse() {
        for (String n : new String[] {"", "abcde", "a b", "a-b", "a#", "é", " a"}) {
            assertFalse(n, VertexNames.isValid(n));
        }
        assertFalse(VertexNames.isValid(null));
    }

    @Test
    public void checkRename_acceptsAFreeName() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(0), "c", vs));
    }

    @Test
    public void checkRename_allowsKeepingTheSameName() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(0), "a", vs));
    }

    @Test
    public void checkRename_rejectsANameInUse() {
        Vector<Vertex> vs = named("a", "b");
        assertEquals("'b' is already used by another vertex.",
                     VertexNames.checkRename(vs.get(0), "b", vs));
    }

    @Test
    public void checkRename_isCaseSensitive() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(1), "A", vs));
    }

    @Test
    public void checkRename_rejectsAnInvalidName() {
        Vector<Vertex> vs = named("a");
        assertEquals("Names are 1-4 letters, digits or _.",
                     VertexNames.checkRename(vs.get(0), "toolong", vs));
    }

    @Test
    public void nextFree_fillsTheLowestGap() {
        assertEquals("2", VertexNames.nextFree(named("0", "1", "3", "x")));
    }

    @Test
    public void nextFree_emptyGraphStartsAtZero() {
        assertEquals("0", VertexNames.nextFree(named()));
    }
}
