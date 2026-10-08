package graphtheory;

import java.util.ArrayList;
import java.util.List;

/** Everything the side panel says, kept out of Swing so it can be tested. Terms follow CONTEXT.md. */
public final class PanelText {

    public static final String INFINITY = "\u221E";

    /** Column names of the table shown when several vertices are selected. */
    public static final String[] VERTEX_COLUMNS = { "Vertex", "Deg", "In", "Out", "" };

    private PanelText() {}

    /** Name/value rows describing one selected vertex (CONTEXT.md, Node Properties). */
    public static String[][] vertexProperties(Vertex v) {
        return new String[][] {
            { "Name",       v.name },
            { "Degree",     String.valueOf(v.degree()) },
            { "In-Degree",  String.valueOf(v.inDegree()) },
            { "Out-Degree", String.valueOf(v.outDegree()) },
            { "Isolated",   yesNo(v.isIsolated()) },
            { "Self-loop",  yesNo(v.hasSelfLoop()) },
            { "Cutpoint",   yesNo(v.isCutpoint) },
            { "Root",       yesNo(v.isRoot) },
        };
    }

    /** One row of the several-vertices table, matching VERTEX_COLUMNS. */
    public static Object[] vertexRow(Vertex v) {
        return new Object[] { v.name, v.degree(), v.inDegree(), v.outDegree(), flags(v) };
    }

    /** The properties that hold, e.g. "root, cutpoint". Empty if none. */
    public static String flags(Vertex v) {
        List<String> fs = new ArrayList<String>();
        if (v.isRoot)        fs.add("root");
        if (v.isCutpoint)    fs.add("cutpoint");
        if (v.isIsolated())  fs.add("isolated");
        if (v.hasSelfLoop()) fs.add("self-loop");
        return join(fs, ", ");
    }

    /** The pair's fact lines, shown above its path list. */
    public static String[] pairFacts(PairSummary s) {
        List<String> lines = new ArrayList<String>();
        lines.add("Ordered pair: (" + s.from.name + ", " + s.to.name + ")");
        lines.add("Adjacent: " + yesNo(s.adjacent));
        lines.add("Reachable: " + yesNo(s.reachable()));
        lines.add("Distance: " + orInfinity(s.distance()));
        if (s.weighted) lines.add("Weighted distance: " + orInfinity(s.weightedDistance()));
        lines.add("Paths: " + s.paths.size());
        return lines.toArray(new String[0]);
    }

    /**
     * One row of the path list, numbers first so they stay visible in a narrow panel,
     * e.g. "1. len 1 \u00b7 weight 3 \u00b7 geodesic, lightest   u -{u,v}-> v".
     */
    public static String pathRow(int index, Walk p, PairSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append(index + 1).append(". len ").append(p.length());
        if (s.weighted) sb.append(" \u00b7 weight ").append(p.weight());
        List<String> tags = new ArrayList<String>();
        if (s.isGeodesic(p)) tags.add("geodesic");
        if (s.weighted && s.isLightest(p)) tags.add("lightest");
        if (!tags.isEmpty()) sb.append(" \u00b7 ").append(join(tags, ", "));
        sb.append("   ").append(p);
        return sb.toString();
    }

    /** "Built walk", or for a Find result e.g. "Euler tour (found)". foundKind is the Find command's kind, e.g. "Euler Tour". */
    public static String walkHeading(String foundKind) {
        if (foundKind == null) return "Built walk";
        return foundKind.charAt(0) + foundKind.substring(1).toLowerCase() + " (found)";
    }

    /** The walk's fact lines, shown under the walk itself. */
    public static String[] walkFacts(Walk w, boolean weighted) {
        List<String> lines = new ArrayList<String>();
        lines.add("Length: " + w.length());
        if (weighted) lines.add("Weight: " + w.weight());
        lines.add("Kind: " + w.kindName());
        lines.add("Trail: " + yesNo(w.isTrail()) + "   Path: " + yesNo(w.isPath()));
        lines.add("Closed: " + yesNo(w.isClosed()) + "   Circuit: " + yesNo(w.isCircuit())
                + "   Cycle: " + yesNo(w.isCycle()));
        return lines.toArray(new String[0]);
    }

    static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private static String orInfinity(int n) {
        return n < 0 ? INFINITY : String.valueOf(n);
    }

    static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
