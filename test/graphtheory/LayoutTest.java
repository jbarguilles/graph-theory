package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class LayoutTest {

    private static Vector<Vertex> at(int... xy) {
        Vector<Vertex> vs = new Vector<Vertex>();
        for (int i = 0; i < xy.length; i += 2) vs.add(new Vertex("v" + i, xy[i], xy[i + 1]));
        return vs;
    }

    @Test
    public void clampInto_pullsOutsideVerticesToTheMargin() {
        Vector<Vertex> vs = at(-50, 5000, 900, -3);
        Layout.clampInto(vs, 800, 600);
        assertEquals(25, vs.get(0).location.x);
        assertEquals(575, vs.get(0).location.y);
        assertEquals(775, vs.get(1).location.x);
        assertEquals(25, vs.get(1).location.y);
    }

    @Test
    public void clampInto_leavesInsideVerticesAlone() {
        Vector<Vertex> vs = at(400, 300);
        Layout.clampInto(vs, 800, 600);
        assertEquals(400, vs.get(0).location.x);
        assertEquals(300, vs.get(0).location.y);
    }

    @Test
    public void arrangeOnCircle_spacesVerticesEvenlyAroundTheCentre() {
        Vector<Vertex> vs = at(0, 0, 0, 0, 0, 0, 0, 0);
        Layout.arrangeOnCircle(vs, 800, 600);   // centre (400,300), radius 600/5 = 120
        assertEquals(520, vs.get(0).location.x);
        assertEquals(300, vs.get(0).location.y);
        assertEquals(400, vs.get(1).location.x);
        assertEquals(420, vs.get(1).location.y);
        assertEquals(280, vs.get(2).location.x);
        assertEquals(300, vs.get(2).location.y);
        assertEquals(400, vs.get(3).location.x);
        assertEquals(180, vs.get(3).location.y);
    }

    @Test
    public void arrangeOnCircle_emptyListIsANoOp() {
        Layout.arrangeOnCircle(new Vector<Vertex>(), 800, 600);
    }
}
