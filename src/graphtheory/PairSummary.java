package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * What the side panel says about an ordered pair (from, to): adjacency, every
 * from–to path as an edge-aware walk (ADR 0002), distance and weighted distance
 * (CONTEXT.md). Computed once when the pair or the graph changes, not on repaint.
 */
public final class PairSummary {

    public final Vertex from;
    public final Vertex to;
    /** Some edge can be crossed in one step from 'from' to 'to'. */
    public final boolean adjacent;
    /** The graph is weighted (some edge weight is not 1). */
    public final boolean weighted;
    /** Every from–to path: lightest first on a weighted graph, else shortest first; ties keep search order. */
    public final List<Walk> paths;

    private final int distance;
    private final int weightedDistance;

    public PairSummary(Vertex from, Vertex to, Vector<Edge> edges) {
        this.from = from;
        this.to = to;
        this.weighted = Edge.isWeighted(edges);
        this.adjacent = !Walk.edgesBetween(from, to, edges).isEmpty();

        // generateEdgePaths is already shortest first; a stable sort by weight keeps that among ties.
        List<Walk> ps = new ArrayList<Walk>(new VertexPair(from, to).generateEdgePaths(edges));
        if (weighted) {
            Collections.sort(ps, new Comparator<Walk>() {
                public int compare(Walk p, Walk q) {
                    return Integer.compare(p.weight(), q.weight());
                }
            });
        }
        this.paths = Collections.unmodifiableList(ps);

        // Every shortest or lightest walk can be shortened to a path, so the path list decides both.
        int d = -1, wd = -1;
        for (Walk p : ps) {
            if (d < 0 || p.length() < d) d = p.length();
            if (wd < 0 || p.weight() < wd) wd = p.weight();
        }
        distance = d;
        weightedDistance = wd;
    }

    public boolean reachable() {
        return !paths.isEmpty();
    }

    /** Edges on a shortest from–to walk (CONTEXT.md, Distance), or -1 if unreachable. */
    public int distance() {
        return distance;
    }

    /** Smallest weight of a from–to walk (CONTEXT.md, Weighted distance), or -1 if unreachable. */
    public int weightedDistance() {
        return weightedDistance;
    }

    /** p is a geodesic: no from–to walk has fewer edges. */
    public boolean isGeodesic(Walk p) {
        return p.length() == distance;
    }

    /** p is a lightest path: no from–to walk weighs less. */
    public boolean isLightest(Walk p) {
        return p.weight() == weightedDistance;
    }
}
