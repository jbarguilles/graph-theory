package graphtheory;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Static lookup table mapping (u, v) → weight for the current graph.
 * Rebuilt by Canvas.refresh() on every redraw, so it is always in sync
 * with the current edgeList. Used by VertexPair for weighted Dijkstra.
 */
public class EdgeRegistry {

    private static Map<String, Integer> weights = new HashMap<String, Integer>();

    /** Rebuild the lookup from the current edge list. */
    public static void rebuild(Vector<Edge> edgeList) {
        weights.clear();
        if (edgeList == null) return;
        for (Edge e : edgeList) {
            if (e == null || e.vertex1 == null || e.vertex2 == null) continue;

            String key = key(e.vertex1, e.vertex2);

            if (e.directed) {
                // Directed arc: only u → v gets the weight.
                weights.put(key, e.weight);
            } else {
                // Undirected: both u → v and v → u get the same weight.
                // A previously-registered directed arc keeps priority.
                if (!weights.containsKey(key)) {
                    weights.put(key, e.weight);
                }
                String rev = key(e.vertex2, e.vertex1);
                if (!weights.containsKey(rev) || weights.get(rev) == null) {
                    weights.put(rev, e.weight);
                }
            }
        }
    }

    /** Weight of u → v, or -1 if there is no such edge. */
    public static int weightOf(Vertex u, Vertex v) {
        if (u == null || v == null) return -1;
        Integer w = weights.get(key(u, v));
        return w == null ? -1 : w;
    }

    private static String key(Vertex a, Vertex b) {
        return a.name + "->" + b.name;
    }
}