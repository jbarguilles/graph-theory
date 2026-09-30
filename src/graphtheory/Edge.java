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

    // Radius used to offset line/arrow from vertex center.
    // Match this to your Vertex drawing radius.
    private static final int VERTEX_RADIUS = 15;
    private static final int ARROW_SIZE = 12;

    // Click tolerance in pixels — how close the cursor must be to the edge line.
    public static final double TOLERANCE = 6.0;

    public Edge(Vertex v1, Vertex v2, boolean directed) {
        vertex1 = v1;
        vertex2 = v2;
        this.directed = directed;
    }

    public boolean isSelfLoop() {
        return vertex1 == vertex2;
    }

    public void draw(Graphics g) {
        // -------- color selection --------
        if (removeHover) {
            g.setColor(new Color(220, 0, 0));       // bright red = about to be removed
        } else if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else if (isBridge) {
            g.setColor(new Color(150, 0, 200));     // purple = bridge
        } else {
            g.setColor(Color.black);
        }

        if (isSelfLoop()) {
            int lx = vertex1.location.x;
            int ly = vertex1.location.y;
            g.drawOval(lx - 10, ly - 40, 24, 24);

            // weight label for self loop
            g.setColor(new Color(80, 80, 80));
            g.drawString("" + weight, lx + 18, ly - 28);
            return;
        }

        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;

        double angle = Math.atan2(y2 - y1, x2 - x1);

        // start & end points trimmed to the vertex boundary
        int startX = (int) (x1 + VERTEX_RADIUS * Math.cos(angle));
        int startY = (int) (y1 + VERTEX_RADIUS * Math.sin(angle));
        int endX   = (int) (x2 - VERTEX_RADIUS * Math.cos(angle));
        int endY   = (int) (y2 - VERTEX_RADIUS * Math.sin(angle));

        // -------- draw the line (thicker when hovered for removal) --------
        if (removeHover && g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawLine(startX, startY, endX, endY);
            g2.setStroke(old);
        } else {
            g.drawLine(startX, startY, endX, endY);
        }

        // -------- draw the arrowhead if directed --------
        if (directed) {
            drawArrowhead(g, endX, endY, angle);
        }

        // -------- weight label at midpoint --------
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2;
        g.setColor(new Color(80, 80, 80));
        g.drawString("" + weight, mx + 4, my - 4);
    }

    /**
     * Draws a filled arrowhead whose tip is at (tipX, tipY) pointing
     * in the direction given by {@code angle}.
     */
    private void drawArrowhead(Graphics g, int tipX, int tipY, double angle) {
        int leftX  = (int) (tipX - ARROW_SIZE * Math.cos(angle - Math.PI / 6));
        int leftY  = (int) (tipY - ARROW_SIZE * Math.sin(angle - Math.PI / 6));
        int rightX = (int) (tipX - ARROW_SIZE * Math.cos(angle + Math.PI / 6));
        int rightY = (int) (tipY - ARROW_SIZE * Math.sin(angle + Math.PI / 6));

        Polygon head = new Polygon();
        head.addPoint(tipX, tipY);
        head.addPoint(leftX, leftY);
        head.addPoint(rightX, rightY);

        Color saved = g.getColor();
        g.fillPolygon(head);
        g.setColor(saved);
    }

    /**
     * Returns true if (px, py) is within {@link #TOLERANCE} pixels of
     * this edge's rendered segment (or its loop ring for self-loops).
     */
    public boolean hasIntersection(int px, int py) {
        // --- self-loop: match the ring we draw with drawOval ---
        if (isSelfLoop()) {
            int lx = vertex1.location.x;
            int ly = vertex1.location.y;
            double cx = lx - 10 + 12;   // center of the drawn oval
            double cy = ly - 40 + 12;
            double r  = 12;
            double d  = Math.hypot(px - cx, py - cy);
            return Math.abs(d - r) <= TOLERANCE;
        }

        // --- normal edge: point-to-segment distance ---
        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;

        if (lenSq == 0) {
            return Math.hypot(px - x1, py - y1) <= TOLERANCE;
        }

        // Project click point onto the segment, clamped to [0, 1]
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY) <= TOLERANCE;
    }
}