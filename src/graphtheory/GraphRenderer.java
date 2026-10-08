package graphtheory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Draws a graph. The Graph canvas, the Properties thumbnail and the Induced
 * Subgraph window all use it. Draws in graph coordinates; apply a transform
 * (see fit) first to scale or move. Colours follow CONTEXT.md, Display Conventions.
 */
public final class GraphRenderer {

    public static final Color OUTLINE  = new Color(0x33, 0x33, 0x33);
    public static final Color EDGE     = new Color(0x44, 0x44, 0x44);
    public static final Color SELECT   = new Color(30, 100, 220);
    public static final Color HOVER    = new Color(120, 170, 240);
    public static final Color REMOVE   = new Color(220, 0, 0);
    public static final Color BRIDGE   = new Color(150, 0, 200);
    public static final Color ROOT     = new Color(0, 170, 0);
    public static final Color CUTPOINT = new Color(255, 140, 0);
    public static final Color ISOLATED = Color.GRAY;
    public static final Color CUT      = new Color(200, 0, 120);

    private static final Color GLOW         = new Color(30, 100, 220, 70);
    private static final Color LABEL_TEXT   = new Color(80, 80, 80);
    private static final Color LABEL_BORDER = new Color(200, 200, 200);
    private static final Font  LABEL_FONT   = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    public static final int NAME_MAX = 14;
    public static final int NAME_MIN = 9;

    /** What to draw besides the graph itself. */
    public static class Options {
        /** Show selection, hover and remove-hover (the Graph canvas only). */
        public boolean interaction;
        /** Minimum vertex cut, drawn as dashed rings. */
        public Set<Vertex> cutVertices = Collections.emptySet();
        /** Minimum edge cut, drawn as dashed edges. */
        public Set<Edge> cutEdges = Collections.emptySet();
    }

    private GraphRenderer() {}

