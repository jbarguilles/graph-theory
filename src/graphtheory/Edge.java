package graphtheory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;

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

    private static final int VERTEX_RADIUS = 15;
    private static final int ARROW_SIZE = 12;

    public static final double TOLERANCE = 6.0;

    /** Perpendicular offset (in pixels) used to separate antiparallel arcs. */
    private static final int CURVE_OFFSET = 16;

    public Edge(Vertex v1, Vertex v2, boolean directed) {
        vertex1 = v1;
        vertex2 = v2;
        this.directed = directed;
    }

    public boolean isSelfLoop() {
        return vertex1 == vertex2;
    }

    /** True if a reverse directed edge vertex2 → vertex1 also exists. */
    private boolean hasReverseArc() {
        if (!directed || isSelfLoop()) return false;
        for (Vertex n : vertex2.outNeighbors) {
            if (n == vertex1) return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        if (removeHover) {
            g.setColor(new Color(220, 0, 0));
        } else if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else if (highlight != null) {
            g.setColor(highlight);
        } else if (isBridge) {
            g.setColor(new Color(150, 0, 200));
        } else {
            g.setColor(Color.black);
        }

        if (isSelfLoop()) {
            int lx = vertex1.location.x;
            int ly = vertex1.location.y;

            int ovalX = lx - 10;
            int ovalY = ly - 40;
            int ovalW = 24;
            int ovalH = 24;

            g.drawOval(ovalX, ovalY, ovalW, ovalH);

            if (directed) {
                int tipX = ovalX + ovalW / 2 + 4;
                int tipY = ovalY;
                drawArrowhead(g, tipX, tipY, 0.0, 8);
            }

            g.setColor(new Color(80, 80, 80));
            g.drawString("" + weight, lx + 18, ly - 28);
            drawStepLabel(g, lx + 18, ly - 14);
            return;
        }

        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;

        double angle = Math.atan2(y2 - y1, x2 - x1);

        int startX = (int) (x1 + VERTEX_RADIUS * Math.cos(angle));
        int startY = (int) (y1 + VERTEX_RADIUS * Math.sin(angle));
        int endX   = (int) (x2 - VERTEX_RADIUS * Math.cos(angle));
        int endY   = (int) (y2 - VERTEX_RADIUS * Math.sin(angle));

        boolean hasReverse = hasReverseArc();

        // Control point: midpoint shifted perpendicular to travel direction
        // when a reverse arc exists. Zero offset → straight line.
        double mx = (startX + endX) / 2.0;
        double my = (startY + endY) / 2.0;
        double perpX = -(endY - startY);
        double perpY =  (endX - startX);
        double perpLen = Math.hypot(perpX, perpY);
        if (perpLen == 0) perpLen = 1;

        int curveOffset = hasReverse ? CURVE_OFFSET : 0;
        double ctrlX = mx + (perpX / perpLen) * curveOffset;
        double ctrlY = my + (perpY / perpLen) * curveOffset;

        if ((removeHover || highlight != null) && g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(3.0f));
            drawQuadCurve(g2, startX, startY, (int) ctrlX, (int) ctrlY, endX, endY);
            g2.setStroke(old);
        } else {
            drawQuadCurve(g, startX, startY, (int) ctrlX, (int) ctrlY, endX, endY);
        }

        if (directed) {
            // Arrowhead tangent = end-tangent of the quadratic (P2 - C).
            double endAngle = Math.atan2(endY - ctrlY, endX - ctrlX);
            drawArrowhead(g, endX, endY, endAngle);
        }

        // Weight label: draw at the curve's apex, offset a bit more.
        int midX = (int) ((startX + 2 * ctrlX + endX) / 4.0);
        int midY = (int) ((startY + 2 * ctrlY + endY) / 4.0);
        g.setColor(new Color(80, 80, 80));
        g.drawString("" + weight, midX + 4, midY - 4);
        drawStepLabel(g, midX + 4, midY + 12);
    }

    /** Draws the walk step numbers (e.g. "#1,4") under the weight label. */
    private void drawStepLabel(Graphics g, int x, int y) {
        if (stepLabel == null) return;
        g.setColor(highlight != null ? highlight : Color.black);
        g.drawString(stepLabel, x, y);
    }

    /** Draws a quadratic Bezier, falling back to a straight line if flat. */
    private void drawQuadCurve(Graphics g, int x0, int y0, int cx, int cy, int x1, int y1) {
        if (cx == (x0 + x1) / 2 && cy == (y0 + y1) / 2) {
            g.drawLine(x0, y0, x1, y1);
            return;
        }
        int segments = 16;
        int prevX = x0, prevY = y0;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double mt = 1 - t;
            double xx = mt * mt * x0 + 2 * mt * t * cx + t * t * x1;
            double yy = mt * mt * y0 + 2 * mt * t * cy + t * t * y1;
            int xi = (int) Math.round(xx);
            int yi = (int) Math.round(yy);
            g.drawLine(prevX, prevY, xi, yi);
            prevX = xi;
            prevY = yi;
        }
    }

    private void drawArrowhead(Graphics g, int tipX, int tipY, double angle) {
        drawArrowhead(g, tipX, tipY, angle, ARROW_SIZE);
    }

    private void drawArrowhead(Graphics g, int tipX, int tipY, double angle, int size) {
        int leftX  = (int) (tipX - size * Math.cos(angle - Math.PI / 6));
        int leftY  = (int) (tipY - size * Math.sin(angle - Math.PI / 6));
        int rightX = (int) (tipX - size * Math.cos(angle + Math.PI / 6));
        int rightY = (int) (tipY - size * Math.sin(angle + Math.PI / 6));

        Polygon head = new Polygon();
        head.addPoint(tipX, tipY);
        head.addPoint(leftX, leftY);
        head.addPoint(rightX, rightY);

        Color saved = g.getColor();
        g.fillPolygon(head);
        g.setColor(saved);
    }

    public boolean hasIntersection(int px, int py) {
        if (isSelfLoop()) {
            int lx = vertex1.location.x;
            int ly = vertex1.location.y;
            double cx = lx - 10 + 12;
            double cy = ly - 40 + 12;
            double r  = 12;
            double d  = Math.hypot(px - cx, py - cy);
            return Math.abs(d - r) <= TOLERANCE;
        }

        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;

        double angle = Math.atan2(y2 - y1, x2 - x1);

        int startX = (int) (x1 + VERTEX_RADIUS * Math.cos(angle));
        int startY = (int) (y1 + VERTEX_RADIUS * Math.sin(angle));
        int endX   = (int) (x2 - VERTEX_RADIUS * Math.cos(angle));
        int endY   = (int) (y2 - VERTEX_RADIUS * Math.sin(angle));

        boolean hasReverse = hasReverseArc();

        double mx = (startX + endX) / 2.0;
        double my = (startY + endY) / 2.0;
        double perpX = -(endY - startY);
        double perpY =  (endX - startX);
        double perpLen = Math.hypot(perpX, perpY);
        if (perpLen == 0) perpLen = 1;

        int curveOffset = hasReverse ? CURVE_OFFSET : 0;
        double ctrlX = mx + (perpX / perpLen) * curveOffset;
        double ctrlY = my + (perpY / perpLen) * curveOffset;

        // Walk along the quadratic; minimum distance to (px, py) must be <= TOLERANCE.
        int segments = 24;
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double mt = 1 - t;
            double xx = mt * mt * startX + 2 * mt * t * ctrlX + t * t * endX;
            double yy = mt * mt * startY + 2 * mt * t * ctrlY + t * t * endY;
            double d = Math.hypot(px - xx, py - yy);
            if (d < best) best = d;
        }
        return best <= TOLERANCE;
    }
}