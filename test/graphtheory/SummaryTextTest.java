package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class SummaryTextTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static Edge w(Edge e, int weight) { e.setWeight(weight); return e; }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    private static String line(List<SummaryText.Section> ss, String label) {
        for (SummaryText.Section s : ss) for (String[] l : s.lines) if (l[0].equals(label)) return l[1];
        return null;
    }

    private static List<String> titles(List<SummaryText.Section> ss) {
        List<String> t = new java.util.ArrayList<String>();
        for (SummaryText.Section s : ss) t.add(s.title);
        return t;
    }

    @Test
    public void sixSectionsInOrder() {
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a), es(), null));
        assertEquals(Arrays.asList("Size and order", "Connectivity", "Structure", "Blocks",
                "Traversals", "Colouring and matching"), titles(ss));
    }

    @Test
    public void edgeSet_showsWeightsOnlyWhenWeighted_neverTruncates() {
        List<Edge> many = new Vector<Edge>();
        for (int i = 0; i < 8; i++) many.add(und(a, b));
        assertEquals("{a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), many, null)), "E"));
        assertEquals("{a, b}:3, (b, a):1",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), es(w(und(a, b), 3), arc(b, a)), null)), "E"));
    }

    @Test
    public void strongConnectivityLineOnlyWithArcs() {
        assertNull(line(SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b)), null)), "Strongly connected"));
        assertEquals("no", line(SummaryText.sections(new PropertiesReport(vs(a, b), es(arc(a, b)), null)), "Strongly connected"));
    }

    @Test
    public void completeGraph_saysThereIsNoVertexCut() {
        String k = line(SummaryText.sections(new PropertiesReport(vs(a, b, c),
                es(und(a, b), und(b, c), und(c, a)), null)), "Vertex connectivity \u03ba(G)");
        assertEquals("2 (no vertex cut: every two vertices are adjacent)", k);
    }

    @Test
    public void reasonsInBrackets() {
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b), und(a, b)), null));
        assertEquals("\u2014 (not simple)", line(ss, "Density"));
        assertEquals("\u2014 (no preference list: a, b)", line(ss, "Stable matching"));
        List<SummaryText.Section> loop = SummaryText.sections(new PropertiesReport(vs(a), es(und(a, a)), null));
        assertEquals("\u2014 (self-loop)", line(loop, "Chromatic number \u03c7(G)"));
        assertEquals("no (self-loop)", line(loop, "Bipartite"));
    }

    @Test
    public void tree_namesItsRoot() {
        a.isRoot = true;
        assertEquals("rooted tree (root a)",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b)), null)), "Tree"));
    }

    @Test
    public void blocksLine() {
        assertEquals("2 (0 nontrivial): {a, b}, {c}",
                line(SummaryText.sections(new PropertiesReport(vs(a, b, c), es(und(a, b)), null)), "Blocks"));
    }

    @Test
    public void html_hasSectionHeadingsAndRows() {
        String html = SummaryText.html(SummaryText.sections(new PropertiesReport(vs(a), es(), null)));
        assertTrue(html.contains("<h3>Size and order</h3>"));
        assertTrue(html.contains("<tr><td valign='top'><b>Order</b></td><td>1</td></tr>"));
    }

    @Test
    public void emptyGraph_everyDashGivesAReason() {
        for (SummaryText.Section s : SummaryText.sections(new PropertiesReport(vs(), es(), null)))
            for (String[] l : s.lines) {
                if (l[1].startsWith("\u2014"))
                    assertTrue(l[0] + " = " + l[1], l[1].matches("\u2014 \\(.+\\)"));
            }
    }

    private static List<Vertex> cycleVertices(int n) {
        List<Vertex> out = new Vector<Vertex>();
        for (int i = 0; i < n; i++) out.add(v("v" + i));
        return out;
    }

    private static List<Edge> cycleEdges(List<Vertex> vs) {
        List<Edge> out = new Vector<Edge>();
        for (int i = 0; i < vs.size(); i++) out.add(und(vs.get(i), vs.get((i + 1) % vs.size())));
        return out;
    }

    private static Vector<Vertex> prefs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    @Test
    public void completeBipartite_withLists_namesSidesAndProposer() {
        Vertex d = v("d");
        a.preferences = prefs(c, d);
        b.preferences = prefs(c, d);
        c.preferences = prefs(a, b);
        d.preferences = prefs(a, b);
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a, b, c, d),
                es(und(a, c), und(a, d), und(b, c), und(b, d)), null));
        assertEquals("yes (K_{2,2}, sides {a, b} and {c, d})", line(ss, "Bipartite"));
        String stable = line(ss, "Stable matching");
        assertTrue(stable, stable.startsWith("2: "));
        assertTrue(stable, stable.endsWith(" (side {a, b} proposes)"));
    }

    @Test
    public void pathPlusIsolatedVertex_isAForestWithItsRoot() {
        a.isRoot = true;
        assertEquals("forest (2 trees; roots a)",
                line(SummaryText.sections(new PropertiesReport(vs(a, b, c), es(und(a, b)), null)), "Tree"));
    }

    @Test
    public void sixteenCycle_chromaticNumberIsCapped() {
        List<Vertex> cyc = cycleVertices(16);
        assertEquals("\u2014 (more than 15 vertices)", line(SummaryText.sections(
                new PropertiesReport(cyc, cycleEdges(cyc), null)), "Chromatic number \u03c7(G)"));
    }

    @Test
    public void twentyOneCycle_hamiltonAndMaximumMatchingAreCapped() {
        List<Vertex> cyc = cycleVertices(21);
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(cyc, cycleEdges(cyc), null));
        assertEquals("\u2014 (more than 20 vertices)", line(ss, "Hamiltonian path"));
        assertEquals("\u2014 (more than 20 vertices)", line(ss, "Maximum matching"));
    }

    @Test
    public void cutWording() {
        List<SummaryText.Section> path = SummaryText.sections(new PropertiesReport(vs(a, b, c),
                es(und(a, b), und(b, c)), null));
        assertEquals("1 (minimum vertex cut: {b})", line(path, "Vertex connectivity \u03ba(G)"));
        assertEquals("1 (minimum edge cut: the edge {a, b})", line(path, "Edge connectivity \u03bb(G)"));
        List<SummaryText.Section> tri = SummaryText.sections(new PropertiesReport(vs(a, b, c),
                es(und(a, b), und(b, c), und(c, a)), null));
        String l = line(tri, "Edge connectivity \u03bb(G)");
        assertTrue(l, l.startsWith("2 (minimum edge cut: the edges "));
    }
}
