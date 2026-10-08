package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PanelTextTest {

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);
    private final Vertex m = new Vertex("m", 0, 0);

    private static Edge und(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, false);
        e.setWeight(w);
        return e;
    }

    @Test
    public void vertexProperties_useGlossaryNames() {
        u.isRoot = true;
        String[][] rows = PanelText.vertexProperties(u);
        assertEquals("Name", rows[0][0]);
        assertEquals("u", rows[0][1]);
        assertEquals("Degree", rows[1][0]);
        assertEquals("Root", rows[7][0]);
        assertEquals("yes", rows[7][1]);
    }

    @Test
    public void flags_listOnlyWhatHolds() {
        u.isRoot = true;
        u.isCutpoint = true;
        assertEquals("root, cutpoint, isolated", PanelText.flags(u));
        assertEquals("isolated", PanelText.flags(v));
    }

    @Test
    public void pairFacts_unweighted_noWeightedDistanceLine() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(und(u, v, 1))));
        assertEquals(Arrays.asList("Ordered pair: (u, v)", "Adjacent: yes", "Reachable: yes",
                "Distance: 1", "Paths: 1"), Arrays.asList(PanelText.pairFacts(s)));
    }

    @Test
    public void pairFacts_unreachable_infinity() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>());
        assertTrue(Arrays.asList(PanelText.pairFacts(s)).contains("Distance: \u221E"));
    }

    @Test
    public void pairFacts_weighted_bothDistances() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(
                und(u, v, 10), und(u, m, 1), und(m, v, 1))));
        java.util.List<String> facts = Arrays.asList(PanelText.pairFacts(s));
        assertTrue(facts.contains("Distance: 1"));
        assertTrue(facts.contains("Weighted distance: 2"));
    }

    @Test
    public void pathRow_weighted_showsWeightAndTags() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(
                und(u, v, 10), und(u, m, 1), und(m, v, 1))));
        assertEquals("1. len 2 \u00b7 weight 2 \u00b7 lightest   u -{u,m}-> m -{m,v}-> v",
                PanelText.pathRow(0, s.paths.get(0), s));
        assertEquals("2. len 1 \u00b7 weight 10 \u00b7 geodesic   u -{u,v}-> v",
                PanelText.pathRow(1, s.paths.get(1), s));
    }

    @Test
    public void pathRow_weighted_bothTags() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(und(u, v, 3))));
        assertEquals("1. len 1 \u00b7 weight 3 \u00b7 geodesic, lightest   u -{u,v}-> v",
                PanelText.pathRow(0, s.paths.get(0), s));
    }

    @Test
    public void pathRow_unweighted_noWeightNoLightest() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(und(u, v, 1))));
        assertEquals("1. len 1 \u00b7 geodesic   u -{u,v}-> v", PanelText.pathRow(0, s.paths.get(0), s));
    }

    @Test
    public void pathRow_noTags_justTheLength() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(
                und(u, v, 1), und(u, m, 1), und(m, v, 1))));
        assertEquals("2. len 2   u -{u,m}-> m -{m,v}-> v", PanelText.pathRow(1, s.paths.get(1), s));
    }

    @Test
    public void walkHeading_builtOrFound() {
        assertEquals("Built walk", PanelText.walkHeading(null));
        assertEquals("Euler tour (found)", PanelText.walkHeading("Euler Tour"));
        assertEquals("Hamiltonian cycle (found)", PanelText.walkHeading("Hamiltonian Cycle"));
    }

    @Test
    public void walkFacts_weightOnlyWhenWeighted() {
        Edge uv = und(u, v, 3);
        Walk w = new Walk(u);
        w.extend(uv);
        assertEquals(Arrays.asList("Length: 1", "Kind: path", "Trail: yes   Path: yes",
                "Closed: no   Circuit: no   Cycle: no"), Arrays.asList(PanelText.walkFacts(w, false)));
        assertEquals("Weight: 3", PanelText.walkFacts(w, true)[1]);
    }

    @Test
    public void neighbours_notation() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0),
               d = new Vertex("d", 0, 0), e = new Vertex("e", 0, 0);
        java.util.List<Vertex> vs = java.util.Arrays.asList(a, b, c, d, e);
        java.util.List<Edge> es = java.util.Arrays.asList(new Edge(a, b, false), new Edge(b, a, false),
                new Edge(a, c, false), new Edge(a, d, true), new Edge(e, a, true), new Edge(a, a, true));
        assertEquals("b \u00d72, c, \u2192a, \u2192d, \u2190e", PanelText.neighbours(a, vs, es));
    }

    @Test
    public void neighbours_isolatedIsBlank_undirectedLoopIsOwnName() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        java.util.List<Vertex> vs = java.util.Arrays.asList(a, b);
        assertEquals("", PanelText.neighbours(b, vs, java.util.Arrays.asList(new Edge(a, a, false))));
        assertEquals("a", PanelText.neighbours(a, vs, java.util.Arrays.asList(new Edge(a, a, false))));
    }

    @Test
    public void verticesRow_matchesColumns() {
        Vertex a = new Vertex("a", 0, 0);
        Object[] row = PanelText.verticesRow(a, java.util.Arrays.asList(a), new java.util.Vector<Edge>());
        assertEquals(PanelText.VERTICES_COLUMNS.length, row.length);
        assertEquals("a", row[0]);
        assertEquals(Integer.valueOf(0), row[1]);
        assertEquals("yes", row[4]);
    }
}
