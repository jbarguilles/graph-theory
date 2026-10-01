package graphtheory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Searches for Euler trails/tours and Hamiltonian paths/cycles.
 * Each search returns the Walk it found, or null if none exists (or the
 * graph is over the size cap). Arcs are only traversed source -> destination.
 * See CONTEXT.md and docs/adr/0003-traversal-checks-respect-direction.md.
 */
public class Traversals {

    /** Mixed graphs need exponential search for Euler; above this many edges we don't try. */
    public static final int EULER_MIXED_EDGE_CAP = 30;
    /** Hamiltonian search is exponential; above this many vertices we don't try. */
    public static final int HAMILTON_VERTEX_CAP = 20;

    // ---------- Euler ----------

    /** True if the graph is mixed and has more edges than the Euler search will try. */
    public static boolean eulerTooLarge(Vector<Edge> eList) {
        return isMixed(eList) && eList.size() > EULER_MIXED_EDGE_CAP;
    }

    /** A trail using every edge exactly once (open or closed), or null. */
    public static Walk eulerTrail(Vector<Vertex> vList, Vector<Edge> eList) {
        return euler(vList, eList, false);
    }

    /** A closed trail using every edge exactly once, or null. */
    public static Walk eulerTour(Vector<Vertex> vList, Vector<Edge> eList) {
        return euler(vList, eList, true);
    }

    private static Walk euler(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        if (vList.isEmpty()) return null;
        // The trivial walk uses all zero edges, but it is not closed.
        if (eList.isEmpty()) return closed ? null : new Walk(vList.firstElement());
        if (!edgesConnected(eList)) return null;

        if (isMixed(eList)) return eulerMixed(vList, eList, closed);
        Vertex start = eList.firstElement().directed
                ? directedEulerStart(vList, eList, closed)
                : undirectedEulerStart(vList, eList, closed);
        return start == null ? null : hierholzer(start, eList);
    }

    private static boolean isMixed(Vector<Edge> eList) {
        boolean anyDirected = false, anyUndirected = false;
        for (Edge e : eList) {
            if (e.directed) anyDirected = true; else anyUndirected = true;
        }
        return anyDirected && anyUndirected;
    }

    /** Number of edge-ends at each vertex, ignoring direction (a self-loop counts 2). */
    private static Map<Vertex, Integer> endCounts(Vector<Edge> eList) {
        Map<Vertex, Integer> ends = new HashMap<Vertex, Integer>();
        for (Edge e : eList) {
            add(ends, e.vertex1, 1);
            add(ends, e.vertex2, 1);
        }
        return ends;
    }

    /** Vertices with an odd number of edge-ends, in vList order. */
    private static List<Vertex> oddVertices(Vector<Vertex> vList, Vector<Edge> eList) {
        Map<Vertex, Integer> ends = endCounts(eList);
        List<Vertex> odd = new Vector<Vertex>();
        for (Vertex v : vList) {
            if (get(ends, v) % 2 != 0) odd.add(v);
        }
        return odd;
    }

    /** Undirected graph: all degrees even (any start), or exactly two odd (start at one) for an open trail. */
    private static Vertex undirectedEulerStart(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        List<Vertex> odd = oddVertices(vList, eList);
        if (odd.isEmpty()) return eList.firstElement().vertex1;
        if (!closed && odd.size() == 2) return odd.get(0);
        return null;
    }

    /** Directed graph: out = in everywhere, or (open trail only) one vertex +1 (the start) and one -1. */
    private static Vertex directedEulerStart(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        Map<Vertex, Integer> net = new HashMap<Vertex, Integer>();   // out-degree - in-degree
        for (Edge e : eList) {
            add(net, e.vertex1, 1);
            add(net, e.vertex2, -1);
        }
        Vertex plus = null;
        int plusCount = 0, minusCount = 0;
        for (Vertex v : vList) {
            int n = get(net, v);
            if (n == 0) continue;
            if (n == 1) { plus = v; plusCount++; }
            else if (n == -1) minusCount++;
            else return null;
        }
        if (plusCount == 0 && minusCount == 0) return eList.firstElement().vertex1;
        if (!closed && plusCount == 1 && minusCount == 1) return plus;
        return null;
    }

