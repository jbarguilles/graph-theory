package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Matchings (CONTEXT.md), ignoring direction; a self-loop is never in a matching. */
public final class Matchings {

    /** Exact maximum matching gives up above this many vertices. */
    public static final int MAXIMUM_VERTEX_CAP = 20;

    private Matchings() {}

    /** A maximal matching: edges taken greedily in edge order. */
    public static List<Edge> maximal(List<Vertex> vs, List<Edge> es) {
        Set<Vertex> known = new HashSet<Vertex>(vs);
        Set<Vertex> used = new HashSet<Vertex>();
        List<Edge> out = new ArrayList<Edge>();
        for (Edge e : es) {
            if (!known.contains(e.vertex1) || !known.contains(e.vertex2)) continue;
            if (e.vertex1 == e.vertex2 || used.contains(e.vertex1) || used.contains(e.vertex2)) continue;
            out.add(e);
            used.add(e.vertex1);
            used.add(e.vertex2);
        }
        return out;
    }

    /** A maximum matching, or null above MAXIMUM_VERTEX_CAP vertices. */
    public static List<Edge> maximum(List<Vertex> vs, List<Edge> es) {
        final int n = vs.size();
        if (n > MAXIMUM_VERTEX_CAP) return null;
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        final Edge[][] join = new Edge[n][n];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (i.intValue() == j.intValue() || join[i][j] != null) continue;
            join[i][j] = join[j][i] = e;
        }
        final int[] memo = new int[1 << n];
        Arrays.fill(memo, -1);
        int full = (1 << n) - 1;
        best(full, join, memo);
        List<Edge> out = new ArrayList<Edge>();
        int mask = full;
        while (mask != 0) {
            int i = Integer.numberOfTrailingZeros(mask);
            int rest = mask & ~(1 << i);
            if (best(mask, join, memo) == best(rest, join, memo)) {
                mask = rest;
                continue;
            }
            for (int j = 0; j < n; j++) {
                if (join[i][j] == null || (rest & (1 << j)) == 0) continue;
                int after = rest & ~(1 << j);
                if (best(mask, join, memo) == 1 + best(after, join, memo)) {
                    out.add(join[i][j]);
                    mask = after;
                    break;
                }
            }
        }
        return out;
    }

    /** The largest matching among the vertices in mask. */
    private static int best(int mask, Edge[][] join, int[] memo) {
        if (mask == 0) return 0;
        if (memo[mask] >= 0) return memo[mask];
        int i = Integer.numberOfTrailingZeros(mask);
        int rest = mask & ~(1 << i);
        int r = best(rest, join, memo);
        for (int j = 0; j < join.length; j++) {
            if (join[i][j] != null && (rest & (1 << j)) != 0) {
                r = Math.max(r, 1 + best(rest & ~(1 << j), join, memo));
            }
        }
        return memo[mask] = r;
    }

    /**
     * The stable matching found by Gale–Shapley with the side containing 'proposer' proposing
     * (side A if proposer is null or not in the graph). null when the graph is not bipartite or
     * some vertex with a neighbour has no preference list.
     * Assumes preference lists are in step with the edges (PreferenceLists.sync).
     */
    public static List<Edge> stable(List<Vertex> vs, List<Edge> es, Vertex proposer) {
        List<List<Vertex>> sides = Structure.bipartiteSides(vs, es);
        if (sides == null || !PreferenceLists.missing(vs, es).isEmpty()) return null;
        List<Vertex> proposers = sides.get(1).contains(proposer) ? sides.get(1) : sides.get(0);

        Map<Vertex, Vertex> partner = new HashMap<Vertex, Vertex>();
        Map<Vertex, Integer> next = new HashMap<Vertex, Integer>();
        ArrayDeque<Vertex> free = new ArrayDeque<Vertex>();
        for (Vertex p : proposers) {
            next.put(p, 0);
            if (p.preferences != null) free.add(p);
        }
        while (!free.isEmpty()) {
            Vertex p = free.poll();
            int k = next.get(p);
            if (k >= p.preferences.size()) continue;
            next.put(p, k + 1);
            Vertex r = p.preferences.get(k);
            Vertex current = partner.get(r);
            if (current == null) {
                partner.put(r, p);
                partner.put(p, r);
            } else if (r.preferences.indexOf(p) < r.preferences.indexOf(current)) {
                partner.put(r, p);
                partner.put(p, r);
                partner.remove(current);
                free.add(current);
            } else {
                free.add(p);
            }
        }
        List<Edge> out = new ArrayList<Edge>();
        for (Vertex p : proposers) {
            Vertex r = partner.get(p);
            if (r == null) continue;
            for (Edge e : es) {
                if ((e.vertex1 == p && e.vertex2 == r) || (e.vertex1 == r && e.vertex2 == p)) {
                    out.add(e);
                    break;
                }
            }
        }
        return out;
    }
}
