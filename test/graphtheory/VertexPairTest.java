package graphtheory;

import org.junit.Test;
import java.util.Arrays;
import java.util.Vector;
import static org.junit.Assert.*;

public class VertexPairTest {

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);
    private final Vertex m = new Vertex("m", 0, 0);

    /** Creates an undirected edge and wires the neighbor lists like Canvas does. */
    private Edge und(Vertex x, Vertex y) {
        x.undirectedNeighbors.add(y);
        y.undirectedNeighbors.add(x);
        return new Edge(x, y, false);
    }

    /** Creates an arc and wires the neighbor lists like Canvas does. */
    private Edge arc(Vertex x, Vertex y) {
        x.outNeighbors.add(y);
        y.inNeighbors.add(x);
        return new Edge(x, y, true);
    }

    private Vector<Edge> edges(Edge... es) {
        return new Vector<Edge>(Arrays.asList(es));
    }

    @Test
    public void edgePaths_mixedParallelEdges_areTwoDistinctPaths() {
        Edge undUV = und(u, v);
        Edge arcUV = arc(u, v);
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(undUV, arcUV));

        assertEquals(2, paths.size());
        assertEquals(1, paths.get(0).length());
        assertEquals(1, paths.get(1).length());
        assertNotSame(paths.get(0).edges().get(0), paths.get(1).edges().get(0));
        java.util.List<Edge> firsts = Arrays.asList(
                paths.get(0).edges().get(0), paths.get(1).edges().get(0));
        assertTrue(firsts.contains(undUV));
        assertTrue(firsts.contains(arcUV));
    }

    @Test
    public void edgePaths_antiparallelArcs_onlyForwardArcUsed() {
        Edge forward = arc(u, v);
        Edge backward = arc(v, u);
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(forward, backward));

        assertEquals(1, paths.size());
        assertEquals(1, paths.get(0).length());
        assertSame(forward, paths.get(0).edges().get(0));
    }

    @Test
    public void edgePaths_arcPointingBackwards_noPath() {
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(arc(v, u)));
        assertTrue(paths.isEmpty());
    }

    @Test
    public void edgePaths_sortedShortestFirst() {
        Edge um = und(u, m);
        Edge mv = und(m, v);
        Edge uv = und(u, v);
        // longer path's edges listed first so the DFS finds it first
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(um, mv, uv));

        assertEquals(2, paths.size());
        assertEquals(1, paths.get(0).length());
        assertEquals(2, paths.get(1).length());
        assertEquals("u -{u,m}-> m -{m,v}-> v", paths.get(1).toString());
    }

    @Test
    public void edgePaths_ignoreSelfLoopsAndNeverRevisitVertices() {
        Edge loop = und(u, u);
        Edge um = und(u, m);
        Edge mv = und(m, v);
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(loop, um, mv));

        assertEquals(1, paths.size());
        assertTrue(paths.get(0).isPath());
    }

    @Test
    public void edgePaths_unreachable_empty() {
        Vector<Walk> paths = new VertexPair(u, v).generateEdgePaths(edges(und(u, m)));
        assertTrue(paths.isEmpty());
    }

    @Test
    public void vertexPaths_mixedParallelEdges_collapsedToOneVertexSequence() {
        und(u, v);
        arc(u, v);
        VertexPair vp = new VertexPair(u, v);
        vp.generateVertexDisjointPaths();

        assertEquals(1, vp.pathList.size());
        int maxWidth = 0;
        for (Vector<Vector<Vertex>> c : vp.VertexDisjointContainer) {
            maxWidth = Math.max(maxWidth, c.size());
        }
        assertEquals(1, maxWidth);
    }
}
