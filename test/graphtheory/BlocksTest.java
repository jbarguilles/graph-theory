package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlocksTest {
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d"), e = v("e");

    @Test
    public void parallelEdges_areNotBridges() {
        assertTrue(Blocks.bridges(vs(a, b), es(und(a, b), und(a, b))).isEmpty());
    }

    @Test
    public void path_everyEdgeIsABridge_arcsIgnoreDirection() {
        Edge ab = und(a, b), cb = arc(c, b);
        assertEquals(new java.util.HashSet<Edge>(Arrays.asList(ab, cb)),
                Blocks.bridges(vs(a, b, c), es(ab, cb)));
    }

    @Test
    public void selfLoop_isNeverABridge() {
        assertTrue(Blocks.bridges(vs(a), es(und(a, a))).isEmpty());
    }

    @Test
    public void bowtie_twoTriangleBlocks_sharingTheCutpoint() {
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c, d, e),
                es(und(a, b), und(b, c), und(c, a), und(c, d), und(d, e), und(e, c)));
        assertEquals(2, bs.size());
        assertEquals(Arrays.asList(a, b, c), bs.get(0).vertices);
        assertEquals(Arrays.asList(c, d, e), bs.get(1).vertices);
        assertEquals(3, bs.get(0).edges.size());
    }

    @Test
    public void parallelEdges_shareABlock_everyEdgeInExactlyOne() {
        Edge ab1 = und(a, b), ab2 = und(a, b), bc = und(b, c);
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c), es(ab1, ab2, bc));
        assertEquals(2, bs.size());
        int total = 0;
        for (Blocks.Block bl : bs) total += bl.edges.size();
        assertEquals(3, total);
        for (Blocks.Block bl : bs) {
            if (bl.edges.contains(ab1)) assertTrue(bl.edges.contains(ab2));
        }
    }

    @Test
    public void isolatedVertex_isABlockOnItsOwn_loopJoinsItsVertexBlock() {
        Edge loop = und(c, c);
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c, d), es(und(a, b), loop));
        assertEquals(3, bs.size());
        assertEquals(Arrays.asList(a, b), bs.get(0).vertices);
        boolean found = false;
        for (Blocks.Block bl : bs) {
            if (bl.vertices.equals(Arrays.asList(c))) { assertEquals(Arrays.asList(loop), bl.edges); found = true; }
        }
        assertTrue(found);
    }

    @Test
    public void nonseparable() {
        assertTrue(Blocks.isNonseparable(vs(a), es()));
        assertTrue(Blocks.isNonseparable(vs(a, b), es(und(a, b))));
        assertFalse(Blocks.isNonseparable(vs(a, b, c), es(und(a, b), und(b, c))));
        assertTrue(Blocks.isNonseparable(vs(a, b, c), es(und(a, b), und(b, c), arc(c, a))));
        assertFalse(Blocks.isNonseparable(vs(a, b), es()));
    }

    @Test
    public void blocksAreListedInVertexOrder_loopOnlyBlockFirst() {
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c), es(und(a, a), und(b, c)));
        assertEquals(2, bs.size());
        assertEquals(Arrays.asList(a), bs.get(0).vertices);
        assertEquals(Arrays.asList(b, c), bs.get(1).vertices);
    }

    @Test
    public void oppositeArcs_areNotBridges_andShareOneBlock() {
        Edge ab = arc(a, b), ba = arc(b, a);
        assertTrue(Blocks.bridges(vs(a, b), es(ab, ba)).isEmpty());
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b), es(ab, ba));
        assertEquals(1, bs.size());
        assertEquals(2, bs.get(0).edges.size());
    }

    @Test
    public void loopOnVertexOfLargerBlock_joinsItWithoutChangingVertices() {
        Edge loop = und(a, a);
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b), es(und(a, b), loop));
        assertEquals(1, bs.size());
        assertEquals(Arrays.asList(a, b), bs.get(0).vertices);
        assertTrue(bs.get(0).edges.contains(loop));
    }

    @Test
    public void nonseparable_moreCases() {
        assertTrue(Blocks.isNonseparable(vs(a, b), es(und(a, b), und(a, b))));
        assertTrue(Blocks.isNonseparable(vs(a), es(und(a, a))));
        assertFalse(Blocks.isNonseparable(vs(), es()));
    }

    @Test
    public void triangleWithPendantEdge() {
        Edge pendant = und(c, d);
        List<Vertex> v4 = vs(a, b, c, d);
        List<Edge> e4 = es(und(a, b), und(b, c), und(c, a), pendant);
        assertEquals(new java.util.HashSet<Edge>(Arrays.asList(pendant)), Blocks.bridges(v4, e4));
        List<Blocks.Block> bs = Blocks.blocks(v4, e4);
        assertEquals(2, bs.size());
        assertEquals(Arrays.asList(a, b, c), bs.get(0).vertices);
        assertEquals(Arrays.asList(c, d), bs.get(1).vertices);
    }
}
