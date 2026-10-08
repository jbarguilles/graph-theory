package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class GraphFileTest {

    static String file(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    static GraphFile.Data read(String... lines) throws GraphFile.FormatException {
        return GraphFile.read(file(lines));
    }

    @Test
    public void write_emptyGraph_isJustTheHeader() {
        assertEquals("graph-theory 1\n", GraphFile.write(new Vector<Vertex>(), new Vector<Edge>()));
    }

    @Test
    public void write_alwaysUsesTheFullForm() {
        Vertex a = new Vertex("a", 120, 200);
        a.isRoot = true;
        Vertex b = new Vertex("b", 300, 200);
        Vector<Vertex> vs = new Vector<Vertex>();
        vs.add(a);
        vs.add(b);
        Edge ab = new Edge(a, b, false);
        Edge ba = new Edge(b, a, true);
        ba.weight = 3;
        Vector<Edge> es = new Vector<Edge>();
        es.add(ab);
        es.add(ba);
        assertEquals(file("graph-theory 1",
                          "vertex a 120 200 root",
                          "vertex b 300 200",
                          "edge a b 1",
                          "arc b a 3"),
                     GraphFile.write(vs, es));
    }

    @Test
    public void roundTrip_mixedGraphWithParallelEdgesAndSelfLoops() throws Exception {
        String text = file("graph-theory 1",
                           "vertex a 120 200 root",
                           "vertex b 300 200",
                           "vertex c 210 340",
                           "edge a b 1",
                           "edge a b 5",
                           "arc a b 2",
                           "arc b c 3",
                           "edge c c 1",
                           "arc a a 4");
        GraphFile.Data d = GraphFile.read(text);
        assertEquals(text, GraphFile.write(d.vertices, d.edges));
        assertTrue(d.unplaced.isEmpty());
    }

    @Test
    public void read_buildsNeighbourListsLikeTheEditor() throws Exception {
        GraphFile.Data d = read("graph-theory 1",
                                "vertex a 0 0",
                                "vertex b 0 0",
                                "edge a b",
                                "arc b a",
                                "edge a a",
                                "arc b b");
        Vertex a = d.vertices.get(0);
        Vertex b = d.vertices.get(1);
        assertEquals(3, a.degree());      // {a,b} + undirected self-loop (2)
        assertEquals(1, a.inDegree());    // (b,a)
        assertEquals(0, a.outDegree());
        assertEquals(1, b.degree());
        assertEquals(1, b.inDegree());    // (b,b)
        assertEquals(2, b.outDegree());   // (b,a), (b,b)
    }

    @Test
    public void read_handWrittenForm_defaultsWeightAndListsUnplacedVertices() throws Exception {
        GraphFile.Data d = read("# a small example",
                                "graph-theory 1",
                                "",
                                "vertex a        # no position",
                                "vertex b 10 20",
                                "vertex c",
                                "edge a b",
                                "arc c a 7",
                                "arc b c");
        assertEquals(3, d.vertices.size());
        assertEquals(2, d.unplaced.size());
        assertSame(d.vertices.get(0), d.unplaced.get(0));
        assertSame(d.vertices.get(2), d.unplaced.get(1));
        assertEquals(10, d.vertices.get(1).location.x);
        assertEquals(20, d.vertices.get(1).location.y);
        assertFalse(d.edges.get(0).directed);
        assertEquals(1, d.edges.get(0).weight);
        assertTrue(d.edges.get(1).directed);
        assertEquals(7, d.edges.get(1).weight);
        assertTrue(d.edges.get(2).directed);
        assertEquals(1, d.edges.get(2).weight);
    }

    @Test
    public void read_ignoresAUtf8ByteOrderMark() throws Exception {
        GraphFile.Data d = GraphFile.read((char) 0xFEFF + "graph-theory 1\nvertex a 1 2\n");
        assertEquals(1, d.vertices.size());
        assertEquals("a", d.vertices.get(0).name);
    }

    @Test
    public void read_acceptsWindowsLineEndings() throws Exception {
        GraphFile.Data d = GraphFile.read("graph-theory 1\r\nvertex a 1 2\r\n");
        assertEquals("a", d.vertices.get(0).name);
        assertEquals(2, d.vertices.get(0).location.y);
    }

    @Test
    public void read_rootWithAndWithoutPosition() throws Exception {
        GraphFile.Data d = read("graph-theory 1", "vertex a root", "vertex b 5 6 root");
        assertTrue(d.vertices.get(0).isRoot);
        assertTrue(d.vertices.get(1).isRoot);   // separate components: no edge between them
        assertEquals(1, d.unplaced.size());
    }

    @Test
    public void read_aVertexMayBeNamedRoot() throws Exception {
        GraphFile.Data d = read("graph-theory 1", "vertex root");
        assertEquals("root", d.vertices.get(0).name);
        assertFalse(d.vertices.get(0).isRoot);
    }

    static void assertRejected(String expectedMessage, String... lines) {
        try {
            read(lines);
            fail("expected FormatException: " + expectedMessage);
        } catch (GraphFile.FormatException ex) {
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void rejects_emptyFile() {
        assertRejected("Line 1: empty file (expected 'graph-theory 1')", "", "# nothing here");
    }

    @Test
    public void rejects_missingHeader() {
        assertRejected("Line 1: not a graph file (expected 'graph-theory 1')", "vertex a");
    }

    @Test
    public void rejects_otherVersion() {
        assertRejected("Line 1: unsupported version (this app reads 'graph-theory 1' and 'graph-theory 2')", "graph-theory 3");
    }

    @Test
    public void rejects_unknownKeyword() {
        assertRejected("Line 2: unknown keyword 'node' (expected vertex, edge, arc or prefer)",
                       "graph-theory 1", "node a");
    }

    @Test
    public void write_withPreferences_isVersion2_withPreferLinesLast() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 10, 0), c = new Vertex("c", 20, 0);
        Vector<Vertex> vs = new Vector<Vertex>(java.util.Arrays.asList(a, b, c));
        Vector<Edge> es = new Vector<Edge>(java.util.Arrays.asList(new Edge(a, b, false), new Edge(c, a, true)));
        a.preferences = new Vector<Vertex>(java.util.Arrays.asList(c, b));
        assertEquals(file("graph-theory 2",
                          "vertex a 0 0", "vertex b 10 0", "vertex c 20 0",
                          "edge a b 1", "arc c a 1",
                          "prefer a c b"),
                     GraphFile.write(vs, es));
    }

    @Test
    public void roundTrip_preferences() throws Exception {
        String text = file("graph-theory 2",
                           "vertex a 0 0", "vertex b 10 0", "vertex c 20 0",
                           "edge a b 1", "edge a c 1",
                           "prefer a c b", "prefer b a");
        GraphFile.Data d = GraphFile.read(text);
        assertEquals(text, GraphFile.write(d.vertices, d.edges));
        assertNull(d.vertices.get(2).preferences);
    }

    @Test
    public void read_version1_stillReads() throws Exception {
        assertEquals(1, read("graph-theory 1", "vertex a").vertices.size());
    }

    @Test
    public void read_preferInVersion1_rejected() {
        assertRejected("Line 4: prefer lines need the header 'graph-theory 2'",
                "graph-theory 1", "vertex a", "vertex b", "prefer a b");
    }

    @Test
    public void read_preferBeforeAnEdge_rejected() {
        assertRejected("Line 5: vertex, edge and arc lines must come before prefer lines",
                "graph-theory 2", "vertex a", "vertex b", "prefer a b", "edge a b");
    }

    @Test
    public void read_preferMustNameExactlyTheNeighbours() {
        assertRejected("Line 7: 'a' must rank each of its neighbours exactly once: b, c",
                "graph-theory 2", "vertex a", "vertex b", "vertex c", "edge a b", "edge c a", "prefer a b");
        assertRejected("Line 5: 'a' must rank each of its neighbours exactly once: b",
                "graph-theory 2", "vertex a", "vertex b", "edge a b", "prefer a b b");
    }

    @Test
    public void read_preferForVertexWithNoNeighbours_rejected() {
        assertRejected("Line 4: 'a' has no neighbours, so it cannot have a prefer line",
                "graph-theory 2", "vertex a", "vertex b", "prefer a b");
    }

    @Test
    public void read_preferNamingUnknownVertex_rejected() {
        assertRejected("Line 5: unknown vertex 'q' (declare it with a vertex line first)",
                "graph-theory 2", "vertex a", "vertex b", "edge a b", "prefer a q");
    }

    @Test
    public void read_preferAcceptsAnArcInNeighbour() throws Exception {
        GraphFile.Data d = read("graph-theory 2", "vertex a", "vertex c", "arc c a", "prefer a c");
        assertEquals(java.util.Arrays.asList(d.vertices.get(1)), d.vertices.get(0).preferences);
    }

    @Test
    public void read_secondPreferForSameVertex_rejected() {
        assertRejected("Line 6: 'a' already has a prefer line",
                "graph-theory 2", "vertex a", "vertex b", "edge a b", "prefer a b", "prefer a b");
    }

    @Test
    public void read_preferWithNoNames_rejected() {
        assertRejected("Line 4: expected 'prefer <vertex> <neighbour> ...'",
                "graph-theory 2", "vertex a", "vertex b", "prefer a");
    }

    @Test
    public void rejects_unknownVertex() {
        assertRejected("Line 3: unknown vertex 'q' (declare it with a vertex line first)",
                       "graph-theory 1", "vertex a", "edge a q");
    }

    @Test
    public void rejects_duplicateVertex() {
        assertRejected("Line 3: vertex 'a' is already declared",
                       "graph-theory 1", "vertex a", "vertex a 1 1");
    }

    @Test
    public void rejects_invalidName() {
        assertRejected("Line 2: invalid vertex name 'abcde' (1-4 letters, digits or _)",
                       "graph-theory 1", "vertex abcde");
    }

    @Test
    public void rejects_negativeWeight() {
        assertRejected("Line 3: weight must not be negative",
                       "graph-theory 1", "vertex a", "edge a a -1");
    }

    @Test
    public void rejects_nonNumber() {
        assertRejected("Line 2: 'x' is not a whole number", "graph-theory 1", "vertex a x 5");
    }

    @Test
    public void rejects_vertexWithOneCoordinate() {
        assertRejected("Line 2: expected 'vertex <name> [<x> <y>] [root]'",
                       "graph-theory 1", "vertex a 5");
    }

    @Test
    public void rejects_edgeWithOneEnd() {
        assertRejected("Line 3: expected 'arc <from> <to> [<weight>]'",
                       "graph-theory 1", "vertex a", "arc a");
    }

    @Test
    public void rejects_twoRootsInOneComponent() {
        assertRejected("Line 4: 'c' is a second root in the same component as 'a'",
                       "graph-theory 1",
                       "vertex a root",
                       "vertex b",
                       "vertex c root",
                       "edge a b",
                       "arc b c");
    }
}
