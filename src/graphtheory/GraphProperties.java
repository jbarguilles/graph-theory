/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author mk
 */
public class GraphProperties {

    public int[][] adjacencyMatrix;
    public int[][] weightedAdjacencyMatrix;
    public int[][] distanceMatrix;
    public Vector<VertexPair> vpList;

    public Vector<Vertex> witnessVertices = new Vector<Vertex>();
    public int vertexConnectivityValue = 0;

    public Vector<Edge> witnessEdges = new Vector<Edge>();
    public int edgeConnectivityValue = 0;

    /** Number of blocks computed by the last call to computeBlocks(). */
    public int blockCount = 0;

    /** List of blocks computed by the last call to computeBlocks(). */
    public Vector<Vector<Edge>> blockList = new Vector<Vector<Edge>>();

    /** Colors assigned by the last call to greedyColoring(). Index = vertexList index. */
    public int[] vertexColors = new int[0];

    /** Chromatic number χ(G), or -1 if the graph is too large to compute. */
    public int chromaticNumberValue = -1;

    public int[][] generateAdjacencyMatrix(Vector<Vertex> vList, Vector<Edge> eList) {
        adjacencyMatrix = new int[vList.size()][vList.size()];
        weightedAdjacencyMatrix = new int[vList.size()][vList.size()];

        for (Edge e : eList) {
            int i = vList.indexOf(e.vertex1);
            int j = vList.indexOf(e.vertex2);
            if (i < 0 || j < 0) continue;

            adjacencyMatrix[i][j] = 1;
            weightedAdjacencyMatrix[i][j] = e.weight;

            if (!e.directed) {
                adjacencyMatrix[j][i] = 1;
                weightedAdjacencyMatrix[j][i] = e.weight;
            }
        }
        return adjacencyMatrix;
    }

