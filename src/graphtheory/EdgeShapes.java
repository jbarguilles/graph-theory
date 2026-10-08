package graphtheory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each edge is drawn. Parallel edges between the same two vertices fan
 * out symmetrically and self-loops on one vertex nest above it, so every edge
 * can be seen and clicked on its own (CONTEXT.md, Display Conventions).
 * Drawing and hit-testing both use this, so what you see is what you click.
 */
public final class EdgeShapes {

    /** Gap between neighbouring control points in a fan; the curves' midpoints end up half this apart. */
    public static final double FAN_SPACING = 28;
    /** How close (pixels) a click must be to an edge to hit it. */
    public static final double TOLERANCE = 6.0;

    private static final int SAMPLES = 32;

    private final List<Edge> edges;
    private final Map<Edge, Double> fanOffset = new IdentityHashMap<Edge, Double>();

    private EdgeShapes(List<Edge> edges) {
        this.edges = edges;
    }

    /** Lays out the given edges. Order within a fan follows the list. */
    public static EdgeShapes of(List<Edge> edges) {
        EdgeShapes s = new EdgeShapes(edges);
        Map<String, List<Edge>> pairs = new LinkedHashMap<String, List<Edge>>();
        for (Edge e : edges) {
            if (e.isSelfLoop()) continue;
            String key = low(e).name + "\u0000" + high(e).name;
            List<Edge> group = pairs.get(key);
            if (group == null) {
                group = new ArrayList<Edge>();
                pairs.put(key, group);
            }
            group.add(e);
        }
        for (List<Edge> group : pairs.values()) {
            int n = group.size();
            for (int i = 0; i < n; i++) {
                s.fanOffset.put(group.get(i), (i - (n - 1) / 2.0) * FAN_SPACING);
            }
        }
        return s;
    }

    /** The endpoint with the smaller name; the pair's perpendicular is worked out from it. */
    private static Vertex low(Edge e) {
        return e.vertex1.name.compareTo(e.vertex2.name) <= 0 ? e.vertex1 : e.vertex2;
    }

    private static Vertex high(Edge e) {
        return low(e) == e.vertex1 ? e.vertex2 : e.vertex1;
    }

    /**
     * A non-loop edge as a quadratic curve {startX, startY, ctrlX, ctrlY, endX, endY}.
     * Start is on vertex1's outline and end on vertex2's, both pointing at the control point,
     * so an arrowhead at the end sits on the outline along the curve's tangent.
     */
    public double[] curve(Edge e) {
        Vertex a = low(e), b = high(e);
        double dx = b.location.x - a.location.x;
        double dy = b.location.y - a.location.y;
        double len = Math.hypot(dx, dy);
        double px = len == 0 ? 0 : -dy / len;
        double py = len == 0 ? -1 : dx / len;
        Double off = fanOffset.get(e);
        double o = off == null ? 0 : off;
        double cx = (a.location.x + b.location.x) / 2.0 + px * o;
        double cy = (a.location.y + b.location.y) / 2.0 + py * o;
        double[] s = towards(e.vertex1, cx, cy);
        double[] t = towards(e.vertex2, cx, cy);
        return new double[] { s[0], s[1], cx, cy, t[0], t[1] };
    }

    /** The point on v's outline in the direction of (x, y). */
    private static double[] towards(Vertex v, double x, double y) {
        double dx = x - v.location.x, dy = y - v.location.y;
        double len = Math.hypot(dx, dy);
        if (len == 0) return new double[] { v.location.x, v.location.y };
        return new double[] { v.location.x + dx / len * Vertex.RADIUS,
                              v.location.y + dy / len * Vertex.RADIUS };
    }

    /** The point at parameter t (0..1) on a curve from curve(). */
    public static double[] at(double[] c, double t) {
        double mt = 1 - t;
        return new double[] {
            mt * mt * c[0] + 2 * mt * t * c[2] + t * t * c[4],
            mt * mt * c[1] + 2 * mt * t * c[3] + t * t * c[5]
        };
    }

    /** Distance from (x, y) to the drawn edge. */
    public double distance(Edge e, double x, double y) {
        double[] c = curve(e);
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= SAMPLES; i++) {
            double[] p = at(c, i / (double) SAMPLES);
            best = Math.min(best, Math.hypot(x - p[0], y - p[1]));
        }
        return best;
    }

    /** The edge closest to (x, y) if it is within TOLERANCE, else null. Ties go to the earlier edge. */
    public Edge nearest(double x, double y) {
        Edge best = null;
        double bestD = TOLERANCE;
        for (Edge e : edges) {
            double d = distance(e, x, y);
            if (d < bestD || (best == null && d == bestD)) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }
}
