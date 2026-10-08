package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Chromatic number (CONTEXT.md, Proper colouring), ignoring direction. */
public final class Colouring {

    /** Exact search gives up above this many vertices. */
    public static final int CAP = 15;
    public static final int NO_PROPER_COLOURING = -1;
    public static final int TOO_LARGE = -2;

    private Colouring() {}

    public static int chromaticNumber(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n == 0) return 0;
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (i.equals(j)) return NO_PROPER_COLOURING;
        }
        if (n > CAP) return TOO_LARGE;
        if (es.isEmpty()) return 1;
        boolean[][] adj = new boolean[n][n];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            adj[i][j] = adj[j][i] = true;
        }
        int[] colour = new int[n];
        for (int k = 1; k < n; k++) {
            Arrays.fill(colour, -1);
            if (canColour(0, k, colour, adj)) return k;
        }
        return n;
    }

    private static boolean canColour(int v, int k, int[] colour, boolean[][] adj) {
        if (v == colour.length) return true;
        for (int c = 0; c < k; c++) {
            boolean clash = false;
            for (int u = 0; u < v; u++) {
                if (adj[v][u] && colour[u] == c) { clash = true; break; }
            }
            if (clash) continue;
            colour[v] = c;
            if (canColour(v + 1, k, colour, adj)) return true;
            colour[v] = -1;
        }
        return false;
    }
}
