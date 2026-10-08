package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Whole-graph structure (CONTEXT.md, Graph). Each method says whether it respects direction. */
public final class Structure {

    private Structure() {}

    /**
     * No loops, and no two edges on one pair except the opposite arcs (a, b) and (b, a).
     * Respects direction only in allowing that arc pair.
     */
    public static boolean isSimple(List<Vertex> vs, List<Edge> es) {
        Map<Integer, List<Edge>> byPair = new HashMap<Integer, List<Edge>>();
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int n = vs.size();
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (i.intValue() == j.intValue()) return false;
            int key = Math.min(i, j) * n + Math.max(i, j);
            List<Edge> on = byPair.get(key);
            if (on == null) byPair.put(key, on = new ArrayList<Edge>());
            on.add(e);
        }
        for (List<Edge> on : byPair.values()) {
            if (on.size() == 1) continue;
            if (on.size() > 2) return false;
            Edge x = on.get(0), y = on.get(1);
            if (!(x.directed && y.directed && x.vertex1 == y.vertex2)) return false;
        }
        return true;
    }

    /** At least one vertex and no edges. Direction does not matter. */
    public static boolean isEmpty(List<Vertex> vs, List<Edge> es) {
        return !vs.isEmpty() && es.isEmpty();
    }

    /** Simple, and every two vertices joined both ways (an undirected edge, or both arcs). Respects direction. */
    public static boolean isComplete(List<Vertex> vs, List<Edge> es) {
        if (vs.isEmpty() || !isSimple(vs, es)) return false;
        boolean[][] step = steps(vs, es);
        for (int i = 0; i < vs.size(); i++) {
            for (int j = 0; j < vs.size(); j++) if (i != j && !step[i][j]) return false;
        }
        return true;
    }

    /** Fraction of ordered pairs (u, v), u != v, with one edge leading u to v. null unless simple with n >= 2. Respects direction. */
    public static Double density(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isSimple(vs, es)) return null;
        boolean[][] step = steps(vs, es);
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) if (i != j && step[i][j]) count++;
        }
        return (double) count / (n * (n - 1));
    }

    /**
     * Contains a cycle, respecting direction (CONTEXT.md, Cyclic). A loop is a cycle; two
     * distinct edges that go u to v and v to u are a cycle of length 2. With neither, a longer
     * cycle exists exactly when an arc lies inside a strongly connected part, or the
     * undirected edges close a cycle.
     */
    public static boolean isCyclic(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        es = within(idx, es);
        for (Edge e : es) if (e.vertex1 == e.vertex2) return true;
        for (int i = 0; i < es.size(); i++) {
            for (int j = 0; j < es.size(); j++) {
                if (i == j) continue;
                Edge x = es.get(i), y = es.get(j);
                if (goes(x, x.vertex1, x.vertex2) && goes(y, x.vertex2, x.vertex1)) return true;
            }
        }
        int n = vs.size();
        boolean[][] reach = steps(vs, es);
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (!reach[i][k]) continue;
                for (int j = 0; j < n; j++) if (reach[k][j]) reach[i][j] = true;
            }
        }
        for (Edge e : es) {
            if (e.directed && reach[idx.get(e.vertex2)][idx.get(e.vertex1)]) return true;
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (Edge e : es) {
            if (e.directed) continue;
            int x = Connectivity.find(parent, idx.get(e.vertex1)), y = Connectivity.find(parent, idx.get(e.vertex2));
            if (x == y) return true;
            parent[x] = y;
        }
        return false;
    }

    /** No cycle ignoring direction, counting parallel edges and loops. */
    public static boolean isForest(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[] parent = new int[vs.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (Edge e : within(idx, es)) {
            int x = Connectivity.find(parent, idx.get(e.vertex1)), y = Connectivity.find(parent, idx.get(e.vertex2));
            if (x == y) return false;
            parent[x] = y;
        }
        return true;
    }

    /** A forest that is connected. Direction does not matter. */
    public static boolean isTree(List<Vertex> vs, List<Edge> es) {
        return isForest(vs, es) && Connectivity.isConnected(vs, es);
    }

    /** A tree with at least three vertices, one of them adjacent to all the others. Direction does not matter. */
    public static boolean isStar(List<Vertex> vs, List<Edge> es) {
        if (vs.size() < 3 || !isTree(vs, es)) return false;
        for (Vertex c : vs) {
            if (PreferenceLists.neighboursOf(c, es).size() == vs.size() - 1) return true;
        }
        return false;
    }

    /**
     * The two sides of a bipartition, ignoring direction, or null if there is none. Each
     * component puts its first vertex (in vertex order) on side A; both lists are in vertex order.
     */
    public static List<List<Vertex>> bipartiteSides(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int n = vs.size();
        List<List<Integer>> adj = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<Integer>());
        for (Edge e : within(idx, es)) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == j) return null;
            adj.get(i).add(j);
            adj.get(j).add(i);
        }
        int[] colour = new int[n];
        Arrays.fill(colour, -1);
        for (int s = 0; s < n; s++) {
            if (colour[s] >= 0) continue;
            colour[s] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int w : adj.get(u)) {
                    if (colour[w] < 0) {
                        colour[w] = 1 - colour[u];
                        queue.add(w);
                    } else if (colour[w] == colour[u]) {
                        return null;
                    }
                }
            }
        }
        List<List<Vertex>> sides = new ArrayList<List<Vertex>>();
        sides.add(new ArrayList<Vertex>());
        sides.add(new ArrayList<Vertex>());
        for (int i = 0; i < n; i++) sides.get(colour[i]).add(vs.get(i));
        return sides;
    }

    /** Simple, bipartite with two non-empty sides, every cross pair adjacent. Direction does not matter. */
    public static boolean isCompleteBipartite(List<Vertex> vs, List<Edge> es) {
        List<List<Vertex>> sides = bipartiteSides(vs, es);
        if (sides == null || sides.get(0).isEmpty() || sides.get(1).isEmpty() || !isSimple(vs, es)) return false;
        for (Vertex x : sides.get(0)) {
            Set<Vertex> nbrs = new HashSet<Vertex>(PreferenceLists.neighboursOf(x, es));
            if (!nbrs.containsAll(sides.get(1))) return false;
        }
        return true;
    }

    /** step[i][j]: some edge leads from vertex i to vertex j. */
    private static boolean[][] steps(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[][] step = new boolean[vs.size()][vs.size()];
        for (Edge e : within(idx, es)) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            step[i][j] = true;
            if (!e.directed) step[j][i] = true;
        }
        return step;
    }

    /** The edges whose two ends are both in the vertex list; any others are skipped. */
    private static List<Edge> within(Map<Vertex, Integer> idx, List<Edge> es) {
        List<Edge> in = new ArrayList<Edge>();
        for (Edge e : es) {
            if (idx.get(e.vertex1) != null && idx.get(e.vertex2) != null) in.add(e);
        }
        return in;
    }

    /** Edge e can be crossed from x to y. */
    private static boolean goes(Edge e, Vertex x, Vertex y) {
        if (e.vertex1 == x && e.vertex2 == y) return true;
        return !e.directed && e.vertex1 == y && e.vertex2 == x;
    }
}
