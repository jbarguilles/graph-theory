package graphtheory;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 100, 0);

    @Test
    public void isWeighted_noEdges_false() {
        assertFalse(Edge.isWeighted(Collections.<Edge>emptyList()));
    }

    @Test
    public void isWeighted_allWeightOne_false() {
        assertFalse(Edge.isWeighted(Arrays.asList(new Edge(a, b, false), new Edge(b, a, true))));
    }

    @Test
    public void isWeighted_oneEdgeNotOne_true() {
        Edge heavy = new Edge(a, b, true);
        heavy.setWeight(5);
        assertTrue(Edge.isWeighted(Arrays.asList(new Edge(a, b, false), heavy)));
    }

    @Test
    public void isWeighted_weightZero_true() {
        Edge free = new Edge(a, b, false);
        free.setWeight(0);
        assertTrue(Edge.isWeighted(Arrays.asList(free)));
    }
}
