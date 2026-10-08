package graphtheory;

import java.awt.Color;
import java.util.List;

public class Edge {

    public Vertex vertex1;
    public Vertex vertex2;
    public boolean directed;
    public boolean wasFocused;
    public boolean wasClicked;
    public int weight = 1;
    public boolean isBridge;

    // Set by Canvas when the Remove Tool hovers over this edge.
    public boolean removeHover = false;

    // Set by Canvas before drawing when this edge is on the built walk or the
    // highlighted pair path. null = not highlighted / no label.
    public Color highlight = null;
    public String stepLabel = null;

    public Edge(Vertex v1, Vertex v2, boolean directed) {
        vertex1 = v1;
        vertex2 = v2;
        this.directed = directed;
    }

    /** Change this edge's weight. */
    public void setWeight(int w) {
        this.weight = w;
    }

    public boolean isSelfLoop() {
        return vertex1 == vertex2;
    }

    /** A weighted graph has at least one edge whose weight is not 1 (CONTEXT.md). */
    public static boolean isWeighted(List<Edge> edges) {
        for (Edge e : edges) {
            if (e.weight != 1) return true;
        }
        return false;
    }
}
