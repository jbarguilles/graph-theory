package graphtheory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ComponentsTest {

    private static Set<Vertex> set(Vertex... vs) {
        return new HashSet<Vertex>(Arrays.asList(vs));
    }

    @Test
    public void isolatedVertexIsItsOwnComponent() {
        Vertex a = new Vertex("a", 0, 0);
        assertEquals(set(a), Components.of(a, Arrays.<Edge>asList()));
    }

    @Test
    public void arcsCountBothWays() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vertex c = new Vertex("c", 0, 0);
        List<Edge> edges = Arrays.asList(new Edge(a, b, true), new Edge(c, b, true));
        assertEquals(set(a, b, c), Components.of(a, edges));
    }

    @Test
    public void parallelEdgesAndSelfLoopAreHandled() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        List<Edge> edges = Arrays.asList(new Edge(a, b, false), new Edge(a, b, false), new Edge(a, a, false));
        assertEquals(set(a, b), Components.of(b, edges));
    }

    @Test
    public void unconnectedVerticesAreExcluded() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vertex c = new Vertex("c", 0, 0);
        Vertex d = new Vertex("d", 0, 0);
        List<Edge> edges = Arrays.asList(new Edge(a, b, false), new Edge(c, d, true));
        assertEquals(set(a, b), Components.of(a, edges));
        assertEquals(set(c, d), Components.of(d, edges));
    }
}
