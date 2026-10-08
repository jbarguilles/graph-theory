package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Connected, components, strongly connected, and the minimum vertex and edge cuts
 * (CONTEXT.md, Graph). Everything except strong connectivity ignores direction. The cuts
 * use max-flow (Menger), so they are exact and parallel edges count separately.
 */
public final class Connectivity {

    private Connectivity() {}

    /** A minimum cut: its size (kappa or lambda) and its members. Members are empty when no cut exists. */
    public static final class Cut<T> {
        public final int size;
        public final List<T> members;

        Cut(int size, List<T> members) {
            this.size = size;
            this.members = Collections.unmodifiableList(members);
        }
    }

    /** Maximal connected sets ignoring direction; each in vertex order, ordered by first vertex. */
    public static List<List<Vertex>> components(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[] parent = new int[vs.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i != null && j != null) parent[find(parent, i)] = find(parent, j);
        }
        List<List<Vertex>> out = new ArrayList<List<Vertex>>();
        Map<Integer, List<Vertex>> byRoot = new HashMap<Integer, List<Vertex>>();
        for (int i = 0; i < vs.size(); i++) {
            int r = find(parent, i);
            List<Vertex> comp = byRoot.get(r);
            if (comp == null) {
                comp = new ArrayList<Vertex>();
                byRoot.put(r, comp);
                out.add(comp);
            }
            comp.add(vs.get(i));
        }
        return out;
    }

    static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    public static boolean isConnected(List<Vertex> vs, List<Edge> es) {
        return !vs.isEmpty() && components(vs, es).size() == 1;
    }

    /** Every vertex reaches every other by a walk respecting direction. A single vertex is strong. */
    public static boolean isStronglyConnected(List<Vertex> vs, List<Edge> es) {
        if (vs.isEmpty()) return false;
        return reachesAll(vs, es, true) && reachesAll(vs, es, false);
    }

    private static boolean reachesAll(List<Vertex> vs, List<Edge> es, boolean forward) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[] seen = new boolean[vs.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        seen[0] = true;
        queue.add(0);
        while (!queue.isEmpty()) {
            Vertex u = vs.get(queue.poll());
            for (Edge e : es) {
                Vertex next = null;
                Vertex from = forward ? e.vertex1 : e.vertex2;
                Vertex to = forward ? e.vertex2 : e.vertex1;
                if (from == u) next = to;
                else if (!e.directed && to == u) next = from;
                Integer j = next == null ? null : idx.get(next);
                if (j != null && !seen[j]) {
                    seen[j] = true;
                    queue.add(j);
                }
            }
        }
        for (boolean s : seen) if (!s) return false;
        return true;
    }

    /** lambda(G) and a minimum edge cut. 0 and empty for a disconnected graph or fewer than two vertices. */
    public static Cut<Edge> minimumEdgeCut(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isConnected(vs, es)) return new Cut<Edge>(0, new ArrayList<Edge>());
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[][] cap = new int[n][n];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null || i.equals(j)) continue;
            cap[i][j]++;
            cap[j][i]++;
        }
        int best = Integer.MAX_VALUE;
        List<Edge> bestCut = null;
        for (int t = 1; t < n; t++) {
            Flow f = new Flow(cap);
            int flow = f.maxFlow(0, t);
            if (flow < best) {
                best = flow;
                boolean[] side = f.sourceSide(0);
                bestCut = new ArrayList<Edge>();
                for (Edge e : es) {
                    Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
                    if (i == null || j == null) continue;
                    if (side[i] != side[j]) bestCut.add(e);
                }
            }
        }
        return new Cut<Edge>(best, bestCut);
    }

    /**
     * kappa(G) and a minimum vertex cut. 0 and empty when disconnected or fewer than two vertices;
     * n - 1 and empty when every two vertices are adjacent (no vertex set disconnects the graph).
     */
    public static Cut<Vertex> minimumVertexCut(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isConnected(vs, es)) return new Cut<Vertex>(0, new ArrayList<Vertex>());
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[][] adj = new boolean[n][n];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (!i.equals(j)) adj[i][j] = adj[j][i] = true;
        }
        int inf = n + 1;
        int best = n - 1;
        List<Vertex> bestCut = new ArrayList<Vertex>();
        for (int s = 0; s < n; s++) {
            for (int t = s + 1; t < n; t++) {
                if (adj[s][t]) continue;
                // Vertex v becomes in-node 2v and out-node 2v+1, joined with capacity 1 (s and t: unlimited).
                int[][] cap = new int[2 * n][2 * n];
                for (int v = 0; v < n; v++) cap[2 * v][2 * v + 1] = (v == s || v == t) ? inf : 1;
                for (int u = 0; u < n; u++) {
                    for (int v = 0; v < n; v++) if (adj[u][v]) cap[2 * u + 1][2 * v] = inf;
                }
                Flow f = new Flow(cap);
                int flow = f.maxFlow(2 * s + 1, 2 * t);
                if (flow < best) {
                    best = flow;
                    boolean[] side = f.sourceSide(2 * s + 1);
                    bestCut = new ArrayList<Vertex>();
                    for (int v = 0; v < n; v++) {
                        if (side[2 * v] && !side[2 * v + 1]) bestCut.add(vs.get(v));
                    }
                }
            }
        }
        return new Cut<Vertex>(best, bestCut);
    }

    /** Edmonds--Karp on a small capacity matrix. */
    private static final class Flow {
        private final int[][] residual;

        Flow(int[][] cap) {
            residual = new int[cap.length][];
            for (int i = 0; i < cap.length; i++) residual[i] = cap[i].clone();
        }

        int maxFlow(int s, int t) {
            int total = 0;
            int n = residual.length;
            while (true) {
                int[] prev = new int[n];
                Arrays.fill(prev, -1);
                prev[s] = s;
                ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
                queue.add(s);
                while (!queue.isEmpty() && prev[t] < 0) {
                    int u = queue.poll();
                    for (int v = 0; v < n; v++) {
                        if (prev[v] < 0 && residual[u][v] > 0) {
                            prev[v] = u;
                            queue.add(v);
                        }
                    }
                }
                if (prev[t] < 0) return total;
                int push = Integer.MAX_VALUE;
                for (int v = t; v != s; v = prev[v]) push = Math.min(push, residual[prev[v]][v]);
                for (int v = t; v != s; v = prev[v]) {
                    residual[prev[v]][v] -= push;
                    residual[v][prev[v]] += push;
                }
                total += push;
            }
        }

        /** After maxFlow: the nodes still reachable from s in the residual graph. */
        boolean[] sourceSide(int s) {
            boolean[] seen = new boolean[residual.length];
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            seen[s] = true;
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v = 0; v < residual.length; v++) {
                    if (!seen[v] && residual[u][v] > 0) {
                        seen[v] = true;
                        queue.add(v);
                    }
                }
            }
            return seen;
        }
    }
}
