package graphtheory;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Static lookup table mapping (u, v) → weight for the current graph.
 * Rebuilt by Canvas.refresh() on every redraw, so it is always in sync
 * with the current edgeList. Used by VertexPair for weighted Dijkstra.
 * Keyed by Vertex identity, so names never matter.
 */
public class EdgeRegistry {

    private static Map<Vertex, Map<Vertex, Integer>> weights = new HashMap<Vertex, Map<Vertex, Integer>>();

    /** Rebuild the lookup from the current edge list. */
    public static void rebuild(Vector<Edge> edgeList) {
        weights.clear();
        if (edgeList == null) return;
        for (Edge e : edgeList) {
            if (e == null || e.vertex1 == null || e.vertex2 == null) continue;

            if (e.directed) {
                // Directed arc: only u → v gets the weight.
                put(e.vertex1, e.vertex2, e.weight);
            } else {
                // Undirected: both u → v and v → u get the same weight.
                // A previously-registered directed arc keeps priority.
                if (!has(e.vertex1, e.vertex2)) put(e.vertex1, e.vertex2, e.weight);
                if (!has(e.vertex2, e.vertex1)) put(e.vertex2, e.vertex1, e.weight);
            }
        }
    }

    /** Weight of u → v, or -1 if there is no such edge. */
    public static int weightOf(Vertex u, Vertex v) {
        if (u == null || v == null) return -1;
        Map<Vertex, Integer> out = weights.get(u);
        Integer w = out == null ? null : out.get(v);
        return w == null ? -1 : w;
    }

    private static boolean has(Vertex u, Vertex v) {
        Map<Vertex, Integer> out = weights.get(u);
        return out != null && out.containsKey(v);
    }

    private static void put(Vertex u, Vertex v, int w) {
        Map<Vertex, Integer> out = weights.get(u);
        if (out == null) {
            out = new HashMap<Vertex, Integer>();
            weights.put(u, out);
        }
        out.put(v, w);
    }
}
