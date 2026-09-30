/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author mk
 */
public class GraphProperties {

    public int[][] adjacencyMatrix;
    public int[][] distanceMatrix;
    public Vector<VertexPair> vpList;

    public int[][] generateAdjacencyMatrix(Vector<Vertex> vList, Vector<Edge> eList) {
        adjacencyMatrix = new int[vList.size()][vList.size()];

        for (Edge e : eList) {
            int i = vList.indexOf(e.vertex1);
            int j = vList.indexOf(e.vertex2);
            if (i < 0 || j < 0) continue;
            adjacencyMatrix[i][j] = 1;
            if (!e.directed) {
                adjacencyMatrix[j][i] = 1;
            }
        }
        return adjacencyMatrix;
    }

    public int[][] generateDistanceMatrix(Vector<Vertex> vList) {
        distanceMatrix = new int[vList.size()][vList.size()];

        for (int a = 0; a < vList.size(); a++)//initialize
        {
            for (int b = 0; b < vList.size(); b++) {
                distanceMatrix[a][b] = 0;
            }
        }

        VertexPair vp;
        int shortestDistance;
        for (int i = 0; i < vList.size(); i++) {
            for (int j = i + 1; j < vList.size(); j++) {
                vp = new VertexPair(vList.get(i), vList.get(j));
                shortestDistance = vp.getShortestDistance();
                distanceMatrix[vList.indexOf(vp.vertex1)][vList.indexOf(vp.vertex2)] = shortestDistance;
                distanceMatrix[vList.indexOf(vp.vertex2)][vList.indexOf(vp.vertex1)] = shortestDistance;
            }
        }
        return distanceMatrix;
    }

    public void displayContainers(Vector<Vertex> vList) {
        vpList = new Vector<VertexPair>();
        int[] kWideGraph = new int[10];
        for (int i = 0; i < kWideGraph.length; i++) {
            kWideGraph[i] = -1;
        }

        VertexPair vp;

        for (int a = 0; a < vList.size(); a++) {    // assign vertex pairs
            for (int b = a + 1; b < vList.size(); b++) {
                vp = new VertexPair(vList.get(a), vList.get(b));
                vpList.add(vp);
                int longestWidth = 0;
                System.out.println(">Vertex Pair " + vList.get(a).name + "-" + vList.get(b).name + "\n All Paths:");
                vp.generateVertexDisjointPaths();
                for (int i = 0; i < vp.VertexDisjointContainer.size(); i++) {//for every container of the vertex pair
                    int width = vp.VertexDisjointContainer.get(i).size();
                    Collections.sort(vp.VertexDisjointContainer.get(i), new descendingWidthComparator());
                    int longestLength = vp.VertexDisjointContainer.get(i).firstElement().size();
                    longestWidth = Math.max(longestWidth, width);
                    System.out.println("\tContainer " + i + " - " + "Width=" + width + " - Length=" + longestLength);

                    for (int j = 0; j < vp.VertexDisjointContainer.get(i).size(); j++) //for every path in the container
                    {
                        System.out.print("\t\tPath " + j + "\n\t\t\t");
                        for (int k = 0; k < vp.VertexDisjointContainer.get(i).get(j).size(); k++) {
                            System.out.print("-" + vp.VertexDisjointContainer.get(i).get(j).get(k).name);
                        }
                        System.out.println();
                    }

                }
                //d-wide for vertexPair
                for (int k = 1; k <= longestWidth; k++) { // 1-wide, 2-wide, 3-wide...
                    int minLength = 999;
                    for (int m = 0; m < vp.VertexDisjointContainer.size(); m++) // for each container with k-wide select shortest length
                    {
                        minLength = Math.min(minLength, vp.VertexDisjointContainer.get(m).size());
                    }
                    if (minLength != 999) {
                        System.out.println(k + "-wide for vertexpair(" + vp.vertex1.name + "-" + vp.vertex2.name + ")=" + minLength);
                        kWideGraph[k] = Math.max(kWideGraph[k], minLength);
                    }
                }
            }
        }

        for (int i = 0; i < kWideGraph.length; i++) {
            if (kWideGraph[i] != -1) {
                System.out.println("D" + i + "(G)=" + kWideGraph[i]);
            }
        }
    }

