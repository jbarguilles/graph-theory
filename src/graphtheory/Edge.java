package graphtheory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

public class Edge {

    public Vertex vertex1;
    public Vertex vertex2;
    public boolean directed;
    public boolean wasFocused;
    public boolean wasClicked;
    public int weight = 1;
    public boolean isBridge;

    // Radius used to offset line/arrow from vertex center.
    // Match this to your Vertex drawing radius.
    private static final int VERTEX_RADIUS = 15;
    private static final int ARROW_SIZE = 12;

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
        if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else if (isBridge) {
            g.setColor(new Color(150, 0, 200));   // purple = bridge
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

        // -------- draw the line --------
        g.drawLine(startX, startY, endX, endY);

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

    public boolean hasIntersection(int x, int y) {
        if (isSelfLoop()) return false;

        int x1 = vertex1.location.x;
        int x2 = vertex2.location.x;
        int y1 = vertex1.location.y;
        int y2 = vertex2.location.y;

        float slope = 0;
        if (x2 != x1) {
            slope = (float) (y2 - y1) / (x2 - x1);   // float division!
        }
        float b = Math.abs(x1 * slope - y1);

        if (y + b <= Math.round(slope * x) + 10
                && y + b >= Math.round(slope * x) - 10) {
            if (x1 > x2 && y1 > y2) {
                if (x <= x1 && x >= x2 && y <= y1 && y >= y2) return true;
            } else if (x1 < x2 && y1 > y2) {
                if (x <= x2 && x >= x1 && y <= y1 && y >= y2) return true;
            } else if (x1 < x2 && y1 < y2) {
                if (x <= x2 && x >= x1 && y <= y2 && y >= y1) return true;
            } else if (x <= x1 && x >= x2 && y <= y2 && y >= y1) {
                return true;
            }
        }
        return false;
    }
}