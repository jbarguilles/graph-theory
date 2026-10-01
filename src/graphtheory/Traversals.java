package graphtheory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Searches for Euler trails/tours and Hamiltonian paths/cycles.
 * Each search returns the Walk it found, or null if none exists (or, for
 * Hamiltonian search, the graph is over the size cap). Arcs are only
 * traversed source -> destination.
 * See CONTEXT.md and docs/adr/0003-traversal-checks-respect-direction.md.
 */
public class Traversals {

    /** Hamiltonian search is exponential; above this many vertices we don't try. */
    public static final int HAMILTON_VERTEX_CAP = 20;

    // ---------- Euler ----------

    /**
     * A trail using every edge exactly once (open or closed).
     * Exact for any size; returns null only when none exists.
     */
    public static Walk eulerTrail(Vector<Vertex> vList, Vector<Edge> eList) {
        return euler(vList, eList, false);
    }

    /**
     * A closed trail using every edge exactly once.
     * Exact for any size; returns null only when none exists.
     */
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
        return start == null ? null : hierholzer(start, eList, arcTails(eList));
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

    /**
     * vList, followed by any edge endpoints missing from it (in edge order).
     * Counting every endpoint keeps the degree tests honest; the fixed order
     * keeps the chosen start deterministic.
     */
    private static Set<Vertex> allVertices(Vector<Vertex> vList, Vector<Edge> eList) {
        Set<Vertex> all = new LinkedHashSet<Vertex>(vList);
        for (Edge e : eList) {
            all.add(e.vertex1);
            all.add(e.vertex2);
        }
        return all;
    }

    /** Vertices with an odd number of edge-ends, in allVertices order. */
    private static List<Vertex> oddVertices(Vector<Vertex> vList, Vector<Edge> eList) {
        Map<Vertex, Integer> ends = endCounts(eList);
        List<Vertex> odd = new Vector<Vertex>();
        for (Vertex v : allVertices(vList, eList)) {
            if (get(ends, v) % 2 != 0) odd.add(v);
        }
        return odd;
    }