    public void drawAdjacencyMatrix(Graphics g, Vector<Vertex> vList, int x, int y) {
        int cSize = 20;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y-30, vList.size() * cSize+cSize, vList.size() * cSize+cSize);
        g.setColor(Color.black);
        g.drawString("AdjacencyMatrix", x, y - cSize);
        for (int i = 0; i < vList.size(); i++) {
            g.setColor(Color.RED);
            g.drawString(vList.get(i).name, x + cSize + i * cSize, y);
            g.drawString(vList.get(i).name, x, cSize + i * cSize + y);
            g.setColor(Color.black);
            for (int j = 0; j < vList.size(); j++) {
                g.drawString("" + adjacencyMatrix[i][j], x + cSize * (j + 1), y + cSize * (i + 1));
            }
        }
    }

    public void drawDistanceMatrix(Graphics g, Vector<Vertex> vList, int x, int y) {
        int cSize = 20;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y-30, vList.size() * cSize+cSize, vList.size() * cSize+cSize);
        g.setColor(Color.black);
        g.drawString("ShortestPathMatrix", x, y - cSize);
        for (int i = 0; i < vList.size(); i++) {
            g.setColor(Color.RED);
            g.drawString(vList.get(i).name, x + cSize + i * cSize, y);
            g.drawString(vList.get(i).name, x, cSize + i * cSize + y);
            g.setColor(Color.black);
            for (int j = 0; j < vList.size(); j++) {
                g.drawString("" + distanceMatrix[i][j], x + cSize * (j + 1), y + cSize * (i + 1));
            }
        }
    }

    public void drawNodePropertiesTable(Graphics g, Vector<Vertex> vList, int x, int y) {
        int rowH = 18;
        int colW = 65;
        String[] headers = {"Name", "Deg", "In", "Out", "Isolated", "Cut", "Root"};

        g.setColor(new Color(230, 230, 230));
        g.fillRect(x, y - 20, headers.length * colW, (vList.size() + 2) * rowH + 10);
        g.setColor(Color.BLACK);
        g.drawString("Node Properties", x, y - 5);

        for (int i = 0; i < headers.length; i++) {
            g.setColor(new Color(80, 80, 80));
            g.drawString(headers[i], x + i * colW + 3, y + rowH);
        }

        for (int r = 0; r < vList.size(); r++) {
            Vertex v = vList.get(r);
            int ry = y + (r + 2) * rowH;
            g.setColor(Color.BLACK);
            g.drawString(v.name,               x + 0 * colW + 3, ry);
            g.drawString("" + v.degree(),      x + 1 * colW + 3, ry);
            g.drawString("" + v.inDegree(),    x + 2 * colW + 3, ry);
            g.drawString("" + v.outDegree(),   x + 3 * colW + 3, ry);
            g.drawString("" + v.isIsolated(),  x + 4 * colW + 3, ry);
            g.drawString("" + v.isCutpoint,    x + 5 * colW + 3, ry);
            g.drawString("" + v.isRoot,        x + 6 * colW + 3, ry);
        }
    }

    // ---- Bridge detection (Tarjan) ----

    public void computeBridges(Vector<Vertex> vList, Vector<Edge> eList) {
        for (Edge e : eList) e.isBridge = false;
        int n = vList.size();
        int[] disc = new int[n];
        int[] low  = new int[n];
        boolean[] visited = new boolean[n];
        int[] timer = {0};
        Arrays.fill(disc, -1);
        for (int i = 0; i < n; i++) {
            if (!visited[i]) dfsBridge(i, -1, vList, eList, disc, low, visited, timer);
        }
    }

    private void dfsBridge(int u, int parentEdge, Vector<Vertex> vList, Vector<Edge> eList,
                           int[] disc, int[] low, boolean[] visited, int[] timer) {
        visited[u] = true;
        disc[u] = low[u] = timer[0]++;
        for (int v : getAllNeighborIndices(u, vList)) {
            if (v == u) continue;
            int eIdx = findEdgeIndex(vList.get(u), vList.get(v), eList);
            if (!visited[v]) {
                dfsBridge(v, eIdx, vList, eList, disc, low, visited, timer);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u] && eIdx >= 0) eList.get(eIdx).isBridge = true;
            } else if (eIdx != parentEdge) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    private int findEdgeIndex(Vertex a, Vertex b, Vector<Edge> eList) {
        for (int i = 0; i < eList.size(); i++) {
            Edge e = eList.get(i);
            if ((e.vertex1 == a && e.vertex2 == b) ||
                (!e.directed && e.vertex1 == b && e.vertex2 == a)) return i;
        }
        return -1;
    }

    // ---- Connectivity (uses all edge types) ----

    private boolean isConnected(Vector<Vertex> vList) {
        if (vList.isEmpty()) return true;
        Vector<Vertex> nonIsolated = new Vector<Vertex>();
        for (Vertex v : vList) { if (!v.isIsolated()) nonIsolated.add(v); }
        if (nonIsolated.isEmpty()) return true;
        Set<Vertex> visited = new HashSet<Vertex>();
        dfsConnected(nonIsolated.firstElement(), visited, vList);
        return visited.size() == nonIsolated.size();
    }

    private void dfsConnected(Vertex v, Set<Vertex> visited, Vector<Vertex> vList) {
        visited.add(v);
        for (Vertex n : v.undirectedNeighbors) { if (!visited.contains(n) && vList.contains(n)) dfsConnected(n, visited, vList); }
        for (Vertex n : v.inNeighbors)         { if (!visited.contains(n) && vList.contains(n)) dfsConnected(n, visited, vList); }
        for (Vertex n : v.outNeighbors)        { if (!visited.contains(n) && vList.contains(n)) dfsConnected(n, visited, vList); }
    }

    // ---- Euler conditions ----

    public boolean hasEulerCircuit(Vector<Vertex> vList) {
        if (!isConnected(vList)) return false;
        for (Vertex v : vList) { if (!v.isIsolated() && v.getDegree() % 2 != 0) return false; }
        return true;
    }

    public boolean hasEulerPath(Vector<Vertex> vList) {
        if (!isConnected(vList)) return false;
        int odd = 0;
        for (Vertex v : vList) { if (!v.isIsolated() && v.getDegree() % 2 != 0) odd++; }
        return odd == 0 || odd == 2;
    }

    // ---- Hamiltonian path / cycle (backtracking, capped at 20 vertices) ----

    public boolean hasHamiltonianPath(Vector<Vertex> vList) {
        if (vList.size() > 20) return false;
        for (int i = 0; i < vList.size(); i++) {
            boolean[] vis = new boolean[vList.size()];
            vis[i] = true;
            if (hamiltonianPathDFS(i, vList, vis, 1)) return true;
        }
        return false;
    }

    private boolean hamiltonianPathDFS(int u, Vector<Vertex> vList, boolean[] vis, int count) {
        if (count == vList.size()) return true;
        for (int v : getAllNeighborIndices(u, vList)) {
            if (!vis[v]) { vis[v] = true; if (hamiltonianPathDFS(v, vList, vis, count + 1)) return true; vis[v] = false; }
        }
        return false;
    }

    public boolean hasHamiltonianCycle(Vector<Vertex> vList) {
        if (vList.size() > 20 || vList.size() < 3) return false;
        for (int i = 0; i < vList.size(); i++) {
            boolean[] vis = new boolean[vList.size()];
            vis[i] = true;
            if (hamiltonianCycleDFS(i, i, vList, vis, 1)) return true;
        }
        return false;
    }

    private boolean hamiltonianCycleDFS(int start, int u, Vector<Vertex> vList, boolean[] vis, int count) {
        if (count == vList.size()) return getAllNeighborIndices(u, vList).contains(start);
        for (int v : getAllNeighborIndices(u, vList)) {
            if (!vis[v]) { vis[v] = true; if (hamiltonianCycleDFS(start, v, vList, vis, count + 1)) return true; vis[v] = false; }
        }
        return false;
    }

    // ---- Graph summary (with Order / Size / Magnitude, and V/E sets) ----

    /**
 * Draws the graph summary box. Returns the total height in pixels,
 * so callers can stack another panel directly below it.
 */
