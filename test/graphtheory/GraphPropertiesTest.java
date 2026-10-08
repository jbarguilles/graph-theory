package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class GraphPropertiesTest {

    private void addUndirectedEdge(Vertex a, Vertex b) {
        a.undirectedNeighbors.add(b);
        b.undirectedNeighbors.add(a);
    }

    @Test
    public void cutpoints_pathOfThree_onlyMiddleIsCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        new GraphProperties().computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertTrue(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    @Test
    public void cutpoints_triangle_noCutpoints() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);
        addUndirectedEdge(c, a);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        new GraphProperties().computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertFalse(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    @Test
    public void cutpoints_selfLoopOnMiddle_middleStillCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);
        b.undirectedNeighbors.add(b); // self-loop on b

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        new GraphProperties().computeCutpoints(vList);

        assertTrue(b.isCutpoint);
    }

    @Test
    public void cutpoints_directedPathOfThree_middleIsCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        // directed a -> b -> c
        a.outNeighbors.add(b); b.inNeighbors.add(a);
        b.outNeighbors.add(c); c.inNeighbors.add(b);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        new GraphProperties().computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertTrue(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    @Test
    public void cutpoints_singleVertex_notCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vector<Vertex> vList = new Vector<>();
        vList.add(a);

        new GraphProperties().computeCutpoints(vList);

        assertFalse(a.isCutpoint);
    }

    @Test
    public void degreeDistributions_mixedGraph_drawsThreeChartsOfTheAdvertisedHeight() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        addUndirectedEdge(a, b);
        a.outNeighbors.add(b);
        b.inNeighbors.add(a);
        Vector<Vertex> vs = new Vector<Vertex>(java.util.Arrays.asList(a, b));
        Vector<Edge> es = new Vector<Edge>(java.util.Arrays.asList(new Edge(a, b, false), new Edge(a, b, true)));

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(600, 900,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        int used = new GraphProperties().drawDegreeDistributions(g, vs, es, 10, 20, 300);
        g.dispose();

        assertEquals(GraphProperties.degreeDistributionsHeight(es), used);
        assertEquals(3 * GraphProperties.degreeDistributionsHeight(new Vector<Edge>()), used);
    }

    @Test
    public void degreeDistributions_noVertices_stillReportsItsHeight() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 100,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        int used = new GraphProperties().drawDegreeDistributions(g, new Vector<Vertex>(), new Vector<Edge>(), 10, 20, 300);
        g.dispose();
        assertEquals(GraphProperties.degreeDistributionsHeight(new Vector<Edge>()), used);
    }
}
