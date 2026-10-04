package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditHistoryTest {

    @Test
    public void fresh_hasNothingToUndoOrRedo() {
        EditHistory h = new EditHistory(50);
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
        assertNull(h.undo("now"));
        assertNull(h.redo("now"));
    }

    @Test
    public void undoThenRedo_walksBackAndForth() {
        EditHistory h = new EditHistory(50);
        h.record("A");                       // the graph was A, now it is B
        assertEquals("A", h.undo("B"));
        assertTrue(h.canRedo());
        assertEquals("B", h.redo("A"));
        assertFalse(h.canRedo());
        assertTrue(h.canUndo());
    }

    @Test
    public void aNewEdit_clearsRedo() {
        EditHistory h = new EditHistory(50);
        h.record("A");
        h.undo("B");
        h.record("A");                       // edited A into C instead
        assertFalse(h.canRedo());
    }

    @Test
    public void oldestSnapshotsAreDroppedPastTheLimit() {
        EditHistory h = new EditHistory(2);
        h.record("1");
        h.record("2");
        h.record("3");
        assertEquals("3", h.undo("4"));
        assertEquals("2", h.undo("3"));
        assertNull(h.undo("2"));
    }

    @Test
    public void clear_forgetsEverything() {
        EditHistory h = new EditHistory(50);
        h.record("A");
        h.undo("B");
        h.record("C");
        h.clear();
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
    }
}