    /**
     * Undirected graph: all degrees even, or exactly two odd (start at one) for an open trail.
     * With all degrees even, one start suffices: a closed trail can be rotated to start anywhere on it.
     */
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
        for (Vertex v : allVertices(vList, eList)) {
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
     * Hierholzer's algorithm from 'start'. tail[i] is the only vertex edge i
     * may be left from, or null if it may be left from either end. Assumes
     * the degree conditions hold for those directions.
     */
    private static Walk hierholzer(Vertex start, Vector<Edge> eList, Vertex[] tail) {
        boolean[] used = new boolean[eList.size()];
        Vector<Vertex> vStack = new Vector<Vertex>();
        Vector<Edge> eStack = new Vector<Edge>();   // eStack[i] = edge used to reach vStack[i]; null for start
        LinkedList<Vertex> vOut = new LinkedList<Vertex>();
        LinkedList<Edge> eOut = new LinkedList<Edge>();

        vStack.add(start);
        eStack.add(null);
        while (!vStack.isEmpty()) {
            Vertex v = vStack.lastElement();
            int i = nextUnusedEdge(v, eList, tail, used);
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

        // Walk.extend re-checks the real direction rules.
        Walk w = new Walk(vOut.getFirst());
        for (Edge e : eOut) {
            if (!w.extend(e)) return null;
        }
        return w;
    }

    private static int nextUnusedEdge(Vertex from, Vector<Edge> eList, Vertex[] tail, boolean[] used) {
        for (int i = 0; i < eList.size(); i++) {
            if (used[i]) continue;
            Edge e = eList.get(i);
            boolean canLeave = tail[i] == null ? (e.vertex1 == from || e.vertex2 == from) : tail[i] == from;
            if (canLeave) return i;
        }
        return -1;
    }

    /** Arcs may only be left from their source; undirected edges from either end (null). */
    private static Vertex[] arcTails(Vector<Edge> eList) {
        Vertex[] tail = new Vertex[eList.size()];
        for (int i = 0; i < eList.size(); i++) {
            Edge e = eList.get(i);
            tail[i] = e.directed ? e.vertex1 : null;
        }
        return tail;
    }

    /**
     * Mixed graph (connected). An open trail must start and end at the two
     * vertices with an odd number of edge-ends, so we reduce it to a tour.
     */
    private static Walk eulerMixed(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        List<Vertex> odd = oddVertices(vList, eList);
        // With every count even an open Euler trail is impossible, so only a closed one can exist.
        if (odd.isEmpty()) return mixedTour(eList);
        if (closed || odd.size() != 2) return null;

        // Add a virtual edge x-y. An Euler trail between x and y (either way
        // round) plus the virtual edge is exactly an Euler tour of the bigger graph.
        Vector<Edge> withVirtual = new Vector<Edge>(eList);
        Edge virtual = new Edge(odd.get(0), odd.get(1), false);
        withVirtual.add(virtual);
        Walk tour = mixedTour(withVirtual);
        if (tour == null) return null;

        // Cut the tour at the virtual edge: start just after it, go round
        // the closed tour, and stop just before it.
        List<Edge> es = tour.edges();
        int cut = es.indexOf(virtual);
        Walk trail = new Walk(tour.vertices().get(cut + 1));
        for (int k = 1; k < es.size(); k++) {
            if (!trail.extend(es.get((cut + k) % es.size()))) return null;
        }
        return trail;
    }

    /**
     * Euler tour of a connected mixed graph, or null.
     *
     * A tour leaves each vertex as often as it enters it, so a tour exists
     * exactly when the undirected edges can be given directions that make
     * every vertex balanced (out = in). The graph is then a balanced directed
     * graph and Hierholzer finds the tour.
     *
     * Balanced means v is left on half of its edge-ends. Each self-loop at v
     * and each arc out of v already accounts for one of those, so v must be
     * left on exactly need(v) of its undirected edges. Choosing, for every
     * undirected edge, the end it is left from so that each v gets need(v)
     * edges is a matching problem, solved with augmenting paths
     * (a unit-capacity max-flow).
     */
    private static Walk mixedTour(Vector<Edge> eList) {
        Map<Vertex, Integer> ends = endCounts(eList);
        Map<Vertex, Integer> need = new HashMap<Vertex, Integer>();
        for (Vertex v : ends.keySet()) {
            if (get(ends, v) % 2 != 0) return null;
            need.put(v, get(ends, v) / 2);
        }
        Vertex[] tail = new Vertex[eList.size()];
        for (int i = 0; i < eList.size(); i++) {
            Edge e = eList.get(i);
            if (isLoop(e) || e.directed) {
                tail[i] = e.vertex1;          // self-loop: left once from v; arc: left from its source
                add(need, tail[i], -1);
            }
        }
        for (Vertex v : need.keySet()) {
            if (get(need, v) < 0) return null;
        }

        // Give each undirected edge the end it will be left from.
        Map<Vertex, Integer> load = new HashMap<Vertex, Integer>();   // undirected edges assigned to v
        for (int i = 0; i < eList.size(); i++) {
            if (tail[i] != null) continue;
            if (!assign(i, eList, tail, need, load, new HashSet<Vertex>())) return null;
        }
        // The need(v) add up to the number of undirected edges, so now every v has exactly need(v).
        return hierholzer(eList.firstElement().vertex1, eList, tail);
    }

    /**
     * Tries to give undirected edge i a tail (augmenting path search).
     * An end with a free slot takes it directly. A full end can still take it
     * if one of its undirected edges can move to its own other end, recursively.
     * 'tried' stops the search visiting a vertex twice.
     */
    private static boolean assign(int i, Vector<Edge> eList, Vertex[] tail,
                                  Map<Vertex, Integer> need, Map<Vertex, Integer> load,
                                  Set<Vertex> tried) {
        Edge e = eList.get(i);
        Vertex[] endsOfE = { e.vertex1, e.vertex2 };
        for (Vertex u : endsOfE) {
            if (!tried.add(u)) continue;
            if (get(load, u) < get(need, u)) {
                setTail(i, u, tail, load);
                return true;
            }
            for (int j = 0; j < eList.size(); j++) {
                Edge f = eList.get(j);
                if (j == i || tail[j] != u || f.directed || isLoop(f)) continue;
                // Move edge j off u (to its other end), freeing u's slot for edge i.
                if (assign(j, eList, tail, need, load, tried)) {
                    setTail(i, u, tail, load);
                    return true;
                }
            }
        }
        return false;
    }

    /** Assigns undirected edge i to leave from u, releasing its old end if it had one. */
    private static void setTail(int i, Vertex u, Vertex[] tail, Map<Vertex, Integer> load) {
        if (tail[i] != null) add(load, tail[i], -1);
        tail[i] = u;
        add(load, u, 1);
    }

    private static boolean isLoop(Edge e) {
        return e.vertex1 == e.vertex2;
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
