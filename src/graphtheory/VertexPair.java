/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

/**
 *
 * @author mk
 */
public class VertexPair {

    public Vertex vertex1;
    public Vertex vertex2;
    public Vector<Vector<Vertex>> pathList;     //all paths
    public Vector<Vector<Vector<Vertex>>> VertexDisjointContainer = new Vector<Vector<Vector<Vertex>>>(); // container of vertex-disjoint sets

    public VertexPair(Vertex v1, Vertex v2) {
        vertex1 = v1;
        vertex2 = v2;
    }


    public int getShortestDistance() {
        Vector<Vertex> visitedNodes = new Vector<Vertex>();
        visitedNodes.add(vertex1);

        int counter = 0;
        while (!visitedNodes.contains(vertex2)) {
            int workingSize = visitedNodes.size();
            for (int i = counter; i < workingSize; i++) {
                Vertex cur = visitedNodes.get(i);
                for (Vertex x : cur.undirectedNeighbors) {
                    if (!visitedNodes.contains(x)) visitedNodes.add(x);
                }
                for (Vertex x : cur.outNeighbors) {
                    if (!visitedNodes.contains(x)) visitedNodes.add(x);
                }
            }
            counter++;
            if (workingSize == visitedNodes.size()) return -1;
        }
        return counter;
    }

    /** Returns the actual vertex sequence of the shortest (geodesic) path, or null if unreachable. */
    public Vector<Vertex> getShortestPath() {
        if (vertex1 == vertex2) {
            Vector<Vertex> p = new Vector<Vertex>();
            p.add(vertex1);
            return p;
        }
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        Queue<Vertex> queue = new LinkedList<Vertex>();
        queue.add(vertex1);
        parent.put(vertex1, null);
        while (!queue.isEmpty()) {
            Vertex cur = queue.poll();
            if (cur == vertex2) break;
            for (Vertex n : cur.undirectedNeighbors) {
                if (!parent.containsKey(n)) { parent.put(n, cur); queue.add(n); }
            }
            for (Vertex n : cur.outNeighbors) {
                if (!parent.containsKey(n)) { parent.put(n, cur); queue.add(n); }
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
     * separately (ADR 0002).
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
            if(!isAlreadyContained(tempPathList))
            VertexDisjointContainer.add(tempPathList);
        }
        
    }
    public boolean isAlreadyContained(Vector<Vector<Vertex>> c){

        for(Vector<Vector<Vertex>> d:VertexDisjointContainer){
                if(d.containsAll(c))
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

        //  System.out.println("Vertex-Disjoint Paths for " + vertex1.name + "-" + vertex2.name);

        pathList.removeAllElements();
        visitedNodes.add(vertex1);
        recursePaths(vertex1, visitedNodes);

    }

    public void recursePaths(Vertex v, Vector<Vertex> visitedNodes) {
        if (visitedNodes.contains(vertex2)) {
            Vector<Vertex> Path = new Vector<Vertex>();
            Path.setSize(visitedNodes.size());
            Collections.copy(Path, visitedNodes);
            // Parallel edges can reach the same vertex twice; the width is
            // vertex-based, so keep each vertex sequence once (ADR 0002).
            if (pathList.contains(Path)) return;
            pathList.add(Path);
            for (Vertex a : Path) {
                System.out.print("-" + a.name);
            }
            System.out.println();
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
    // public void

    public class Paths {

        public Vector<Vertex> Path = new Vector<Vertex>();
    }
}
