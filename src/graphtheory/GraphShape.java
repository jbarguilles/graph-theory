package graphtheory;

import java.util.ArrayList;
import java.util.List;

/**
 * Which vertices and edges a graph has (by identity) and the edges' weights.
 * Positions and names are not part of it, so moving or renaming a vertex keeps
 * the shape. Used to drop a found walk once the graph it was found in changes
 * (CONTEXT.md, Built and found walks).
 */
public final class GraphShape {

    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private final int[] weights;

    private GraphShape(List<Vertex> vs, List<Edge> es) {
        vertices = new ArrayList<Vertex>(vs);
        edges = new ArrayList<Edge>(es);
        weights = new int[es.size()];
        for (int i = 0; i < weights.length; i++) weights[i] = es.get(i).weight;
    }

    public static GraphShape of(List<Vertex> vs, List<Edge> es) {
        return new GraphShape(vs, es);
    }

    /** Same vertex and edge objects, in the same order, with the same weights. */
    public boolean matches(List<Vertex> vs, List<Edge> es) {
        if (vs.size() != vertices.size() || es.size() != edges.size()) return false;
        for (int i = 0; i < vertices.size(); i++) if (vs.get(i) != vertices.get(i)) return false;
        for (int i = 0; i < edges.size(); i++) {
            if (es.get(i) != edges.get(i) || es.get(i).weight != weights[i]) return false;
        }
        return true;
    }
}
