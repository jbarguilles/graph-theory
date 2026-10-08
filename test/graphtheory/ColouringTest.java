package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColouringTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void triangleNeedsThree_arcsCountAsAdjacent() {
        assertEquals(3, Colouring.chromaticNumber(vs(a, b, c), es(und(a, b), arc(b, c), arc(a, c))));
    }

    @Test
    public void noEdgesIsOne_noVerticesIsZero() {
        assertEquals(1, Colouring.chromaticNumber(vs(a, b), es()));
        assertEquals(0, Colouring.chromaticNumber(vs(), es()));
    }

    @Test
    public void edgesWithAnEndpointOutsideTheVertices_areSkipped() {
        Vertex outsider = new Vertex("z", 0, 0);
        assertEquals(1, Colouring.chromaticNumber(vs(a, b), es(und(a, outsider), und(outsider, outsider))));
        assertEquals(2, Colouring.chromaticNumber(vs(a, b), es(und(a, b), und(b, outsider))));
    }

    @Test
    public void selfLoop_hasNoProperColouring() {
        assertEquals(Colouring.NO_PROPER_COLOURING, Colouring.chromaticNumber(vs(a, b), es(und(a, b), arc(b, b))));
    }

    @Test
    public void tooLarge() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i <= Colouring.CAP; i++) many.add(v("v" + i));
        assertEquals(Colouring.TOO_LARGE, Colouring.chromaticNumber(many, es()));
    }
}
