package graphtheory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/** Preference lists (CONTEXT.md): each names exactly its vertex's neighbours other than itself. */
public final class PreferenceLists {

    private PreferenceLists() {}

    /** v's neighbours other than itself, ignoring direction, each once, in the order of their first edge. */
    public static List<Vertex> neighboursOf(Vertex v, List<Edge> edges) {
        List<Vertex> out = new ArrayList<Vertex>();
        for (Edge e : edges) {
            Vertex other = e.vertex1 == v ? e.vertex2 : e.vertex2 == v ? e.vertex1 : null;
            if (other != null && other != v && !out.contains(other)) out.add(other);
        }
        return out;
    }

    /**
     * Brings every list in step with the edges: non-neighbours leave, new neighbours join the
     * end, and a list with no neighbours left becomes no list. Idempotent.
     */
    public static void sync(List<Vertex> vertices, List<Edge> edges) {
        for (Vertex v : vertices) {
            if (v.preferences == null) continue;
            List<Vertex> nbrs = neighboursOf(v, edges);
            Vector<Vertex> kept = new Vector<Vertex>();
            for (Vertex p : v.preferences) if (nbrs.contains(p) && !kept.contains(p)) kept.add(p);
            for (Vertex n : nbrs) if (!kept.contains(n)) kept.add(n);
            v.preferences = kept.isEmpty() ? null : kept;
        }
    }

    /** Vertices that have a neighbour but no preference list, in vertex order. */
    public static List<Vertex> missing(List<Vertex> vertices, List<Edge> edges) {
        List<Vertex> out = new ArrayList<Vertex>();
        for (Vertex v : vertices) {
            if (v.preferences == null && !neighboursOf(v, edges).isEmpty()) out.add(v);
        }
        return out;
    }

    public static boolean any(List<Vertex> vertices) {
        for (Vertex v : vertices) if (v.preferences != null) return true;
        return false;
    }
}
