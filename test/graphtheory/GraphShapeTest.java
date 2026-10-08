package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphShapeTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Edge ab = new Edge(a, b, false);
    private final Vector<Vertex> vs = new Vector<Vertex>(Arrays.asList(a, b));
    private final Vector<Edge> es = new Vector<Edge>(Arrays.asList(ab));

    @Test
    public void unchanged_matches() {
        assertTrue(GraphShape.of(vs, es).matches(vs, es));
    }

    @Test
    public void movingOrRenaming_stillMatches() {
        GraphShape s = GraphShape.of(vs, es);
        a.location.x = 300;
        b.name = "z";
        assertTrue(s.matches(vs, es));
    }

    @Test
    public void weightChanged_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        ab.setWeight(5);
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void edgeAddedOrRemoved_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        es.add(new Edge(b, a, true));
        assertFalse(s.matches(vs, es));
        es.remove(1);
        es.remove(0);
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void vertexAdded_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        vs.add(new Vertex("c", 0, 0));
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void sameShapeButNewObjects_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        List<Edge> copy = Arrays.asList(new Edge(a, b, false));
        assertFalse(s.matches(vs, copy));
    }
}
