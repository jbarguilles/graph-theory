package graphtheory;

import java.util.ArrayList;
import java.util.List;

/** The Overview summary: titled sections of (label, value) lines (CONTEXT.md terms; yes/no). */
public final class SummaryText {

    public static final String NONE = "\u2014";
    public static final String NO_VERTICES = NONE + " (no vertices)";

    private SummaryText() {}

    public static final class Section {
        public final String title;
        /** Each {label, value}. */
        public final List<String[]> lines = new ArrayList<String[]>();

        Section(String title) { this.title = title; }

        void add(String label, String value) { lines.add(new String[] { label, value }); }
    }

    public static List<Section> sections(PropertiesReport r) {
        int n = r.vertices.size();
        boolean none = n == 0;
        List<Section> out = new ArrayList<Section>();

        Section size = new Section("Size and order");
        size.add("Order", "" + n);
        size.add("Size", "" + r.edges.size());
        size.add("Magnitude", "" + (n + r.edges.size()));
        size.add("V", vertexSet(r.vertices));
        size.add("E", edges(r.edges, r.weighted));
        out.add(size);

        Section con = new Section("Connectivity");
        con.add("Connected", none ? NO_VERTICES : r.connected ? "yes" : "no (" + r.components.size() + " components)");
        con.add("Components", none ? NO_VERTICES : sets(r.components));
        if (r.stronglyConnected != null) con.add("Strongly connected", yesNo(r.stronglyConnected));
        String kappa = "" + r.vertexCut.size;
        if (!r.vertexCut.members.isEmpty()) kappa += " (minimum vertex cut: " + vertexSet(r.vertexCut.members) + ")";
        else if (r.connected && n > 1) kappa += " (no vertex cut: every two vertices are adjacent)";
        con.add("Vertex connectivity \u03ba(G)", none ? NO_VERTICES : kappa);
        String lambda = "" + r.edgeCut.size;
        if (!r.edgeCut.members.isEmpty()) lambda += " (minimum edge cut: the edge" + (r.edgeCut.members.size() > 1 ? "s " : " ")
                + edges(r.edgeCut.members, false) + ")";
        con.add("Edge connectivity \u03bb(G)", none ? NO_VERTICES : lambda);
        List<Edge> bridges = new ArrayList<Edge>();
        for (Edge e : r.edges) if (r.bridges.contains(e)) bridges.add(e);
        con.add("Bridges", bridges.isEmpty() ? "0" : bridges.size() + ": " + edges(bridges, false));
        out.add(con);

        Section st = new Section("Structure");
        st.add("Simple", none ? NO_VERTICES : yesNo(r.simple));
        st.add("Empty", none ? NO_VERTICES : yesNo(r.empty));
        st.add("Complete", none ? NO_VERTICES : yesNo(r.complete));
        st.add("Density", r.density != null ? String.format("%.2f", r.density)
                : NONE + (n < 2 ? " (fewer than two vertices)" : " (not simple)"));
        st.add("Cyclic", none ? NO_VERTICES : yesNo(r.cyclic));
        st.add("Tree", none ? NO_VERTICES : treeLine(r));
        st.add("Star", none ? NO_VERTICES : yesNo(r.star));
        st.add("Bipartite", none ? NO_VERTICES : bipartiteLine(r));
        out.add(st);

        Section bl = new Section("Blocks");
        bl.add("Nonseparable", none ? NO_VERTICES : yesNo(r.nonseparable));
        int nontrivial = 0;
        List<List<Vertex>> blockSets = new ArrayList<List<Vertex>>();
        for (Blocks.Block b : r.blocks) {
            if (b.vertices.size() >= 3) nontrivial++;
            blockSets.add(b.vertices);
        }
        bl.add("Blocks", none ? NO_VERTICES : r.blocks.size() + " (" + nontrivial + " nontrivial): " + sets(blockSets));
        out.add(bl);

        Section tr = new Section("Traversals");
        String big = NONE + " (more than " + Traversals.HAMILTON_VERTEX_CAP + " vertices)";
        tr.add("Euler trail", yesNo(r.eulerTrail));
        tr.add("Euler tour", yesNo(r.eulerTour));
        tr.add("Hamiltonian path", r.hamiltonTooLarge ? big : yesNo(r.hamiltonianPath));
        tr.add("Hamiltonian cycle", r.hamiltonTooLarge ? big : yesNo(r.hamiltonianCycle));
        out.add(tr);

        Section cm = new Section("Colouring and matching");
        cm.add("Chromatic number \u03c7(G)", none ? NO_VERTICES
                : r.chromatic == Colouring.NO_PROPER_COLOURING ? NONE + " (self-loop)"
                : r.chromatic == Colouring.TOO_LARGE ? NONE + " (more than " + Colouring.CAP + " vertices)"
                : "" + r.chromatic);
        cm.add("Maximal matching", none ? NO_VERTICES : matching(r.maximal));
        String tooMany = NONE + " (more than " + Matchings.MAXIMUM_VERTEX_CAP + " vertices)";
        cm.add("Maximum matching", none ? NO_VERTICES : r.maximum == null ? tooMany : matching(r.maximum));
        cm.add("Perfect matching", none ? NO_VERTICES : r.maximum == null ? tooMany : yesNo(r.perfect));
        cm.add("Stable matching", stableLine(r));
        out.add(cm);
        return out;
    }

