package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Properties tab's matrices (CONTEXT.md: Adjacency matrix, Distance, Weighted distance). */
public final class GraphMatrices {

    /** A distance entry for a pair with no walk between them. */
    public static final int UNREACHABLE = -1;

    private GraphMatrices() {}

    /** Entry (u, v) counts the edges along which you can leave u and arrive at v; an undirected loop counts 2. */
    public static int[][] adjacency(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = index(vs);
        int[][] m = new int[vs.size()][vs.size()];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (e.directed) {
                m[i][j]++;
            } else if (i.equals(j)) {
                m[i][i] += 2;
            } else {
                m[i][j]++;
                m[j][i]++;
            }
        }
        return m;
    }

    /** Entry (u, v) = the fewest edges on a walk from u to v (respecting direction), or UNREACHABLE. */
    public static int[][] distances(List<Vertex> vs, List<Edge> es) {
        List<List<int[]>> steps = steps(vs, es);
        int n = vs.size();
        int[][] d = new int[n][];
        for (int s = 0; s < n; s++) {
            int[] row = new int[n];
            Arrays.fill(row, UNREACHABLE);
            row[s] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int[] st : steps.get(u)) {
                    if (row[st[0]] == UNREACHABLE) {
                        row[st[0]] = row[u] + 1;
                        queue.add(st[0]);
                    }
                }
            }
            d[s] = row;
        }
        return d;
    }

    /** Entry (u, v) = the smallest weight of a walk from u to v (respecting direction), or UNREACHABLE. */
    public static int[][] weightedDistances(List<Vertex> vs, List<Edge> es) {
        List<List<int[]>> steps = steps(vs, es);
        int n = vs.size();
        int[][] d = new int[n][];
        for (int s = 0; s < n; s++) {
            int[] row = new int[n];
            Arrays.fill(row, UNREACHABLE);
            boolean[] done = new boolean[n];
            row[s] = 0;
            for (int round = 0; round < n; round++) {
                int u = -1;
                for (int i = 0; i < n; i++) {
                    if (!done[i] && row[i] != UNREACHABLE && (u < 0 || row[i] < row[u])) u = i;
                }
                if (u < 0) break;
                done[u] = true;
                for (int[] st : steps.get(u)) {
                    int nd = row[u] + st[1];
                    if (row[st[0]] == UNREACHABLE || nd < row[st[0]]) row[st[0]] = nd;
                }
            }
            d[s] = row;
        }
        return d;
    }

    /** For each vertex index, the steps {to, weight} a walk can take from it. */
    private static List<List<int[]>> steps(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = index(vs);
        List<List<int[]>> steps = new ArrayList<List<int[]>>();
        for (int i = 0; i < vs.size(); i++) steps.add(new ArrayList<int[]>());
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            steps.get(i).add(new int[] {j, e.weight});
            if (!e.directed) steps.get(j).add(new int[] {i, e.weight});
        }
        return steps;
    }

    static Map<Vertex, Integer> index(List<Vertex> vs) {
        Map<Vertex, Integer> idx = new HashMap<Vertex, Integer>();
        for (int i = 0; i < vs.size(); i++) idx.put(vs.get(i), i);
        return idx;
    }
}
