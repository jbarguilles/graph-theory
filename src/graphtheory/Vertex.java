/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.awt.Color;
import java.awt.Point;
import java.util.Vector;
import java.awt.Graphics;

/**
 *
 * @author mk
 */
public class Vertex implements Comparable {

    public String name;
    public Point location;
    public boolean wasFocused;
    public boolean wasClicked;
    public boolean removeHover;

    /** Color assigned by greedy coloring; -1 = uncolored (default white). */
    public int colorId = -1;

    private int size1 = 30;
    private int size2 = 40;
    public Vector<Vertex> undirectedNeighbors;
    public Vector<Vertex> inNeighbors;
    public Vector<Vertex> outNeighbors;
    public boolean isCutpoint;
    public boolean isRoot;
    /** Radius of a vertex on the canvas. Hit-testing, edge endpoints and drawing all use it. */
    public static final int RADIUS = 18;

    /** Palette for greedy coloring. Cycles through if colorId is large. */
    public static final Color[] PALETTE = {
        new Color(255, 179, 186),
        new Color(186, 225, 255),
        new Color(186, 255, 201),
        new Color(255, 255, 186),
        new Color(255, 200, 255),
        new Color(200, 255, 255),
        new Color(220, 200, 170),
        new Color(220, 220, 220),
    };

    public Vertex(String name, int x, int y) {
        this.name = name;
        location = new Point(x, y);
        undirectedNeighbors = new Vector<Vertex>();
        inNeighbors = new Vector<Vertex>();
        outNeighbors = new Vector<Vertex>();
    }

    public void addUndirectedNeighbor(Vertex v) {
        undirectedNeighbors.add(v);
    }

    public boolean hasIntersection(int x, int y) {
        return Math.hypot(x - location.x, y - location.y) <= RADIUS;
    }

    public boolean connectedToVertex(Vertex v) {
        return undirectedNeighbors.contains(v)
            || inNeighbors.contains(v)
            || outNeighbors.contains(v);
    }

    public boolean hasSelfLoop() {
        for (Vertex v : undirectedNeighbors) if (v == this) return true;
        for (Vertex v : outNeighbors)        if (v == this) return true;
        for (Vertex v : inNeighbors)         if (v == this) return true;
        return false;
    }

    public int degree() {
        int d = 0;
        boolean sawSelfLoop = false;
        for (Vertex v : undirectedNeighbors) {
            if (v == this) sawSelfLoop = true;
            else d++;
        }
        if (sawSelfLoop) d += 2;
        return d;
    }

    public int inDegree() {
        return inNeighbors.size();
    }

    public int outDegree() {
        return outNeighbors.size();
    }

    public int getDegree() {
        return degree() + inDegree() + outDegree();
    }

    public boolean isIsolated() {
        return undirectedNeighbors.isEmpty() && inNeighbors.isEmpty() && outNeighbors.isEmpty();
    }

    public int compareTo(Object v) {
        if (((Vertex) v).getDegree() > getDegree()) return 1;
        else if (((Vertex) v).getDegree() < getDegree()) return -1;
        else return 0;
    }

    public void draw(Graphics g) {
        // 1) Body fill
        if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else {
            g.setColor(Color.black);
        }
        g.fillOval(location.x - size2 / 2, location.y - size2 / 2, size2, size2);

        // 2) Inner disc — palette color if colored, else white
        if (colorId >= 0) {
            g.setColor(PALETTE[colorId % PALETTE.length]);
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillOval(location.x - size1 / 2, location.y - size1 / 2, size1, size1);

        // 3) Property ring on top
        if (removeHover) {
            g.setColor(new Color(220, 0, 0));
            g.drawOval(location.x - size2 / 2 - 6, location.y - size2 / 2 - 6, size2 + 12, size2 + 12);
            g.drawOval(location.x - size2 / 2 - 5, location.y - size2 / 2 - 5, size2 + 10, size2 + 10);
        } else if (isRoot) {
            g.setColor(new Color(0, 180, 0));
            g.drawOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
            g.drawOval(location.x - size2 / 2 - 3, location.y - size2 / 2 - 3, size2 + 6, size2 + 6);
        } else if (isCutpoint) {
            g.setColor(new Color(255, 140, 0));
            g.drawOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
            g.drawOval(location.x - size2 / 2 - 3, location.y - size2 / 2 - 3, size2 + 6, size2 + 6);
        } else if (isIsolated()) {
            g.setColor(Color.GRAY);
            g.drawOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
        }

        // 4) Name on top
        g.setColor(Color.BLACK);
        g.drawString(name, location.x, location.y);
    }
}