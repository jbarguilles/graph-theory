package graphtheory;

import java.util.ArrayDeque;

/**
 * Undo/redo as stacks of graph snapshots (.graph text). The caller records
 * the snapshot from before each edit; undo/redo hand back the snapshot to
 * restore and take the current one in exchange.
 */
public class EditHistory {

    private final int limit;
    private final ArrayDeque<String> undo = new ArrayDeque<String>();
    private final ArrayDeque<String> redo = new ArrayDeque<String>();

    public EditHistory(int limit) {
        this.limit = limit;
    }

    /** Call after an edit with the snapshot from before it. */
    public void record(String before) {
        push(undo, before);
        redo.clear();
    }

    public boolean canUndo() { return !undo.isEmpty(); }

    public boolean canRedo() { return !redo.isEmpty(); }

    /** The snapshot to restore, or null if there is nothing to undo. */
    public String undo(String current) {
        if (undo.isEmpty()) return null;
        push(redo, current);
        return undo.pop();
    }

    /** The snapshot to restore, or null if there is nothing to redo. */
    public String redo(String current) {
        if (redo.isEmpty()) return null;
        push(undo, current);
        return redo.pop();
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }

    private void push(ArrayDeque<String> stack, String snapshot) {
        stack.push(snapshot);
        if (stack.size() > limit) stack.removeLast();
    }
}
