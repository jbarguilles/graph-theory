package graphtheory;

import java.awt.Color;
import java.awt.Graphics;

public class Edge {

    public Vertex vertex1;
    public Vertex vertex2;
    public boolean directed;
    public boolean wasFocused;
    public boolean wasClicked;
    public int weight = 1;
    public boolean isBridge;

    public Edge(Vertex v1, Vertex v2, boolean directed) {
        vertex1 = v1;
        vertex2 = v2;
        this.directed = directed;
    }

    public boolean isSelfLoop() {
        return vertex1 == vertex2;
    }

    public void draw(Graphics g) {
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
        } else {
            g.drawLine(vertex1.location.x, vertex1.location.y,
                       vertex2.location.x, vertex2.location.y);
            if (directed) {
                drawArrowhead(g);
            }
            // weight label at midpoint
            int mx = (vertex1.location.x + vertex2.location.x) / 2;
            int my = (vertex1.location.y + vertex2.location.y) / 2;
            g.setColor(new Color(80, 80, 80));
            g.drawString("" + weight, mx + 4, my - 4);
        }
    }

    private void drawArrowhead(Graphics g) {
        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 12;
        g.drawLine(x2, y2,
                (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6)),
                (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6)));
        g.drawLine(x2, y2,
                (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6)),
                (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6)));
    }

    public boolean hasIntersection(int x, int y) {
        if (isSelfLoop()) return false;
        int x1, x2, y1, y2;
        x1 = vertex1.location.x;
        x2 = vertex2.location.x;
        y1 = vertex1.location.y;
        y2 = vertex2.location.y;
        float slope = 0;
        if (x2 != x1) {
            slope = (y2 - y1) / (x2 - x1);
        }
        float b = Math.abs(x1 * slope - y1);
        if (y + b <= Math.round(slope * x) + 10 && y + b >= Math.round(slope * x) - 10) {
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
