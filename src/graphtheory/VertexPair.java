/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

/**
 *
 * @author mk
 */
public class VertexPair {

    public Vertex vertex1;
    public Vertex vertex2;
    public Vector<Vector<Vertex>> pathList;     //all paths
    public Vector<Vector<Vector<Vertex>>> VertexDisjointContainer =
            new Vector<Vector<Vector<Vertex>>>();

    public VertexPair(Vertex v1, Vertex v2) {
        vertex1 = v1;
        vertex2 = v2;
    }

    /**
     * Weighted shortest distance using Dijkstra.
     * Returns -1 if unreachable. If all weights are 1, this equals the
     * BFS hop count.
     */
    public int getShortestDistance() {
        if (vertex1 == vertex2) return 0;

        final Map<Vertex, Integer> dist = new HashMap<Vertex, Integer>();
        PriorityQueue<Vertex> pq = new PriorityQueue<Vertex>(11,
                (a, b) -> Integer.compare(
                        dist.getOrDefault(a, Integer.MAX_VALUE),
                        dist.getOrDefault(b, Integer.MAX_VALUE)));

        dist.put(vertex1, 0);
        pq.add(vertex1);

        while (!pq.isEmpty()) {
            Vertex u = pq.poll();
            if (u == vertex2) return dist.get(u);
            int du = dist.get(u);

            for (Vertex v : allNeighbors(u)) {
                int w = EdgeRegistry.weightOf(u, v);
                if (w < 0) continue;                       // no edge in this direction
                int nd = du + w;
                if (nd < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, nd);
                    pq.add(v);
                }
            }
        }
        return -1;
    }

    /**
     * Weighted geodesic path via Dijkstra with parent pointers.
     * Returns null if unreachable.
     */
    public Vector<Vertex> getShortestPath() {
        if (vertex1 == vertex2) {
            Vector<Vertex> p = new Vector<Vertex>();
            p.add(vertex1);
            return p;
        }

        final Map<Vertex, Integer> dist = new HashMap<Vertex, Integer>();
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        PriorityQueue<Vertex> pq = new PriorityQueue<Vertex>(11,
                (a, b) -> Integer.compare(
                        dist.getOrDefault(a, Integer.MAX_VALUE),
                        dist.getOrDefault(b, Integer.MAX_VALUE)));

        dist.put(vertex1, 0);
        pq.add(vertex1);

        while (!pq.isEmpty()) {
            Vertex u = pq.poll();
            if (u == vertex2) break;
            int du = dist.get(u);

            for (Vertex v : allNeighbors(u)) {
                int w = EdgeRegistry.weightOf(u, v);
                if (w < 0) continue;
                int nd = du + w;
                if (nd < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, nd);
                    parent.put(v, u);
                    pq.add(v);
                }
            }
        }

        if (!parent.containsKey(vertex2)) return null;
        Vector<Vertex> path = new Vector<Vertex>();
        for (Vertex cur = vertex2; cur != null; cur = parent.get(cur)) {
            path.insertElementAt(cur, 0);
        }
        return path;
    }

    /**
     * All paths from vertex1 to vertex2 as edge-aware walks, shortest first.
     * Paths with the same vertices but different parallel edges are listed
     * separately (ADR 0002). If vertex1 == vertex2 the result is the single
     * trivial walk.
     */
    public Vector<Walk> generateEdgePaths(Vector<Edge> eList) {
        Vector<Walk> result = new Vector<Walk>();
        recurseEdgePaths(new Walk(vertex1), eList, result);
        Collections.sort(result, new Comparator<Walk>() {
            public int compare(Walk p, Walk q) {
                return p.length() - q.length();
            }
        });
        return result;
    }

    private void recurseEdgePaths(Walk w, Vector<Edge> eList, Vector<Walk> result) {
        if (w.end() == vertex2) {
            result.add(w.copy());
            return;
        }
        for (Edge e : eList) {
            if (!Walk.canTraverse(e, w.end())) continue;
            if (w.visits(Walk.otherEnd(e, w.end()))) continue;
            w.extend(e);
            recurseEdgePaths(w, eList, result);
            w.undo();
        }
    }

    /** All neighbors of u reachable via one edge (out + undirected). */
    private List<Vertex> allNeighbors(Vertex u) {
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex v : u.outNeighbors)        if (!result.contains(v)) result.add(v);
        for (Vertex v : u.undirectedNeighbors) if (!result.contains(v)) result.add(v);
        return result;
    }

    public void generateVertexDisjointPaths() {
        VertexDisjointContainer.removeAllElements();
        generatePaths();
        Vector<Vector<Vertex>> tempPathList;

        for (int i = 0; i < pathList.size(); i++) {
            tempPathList = new Vector<Vector<Vertex>>();
            tempPathList.add(pathList.get(i));
            for (int j = 0; j < pathList.size(); j++) {
                if (i != j) {
                    int disjointCount = 0;
                    for (int k = 0; k < tempPathList.size(); k++) {
                        if (areDisjointPaths(pathList.get(j), tempPathList.get(k))) {
                            disjointCount++;
                        }
                    }
                    if (disjointCount == tempPathList.size()) {
                        tempPathList.add(pathList.get(j));
                    }
                }
            }
            if (!isAlreadyContained(tempPathList))
                VertexDisjointContainer.add(tempPathList);
        }
    }

    public boolean isAlreadyContained(Vector<Vector<Vertex>> c) {
        for (Vector<Vector<Vertex>> d : VertexDisjointContainer) {
            if (d.containsAll(c))
                return true;
        }
        return false;
    }

    public boolean areDisjointPaths(Vector<Vertex> path1, Vector<Vertex> path2) {
        List<Vertex> setA = new ArrayList<Vertex>();
        List<Vertex> setB = new ArrayList<Vertex>();

        setA = path1.subList(1, path1.size() - 1);
        setB = path2.subList(1, path2.size() - 1);
        return Collections.disjoint(setA, setB);
    }

    public void generatePaths() {
        pathList = new Vector<Vector<Vertex>>();
        Vector<Vertex> visitedNodes = new Vector<Vertex>();

        pathList.removeAllElements();
        visitedNodes.add(vertex1);
        recursePaths(vertex1, visitedNodes);
    }

    public void recursePaths(Vertex v, Vector<Vertex> visitedNodes) {
        if (visitedNodes.contains(vertex2)) {
            Vector<Vertex> Path = new Vector<Vertex>();
            Path.setSize(visitedNodes.size());
            Collections.copy(Path, visitedNodes);
            // A neighbour can appear in more than one neighbour list (e.g. {u,v} and (u,v));
            // the width is vertex-based, so keep each vertex sequence once (ADR 0002).
            if (pathList.contains(Path)) return;
            pathList.add(Path);
        } else {
            for (Vertex x : v.undirectedNeighbors) {
                if (!visitedNodes.contains(x)) {
                    int origSize = visitedNodes.size();
                    visitedNodes.add(x);
                    recursePaths(x, visitedNodes);
                    visitedNodes.setSize(origSize);
                }
            }
            for (Vertex x : v.outNeighbors) {
                if (!visitedNodes.contains(x)) {
                    int origSize = visitedNodes.size();
                    visitedNodes.add(x);
                    recursePaths(x, visitedNodes);
                    visitedNodes.setSize(origSize);
                }
            }
        }
    }

    public class Paths {
        public Vector<Vertex> Path = new Vector<Vertex>();
    }
}