    /** Draws on a copy of g, so the caller's colour, stroke, font and rendering hints are untouched. */
    public static void paint(Graphics2D g0, List<Vertex> vertices, List<Edge> edges, Options o) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            EdgeShapes shapes = EdgeShapes.of(edges);
            boolean weighted = Edge.isWeighted(edges);
            for (Edge e : edges) drawEdge(g, shapes, e, o);
            for (Edge e : edges) drawEdgeLabel(g, shapes, e, weighted);
            for (Vertex v : vertices) drawVertex(g, v, o);
            // After the vertices, so rings and the selection glow never paint over an arrowhead.
            for (Edge e : edges) if (e.directed) drawArrowhead(g, shapes, e, o);
        } finally {
            g.dispose();
        }
    }

    /** Scales (never up) and centres the vertices' bounding box in a w×h area, keeping margin free. */
    public static AffineTransform fit(List<Vertex> vs, int w, int h, int margin) {
        AffineTransform t = new AffineTransform();
        if (vs.isEmpty()) return t;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Vertex v : vs) {
            minX = Math.min(minX, v.location.x);
            minY = Math.min(minY, v.location.y);
            maxX = Math.max(maxX, v.location.x);
            maxY = Math.max(maxY, v.location.y);
        }
        double sw = Math.max(1, maxX - minX);
        double sh = Math.max(1, maxY - minY);
        double availW = Math.max(1, w - 2.0 * margin);
        double availH = Math.max(1, h - 2.0 * margin);
        double s = Math.min(1.0, Math.min(availW / sw, availH / sh));
        t.translate(w / 2.0 - (minX + maxX) / 2.0 * s, h / 2.0 - (minY + maxY) / 2.0 * s);
        t.scale(s, s);
        return t;
    }

    /** One colour per meaning; the first state that applies wins. */
    static Color edgeColor(Edge e, Options o) {
        if (o.interaction && e.removeHover) return REMOVE;
        if (o.interaction && e.wasClicked)  return SELECT;
        if (o.interaction && e.wasFocused)  return HOVER;
        if (o.cutEdges.contains(e))         return CUT;
        if (e.highlight != null)            return e.highlight;
        if (e.isBridge)                     return BRIDGE;
        return EDGE;
    }

    private static void drawEdge(Graphics2D g, EdgeShapes shapes, Edge e, Options o) {
        Color c = edgeColor(e, o);
        float width = c == EDGE ? 1.5f : 3f;
        g.setColor(c);
        g.setStroke(c == CUT ? dashed(width) : new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (e.isSelfLoop()) {
            double[] l = shapes.loop(e);
            g.draw(circle(l[0], l[1], l[2]));
        } else {
            double[] cv = shapes.curve(e);
            g.draw(new QuadCurve2D.Double(cv[0], cv[1], cv[2], cv[3], cv[4], cv[5]));
        }
    }

    private static void drawArrowhead(Graphics2D g, EdgeShapes shapes, Edge e, Options o) {
        g.setColor(edgeColor(e, o));
        if (e.isSelfLoop()) {
            double[] l = shapes.loop(e);
            // clockwise, at the top of the loop
            arrowhead(g, l[0], l[1] - l[2], 0.0, 9);
        } else {
            double[] cv = shapes.curve(e);
            arrowhead(g, cv[4], cv[5], Math.atan2(cv[5] - cv[3], cv[4] - cv[2]), 12);
        }
    }

    private static void arrowhead(Graphics2D g, double tipX, double tipY, double angle, double size) {
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - size * Math.cos(angle - Math.PI / 7), tipY - size * Math.sin(angle - Math.PI / 7));
        head.lineTo(tipX - size * Math.cos(angle + Math.PI / 7), tipY - size * Math.sin(angle + Math.PI / 7));
        head.closePath();
        g.fill(head);
    }

    /** Weight (on a weighted graph) and walk step numbers, in one box on the edge's midpoint. */
    private static void drawEdgeLabel(Graphics2D g, EdgeShapes shapes, Edge e, boolean weighted) {
        String weight = weighted ? String.valueOf(e.weight) : "";
        String step = e.stepLabel == null ? "" : e.stepLabel;
        if (weight.isEmpty() && step.isEmpty()) return;
        String first = weight.isEmpty() || step.isEmpty() ? weight : weight + " ";

        g.setFont(LABEL_FONT);
        FontMetrics fm = g.getFontMetrics();
        double bw = fm.stringWidth(first + step) + 8;

        // (x, y) is the box centre. A loop's box starts just right of the loop's top, whose
        // height steps by 2 * LOOP_STEP per nested loop, so the boxes stack without overlapping.
        double x, y;
        if (e.isSelfLoop()) {
            double[] l = shapes.loop(e);
            x = l[0] + 10 + bw / 2;
            y = l[1] - l[2];
        } else {
            double[] p = EdgeShapes.at(shapes.curve(e), 0.5);
            x = p[0];
            y = p[1];
        }

        double bh = fm.getAscent() + 4;
        double bx = x - bw / 2, by = y - bh / 2;
        RoundRectangle2D box = new RoundRectangle2D.Double(bx, by, bw, bh, 6, 6);
        g.setColor(Color.WHITE);
        g.fill(box);
        g.setStroke(new BasicStroke(1f));
        g.setColor(LABEL_BORDER);
        g.draw(box);

        float tx = (float) (bx + 4);
        float ty = (float) (by + 2 + fm.getAscent() - fm.getDescent() / 2.0);
        g.setColor(LABEL_TEXT);
        g.drawString(first, tx, ty);
        if (!step.isEmpty()) {
            g.setColor(e.highlight != null ? e.highlight : Color.BLACK);
            g.drawString(step, tx + fm.stringWidth(first), ty);
        }
    }

    private static void drawVertex(Graphics2D g, Vertex v, Options o) {
        double x = v.location.x, y = v.location.y, r = Vertex.RADIUS;
        boolean selected = o.interaction && v.wasClicked;
        boolean hovered = o.interaction && v.wasFocused && !selected;

        if (selected) {
            g.setColor(GLOW);
            g.setStroke(new BasicStroke(6f));
            g.draw(circle(x, y, r + 2));
        }

        g.setColor(v.colorId >= 0 ? Vertex.PALETTE[v.colorId % Vertex.PALETTE.length] : Color.WHITE);
        g.fill(circle(x, y, r));
        g.setColor(selected ? SELECT : hovered ? HOVER : OUTLINE);
        g.setStroke(new BasicStroke(selected || hovered ? 3f : 2f));
        g.draw(circle(x, y, r));

        if (o.interaction && v.removeHover) {
            ring(g, x, y, r + 6, REMOVE, new BasicStroke(3f));
        } else {
            if (v.isCutpoint)       ring(g, x, y, r + 4, CUTPOINT, new BasicStroke(2f));
            else if (v.isIsolated()) ring(g, x, y, r + 4, ISOLATED, new BasicStroke(2f));
            if (v.isRoot)           ring(g, x, y, r + 8, ROOT, new BasicStroke(2f));
            if (o.cutVertices.contains(v)) ring(g, x, y, r + 12, CUT, dashed(2f));
        }

        drawName(g, v.name, x, y);
    }

    private static void ring(Graphics2D g, double x, double y, double r, Color c, Stroke s) {
        g.setColor(c);
        g.setStroke(s);
        g.draw(circle(x, y, r));
    }

    /** The bold name font, shrunk from NAME_MAX until the name fits inside the vertex. */
    static Font nameFont(Graphics2D g, String name) {
        for (int size = NAME_MAX; size > NAME_MIN; size--) {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, size);
            if (g.getFontMetrics(f).stringWidth(name) <= 2 * Vertex.RADIUS - 8) return f;
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, NAME_MIN);
    }

    private static void drawName(Graphics2D g, String name, double x, double y) {
        g.setFont(nameFont(g, name));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.BLACK);
        g.drawString(name,
                (float) (x - fm.stringWidth(name) / 2.0),
                (float) (y + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private static Shape circle(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
    }

    private static BasicStroke dashed(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10f, new float[] { 6f, 4f }, 0f);
    }
}
