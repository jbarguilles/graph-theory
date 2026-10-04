package graphtheory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Connected components, taken from the edge list with every edge treated as undirected. */
public final class Components {

    private Components() {}

    /** The vertices in start's connected component, including start. */
    public static Set<Vertex> of(Vertex start, List<Edge> edges) {
        Set<Vertex> visited = new HashSet<Vertex>();
        ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Vertex u = queue.poll();
            for (Edge e : edges) {
                Vertex other = e.vertex1 == u ? e.vertex2 : e.vertex2 == u ? e.vertex1 : null;
                if (other != null && visited.add(other)) queue.add(other);
            }
        }
        return visited;
    }
}
