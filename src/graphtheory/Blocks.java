package graphtheory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bridges, blocks and nonseparability (CONTEXT.md), ignoring direction. Works on edges, not
 * neighbour sets, so parallel edges are distinct (ADR 0002): a parallel edge is never a bridge
 * and both copies land in the same block.
 */
public final class Blocks {

    private Blocks() {}

    public static final class Block {
        /** In vertex order. */
        public final List<Vertex> vertices;
        public final List<Edge> edges;

        Block(List<Vertex> vertices, List<Edge> edges) {
            this.vertices = Collections.unmodifiableList(vertices);
            this.edges = Collections.unmodifiableList(edges);
        }
    }

    public static Set<Edge> bridges(List<Vertex> vs, List<Edge> es) {
        Tarjan t = new Tarjan(vs, es);
        return Collections.unmodifiableSet(t.bridges);
    }

    /** Every block; isolated vertices are blocks of their own, and self-loops join a block of their vertex. */
    public static List<Block> blocks(List<Vertex> vs, List<Edge> es) {
        Tarjan t = new Tarjan(vs, es);
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        List<List<Edge>> edgeSets = new ArrayList<List<Edge>>(t.blocks);
        // Self-loops: into the first block holding their vertex, else a block of their own vertex.
        for (Edge e : es) {
            if (e.vertex1 != e.vertex2 || !idx.containsKey(e.vertex1)) continue;
            List<Edge> home = null;
            for (List<Edge> set : edgeSets) {
                if (touches(set, e.vertex1)) { home = set; break; }
            }
            if (home == null) {
                home = new ArrayList<Edge>();
                edgeSets.add(home);
            }
            home.add(e);
        }
        List<Block> out = new ArrayList<Block>();
        Set<Vertex> covered = new HashSet<Vertex>();
        for (List<Edge> set : edgeSets) {
            List<Vertex> verts = new ArrayList<Vertex>();
            for (Vertex v : vs) if (touches(set, v)) verts.add(v);
            covered.addAll(verts);
            out.add(new Block(verts, set));
        }
        for (Vertex v : vs) {
            if (!covered.contains(v)) out.add(new Block(Arrays.asList(v), new ArrayList<Edge>()));
        }
        // Blocks come out ordered by their first vertex (stable: ties keep DFS order).
        final Map<Vertex, Integer> order = idx;
        Collections.sort(out, new java.util.Comparator<Block>() {
            public int compare(Block x, Block y) {
                return order.get(x.vertices.get(0)) - order.get(y.vertices.get(0));
            }
        });
        return out;
    }

    /** Connected with no cutpoint: a vertex lying in two blocks is a cutpoint. */
    public static boolean isNonseparable(List<Vertex> vs, List<Edge> es) {
        if (!Connectivity.isConnected(vs, es)) return false;
        Set<Vertex> seen = new HashSet<Vertex>();
        for (Block b : blocks(vs, es)) {
            for (Vertex v : b.vertices) if (!seen.add(v)) return false;
        }
        return true;
    }

    private static boolean touches(List<Edge> set, Vertex v) {
        for (Edge e : set) if (e.vertex1 == v || e.vertex2 == v) return true;
        return false;
    }

    /** Tarjan's DFS over edges (not neighbour vertices), skipping the parent edge, not the parent vertex. */
    private static final class Tarjan {
        final Set<Edge> bridges = new HashSet<Edge>();
        final List<List<Edge>> blocks = new ArrayList<List<Edge>>();
        private final List<Edge> es;
        private final List<List<int[]>> inc = new ArrayList<List<int[]>>();   // {edgeIndex, otherVertex}
        private final int[] disc, low;
        private final List<Edge> stack = new ArrayList<Edge>();
        private int timer = 0;

        Tarjan(List<Vertex> vs, List<Edge> es) {
            this.es = es;
            Map<Vertex, Integer> idx = GraphMatrices.index(vs);
            for (int i = 0; i < vs.size(); i++) inc.add(new ArrayList<int[]>());
            for (int k = 0; k < es.size(); k++) {
                Integer i = idx.get(es.get(k).vertex1), j = idx.get(es.get(k).vertex2);
                if (i == null || j == null || i.equals(j)) continue;
                inc.get(i).add(new int[] {k, j});
                inc.get(j).add(new int[] {k, i});
            }
            disc = new int[vs.size()];
            low = new int[vs.size()];
            Arrays.fill(disc, -1);
            for (int i = 0; i < vs.size(); i++) if (disc[i] < 0) dfs(i, -1);
        }

        private void dfs(int u, int parentEdge) {
            disc[u] = low[u] = timer++;
            for (int[] st : inc.get(u)) {
                int k = st[0], w = st[1];
                if (k == parentEdge) continue;
                if (disc[w] < 0) {
                    stack.add(es.get(k));
                    dfs(w, k);
                    low[u] = Math.min(low[u], low[w]);
                    if (low[w] > disc[u]) bridges.add(es.get(k));
                    if (low[w] >= disc[u]) {
                        List<Edge> block = new ArrayList<Edge>();
                        Edge top;
                        do {
                            top = stack.remove(stack.size() - 1);
                            block.add(0, top);
                        } while (top != es.get(k));
                        blocks.add(block);
                    }
                } else if (disc[w] < disc[u]) {
                    stack.add(es.get(k));
                    low[u] = Math.min(low[u], disc[w]);
                }
            }
        }
    }
}
