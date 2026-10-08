package graphtheory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DegreeDistributionTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Vertex c = new Vertex("c", 0, 0);

    /** An undirected edge, wired into the neighbour lists the way Canvas does. */
    private static Edge und(Vertex x, Vertex y) {
        x.undirectedNeighbors.add(y);
        if (x != y) y.undirectedNeighbors.add(x);
        return new Edge(x, y, false);
    }

    /** An arc, wired into the neighbour lists the way Canvas does. */
    private static Edge arc(Vertex x, Vertex y) {
        x.outNeighbors.add(y);
        y.inNeighbors.add(x);
        return new Edge(x, y, true);
    }

    @Test
    public void counts_degree_usesUndirectedDegreeOnly() {
        // a -> b is an arc, {b, c} is undirected: b has degree 1, not 2.
        arc(a, b);
        und(b, c);
        int[] counts = DegreeDistribution.counts(Arrays.asList(a, b, c), DegreeDistribution.Kind.DEGREE);
        assertArrayEquals(new int[] { 1, 2 }, counts);   // a: 0;  b, c: 1
    }

    @Test
    public void counts_inAndOutDegree() {
        arc(a, b);
        arc(a, c);
        List<Vertex> vs = Arrays.asList(a, b, c);
        assertArrayEquals(new int[] { 1, 2 }, DegreeDistribution.counts(vs, DegreeDistribution.Kind.IN_DEGREE));
        assertArrayEquals(new int[] { 2, 0, 1 }, DegreeDistribution.counts(vs, DegreeDistribution.Kind.OUT_DEGREE));
    }

    @Test
    public void counts_selfLoopAddsTwoToDegree() {
        und(a, a);
        assertArrayEquals(new int[] { 0, 0, 1 },
                DegreeDistribution.counts(Arrays.asList(a), DegreeDistribution.Kind.DEGREE));
    }

    @Test
    public void counts_noVertices_empty() {
        assertEquals(0, DegreeDistribution.counts(Collections.<Vertex>emptyList(),
                DegreeDistribution.Kind.DEGREE).length);
    }

    @Test
    public void kinds_undirectedOnly_degree() {
        assertEquals(Arrays.asList(DegreeDistribution.Kind.DEGREE),
                DegreeDistribution.kindsFor(Arrays.asList(und(a, b))));
    }

    @Test
    public void kinds_noEdges_degree() {
        assertEquals(Arrays.asList(DegreeDistribution.Kind.DEGREE),
                DegreeDistribution.kindsFor(Collections.<Edge>emptyList()));
    }

    @Test
    public void kinds_directedOnly_inAndOut() {
        assertEquals(Arrays.asList(DegreeDistribution.Kind.IN_DEGREE, DegreeDistribution.Kind.OUT_DEGREE),
                DegreeDistribution.kindsFor(Arrays.asList(arc(a, b))));
    }

    @Test
    public void kinds_mixed_allThree() {
        assertEquals(Arrays.asList(DegreeDistribution.Kind.DEGREE,
                        DegreeDistribution.Kind.IN_DEGREE, DegreeDistribution.Kind.OUT_DEGREE),
                DegreeDistribution.kindsFor(Arrays.asList(und(a, b), arc(b, c))));
    }

    @Test
    public void kind_titlesUseGlossaryTerms() {
        assertEquals("Degree distribution", DegreeDistribution.Kind.DEGREE.title);
        assertEquals("In-degree distribution", DegreeDistribution.Kind.IN_DEGREE.title);
        assertEquals("Out-degree distribution", DegreeDistribution.Kind.OUT_DEGREE.title);
    }
}
