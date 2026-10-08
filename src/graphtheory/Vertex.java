/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graphtheory;

import java.awt.Color;
import java.awt.Point;
import java.util.Vector;

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

    public Vector<Vertex> undirectedNeighbors;
    public Vector<Vertex> inNeighbors;
    public Vector<Vertex> outNeighbors;
    public boolean isCutpoint;
    public boolean isRoot;
    /**
     * This vertex's preference list (CONTEXT.md): its neighbours, most preferred first.
     * null = no list. PreferenceLists.sync keeps it naming exactly the neighbours.
     */
    public Vector<Vertex> preferences = null;
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
        for (Vertex v : undirectedNeighbors) {
            d += (v == this) ? 2 : 1;
        }
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
}
