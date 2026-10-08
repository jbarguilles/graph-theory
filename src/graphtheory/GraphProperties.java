/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author mk
 */
public class GraphProperties {

    // ---- Degree distributions (CONTEXT.md, Degree distribution) ----

    private static final int DIST_PLOT_H = 140;
    private static final int DIST_LEFT_PAD = 60;
    private static final int DIST_TOP_PAD = 22;      // room for the chart's title
    private static final int DIST_BOTTOM_PAD = 40;   // tick labels and the axis label
    private static final int DIST_GAP = 20;          // between stacked charts
    private static final Color DIST_BAR = new Color(90, 110, 130);

    /** Height drawDegreeDistributions uses for this graph: one chart per distribution it has. */
    public static int degreeDistributionsHeight(Vector<Edge> eList) {
        int perChart = DIST_TOP_PAD + DIST_PLOT_H + DIST_BOTTOM_PAD + DIST_GAP;
        return DegreeDistribution.kindsFor(eList).size() * perChart;
    }

    /**
     * Draws the degree distribution, or the in-degree and out-degree distributions,
     * or all three for a mixed graph, stacked, as bar charts of the fraction of
     * vertices with each degree. They share one y-axis scale so they can be compared.
     * Returns the height used (always degreeDistributionsHeight(eList)).
     */
    public static int drawDegreeDistributions(Graphics g0, Vector<Vertex> vList, Vector<Edge> eList,
                                       int x, int y, int plotW) {
        java.util.List<DegreeDistribution.Kind> kinds = DegreeDistribution.kindsFor(eList);
        int n = vList.size();

        // One y-axis scale for every chart: the largest fraction, rounded up to 0.05 (at least 0.10).
        int maxCount = 0;
        for (DegreeDistribution.Kind k : kinds) {
            for (int c : DegreeDistribution.counts(vList, k)) maxCount = Math.max(maxCount, c);
        }
        int ticks = n == 0 ? 2 : Math.max(2, (int) Math.ceil(20.0 * maxCount / n - 1e-9));
        double axisMax = ticks * 0.05;

        java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cy = y;
            for (DegreeDistribution.Kind k : kinds) {
                drawDistributionChart(g, k, DegreeDistribution.counts(vList, k), n,
                        x, cy, plotW, ticks, axisMax);
                cy += DIST_TOP_PAD + DIST_PLOT_H + DIST_BOTTOM_PAD + DIST_GAP;
            }
        } finally {
            g.dispose();
        }
        return degreeDistributionsHeight(eList);
    }

    private static void drawDistributionChart(java.awt.Graphics2D g, DegreeDistribution.Kind kind, int[] counts,
                                       int n, int x, int y, int plotW, int ticks, double axisMax) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        int plotX = x + DIST_LEFT_PAD;
        int plotY = y + DIST_TOP_PAD;
        int plotBottom = plotY + DIST_PLOT_H;

        g.setColor(Color.BLACK);
        g.drawString(kind.title, x, y + fm.getAscent());

        g.setColor(new Color(0xF4, 0xF4, 0xF4));
        g.fillRect(plotX, plotY, plotW, DIST_PLOT_H);

        // Gridlines every 0.05; labels on at most about 7 of them so they stay readable.
        int labelEvery = (int) Math.ceil(ticks / 7.0);
        for (int i = 0; i <= ticks; i++) {
            double f = i * 0.05;
            int gy = plotBottom - (int) Math.round(f / axisMax * DIST_PLOT_H);
            if (i > 0) {
                g.setColor(new Color(0xDD, 0xDD, 0xDD));
                g.drawLine(plotX, gy, plotX + plotW, gy);
            }
            if (i % labelEvery != 0) continue;
            g.setColor(Color.BLACK);
            String label = String.format("%.2f", f);
            g.drawString(label, plotX - fm.stringWidth(label) - 6, gy + 4);
            g.drawLine(plotX - 3, gy, plotX, gy);
        }

        if (n > 0) {
            int slots = counts.length;
            double slotW = (double) plotW / slots;
            double gap = slotW * 0.15;
            for (int d = 0; d < slots; d++) {
                if (counts[d] == 0) continue;
                int barH = (int) Math.round((double) counts[d] / n / axisMax * DIST_PLOT_H);
                int barX = plotX + (int) Math.round(d * slotW + gap / 2);
                int barW = Math.max(1, (int) Math.round(slotW - gap));
                g.setColor(DIST_BAR);
                g.fillRect(barX, plotBottom - barH, barW, barH);
            }

            // Degree labels, thinned so they never overlap.
            int widest = fm.stringWidth(String.valueOf(slots - 1)) + 6;
            int step = Math.max(1, (int) Math.ceil(widest / slotW));
            g.setColor(Color.BLACK);
            for (int d = 0; d < slots; d += step) {
                int cx = plotX + (int) Math.round((d + 0.5) * slotW);
                String label = String.valueOf(d);
                g.drawString(label, cx - fm.stringWidth(label) / 2, plotBottom + 14);
                g.drawLine(cx, plotBottom, cx, plotBottom + 3);
            }
        }

        g.setColor(Color.BLACK);
        g.drawRect(plotX, plotY, plotW, DIST_PLOT_H);

        String xLabel = kind == DegreeDistribution.Kind.DEGREE ? "degree"
                : kind == DegreeDistribution.Kind.IN_DEGREE ? "in-degree" : "out-degree";
        g.drawString(xLabel, plotX + plotW / 2 - fm.stringWidth(xLabel) / 2, plotBottom + DIST_BOTTOM_PAD - 6);

        java.awt.Graphics2D r = (java.awt.Graphics2D) g.create();
        try {
            String yLabel = "fraction of vertices";
            int textW = fm.stringWidth(yLabel);
            r.rotate(-Math.PI / 2, x + 12, plotY + DIST_PLOT_H / 2);
            r.drawString(yLabel, (float) (x + 12 - textW / 2.0), (float) (plotY + DIST_PLOT_H / 2 + fm.getAscent() / 2.0));
        } finally {
            r.dispose();
        }
    }

    /** Tooltip for the charts drawn at (x, y) with plotW: "degree 3: 4 vertices (0.40)", or null off the bars' columns. */
    public static String distributionTooltip(Vector<Vertex> vList, Vector<Edge> eList, int x, int y, int plotW,
                                             int mx, int my) {
        int n = vList.size();
        if (n == 0 || my < y) return null;
        java.util.List<DegreeDistribution.Kind> kinds = DegreeDistribution.kindsFor(eList);
        int perChart = DIST_TOP_PAD + DIST_PLOT_H + DIST_BOTTOM_PAD + DIST_GAP;
        int i = (my - y) / perChart;
        if (i >= kinds.size()) return null;
        int plotY = y + i * perChart + DIST_TOP_PAD;
        int plotX = x + DIST_LEFT_PAD;
        if (my < plotY || my > plotY + DIST_PLOT_H || mx < plotX || mx >= plotX + plotW) return null;
        DegreeDistribution.Kind kind = kinds.get(i);
        int[] counts = DegreeDistribution.counts(vList, kind);
        int d = (int) ((mx - plotX) / ((double) plotW / counts.length));
        if (d < 0 || d >= counts.length) return null;
        String label = kind == DegreeDistribution.Kind.DEGREE ? "degree"
                : kind == DegreeDistribution.Kind.IN_DEGREE ? "in-degree" : "out-degree";
        return label + " " + d + ": " + counts[d] + (counts[d] == 1 ? " vertex" : " vertices")
                + String.format(" (%.2f)", (double) counts[d] / n);
    }

    // ---- Graph coloring ----

    /** Colours the vertices greedily, highest degree first, into each vertex's colorId. */
    public void greedyColoring(Vector<Vertex> vList) {
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

        for (int i = 0; i < n; i++) vList.get(i).colorId = color[i];
    }

    public void clearColoring(Vector<Vertex> vList) {
        for (Vertex v : vList) v.colorId = -1;
    }

    private List<Vertex> allNeighborsForColoring(Vertex u) {
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex v : u.undirectedNeighbors) if (!result.contains(v)) result.add(v);
        for (Vertex v : u.outNeighbors)        if (!result.contains(v)) result.add(v);
        for (Vertex v : u.inNeighbors)         if (!result.contains(v)) result.add(v);
        return result;
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
}