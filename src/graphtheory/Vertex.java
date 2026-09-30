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
    public boolean removeHover;   // red preview when Remove Tool hovers

    private int size1 = 30;
    private int size2 = 40;
    public Vector<Vertex> undirectedNeighbors;
    public Vector<Vertex> inNeighbors;
    public Vector<Vertex> outNeighbors;
    public boolean isCutpoint;
    public boolean isRoot;

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
        double distance = Math.sqrt(Math.pow((x - location.x), 2) + Math.pow((y - location.y), 2));
        return distance <= size2 / 2;
    }

    public boolean connectedToVertex(Vertex v) {
        return undirectedNeighbors.contains(v)
            || inNeighbors.contains(v)
            || outNeighbors.contains(v);
    }

    /**
     * True if this vertex has any self-loop, undirected or directed.
     */
    public boolean hasSelfLoop() {
        for (Vertex v : undirectedNeighbors) {
            if (v == this) return true;
        }
        for (Vertex v : outNeighbors) {
            if (v == this) return true;
        }
        // inNeighbors is covered by outNeighbors for a directed self-loop,
        // but check defensively in case the data was hand-edited.
        for (Vertex v : inNeighbors) {
            if (v == this) return true;
        }
        return false;
    }

    /**
     * Undirected degree. A self-loop contributes 2.
     *
     * This is robust even if the neighbor list accidentally stores `this`
     * more than once — we only count 2 for any number of self-loop entries.
     */
    public int degree() {
        int d = 0;
        boolean sawSelfLoop = false;
        for (Vertex v : undirectedNeighbors) {
            if (v == this) {
                sawSelfLoop = true;   // do not double-count duplicates
            } else {
                d++;
            }
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

    /**
     * Total degree = undirected degree + in-degree + out-degree.
     * For an undirected self-loop, degree() already adds 2, and
     * in/out are empty, so the total is 2 (correct).
     * For a directed self-loop, degree() is 0, in=1, out=1, total 2 (correct).
     */
    public int getDegree() {
        return degree() + inDegree() + outDegree();
    }

    public boolean isIsolated() {
        return undirectedNeighbors.isEmpty() && inNeighbors.isEmpty() && outNeighbors.isEmpty();
    }

    public int compareTo(Object v) {
        if (((Vertex) v).getDegree() > getDegree()) {
            return 1;
        } else if (((Vertex) v).getDegree() < getDegree()) {
            return -1;
        } else {
            return 0;
        }
    }

    public void draw(Graphics g) {
        // Draw property ring behind the vertex circle
        if (removeHover) {
            g.setColor(new Color(220, 0, 0));
            g.fillOval(location.x - size2 / 2 - 6, location.y - size2 / 2 - 6, size2 + 12, size2 + 12);
        } else if (isRoot) {
            g.setColor(new Color(0, 180, 0));
            g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
        } else if (isCutpoint) {
            g.setColor(new Color(255, 140, 0));
            g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
        } else if (isIsolated()) {
            g.setColor(Color.GRAY);
            g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
        }

        if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else {
            g.setColor(Color.black);
        }

        g.fillOval(location.x - size2 / 2, location.y - size2 / 2, size2, size2);
        g.setColor(Color.WHITE);
        g.fillOval(location.x - size1 / 2, location.y - size1 / 2, size1, size1);
        g.setColor(Color.BLACK);
        g.drawString(name, location.x, location.y);
    }
}