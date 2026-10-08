package graphtheory;

import java.awt.event.KeyEvent;

/** The editing tools: the ids Canvas keeps in selectedTool, with their labels and hints. */
public final class Tools {

    public static final int NONE = 0;
    public static final int VERTEX = 1;
    public static final int EDGE = 2;
    public static final int GRAB = 3;
    public static final int REMOVE = 4;
    public static final int ARC = 5;
    public static final int PAIR = 6;
    public static final int WEIGHT = 7;
    public static final int ROOT = 8;
    public static final int WALK = 9;

    /** Order in the palette and the Tools menu. */
    public static final int[] ORDER = { GRAB, VERTEX, EDGE, ARC, WEIGHT, ROOT, REMOVE, PAIR, WALK };

    private Tools() {}

    /** Palette button text. */
    public static String shortLabel(int tool) {
        switch (tool) {
            case GRAB:   return "Grab";
            case VERTEX: return "Vertex";
            case EDGE:   return "Edge";
            case ARC:    return "Arc";
            case WEIGHT: return "Weight";
            case ROOT:   return "Root";
            case REMOVE: return "Remove";
            case PAIR:   return "Pair";
            case WALK:   return "Walk";
            default:     return "";
        }
    }

    /** Tools menu text (the names the menu has always used). */
    public static String menuLabel(int tool) {
        switch (tool) {
            case GRAB:   return "Grab Tool";
            case VERTEX: return "Add Vertex";
            case EDGE:   return "Add Edges";
            case ARC:    return "Add Directed Edge";
            case WEIGHT: return "Set Edge Weight";
            case ROOT:   return "Mark as Root";
            case REMOVE: return "Remove Tool";
            case PAIR:   return "Select Pair";
            case WALK:   return "Build Walk";
            default:     return "";
        }
    }

    /** Key used with Ctrl (the shortcuts the menu has always used). */
    public static int shortcut(int tool) {
        switch (tool) {
            case GRAB:   return KeyEvent.VK_G;
            case VERTEX: return KeyEvent.VK_A;
            case EDGE:   return KeyEvent.VK_E;
            case ARC:    return KeyEvent.VK_D;
            case WEIGHT: return KeyEvent.VK_W;
            case ROOT:   return KeyEvent.VK_T;
            case REMOVE: return KeyEvent.VK_R;
            case PAIR:   return KeyEvent.VK_P;
            case WALK:   return KeyEvent.VK_L;
            default:     return 0;
        }
    }

    /** Status-bar text while the tool is active. */
    public static String hint(int tool) {
        switch (tool) {
            case GRAB:   return "Click a vertex to select it, drag to move it. Double-click a vertex to rename it, or an edge to set its weight.";
            case VERTEX: return "Click empty space to add a vertex.";
            case EDGE:   return "Drag from one vertex to another to add an undirected edge. Release on the same vertex for a self-loop.";
            case ARC:    return "Drag from the source vertex to the destination to add a directed edge.";
            case WEIGHT: return "Click an edge to set its weight.";
            case ROOT:   return "Click a vertex to make it (or stop it being) the root of its component.";
            case REMOVE: return "Click a vertex or edge to remove it.";
            case PAIR:   return "Click two vertices to inspect the ordered pair. Up/Down or the list on the right browse its paths. Esc clears the pair.";
            case WALK:   return "Click a vertex to start, then vertices or edges to extend. Backspace or right-click undoes a step, Esc clears. A found walk is read-only: click a vertex to start a new one.";
            default:     return "Pick a tool on the left.";
        }
    }
}
