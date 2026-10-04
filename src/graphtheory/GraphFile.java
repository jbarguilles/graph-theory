package graphtheory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Reads and writes the .graph text format (ADR 0004):
 *
 *   graph-theory 1
 *   vertex <name> [<x> <y>] [root]
 *   edge <a> <b> [<weight>]      undirected
 *   arc  <a> <b> [<weight>]      directed a → b
 *
 * '#' starts a comment; blank lines are ignored.
 */
public final class GraphFile {

    public static final String HEADER = "graph-theory 1";

    private GraphFile() {}

    /** A problem in a graph file; the message starts with "Line N: ". */
    public static class FormatException extends Exception {
        public final int line;

        public FormatException(int line, String message) {
            super("Line " + line + ": " + message);
            this.line = line;
        }
    }

    /** A graph read from a file. unplaced = vertices declared without coordinates (left at 0,0). */
    public static class Data {
        public final Vector<Vertex> vertices = new Vector<Vertex>();
        public final Vector<Edge> edges = new Vector<Edge>();
        public final Vector<Vertex> unplaced = new Vector<Vertex>();
    }

    /** The full form: every vertex with coordinates, every edge with its weight. */
    public static String write(List<Vertex> vertices, List<Edge> edges) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (Vertex v : vertices) {
            sb.append("vertex ").append(v.name)
              .append(' ').append(v.location.x)
              .append(' ').append(v.location.y);
            if (v.isRoot) sb.append(" root");
            sb.append('\n');
        }
        for (Edge e : edges) {
            sb.append(e.directed ? "arc " : "edge ")
              .append(e.vertex1.name).append(' ')
              .append(e.vertex2.name).append(' ')
              .append(e.weight).append('\n');
        }
        return sb.toString();
    }

    public static Data read(String text) throws FormatException {
        Data data = new Data();
        Map<String, Vertex> byName = new HashMap<String, Vertex>();
        Map<Vertex, Integer> rootLine = new HashMap<Vertex, Integer>();
        String[] lines = text.split("\r?\n", -1);
        boolean sawHeader = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = lines[i];
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] t = line.split("\\s+");

            if (!sawHeader) {
                if (!t[0].equals("graph-theory")) {
                    throw new FormatException(lineNo, "not a graph file (expected '" + HEADER + "')");
                }
                if (t.length != 2 || !t[1].equals("1")) {
                    throw new FormatException(lineNo, "unsupported version (this app reads '" + HEADER + "')");
                }
                sawHeader = true;
            } else if (t[0].equals("vertex")) {
                readVertex(t, lineNo, data, byName, rootLine);
            } else if (t[0].equals("edge") || t[0].equals("arc")) {
                readEdge(t, lineNo, data, byName);
            } else {
                throw new FormatException(lineNo,
                        "unknown keyword '" + t[0] + "' (expected vertex, edge or arc)");
            }
        }
        if (!sawHeader) throw new FormatException(1, "empty file (expected '" + HEADER + "')");
        checkRoots(data, rootLine);
        return data;
    }

    private static void readVertex(String[] t, int lineNo, Data data,
                                   Map<String, Vertex> byName, Map<Vertex, Integer> rootLine)
            throws FormatException {
        int n = t.length;
        boolean root = n > 2 && t[n - 1].equals("root");
        if (root) n--;
        if (n != 2 && n != 4) {
            throw new FormatException(lineNo, "expected 'vertex <name> [<x> <y>] [root]'");
        }
        String name = t[1];
        if (!VertexNames.isValid(name)) {
            throw new FormatException(lineNo,
                    "invalid vertex name '" + name + "' (" + VertexNames.RULE + ")");
        }
        if (byName.containsKey(name)) {
            throw new FormatException(lineNo, "vertex '" + name + "' is already declared");
        }
        Vertex v;
        if (n == 4) {
            v = new Vertex(name, parseInt(t[2], lineNo), parseInt(t[3], lineNo));
        } else {
            v = new Vertex(name, 0, 0);
            data.unplaced.add(v);
        }
        v.isRoot = root;
        if (root) rootLine.put(v, lineNo);
        byName.put(name, v);
        data.vertices.add(v);
    }

    private static void readEdge(String[] t, int lineNo, Data data, Map<String, Vertex> byName)
            throws FormatException {
        if (t.length != 3 && t.length != 4) {
            throw new FormatException(lineNo, "expected '" + t[0] + " <from> <to> [<weight>]'");
        }
        Vertex a = lookup(t[1], lineNo, byName);
        Vertex b = lookup(t[2], lineNo, byName);
        boolean directed = t[0].equals("arc");
        Edge e = new Edge(a, b, directed);
        if (t.length == 4) {
            int w = parseInt(t[3], lineNo);
            if (w < 0) throw new FormatException(lineNo, "weight must not be negative");
            e.weight = w;
        }
        if (directed) {
            a.outNeighbors.add(b);
            b.inNeighbors.add(a);
        } else {
            a.addUndirectedNeighbor(b);
            if (a != b) b.addUndirectedNeighbor(a);
        }
        data.edges.add(e);
    }

    private static Vertex lookup(String name, int lineNo, Map<String, Vertex> byName)
            throws FormatException {
        Vertex v = byName.get(name);
        if (v == null) {
            throw new FormatException(lineNo,
                    "unknown vertex '" + name + "' (declare it with a vertex line first)");
        }
        return v;
    }

    private static int parseInt(String s, int lineNo) throws FormatException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new FormatException(lineNo, "'" + s + "' is not a whole number");
        }
    }

    /** At most one root per connected component (edges taken as undirected). */
    private static void checkRoots(Data data, Map<Vertex, Integer> rootLine) throws FormatException {
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        for (Vertex v : data.vertices) parent.put(v, v);
        for (Edge e : data.edges) parent.put(find(parent, e.vertex1), find(parent, e.vertex2));

        Map<Vertex, Vertex> rootOfComponent = new HashMap<Vertex, Vertex>();
        for (Vertex v : data.vertices) {
            if (!v.isRoot) continue;
            Vertex component = find(parent, v);
            Vertex earlier = rootOfComponent.get(component);
            if (earlier != null) {
                throw new FormatException(rootLine.get(v), "'" + v.name
                        + "' is a second root in the same component as '" + earlier.name + "'");
            }
            rootOfComponent.put(component, v);
        }
    }

    private static Vertex find(Map<Vertex, Vertex> parent, Vertex v) {
        while (parent.get(v) != v) v = parent.get(v);
        return v;
    }
}
