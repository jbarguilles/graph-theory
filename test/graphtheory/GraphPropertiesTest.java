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
}
