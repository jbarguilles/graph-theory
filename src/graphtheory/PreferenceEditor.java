package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/** A working copy of the preference lists; apply() writes it into the vertices in one go. */
public class PreferenceEditor {

    private final List<Vertex> vertices = new ArrayList<Vertex>();
    private final List<Edge> edges;
    private final Map<Vertex, List<Vertex>> lists = new HashMap<Vertex, List<Vertex>>();
    private final Map<Vertex, List<Vertex>> original = new HashMap<Vertex, List<Vertex>>();

    public PreferenceEditor(List<Vertex> vs, List<Edge> es) {
        edges = es;
        for (Vertex v : vs) {
            if (PreferenceLists.neighboursOf(v, es).isEmpty()) continue;
            vertices.add(v);
            List<Vertex> copy = v.preferences == null ? null : new ArrayList<Vertex>(v.preferences);
            lists.put(v, copy);
            original.put(v, copy == null ? null : new ArrayList<Vertex>(copy));
        }
    }

    /** Vertices that can have a list (those with a neighbour), in vertex order. */
    public List<Vertex> vertices() { return vertices; }

    /** null = no list. */
    public List<Vertex> listOf(Vertex v) {
        List<Vertex> l = lists.get(v);
        return l == null ? null : Collections.unmodifiableList(l);
    }

    /** A list of v's neighbours in the order of their first edge. */
    public void create(Vertex v) { lists.put(v, new ArrayList<Vertex>(PreferenceLists.neighboursOf(v, edges))); }

    public void clear(Vertex v) { lists.put(v, null); }

    /** Moves entry i up one place; returns its new index. */
    public int moveUp(Vertex v, int i) { return swap(v, i, i - 1); }

    /** Moves entry i down one place; returns its new index. */
    public int moveDown(Vertex v, int i) { return swap(v, i, i + 1); }

    private int swap(Vertex v, int i, int j) {
        List<Vertex> l = lists.get(v);
        if (l == null || i < 0 || i >= l.size() || j < 0 || j >= l.size()) return i;
        Vertex t = l.get(i);
        l.set(i, l.get(j));
        l.set(j, t);
        return j;
    }

    public boolean changed() {
        for (Vertex v : vertices) {
            List<Vertex> now = lists.get(v), was = original.get(v);
            if (now == null ? was != null : !now.equals(was)) return true;
        }
        return false;
    }

    public void apply() {
        for (Vertex v : vertices) {
            List<Vertex> l = lists.get(v);
            v.preferences = l == null ? null : new Vector<Vertex>(l);
        }
    }
}