    /** The summary as HTML for a read-only, selectable JEditorPane. Names are [A-Za-z0-9_], so no escaping is needed. */
    public static String html(List<Section> sections) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif'>");
        for (Section s : sections) {
            sb.append("<h3>").append(s.title).append("</h3><table>");
            for (String[] l : s.lines) {
                sb.append("<tr><td valign='top'><b>").append(l[0]).append("</b></td><td>")
                  .append(l[1]).append("</td></tr>");
            }
            sb.append("</table>");
        }
        return sb.append("</body></html>").toString();
    }

    private static String treeLine(PropertiesReport r) {
        List<String> roots = new ArrayList<String>();
        for (Vertex v : r.vertices) if (v.isRoot) roots.add(v.name);
        if (r.tree) return roots.size() == 1 ? "rooted tree (root " + roots.get(0) + ")" : "tree";
        if (!r.forest) return "no";
        String kind = roots.size() == r.components.size() ? "rooted forest" : "forest";
        String trees = r.components.size() + " trees";
        return kind + " (" + trees + (roots.isEmpty() ? "" : "; roots " + PanelText.join(roots, ", ")) + ")";
    }

    private static String bipartiteLine(PropertiesReport r) {
        if (r.sides == null) {
            for (Edge e : r.edges) if (e.vertex1 == e.vertex2) return "no (self-loop)";
            return "no";
        }
        String k = r.completeBipartite
                ? "K_{" + r.sides.get(0).size() + "," + r.sides.get(1).size() + "}, " : "";
        return "yes (" + k + "sides " + vertexSet(r.sides.get(0)) + " and " + vertexSet(r.sides.get(1)) + ")";
    }

    private static String stableLine(PropertiesReport r) {
        if (r.vertices.isEmpty()) return NO_VERTICES;
        if (r.sides == null) return NONE + " (not bipartite)";
        if (!r.missingPreferences.isEmpty()) {
            List<String> names = new ArrayList<String>();
            for (Vertex v : r.missingPreferences) names.add(v.name);
            return NONE + " (no preference list: " + PanelText.join(names, ", ") + ")";
        }
        List<Vertex> side = r.sides.get(1).contains(r.proposer) ? r.sides.get(1) : r.sides.get(0);
        return matching(r.stable) + " (side " + vertexSet(side) + " proposes)";
    }

    private static String matching(List<Edge> m) {
        return m.isEmpty() ? "0" : m.size() + ": " + edges(m, false);
    }

    static String vertexSet(List<Vertex> vs) {
        List<String> names = new ArrayList<String>();
        for (Vertex v : vs) names.add(v.name);
        return "{" + PanelText.join(names, ", ") + "}";
    }

    private static String sets(List<List<Vertex>> sets) {
        List<String> parts = new ArrayList<String>();
        for (List<Vertex> s : sets) parts.add(vertexSet(s));
        return PanelText.join(parts, ", ");
    }

    /** {a, b} for an edge, (a, b) for an arc, ":w" after each when weighted. */
    static String edges(List<Edge> es, boolean weighted) {
        List<String> parts = new ArrayList<String>();
        for (Edge e : es) {
            String s = e.directed ? "(" + e.vertex1.name + ", " + e.vertex2.name + ")"
                                  : "{" + e.vertex1.name + ", " + e.vertex2.name + "}";
            parts.add(weighted ? s + ":" + e.weight : s);
        }
        return PanelText.join(parts, ", ");
    }

    private static String yesNo(boolean b) { return PanelText.yesNo(b); }
}
