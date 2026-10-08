package graphtheory;

import java.util.ArrayList;
import java.util.List;

/**
 * Degree, in-degree and out-degree distributions (CONTEXT.md, Degree distribution).
 * Each kind uses the glossary's own degree, so the charts agree with the Node
 * Properties table and the side panel.
 */
public final class DegreeDistribution {

    public enum Kind {
        DEGREE("Degree distribution"),
        IN_DEGREE("In-degree distribution"),
        OUT_DEGREE("Out-degree distribution");

        public final String title;

        Kind(String title) {
            this.title = title;
        }

        int of(Vertex v) {
            switch (this) {
                case IN_DEGREE:  return v.inDegree();
                case OUT_DEGREE: return v.outDegree();
                default:         return v.degree();
            }
        }
    }

    private DegreeDistribution() {}

    /** counts[k] = number of vertices whose degree of this kind is k, for k = 0 .. the largest. Empty if no vertices. */
    public static int[] counts(List<Vertex> vs, Kind kind) {
        int max = -1;
        for (Vertex v : vs) max = Math.max(max, kind.of(v));
        int[] counts = new int[max + 1];
        for (Vertex v : vs) counts[kind.of(v)]++;
        return counts;
    }

    /**
     * The distributions this graph has: degree if it has an undirected edge (or no
     * edges at all), in-degree and out-degree if it has an arc.
     */
    public static List<Kind> kindsFor(List<Edge> es) {
        boolean undirected = false, directed = false;
        for (Edge e : es) {
            if (e.directed) directed = true;
            else undirected = true;
        }
        List<Kind> kinds = new ArrayList<Kind>();
        if (undirected || !directed) kinds.add(Kind.DEGREE);
        if (directed) {
            kinds.add(Kind.IN_DEGREE);
            kinds.add(Kind.OUT_DEGREE);
        }
        return kinds;
    }
}