    /**
     * Hierholzer's algorithm from 'start'. Assumes the degree conditions hold
     * and the graph is purely undirected or purely directed.
     */
    private static Walk hierholzer(Vertex start, Vector<Edge> eList) {
        boolean[] used = new boolean[eList.size()];
        Vector<Vertex> vStack = new Vector<Vertex>();
        Vector<Edge> eStack = new Vector<Edge>();   // eStack[i] = edge used to reach vStack[i]; null for start
        LinkedList<Vertex> vOut = new LinkedList<Vertex>();
        LinkedList<Edge> eOut = new LinkedList<Edge>();

        vStack.add(start);
        eStack.add(null);
        while (!vStack.isEmpty()) {
            Vertex v = vStack.lastElement();
            int i = nextUnusedEdge(v, eList, used);
            if (i >= 0) {
                used[i] = true;
                vStack.add(Walk.otherEnd(eList.get(i), v));
                eStack.add(eList.get(i));
            } else {
                vOut.addFirst(vStack.remove(vStack.size() - 1));
                Edge e = eStack.remove(eStack.size() - 1);
                if (e != null) eOut.addFirst(e);
            }
        }
        if (eOut.size() != eList.size()) return null;

        Walk w = new Walk(vOut.getFirst());
        for (Edge e : eOut) w.extend(e);
        return w;
    }

    private static int nextUnusedEdge(Vertex from, Vector<Edge> eList, boolean[] used) {
        for (int i = 0; i < eList.size(); i++) {
            if (!used[i] && Walk.canTraverse(eList.get(i), from)) return i;
        }
        return -1;
    }

    /** Mixed graph: backtracking over edges, after a parity check that ignores direction. */
    private static Walk eulerMixed(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        if (eList.size() > EULER_MIXED_EDGE_CAP) return null;

        List<Vertex> odd = oddVertices(vList, eList);
        List<Vertex> starts;
        if (closed) {
            if (!odd.isEmpty()) return null;
            starts = new Vector<Vertex>();
            starts.add(eList.firstElement().vertex1);   // a tour passes every edge's ends
        } else if (odd.size() == 2) {
            starts = odd;
        } else if (odd.isEmpty()) {
            Map<Vertex, Integer> ends = endCounts(eList);
            starts = new Vector<Vertex>();
            for (Vertex v : vList) {
                if (get(ends, v) > 0) starts.add(v);
            }
        } else {
            return null;
        }

        for (Vertex s : starts) {
            Walk w = new Walk(s);
            if (extendEuler(w, eList, new boolean[eList.size()], eList.size(), closed)) return w;
        }
        return null;
    }

    private static boolean extendEuler(Walk w, Vector<Edge> eList, boolean[] used,
                                       int remaining, boolean closed) {
        if (remaining == 0) return !closed || w.end() == w.start();
        Vertex at = w.end();
        for (int i = 0; i < eList.size(); i++) {
            Edge e = eList.get(i);
            if (used[i] || !Walk.canTraverse(e, at)) continue;
            used[i] = true;
            w.extend(e);
            if (extendEuler(w, eList, used, remaining - 1, closed)) return true;
            w.undo();
            used[i] = false;
        }
        return false;
    }

    /** True if every edge lies in one connected piece (direction ignored). */
    private static boolean edgesConnected(Vector<Edge> eList) {
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        for (Edge e : eList) union(parent, e.vertex1, e.vertex2);
        Vertex root = find(parent, eList.firstElement().vertex1);
        for (Edge e : eList) {
            if (find(parent, e.vertex1) != root) return false;
        }
        return true;
    }

    private static Vertex find(Map<Vertex, Vertex> parent, Vertex v) {
        Vertex p = parent.get(v);
        if (p == null) { parent.put(v, v); return v; }
        if (p == v) return v;
        Vertex root = find(parent, p);
        parent.put(v, root);
        return root;
    }

    private static void union(Map<Vertex, Vertex> parent, Vertex x, Vertex y) {
        Vertex rx = find(parent, x), ry = find(parent, y);
        if (rx != ry) parent.put(rx, ry);
    }

    private static int get(Map<Vertex, Integer> m, Vertex v) {
        Integer n = m.get(v);
        return n == null ? 0 : n;
    }

    private static void add(Map<Vertex, Integer> m, Vertex v, int delta) {
        m.put(v, get(m, v) + delta);
    }
}
