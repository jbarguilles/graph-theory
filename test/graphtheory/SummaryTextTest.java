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
                es(und(a, b), und(b, c), und(c, a)), null)), "Vertex connectivity κ(G)");
        assertEquals("2 (no vertex cut: every two vertices are adjacent)", k);
    }

    @Test
    public void reasonsInBrackets() {
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b), und(a, b)), null));
        assertEquals("— (not simple)", line(ss, "Density"));
        assertEquals("— (no preference list: a, b)", line(ss, "Stable matching"));
        List<SummaryText.Section> loop = SummaryText.sections(new PropertiesReport(vs(a), es(und(a, a)), null));
        assertEquals("— (self-loop)", line(loop, "Chromatic number χ(G)"));
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
    public void html_escapesNothingSurprising_andHasHeadings() {
        String html = SummaryText.html(SummaryText.sections(new PropertiesReport(vs(a), es(), null)));
        assertTrue(html.contains("<h3>Size and order</h3>"));
        assertTrue(html.contains("Order"));
    }
}
