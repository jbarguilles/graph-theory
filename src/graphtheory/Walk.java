package graphtheory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * A walk v0, e1, v1, ..., ek, vk, stored as its vertices and edges.
 * A walk is identified by its edges, not just its vertices, because a mixed
 * graph can join the same two vertices by more than one edge.
 * See CONTEXT.md ("Walk") and docs/adr/0002-edge-aware-walks.md.
 */
public class Walk {

    private final Vector<Vertex> vertices = new Vector<Vertex>();  // v0..vk
    private final Vector<Edge> edges = new Vector<Edge>();         // e1..ek

    public Walk(Vertex start) {
        vertices.add(start);
    }

    /** True if e can be traversed starting at 'from'. Arcs only go source -> destination. */
    public static boolean canTraverse(Edge e, Vertex from) {
        if (e.directed) return e.vertex1 == from;
        return e.vertex1 == from || e.vertex2 == from;
    }

    /** The vertex reached by traversing e from 'from'. Assumes canTraverse(e, from). */
    public static Vertex otherEnd(Edge e, Vertex from) {
        return e.vertex1 == from ? e.vertex2 : e.vertex1;
    }

    /** All edges in eList that take one step from 'from' to 'to'. */
    public static Vector<Edge> edgesBetween(Vertex from, Vertex to, Vector<Edge> eList) {
        Vector<Edge> result = new Vector<Edge>();
        for (Edge e : eList) {
            if (canTraverse(e, from) && otherEnd(e, from) == to) result.add(e);
        }
        return result;
    }

    /** "{from,to}" for an undirected edge (in travel order), "(source,dest)" for an arc. */
    public static String edgeLabel(Edge e, Vertex from) {
        if (e.directed) return "(" + e.vertex1.name + "," + e.vertex2.name + ")";
        return "{" + from.name + "," + otherEnd(e, from).name + "}";
    }

    /** Appends e as the next step. Returns false, changing nothing, if e can't be traversed from end(). */
    public boolean extend(Edge e) {
        Vertex from = end();
        if (!canTraverse(e, from)) return false;
        edges.add(e);
        vertices.add(otherEnd(e, from));
        return true;
    }

    /** Removes the last step. Returns false if the walk is already trivial. */
    public boolean undo() {
        if (edges.isEmpty()) return false;
        edges.remove(edges.size() - 1);
        vertices.remove(vertices.size() - 1);
        return true;
    }

    public Walk copy() {
        Walk w = new Walk(start());
        for (Edge e : edges) w.extend(e);
        return w;
    }

    public Vertex start() { return vertices.firstElement(); }
    public Vertex end()   { return vertices.lastElement(); }
    public int length()   { return edges.size(); }

    public List<Edge> edges()      { return Collections.unmodifiableList(edges); }
    public List<Vertex> vertices() { return Collections.unmodifiableList(vertices); }

    public boolean uses(Edge e)     { return edges.contains(e); }
    public boolean visits(Vertex v) { return vertices.contains(v); }

    /** 1-based step numbers at which e is traversed, e.g. [1, 4]. */
    public List<Integer> stepsUsing(Edge e) {
        List<Integer> steps = new Vector<Integer>();
        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i) == e) steps.add(i + 1);
        }
        return steps;
    }

    /** No repeated edge. */
    public boolean isTrail() {
        return new HashSet<Edge>(edges).size() == edges.size();
    }

    /** No repeated vertex. */
    public boolean isPath() {
        return new HashSet<Vertex>(vertices).size() == vertices.size();
    }

    /** Length >= 1 and v0 = vk. A trivial walk is not closed. */
    public boolean isClosed() {
        return length() >= 1 && start() == end();
    }

    /** Closed trail. */
    public boolean isCircuit() {
        return isClosed() && isTrail();
    }

    /** Circuit whose only repeated vertex is v0 = vk. */
    public boolean isCycle() {
        if (!isCircuit()) return false;
        List<Vertex> allButLast = vertices.subList(0, vertices.size() - 1);
        return new HashSet<Vertex>(allButLast).size() == allButLast.size();
    }

    /** e.g. "a -{a,b}-> b -(b,c)-> c". */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(start().name);
        for (int i = 0; i < edges.size(); i++) {
            sb.append(" -").append(edgeLabel(edges.get(i), vertices.get(i)))
              .append("-> ").append(vertices.get(i + 1).name);
        }
        return sb.toString();
    }
}