public int drawGraphSummary(Graphics g, Vector<Vertex> vList, Vector<Edge> eList, int x, int y) {
    int bridgeCount = 0;
    for (Edge e : eList) { if (e.isBridge) bridgeCount++; }

    boolean eulerCircuit = hasEulerCircuit(vList);
    boolean eulerPath    = hasEulerPath(vList);
    boolean hamPath      = vList.size() <= 20 && hasHamiltonianPath(vList);
    boolean hamCycle     = vList.size() <= 20 && hasHamiltonianCycle(vList);
    boolean tooLarge     = vList.size() > 20;

    int order     = vList.size();
    int size      = eList.size();
    int magnitude = order + size;

    // ---- V line ----
    StringBuilder vBody = new StringBuilder();
    for (int i = 0; i < vList.size(); i++) {
        if (i > 0) vBody.append(", ");
        vBody.append(vList.get(i).name);
    }
    String vLine = "V = {" + vBody + "}" ;

    // ---- E line, truncated to at most 6 edges ----
    int maxEdgesShown = 6;
    int shown = Math.min(size, maxEdgesShown);
    StringBuilder eBody = new StringBuilder();
    for (int i = 0; i < shown; i++) {
        if (i > 0) eBody.append(", ");
        Edge e = eList.get(i);
        if (e.directed) {
            eBody.append("(").append(e.vertex1.name).append(", ")
                 .append(e.vertex2.name).append(")");
        } else {
            eBody.append("{").append(e.vertex1.name).append(", ")
                 .append(e.vertex2.name).append("}");
        }
    }
    if (size > maxEdgesShown) eBody.append(", ...");
    String eLine = "E = {" + eBody + "}";

    String[] lines = {
        "Graph Summary",
        "Order |V|: " + order,
        "Size |E|: " + size,
        "Magnitude |V|+|E|: " + magnitude,
        "Bridges: " + bridgeCount + (bridgeCount > 0 ? " (purple)" : ""),
        "Euler Circuit: " + (eulerCircuit ? "Yes" : "No"),
        "Euler Path (Trail): " + (eulerPath ? "Yes" : "No"),
        "Hamiltonian Path: " + (tooLarge ? ">20 vertices" : (hamPath ? "Yes" : "No")),
        "Hamiltonian Cycle: " + (tooLarge ? ">20 vertices" : (hamCycle ? "Yes" : "No")),
        vLine,
        eLine,
    };

    int rowH = 16;
    int w = 340;
    int h = lines.length * rowH + 6;

    g.setColor(new Color(255, 255, 220));
    g.fillRect(x, y - 14, w, h);
    g.setColor(Color.BLACK);
    g.drawRect(x, y - 14, w, h);

    for (int i = 0; i < lines.length; i++) {
        if (i == 0) g.setColor(new Color(60, 60, 60));
        else        g.setColor(Color.BLACK);
        g.drawString(lines[i], x + 4, y + i * rowH);
    }
    return h;
}

    // ---- Cutpoints ----

    public void computeCutpoints(Vector<Vertex> vList) {
        int n = vList.size();
        int[] disc = new int[n];
        int[] low = new int[n];
        int[] parent = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] ap = new boolean[n];
        int[] timer = {0};
        Arrays.fill(parent, -1);

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfsAP(i, vList, disc, low, parent, visited, ap, timer);
            }
        }
        for (int i = 0; i < n; i++) {
            vList.get(i).isCutpoint = ap[i];
        }
    }

    private void dfsAP(int u, Vector<Vertex> vList, int[] disc, int[] low,
                       int[] parent, boolean[] visited, boolean[] ap, int[] timer) {
        visited[u] = true;
        disc[u] = low[u] = timer[0]++;
        int children = 0;

        for (int v : getAllNeighborIndices(u, vList)) {
            if (v == u) continue;
            if (!visited[v]) {
                children++;
                parent[v] = u;
                dfsAP(v, vList, disc, low, parent, visited, ap, timer);
                low[u] = Math.min(low[u], low[v]);
                if (parent[u] == -1 && children > 1) ap[u] = true;
                if (parent[u] != -1 && low[v] >= disc[u]) ap[u] = true;
            } else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    private Set<Integer> getAllNeighborIndices(int u, Vector<Vertex> vList) {
        Vertex vu = vList.get(u);
        Set<Integer> neighbors = new HashSet<>();
        for (Vertex n : vu.undirectedNeighbors) {
            int idx = vList.indexOf(n);
            if (idx >= 0) neighbors.add(idx);
        }
        for (Vertex n : vu.inNeighbors) {
            int idx = vList.indexOf(n);
            if (idx >= 0) neighbors.add(idx);
        }
        for (Vertex n : vu.outNeighbors) {
            int idx = vList.indexOf(n);
            if (idx >= 0) neighbors.add(idx);
        }
        return neighbors;
    }

    public Vector<Vertex> vertexConnectivity(Vector<Vertex> vList) {
        Vector<Vertex> origList = new Vector<Vertex>();
        Vector<Vertex> tempList = new Vector<Vertex>();
        Vector<Vertex> toBeRemoved = new Vector<Vertex>();
        Vertex victim;

        origList.setSize(vList.size());
        Collections.copy(origList, vList);

        int maxPossibleRemove = 0;
        while (graphConnectivity(origList)) {
            Collections.sort(origList, new ascendingDegreeComparator());
            maxPossibleRemove = origList.firstElement().getDegree();

            for (Vertex v : origList) {
                if (v.getDegree() == maxPossibleRemove) {
                    for (Vertex z : v.undirectedNeighbors) {
                        if (!tempList.contains(z)) {
                            tempList.add(z);
                        }
                    }
                }
            }

            while (graphConnectivity(origList) && tempList.size() > 0) {
                Collections.sort(tempList, new descendingDegreeComparator());
                victim = tempList.firstElement();
                tempList.removeElementAt(0);
                origList.remove(victim);
                for (Vertex x : origList) {
                    x.undirectedNeighbors.remove(victim);
                }
                toBeRemoved.add(victim);
            }
            tempList.removeAllElements();
        }

        return toBeRemoved;
    }

    private boolean graphConnectivity(Vector<Vertex> vList) {

        Vector<Vertex> visitedList = new Vector<Vertex>();

        recurseGraphConnectivity(vList.firstElement().undirectedNeighbors, visitedList); //recursive function
        if (visitedList.size() != vList.size()) {
            return false;
        } else {
            return true;
        }
    }

    private void recurseGraphConnectivity(Vector<Vertex> vList, Vector<Vertex> visitedList) {
        for (Vertex v : vList) {
            {
                if (!visitedList.contains(v)) {
                    visitedList.add(v);
                    recurseGraphConnectivity(v.undirectedNeighbors, visitedList);
                }
            }
        }
    }

    private class ascendingDegreeComparator implements Comparator {

        public int compare(Object v1, Object v2) {

            if (((Vertex) v1).getDegree() > ((Vertex) v2).getDegree()) {
                return 1;
            } else if (((Vertex) v1).getDegree() > ((Vertex) v2).getDegree()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private class descendingDegreeComparator implements Comparator {

        public int compare(Object v1, Object v2) {

            if (((Vertex) v1).getDegree() > ((Vertex) v2).getDegree()) {
                return -1;
            } else if (((Vertex) v1).getDegree() > ((Vertex) v2).getDegree()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private class descendingWidthComparator implements Comparator {

        public int compare(Object v1, Object v2) {

            if (((Vector<Vertex>) v1).size() > (((Vector<Vertex>) v2).size())) {
                return -1;
            } else if (((Vector<Vertex>) v1).size() < (((Vector<Vertex>) v2).size())) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}