    public int[][] generateDistanceMatrix(Vector<Vertex> vList) {
        distanceMatrix = new int[vList.size()][vList.size()];

        for (int i = 0; i < vList.size(); i++) {
            for (int j = 0; j < vList.size(); j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                    continue;
                }
                VertexPair vp = new VertexPair(vList.get(i), vList.get(j));
                distanceMatrix[i][j] = vp.getShortestDistance();
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

        for (int a = 0; a < vList.size(); a++) {
            for (int b = a + 1; b < vList.size(); b++) {
                vp = new VertexPair(vList.get(a), vList.get(b));
                vpList.add(vp);
                int longestWidth = 0;
                System.out.println(">Vertex Pair " + vList.get(a).name + "-" + vList.get(b).name + "\n All Paths:");
                vp.generateVertexDisjointPaths();
                for (int i = 0; i < vp.VertexDisjointContainer.size(); i++) {
                    int width = vp.VertexDisjointContainer.get(i).size();
                    Collections.sort(vp.VertexDisjointContainer.get(i), new descendingWidthComparator());
                    int longestLength = vp.VertexDisjointContainer.get(i).firstElement().size();
                    longestWidth = Math.max(longestWidth, width);
                    System.out.println("\tContainer " + i + " - " + "Width=" + width + " - Length=" + longestLength);

                    for (int j = 0; j < vp.VertexDisjointContainer.get(i).size(); j++) {
                        System.out.print("\t\tPath " + j + "\n\t\t\t");
                        for (int k = 0; k < vp.VertexDisjointContainer.get(i).get(j).size(); k++) {
                            System.out.print("-" + vp.VertexDisjointContainer.get(i).get(j).get(k).name);
                        }
                        System.out.println();
                    }
                }
                for (int k = 1; k <= longestWidth; k++) {
                    int minLength = 999;
                    for (int m = 0; m < vp.VertexDisjointContainer.size(); m++) {
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
        g.fillRect(x, y - 30, vList.size() * cSize + cSize, vList.size() * cSize + cSize);
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
        g.fillRect(x, y - 30, vList.size() * cSize + cSize, vList.size() * cSize + cSize);
        g.setColor(Color.black);
        g.drawString("ShortestPathMatrix (weighted; \u221E = unreachable)", x, y - cSize);
        for (int i = 0; i < vList.size(); i++) {
            g.setColor(Color.RED);
            g.drawString(vList.get(i).name, x + cSize + i * cSize, y);
            g.drawString(vList.get(i).name, x, cSize + i * cSize + y);
            g.setColor(Color.black);
            for (int j = 0; j < vList.size(); j++) {
                int d = distanceMatrix[i][j];
                String cell = (d < 0) ? "\u221E" : ("" + d);
                g.drawString(cell, x + cSize * (j + 1), y + cSize * (i + 1));
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
            g.drawString(v.name,              x + 0 * colW + 3, ry);
            g.drawString("" + v.degree(),     x + 1 * colW + 3, ry);
            g.drawString("" + v.inDegree(),   x + 2 * colW + 3, ry);
            g.drawString("" + v.outDegree(),  x + 3 * colW + 3, ry);
            g.drawString("" + v.isIsolated(), x + 4 * colW + 3, ry);
            g.drawString("" + v.isCutpoint,   x + 5 * colW + 3, ry);
            g.drawString("" + v.isRoot,       x + 6 * colW + 3, ry);
        }
    }

    public int drawAdjacencyList(Graphics g, Vector<Vertex> vList, int x, int y) {
        int rowH = 18;

        g.setColor(Color.BLACK);
        g.drawString("Adjacency List", x, y - 5);

        int ty = y + rowH;
        for (Vertex v : vList) {
            StringBuilder sb = new StringBuilder();
            sb.append(v.name).append(" : ");

            boolean wroteSomething = false;

            if (!v.undirectedNeighbors.isEmpty()) {
                sb.append("[");
                for (int i = 0; i < v.undirectedNeighbors.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(v.undirectedNeighbors.get(i).name);
                }
                sb.append("]");
                wroteSomething = true;
            }

            if (!v.outNeighbors.isEmpty()) {
                if (wroteSomething) sb.append("  ");
                sb.append("out:(");
                for (int i = 0; i < v.outNeighbors.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(v.outNeighbors.get(i).name);
                }
                sb.append(")");
                wroteSomething = true;
            }

            if (!v.inNeighbors.isEmpty()) {
                if (wroteSomething) sb.append("  ");
                sb.append("in:(");
                for (int i = 0; i < v.inNeighbors.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(v.inNeighbors.get(i).name);
                }
                sb.append(")");
                wroteSomething = true;
            }

            if (!wroteSomething) {
                sb.append("(isolated)");
            }

            g.setColor(Color.BLACK);
            g.drawString(sb.toString(), x + 4, ty);
            ty += rowH;
        }

        return (vList.size() + 1) * rowH + 6;
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

    // ---- Euler / Hamiltonian answers for the summary, cached per graph change ----
    // Same searches as the Find menu items, so the two never disagree (ADR 0003).

    private boolean traversalSummaryValid = false;
    private String cachedEulerTrail, cachedEulerTour, cachedHamPath, cachedHamCycle;

    /** Canvas calls this whenever vertices or edges are added or removed. */
    public void invalidateTraversalSummary() {
        traversalSummaryValid = false;
    }

    private void ensureTraversalSummary(Vector<Vertex> vList, Vector<Edge> eList) {
        if (traversalSummaryValid) return;
        cachedEulerTrail = yesNo(Traversals.eulerTrail(vList, eList) != null);
        cachedEulerTour  = yesNo(Traversals.eulerTour(vList, eList) != null);
        if (Traversals.hamiltonTooLarge(vList)) {
            cachedHamPath = cachedHamCycle = "> " + Traversals.HAMILTON_VERTEX_CAP + " vertices";
        } else {
            cachedHamPath  = yesNo(Traversals.hamiltonianPath(vList, eList) != null);
            cachedHamCycle = yesNo(Traversals.hamiltonianCycle(vList, eList) != null);
        }
        traversalSummaryValid = true;
    }

    // ---- Block / biconnected component (Tarjan stack-based) ----

    public Vector<Vector<Edge>> computeBlocks(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        int[] disc = new int[n];
        int[] low = new int[n];
        int[] parent = new int[n];
        boolean[] visited = new boolean[n];
        int[] timer = {0};
        Arrays.fill(disc, -1);
        Arrays.fill(parent, -1);

        Vector<Vector<Edge>> blocks = new Vector<Vector<Edge>>();
        Vector<Edge> edgeStack = new Vector<Edge>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfsBlock(i, vList, eList, disc, low, parent, visited, timer,
                         edgeStack, blocks);
            }
        }

        if (!edgeStack.isEmpty()) {
            Vector<Edge> last = new Vector<Edge>(edgeStack);
            blocks.add(last);
            edgeStack.clear();
        }

        for (int b = 0; b < blocks.size(); b++) {
            for (Edge e : blocks.get(b)) {
                e.blockId = b;
            }
        }
        this.blockCount = blocks.size();
        this.blockList = blocks;
        return blocks;
    }

    private void dfsBlock(int u, Vector<Vertex> vList, Vector<Edge> eList,
                          int[] disc, int[] low, int[] parent, boolean[] visited, int[] timer,
                          Vector<Edge> edgeStack, Vector<Vector<Edge>> blocks) {
        visited[u] = true;
        disc[u] = low[u] = timer[0]++;
        int children = 0;

        for (int v : getAllNeighborIndices(u, vList)) {
            if (v == u) continue;
            Edge e = findEdge(vList.get(u), vList.get(v), eList);
            if (e == null) continue;

            if (!visited[v]) {
                children++;
                parent[v] = u;
                edgeStack.add(e);
                dfsBlock(v, vList, eList, disc, low, parent, visited, timer,
                         edgeStack, blocks);
                low[u] = Math.min(low[u], low[v]);

                boolean isRootWithTwo = (parent[u] == -1 && children > 1);
                boolean isArticulation = (parent[u] != -1 && low[v] >= disc[u]);
                if (isRootWithTwo || isArticulation) {
                    Vector<Edge> block = new Vector<Edge>();
                    while (!edgeStack.isEmpty()) {
                        Edge top = edgeStack.remove(edgeStack.size() - 1);
                        block.add(top);
                        if (top == e) break;
                    }
                    blocks.add(block);
                }
            } else if (v != parent[u] && disc[v] < disc[u]) {
                edgeStack.add(e);
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    private Edge findEdge(Vertex a, Vertex b, Vector<Edge> eList) {
        for (Edge e : eList) {
            if (e.vertex1 == a && e.vertex2 == b) return e;
            if (!e.directed && e.vertex1 == b && e.vertex2 == a) return e;
        }
        return null;
    }

    public Set<Vertex> blockVertices(Vector<Edge> block) {
        Set<Vertex> s = new HashSet<Vertex>();
        for (Edge e : block) {
            s.add(e.vertex1);
            s.add(e.vertex2);
        }
        return s;
    }

    public int countNontrivialBlocks() {
        int count = 0;
        for (Vector<Edge> block : blockList) {
            if (blockVertices(block).size() >= 3) count++;
        }
        return count;
    }

    public boolean hasMaximalBlocks() {
        return blockCount > 0;
    }

    public String formatBlocks(int maxLen) {
        if (blockList.isEmpty()) return "\u2014";
        StringBuilder sb = new StringBuilder();
        for (int b = 0; b < blockList.size(); b++) {
            if (b > 0) sb.append(", ");
            Vector<Edge> block = blockList.get(b);
            Set<Vertex> verts = blockVertices(block);

            List<Vertex> ordered = new ArrayList<Vertex>(verts);
            ordered.sort(new Comparator<Vertex>() {
                public int compare(Vertex a, Vertex c) {
                    return a.name.compareTo(c.name);
                }
            });

            sb.append("{");
            for (int i = 0; i < ordered.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(ordered.get(i).name);
            }
            sb.append("}");
        }
        String result = sb.toString();
        if (result.length() > maxLen) {
            result = result.substring(0, Math.max(0, maxLen - 3)) + "...";
        }
        return result;
    }

    public boolean isNonseparable(Vector<Vertex> vList) {
        if (vList.size() < 2) return true;
        if (!isConnected(vList)) return false;
        for (Vertex v : vList) if (v.isCutpoint) return false;
        return true;
    }

    // ---- Graph coloring ----

    public int[] greedyColoring(Vector<Vertex> vList) {
        int n = vList.size();
        int[] color = new int[n];
        Arrays.fill(color, -1);

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(
                vList.get(b).getDegree(), vList.get(a).getDegree()));

        for (int idx : order) {
            Vertex v = vList.get(idx);

            Set<Integer> used = new HashSet<Integer>();
            for (Vertex nbor : allNeighborsForColoring(v)) {
                int ni = vList.indexOf(nbor);
                if (ni >= 0 && color[ni] >= 0) used.add(color[ni]);
            }

            int c = 0;
            while (used.contains(c)) c++;
            color[idx] = c;
        }

        this.vertexColors = color;
        for (int i = 0; i < n; i++) vList.get(i).colorId = color[i];
        return color;
    }

    public void clearColoring(Vector<Vertex> vList) {
        for (Vertex v : vList) v.colorId = -1;
        this.vertexColors = new int[0];
    }

    public int chromaticNumber(Vector<Vertex> vList) {
        int n = vList.size();
        if (n == 0) { chromaticNumberValue = 0; return 0; }
        if (n > 15) { chromaticNumberValue = -1; return -1; }

        boolean anyEdge = false;
        for (Vertex v : vList) if (v.getDegree() > 0) { anyEdge = true; break; }
        if (!anyEdge) { chromaticNumberValue = 1; return 1; }

        boolean[][] adj = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            Vertex vi = vList.get(i);
            for (int j = i + 1; j < n; j++) {
                Vertex vj = vList.get(j);
                if (areAdjacent(vi, vj)) {
                    adj[i][j] = true;
                    adj[j][i] = true;
                }
            }
        }

        int[] color = new int[n];

        for (int k = 1; k <= n; k++) {
            Arrays.fill(color, -1);
            if (canColor(0, k, color, adj, n)) {
                chromaticNumberValue = k;
                return k;
            }
        }
        chromaticNumberValue = n;
        return n;
    }

    private boolean canColor(int v, int k, int[] color, boolean[][] adj, int n) {
        if (v == n) return true;
        for (int c = 0; c < k; c++) {
            boolean conflict = false;
            for (int u = 0; u < v; u++) {
                if (adj[v][u] && color[u] == c) { conflict = true; break; }
            }
            if (conflict) continue;

            color[v] = c;
            if (canColor(v + 1, k, color, adj, n)) return true;
            color[v] = -1;
        }
        return false;
    }

    private boolean areAdjacent(Vertex a, Vertex b) {
        if (a.undirectedNeighbors.contains(b) || b.undirectedNeighbors.contains(a)) return true;
        if (a.outNeighbors.contains(b) || b.outNeighbors.contains(a)) return true;
        if (a.inNeighbors.contains(b) || b.inNeighbors.contains(a)) return true;
        return false;
    }

    private List<Vertex> allNeighborsForColoring(Vertex u) {
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex v : u.undirectedNeighbors) if (!result.contains(v)) result.add(v);
        for (Vertex v : u.outNeighbors)        if (!result.contains(v)) result.add(v);
        for (Vertex v : u.inNeighbors)         if (!result.contains(v)) result.add(v);
        return result;
    }

    public String formatColoring(Vector<Vertex> vList, int maxLen) {
        if (vertexColors.length != vList.size()) return "\u2014";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(vList.get(i).name);
            sb.append("=");
            int c = vertexColors[i];
            if (c < 0) {
                sb.append("?");
            } else {
                sb.append((char) ('A' + (c % 26)));
            }
        }
        String result = sb.toString();
        if (result.length() > maxLen) {
            result = result.substring(0, Math.max(0, maxLen - 3)) + "...";
        }
        return result;
    }

    // ---- Matching ----

    public Vector<Edge> maximalMatching(Vector<Vertex> vList, Vector<Edge> eList) {
        Set<Vertex> matched = new HashSet<Vertex>();
        Vector<Edge> result = new Vector<Edge>();

        for (Edge e : eList) {
            if (e.directed) continue;
            if (e.vertex1 == e.vertex2) continue;
            if (matched.contains(e.vertex1) || matched.contains(e.vertex2)) continue;

            result.add(e);
            matched.add(e.vertex1);
            matched.add(e.vertex2);
        }
        return result;
    }

    public Vector<Edge> maximumBipartiteMatching(Vector<Vertex> vList, Vector<Edge> eList) {
        if (vList.isEmpty()) return new Vector<Edge>();
        Vector<Vertex>[] sides = bipartiteSides(vList);
        if (sides == null) return null;

        Vector<Vertex> A = sides[0];

        Map<Vertex, Vertex> matchA = new HashMap<Vertex, Vertex>();
        Map<Vertex, Vertex> matchB = new HashMap<Vertex, Vertex>();

        for (Vertex a : A) {
            Set<Vertex> visited = new HashSet<Vertex>();
            tryAugment(a, visited, matchA, matchB);
        }

        Vector<Edge> result = new Vector<Edge>();
        for (Map.Entry<Vertex, Vertex> entry : matchA.entrySet()) {
            Vertex a = entry.getKey();
            Vertex b = entry.getValue();
            for (Edge e : eList) {
                if ((e.vertex1 == a && e.vertex2 == b) ||
                    (!e.directed && e.vertex1 == b && e.vertex2 == a)) {
                    result.add(e);
                    break;
                }
            }
        }
        return result;
    }

    private boolean tryAugment(Vertex a, Set<Vertex> visited,
                               Map<Vertex, Vertex> matchA, Map<Vertex, Vertex> matchB) {
        for (Vertex b : allNeighborsForColoring(a)) {
            if (matchB.containsKey(b) && !visited.add(b)) continue;

            Vertex bCurrent = matchB.get(b);
            if (bCurrent == null || tryAugment(bCurrent, visited, matchA, matchB)) {
                matchA.put(a, b);
                matchB.put(b, a);
                return true;
            }
        }
        return false;
    }

    public boolean hasPerfectMatching(Vector<Vertex> vList, Vector<Edge> eList) {
        if (vList.size() % 2 != 0) return false;
        Vector<Edge> mm = maximumBipartiteMatching(vList, eList);
        if (mm == null) return false;
        return mm.size() * 2 == vList.size();
    }

    public Vector<Edge> stableMatching(Vector<Vertex> vList, Vector<Edge> eList) {
        Vector<Vertex>[] sides = bipartiteSides(vList);
        if (sides == null) return null;

        Vector<Vertex> proposers = sides[0];

        Map<Vertex, List<Vertex>> prefs = new HashMap<Vertex, List<Vertex>>();
        for (Vertex v : vList) {
            List<Vertex> lst = allNeighborsForColoring(v);
            lst.sort((x, y) -> x.name.compareTo(y.name));
            prefs.put(v, lst);
        }

        Map<Vertex, Integer> nextProposal = new HashMap<Vertex, Integer>();
        for (Vertex p : proposers) nextProposal.put(p, 0);

        Map<Vertex, Vertex> receiverPartner = new HashMap<Vertex, Vertex>();
        Map<Vertex, Vertex> proposerPartner = new HashMap<Vertex, Vertex>();

        ArrayDeque<Vertex> free = new ArrayDeque<Vertex>();
        for (Vertex p : proposers) {
            if (!prefs.get(p).isEmpty()) free.add(p);
        }

        while (!free.isEmpty()) {
            Vertex p = free.poll();
            int idx = nextProposal.get(p);
            List<Vertex> prefList = prefs.get(p);
            if (idx >= prefList.size()) continue;

            Vertex r = prefList.get(idx);
            nextProposal.put(p, idx + 1);

            if (!receiverPartner.containsKey(r) || receiverPartner.get(r) == null) {
                receiverPartner.put(r, p);
                proposerPartner.put(p, r);
            } else {
                Vertex current = receiverPartner.get(r);
                List<Vertex> rPrefs = prefs.get(r);
                int pRank = rPrefs.indexOf(p);
                int curRank = rPrefs.indexOf(current);
                if (pRank < curRank) {
                    receiverPartner.put(r, p);
                    proposerPartner.put(p, r);
                    proposerPartner.remove(current);
                    free.add(current);
                } else {
                    free.add(p);
                }
            }
        }

        Vector<Edge> result = new Vector<Edge>();
        for (Map.Entry<Vertex, Vertex> entry : proposerPartner.entrySet()) {
            Vertex a = entry.getKey();
            Vertex b = entry.getValue();
            for (Edge e : eList) {
                if ((e.vertex1 == a && e.vertex2 == b) ||
                    (!e.directed && e.vertex1 == b && e.vertex2 == a)) {
                    result.add(e);
                    break;
                }
            }
        }
        return result;
    }

    public String formatMatching(Vector<Edge> matching, int maxLen) {
        if (matching == null) return "\u2014";
        if (matching.isEmpty()) return "0 edges";

        StringBuilder sb = new StringBuilder();
        sb.append(matching.size()).append(" edge");
        if (matching.size() != 1) sb.append("s");
        sb.append(": ");

        for (int i = 0; i < matching.size(); i++) {
            if (i > 0) sb.append(", ");
            Edge e = matching.get(i);
            sb.append("{").append(e.vertex1.name).append("-").append(e.vertex2.name).append("}");
        }
        String result = sb.toString();
        if (result.length() > maxLen) {
            result = result.substring(0, Math.max(0, maxLen - 3)) + "...";
        }
        return result;
    }

    // ---- Connectivity ----

    public boolean isConnected(Vector<Vertex> vList) {
        return countComponents(vList) == 1;
    }

    public int countComponents(Vector<Vertex> vList) {
        if (vList.isEmpty()) return 0;

        Set<Vertex> visited = new HashSet<Vertex>();
        int components = 0;

        for (Vertex start : vList) {
            if (visited.contains(start)) continue;
            components++;
            ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
            visited.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                Vertex u = queue.poll();
                for (Vertex n : u.undirectedNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
                for (Vertex n : u.outNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
                for (Vertex n : u.inNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
            }
        }
        return components;
    }

    public Vector<Vector<Vertex>> getComponents(Vector<Vertex> vList) {
        Vector<Vector<Vertex>> components = new Vector<Vector<Vertex>>();
        if (vList.isEmpty()) return components;

        Set<Vertex> visited = new HashSet<Vertex>();

        for (Vertex start : vList) {
            if (visited.contains(start)) continue;

            Vector<Vertex> comp = new Vector<Vertex>();
            ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
            visited.add(start);
            queue.add(start);

            while (!queue.isEmpty()) {
                Vertex u = queue.poll();
                comp.add(u);

                for (Vertex n : u.undirectedNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
                for (Vertex n : u.outNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
                for (Vertex n : u.inNeighbors) {
                    if (!visited.contains(n) && vList.contains(n)) { visited.add(n); queue.add(n); }
                }
            }
            components.add(comp);
        }
        return components;
    }

    public String formatComponents(Vector<Vertex> vList, int maxLen) {
        Vector<Vector<Vertex>> comps = getComponents(vList);
        if (comps.isEmpty()) return "\u2014";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("{");
            Vector<Vertex> comp = comps.get(i);
            for (int j = 0; j < comp.size(); j++) {
                if (j > 0) sb.append(", ");
                sb.append(comp.get(j).name);
            }
            sb.append("}");
        }

        String result = sb.toString();
        if (result.length() > maxLen) {
            result = result.substring(0, Math.max(0, maxLen - 3)) + "...";
        }
        return result;
    }

    public boolean isStronglyConnected(Vector<Vertex> vList) {
        int n = vList.size();
        if (n <= 1) return false;

        Vertex start = vList.firstElement();
        if (!reachesAll(start, vList, true))  return false;
        if (!reachesAll(start, vList, false)) return false;
        return true;
    }

    private boolean reachesAll(Vertex start, Vector<Vertex> vList, boolean forward) {
        Set<Vertex> visited = new HashSet<Vertex>();
        ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Vertex u = queue.poll();
            for (Vertex n : u.undirectedNeighbors) {
                if (vList.contains(n) && visited.add(n)) queue.add(n);
            }
            if (forward) {
                for (Vertex n : u.outNeighbors) {
                    if (vList.contains(n) && visited.add(n)) queue.add(n);
                }
            } else {
                for (Vertex n : u.inNeighbors) {
                    if (vList.contains(n) && visited.add(n)) queue.add(n);
                }
            }
        }

        for (Vertex v : vList) {
            if (!visited.contains(v)) return false;
        }
        return true;
    }

    public boolean hasDirectedEdges(Vector<Edge> eList) {
        for (Edge e : eList) if (e.directed) return true;
        return false;
    }

    private String strongConnectivityLabel(Vector<Vertex> vList, Vector<Edge> eList) {
        if (!hasDirectedEdges(eList)) return "\u2014";
        return isStronglyConnected(vList) ? "Yes" : "No";
    }

    // ---- Simple / Multigraph ----

    public boolean isSimple(Vector<Vertex> vList, Vector<Edge> eList) {
        for (Edge e : eList) {
            if (e.vertex1 == e.vertex2) return false;
        }

        for (int i = 0; i < eList.size(); i++) {
            Edge a = eList.get(i);
            for (int j = i + 1; j < eList.size(); j++) {
                Edge b = eList.get(j);

                if (a.directed && b.directed
                        && a.vertex1 == b.vertex1 && a.vertex2 == b.vertex2) {
                    return false;
                }

                if (!a.directed && !b.directed) {
                    boolean same =
                            (a.vertex1 == b.vertex1 && a.vertex2 == b.vertex2) ||
                            (a.vertex1 == b.vertex2 && a.vertex2 == b.vertex1);
                    if (same) return false;
                }

                if (a.directed != b.directed) {
                    boolean same =
                            (a.vertex1 == b.vertex1 && a.vertex2 == b.vertex2) ||
                            (a.vertex1 == b.vertex2 && a.vertex2 == b.vertex1);
                    if (same) return false;
                }
            }
        }
        return true;
    }

    // ---- Bipartite / Complete bipartite ----

    public boolean isBipartite(Vector<Vertex> vList) {
        if (vList.isEmpty()) return true;

        Map<Vertex, Integer> color = new HashMap<Vertex, Integer>();

        for (Vertex start : vList) {
            if (color.containsKey(start)) continue;

            ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
            color.put(start, 0);
            queue.add(start);

            while (!queue.isEmpty()) {
                Vertex u = queue.poll();
                int cu = color.get(u);
                int next = 1 - cu;

                for (Vertex w : bipartiteNeighbors(u)) {
                    if (!color.containsKey(w)) {
                        color.put(w, next);
                        queue.add(w);
                    } else if (color.get(w) == cu) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public Vector<Vertex>[] bipartiteSides(Vector<Vertex> vList) {
        if (vList.isEmpty()) return null;

        Map<Vertex, Integer> color = new HashMap<Vertex, Integer>();

        for (Vertex start : vList) {
            if (color.containsKey(start)) continue;

            ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
            color.put(start, 0);
            queue.add(start);

            while (!queue.isEmpty()) {
                Vertex u = queue.poll();
                int cu = color.get(u);
                int next = 1 - cu;

                for (Vertex w : bipartiteNeighbors(u)) {
                    if (!color.containsKey(w)) {
                        color.put(w, next);
                        queue.add(w);
                    } else if (color.get(w) == cu) {
                        return null;
                    }
                }
            }
        }

        for (Vertex v : vList) if (!color.containsKey(v)) color.put(v, 0);

        Vector<Vertex>[] sides = new Vector[2];
        sides[0] = new Vector<Vertex>();
        sides[1] = new Vector<Vertex>();
        for (Vertex v : vList) {
            int c = color.get(v);
            sides[c].add(v);
        }
        return sides;
    }

    public boolean isCompleteBipartite(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        if (n < 2) return false;

        Vector<Vertex>[] sides = bipartiteSides(vList);
        if (sides == null) return false;
        if (sides[0].isEmpty() || sides[1].isEmpty()) return false;

        int m = sides[0].size();
        int k = sides[1].size();

        int crossEdges = 0;
        for (Edge e : eList) {
            boolean a0 = sides[0].contains(e.vertex1);
            boolean a1 = sides[0].contains(e.vertex2);
            boolean b0 = sides[1].contains(e.vertex1);
            boolean b1 = sides[1].contains(e.vertex2);

            boolean cross = (a0 && b1) || (b0 && a1);
            boolean same  = (a0 && a1) || (b0 && b1);

            if (same)  return false;
            if (cross) crossEdges++;
        }
        return crossEdges == m * k;
    }

    public String bipartiteLabel(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        if (n <= 1) return "\u2014";

        Vector<Vertex>[] sides = bipartiteSides(vList);
        if (sides == null) return "No";

        int m = sides[0].size();
        int k = sides[1].size();
        boolean complete = isCompleteBipartite(vList, eList);

        StringBuilder sb = new StringBuilder();
        sb.append("Yes \u2014 ");
        if (complete) {
            sb.append("Complete K_{").append(m).append(",").append(k).append("}; ");
        }
        sb.append("A = {");
        for (int i = 0; i < sides[0].size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(sides[0].get(i).name);
        }
        sb.append("}, B = {");
        for (int i = 0; i < sides[1].size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(sides[1].get(i).name);
        }
        sb.append("}");

        String result = sb.toString();
        if (result.length() > 80) {
            result = result.substring(0, 77) + "...";
        }
        return result;
    }

    private List<Vertex> bipartiteNeighbors(Vertex u) {
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex v : u.undirectedNeighbors) if (!result.contains(v)) result.add(v);
        for (Vertex v : u.outNeighbors)        if (!result.contains(v)) result.add(v);
        for (Vertex v : u.inNeighbors)         if (!result.contains(v)) result.add(v);
        return result;
    }

    // ---- Empty / Complete ----

    public boolean isEmptyGraph(Vector<Vertex> vList, Vector<Edge> eList) {
        return eList.isEmpty();
    }

    public boolean isComplete(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        if (n <= 1) return true;

        boolean anyDirected = hasDirectedEdges(eList);
        boolean anyUndirected = false;
        for (Edge e : eList) if (!e.directed) { anyUndirected = true; break; }

        if (anyDirected && anyUndirected) return false;

        if (anyDirected) {
            if (eList.size() != n * (n - 1)) return false;
        } else {
            if (eList.size() != n * (n - 1) / 2) return false;
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Vertex a = vList.get(i), b = vList.get(j);
                boolean hasArcAB = false, hasArcBA = false, hasUndirected = false;
                for (Edge e : eList) {
                    if (e.vertex1 == a && e.vertex2 == b) {
                        if (e.directed) hasArcAB = true; else hasUndirected = true;
                    }
                    if (e.vertex1 == b && e.vertex2 == a) {
                        if (e.directed) hasArcBA = true; else hasUndirected = true;
                    }
                }
                if (anyDirected) {
                    if (!hasArcAB || !hasArcBA) return false;
                } else {
                    if (!hasUndirected) return false;
                }
            }
        }
        return true;
    }

    // ---- Density ----

    public double density(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        if (n <= 1) return 0.0;

        boolean anyDirected = hasDirectedEdges(eList);
        double maxEdges = anyDirected
                ? (double) n * (n - 1)
                : (double) n * (n - 1) / 2.0;
        if (maxEdges == 0) return 0.0;

        return eList.size() / maxEdges;
    }

    public String sparseDenseLabel(Vector<Vertex> vList, Vector<Edge> eList) {
        int n = vList.size();
        if (n <= 1) return "\u2014";

        double d = density(vList, eList);
        if (d < 0.25) return "Sparse";
        if (d > 0.75) return "Dense";
        return "In between";
    }

    // ---- Acyclic / Forest / Tree / Star / Rooted tree ----

    public boolean isAcyclic(Vector<Vertex> vList) {
        Set<Vertex> visited = new HashSet<Vertex>();

        for (Vertex start : vList) {
            if (visited.contains(start)) continue;
            if (hasCycleDFS(start, null, visited)) return false;
        }
        return true;
    }

    private boolean hasCycleDFS(Vertex u, Vertex parent, Set<Vertex> visited) {
        visited.add(u);

        for (Vertex w : allNeighborsUndirected(u)) {
            if (w == u) return true;
            if (!visited.contains(w)) {
                if (hasCycleDFS(w, u, visited)) return true;
            } else if (w != parent) {
                return true;
            }
        }
        return false;
    }

    private List<Vertex> allNeighborsUndirected(Vertex u) {
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex v : u.undirectedNeighbors) if (!result.contains(v)) result.add(v);
        for (Vertex v : u.outNeighbors)        if (!result.contains(v)) result.add(v);
        for (Vertex v : u.inNeighbors)         if (!result.contains(v)) result.add(v);
        return result;
    }

    public boolean isForest(Vector<Vertex> vList) {
        return isAcyclic(vList);
    }

    public boolean isFreeTree(Vector<Vertex> vList) {
        if (vList.isEmpty()) return false;
        return isConnected(vList) && isAcyclic(vList);
    }

    public boolean isStar(Vector<Vertex> vList) {
        int n = vList.size();
        if (n < 3) return false;
        if (!isFreeTree(vList)) return false;

        int centerCount = 0;
        for (Vertex v : vList) {
            if (v.getDegree() == n - 1) centerCount++;
        }
        return centerCount == 1;
    }

    public boolean isRootedTree(Vector<Vertex> vList) {
        if (!isFreeTree(vList)) return false;

        int rootCount = 0;
        for (Vertex v : vList) if (v.isRoot) rootCount++;
        return rootCount == 1;
    }

    public Vertex getRoot(Vector<Vertex> vList) {
        Vertex root = null;
        for (Vertex v : vList) {
            if (v.isRoot) {
                if (root != null) return null;
                root = v;
            }
        }
        return root;
    }

    public String treeLabel(Vector<Vertex> vList) {
        if (vList.isEmpty()) return "\u2014";

        if (isFreeTree(vList)) {
            if (isRootedTree(vList)) {
                Vertex r = getRoot(vList);
                return "Rooted tree (root = " + r.name + ")";
            }
            return "Free tree";
        }

        if (isForest(vList)) {
            Vector<Vector<Vertex>> comps = getComponents(vList);
            StringBuilder sb = new StringBuilder();
            sb.append("Forest (").append(comps.size()).append(" trees");
            StringBuilder roots = new StringBuilder();
            for (Vector<Vertex> comp : comps) {
                Vertex compRoot = null;
                for (Vertex v : comp) {
                    if (v.isRoot) {
                        if (compRoot != null) { compRoot = null; break; }
                        compRoot = v;
                    }
                }
                if (compRoot != null) {
                    if (roots.length() > 0) roots.append(", ");
                    roots.append(compRoot.name);
                }
            }
            if (roots.length() > 0) sb.append("; roots = ").append(roots);
            sb.append(")");
            return sb.toString();
        }

        return "No";
    }


    // ---- Graph summary ----

    public int drawGraphSummary(Graphics g, Vector<Vertex> vList, Vector<Edge> eList, int x, int y) {
        int bridgeCount = 0;
        for (Edge e : eList) { if (e.isBridge) bridgeCount++; }

        ensureTraversalSummary(vList, eList);
        String eulerTrail = cachedEulerTrail;
        String eulerTour  = cachedEulerTour;
        String hamPath    = cachedHamPath;
        String hamCycle   = cachedHamCycle;

        int order     = vList.size();
        int size      = eList.size();
        int magnitude = order + size;

        StringBuilder vBody = new StringBuilder();
        for (int i = 0; i < vList.size(); i++) {
            if (i > 0) vBody.append(", ");
            vBody.append(vList.get(i).name);
        }
        String vLine = "V = {" + vBody + "}";

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

        String connectedStr = isConnected(vList)
                ? "Yes"
                : ("No (" + countComponents(vList) + " components)");

        String componentsStr = formatComponents(vList, 60);

        String densityStr = (vList.size() <= 1)
                ? "\u2014"
                : (String.format("%.2f", density(vList, eList))
                   + " (" + sparseDenseLabel(vList, eList) + ")");

        String bipartiteStr = bipartiteLabel(vList, eList);

        String treeStr = treeLabel(vList);

        String starStr = (vList.size() <= 1)
                ? "\u2014"
                : (isStar(vList) ? "Yes" : "No");

        String emptyStr = (vList.size() <= 1)
                ? "\u2014"
                : (isEmptyGraph(vList, eList) ? "Yes" : "No");

        String completeStr = (vList.size() < 1)
                ? "\u2014"
                : (isComplete(vList, eList) ? "Yes" : "No");

        String simpleStr = vList.isEmpty()
                ? "\u2014"
                : (isSimple(vList, eList) ? "Yes" : "No");

        String cyclicStr = vList.isEmpty()
                ? "\u2014"
                : (isAcyclic(vList) ? "Acyclic" : "Cyclic");

        String nontrivialCount = vList.isEmpty()
                ? "0"
                : ("" + countNontrivialBlocks());

        String blocksStr = vList.isEmpty()
                ? "\u2014"
                : ("" + blockCount + " ("
                    + countNontrivialBlocks() + " nontrivial)");

        String blockVerticesStr = vList.isEmpty()
                ? "\u2014"
                : formatBlocks(160);

        String nonsepStr = vList.size() < 2
                ? "\u2014"
                : (isNonseparable(vList) ? "Yes" : "No");

        int chi = chromaticNumber(vList);
        String chiStr = (vList.isEmpty()) ? "\u2014"
                      : (chi == -1 ? ">15 vertices" : "" + chi);
        String coloringStr = formatColoring(vList, 100);

        Vector<Edge> maximalM = maximalMatching(vList, eList);
        Vector<Edge> maximumM = maximumBipartiteMatching(vList, eList);
        String maximalMStr = formatMatching(maximalM, 100);
        String maximumMStr = (maximumM == null)
                ? "\u2014 (not bipartite)"
                : formatMatching(maximumM, 100);
        String perfectMStr = (vList.size() < 2)
                ? "\u2014"
                : (hasPerfectMatching(vList, eList) ? "Yes" : "No");
        String stableMStr = formatMatching(stableMatching(vList, eList), 100);

        String[] lines = {
            "Graph Summary",
            "Order |V|: " + order,
            "Size |E|: " + size,
            "Magnitude |V|+|E|: " + magnitude,
            "Connectivity \u03BA(G): " + vertexConnectivityValue,
            "Edge connectivity \u03BB(G): " + edgeConnectivityValue,
            "Connected: " + connectedStr,
            "Components: " + componentsStr,
            "Strongly connected: " + strongConnectivityLabel(vList, eList),
            "Bipartite: " + bipartiteStr,
            "Density |E|/maxE: " + densityStr,
            "Tree: " + treeStr,
            "Star: " + starStr,
            "Empty: " + emptyStr,
            "Complete: " + completeStr,
            "Simple: " + simpleStr,
            "Cyclic/Acyclic: " + cyclicStr,
            "Blocks: " + blocksStr,
            "Nontrivial blocks: " + nontrivialCount,
            "Nonseparable: " + nonsepStr,
            "Block vertices: " + blockVerticesStr,
            "Bridges: " + bridgeCount + (bridgeCount > 0 ? " (purple)" : ""),
            "Euler Trail: " + eulerTrail,
            "Euler Tour: " + eulerTour,
            "Hamiltonian Path: " + hamPath,
            "Hamiltonian Cycle: " + hamCycle,
            "Chromatic number \u03C7(G): " + chiStr,
            "Coloring (greedy): " + coloringStr,
            "Maximal matching: " + maximalMStr,
            "Maximum matching: " + maximumMStr,
            "Perfect matching: " + perfectMStr,
            "Stable matching: " + stableMStr,
            vLine,
            eLine,
        };

        int rowH = 16;
        int w = 640;
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

    private static String yesNo(boolean b) {
        return b ? "Yes" : "No";
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

    // ---- Vertex connectivity κ(G) ----

    public int vertexConnectivity(Vector<Vertex> vList) {
        witnessVertices = new Vector<Vertex>();
        vertexConnectivityValue = 0;

        int n = vList.size();
        if (n <= 1) return 0;

        if (!isWeaklyConnected(vList, new HashSet<Vertex>())) {
            return 0;
        }

        for (int k = 1; k < n; k++) {
            Vector<Vertex> cut = new Vector<Vertex>();
            if (findCutOfSize(vList, k, 0, cut)) {
                vertexConnectivityValue = k;
                witnessVertices = cut;
                return k;
            }
        }

        vertexConnectivityValue = n - 1;
        for (int i = 0; i < n - 1; i++) {
            witnessVertices.add(vList.get(i));
        }
        return n - 1;
    }

    private boolean findCutOfSize(Vector<Vertex> vList, int k, int start, Vector<Vertex> out) {
        if (out.size() == k) {
            return !isWeaklyConnected(vList, new HashSet<Vertex>(out));
        }
        for (int i = start; i < vList.size(); i++) {
            Vertex v = vList.get(i);
            out.add(v);
            if (findCutOfSize(vList, k, i + 1, out)) return true;
            out.remove(out.size() - 1);
        }
        return false;
    }

    private boolean isWeaklyConnected(Vector<Vertex> vList, Set<Vertex> removed) {
        Vector<Vertex> alive = new Vector<Vertex>();
        for (Vertex v : vList) {
            if (removed.contains(v)) continue;
            alive.add(v);
        }
        if (alive.size() <= 1) return true;

        Vector<Vertex> nonIsolated = new Vector<Vertex>();
        for (Vertex v : alive) {
            boolean hasAnyEdge =
                !v.undirectedNeighbors.isEmpty() ||
                !v.inNeighbors.isEmpty() ||
                !v.outNeighbors.isEmpty();
            if (hasAnyEdge) nonIsolated.add(v);
        }

        if (nonIsolated.isEmpty()) return false;

        Set<Vertex> visited = new HashSet<Vertex>();
        ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
        Vertex start = nonIsolated.firstElement();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Vertex u = queue.poll();
            for (Vertex n : u.undirectedNeighbors) {
                if (removed.contains(n) || !alive.contains(n)) continue;
                if (visited.add(n)) queue.add(n);
            }
            for (Vertex n : u.outNeighbors) {
                if (removed.contains(n) || !alive.contains(n)) continue;
                if (visited.add(n)) queue.add(n);
            }
            for (Vertex n : u.inNeighbors) {
                if (removed.contains(n) || !alive.contains(n)) continue;
                if (visited.add(n)) queue.add(n);
            }
        }

        for (Vertex v : nonIsolated) {
            if (!visited.contains(v)) return false;
        }
        return true;
    }

    // ---- Edge connectivity λ(G) ----

    public int edgeConnectivity(Vector<Vertex> vList, Vector<Edge> eList) {
        witnessEdges = new Vector<Edge>();
        edgeConnectivityValue = 0;

        int n = vList.size();
        if (n <= 1) return 0;

        if (!isWeaklyConnected(vList, new HashSet<Vertex>())) {
            return 0;
        }

        int m = eList.size();
        if (m == 0) return 0;

        for (int k = 1; k <= m; k++) {
            Vector<Edge> cut = new Vector<Edge>();
            if (findEdgeCutOfSize(vList, eList, k, 0, cut)) {
                edgeConnectivityValue = k;
                witnessEdges = cut;
                return k;
            }
        }

        edgeConnectivityValue = m;
        witnessEdges = new Vector<Edge>(eList);
        return m;
    }

    private boolean findEdgeCutOfSize(Vector<Vertex> vList, Vector<Edge> eList,
                                      int k, int start, Vector<Edge> out) {
        if (out.size() == k) {
            return !isWeaklyConnectedWithoutEdges(vList, out);
        }
        for (int i = start; i < eList.size(); i++) {
            Edge e = eList.get(i);
            out.add(e);
            if (findEdgeCutOfSize(vList, eList, k, i + 1, out)) return true;
            out.remove(out.size() - 1);
        }
        return false;
    }

    private boolean isWeaklyConnectedWithoutEdges(Vector<Vertex> vList, Vector<Edge> removedEdges) {
        int n = vList.size();
        if (n <= 1) return true;

        Set<Vertex> visited = new HashSet<Vertex>();
        ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
        Vertex start = vList.firstElement();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Vertex u = queue.poll();
            for (Vertex w : u.undirectedNeighbors) {
                if (!edgeIsRemoved(u, w, removedEdges)) {
                    if (visited.add(w)) queue.add(w);
                }
            }
            for (Vertex w : u.outNeighbors) {
                if (!edgeIsRemoved(u, w, removedEdges)) {
                    if (visited.add(w)) queue.add(w);
                }
            }
            for (Vertex w : u.inNeighbors) {
                if (!edgeIsRemoved(w, u, removedEdges)) {
                    if (visited.add(w)) queue.add(w);
                }
            }
        }

        for (Vertex v : vList) {
            if (!visited.contains(v)) return false;
        }
        return true;
    }

    private boolean edgeIsRemoved(Vertex u, Vertex v, Vector<Edge> removedEdges) {
        for (Edge e : removedEdges) {
            if (e.vertex1 == u && e.vertex2 == v) return true;
            if (!e.directed && e.vertex1 == v && e.vertex2 == u) return true;
        }
        return false;
    }

    private class descendingWidthComparator implements Comparator {
        public int compare(Object v1, Object v2) {
            if (((Vector<Vertex>) v1).size() > (((Vector<Vertex>) v2).size())) return -1;
            else if (((Vector<Vertex>) v1).size() < (((Vector<Vertex>) v2).size())) return 1;
            else return 0;
        }
    }
}