package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Everything the Properties tab shows, computed once from the graph (and the proposing side
 * for stable matching). Immutable; Canvas builds a new one when the graph text changes.
 */
public final class PropertiesReport {

    public final List<Vertex> vertices;
    public final List<Edge> edges;
    public final boolean weighted, hasArcs;

    public final int[][] adjacency, distances;
    /** null unless the graph is weighted. */
    public final int[][] weightedDistances;

    public final List<List<Vertex>> components;
    public final boolean connected;
    /** null when the graph has no arcs. */
    public final Boolean stronglyConnected;
    public final Connectivity.Cut<Vertex> vertexCut;
    public final Connectivity.Cut<Edge> edgeCut;
    public final Set<Edge> bridges;

    public final boolean simple, empty, complete, cyclic, forest, tree, star, completeBipartite;
    /** null when undefined (fewer than two vertices, or not simple). */
    public final Double density;
    /** null when not bipartite. */
    public final List<List<Vertex>> sides;

    public final List<Blocks.Block> blocks;
    public final boolean nonseparable;

    public final boolean hamiltonTooLarge;
    public final boolean tour, eulerTrail, eulerTour, hamiltonianPath, hamiltonianCycle;

    /** Colouring.chromaticNumber's result, including its NO_PROPER_COLOURING / TOO_LARGE codes. */
    public final int chromatic;
    public final List<Edge> maximal;
    /** null above Matchings.MAXIMUM_VERTEX_CAP. */
    public final List<Edge> maximum;
    /** null when maximum is null or there are no vertices. */
    public final Boolean perfect;
    /** The vertex whose side proposes (null with no vertices). */
    public final Vertex proposer;
    /** null when not bipartite or lists are missing. */
    public final List<Edge> stable;
    public final List<Vertex> missingPreferences;

    public PropertiesReport(List<Vertex> vs, List<Edge> es, Vertex proposer) {
        vertices = Collections.unmodifiableList(new ArrayList<Vertex>(vs));
        edges = Collections.unmodifiableList(new ArrayList<Edge>(es));
        weighted = Edge.isWeighted(es);
        boolean arcs = false;
        for (Edge e : es) arcs |= e.directed;
        hasArcs = arcs;

        adjacency = GraphMatrices.adjacency(vs, es);
        distances = GraphMatrices.distances(vs, es);
        weightedDistances = weighted ? GraphMatrices.weightedDistances(vs, es) : null;

        components = Connectivity.components(vs, es);
        connected = Connectivity.isConnected(vs, es);
        stronglyConnected = hasArcs ? Connectivity.isStronglyConnected(vs, es) : null;
        vertexCut = Connectivity.minimumVertexCut(vs, es);
        edgeCut = Connectivity.minimumEdgeCut(vs, es);
        bridges = Blocks.bridges(vs, es);

        simple = Structure.isSimple(vs, es);
        empty = Structure.isEmpty(vs, es);
        complete = Structure.isComplete(vs, es);
        density = Structure.density(vs, es);
        cyclic = Structure.isCyclic(vs, es);
        forest = Structure.isForest(vs, es);
        tree = Structure.isTree(vs, es);
        star = Structure.isStar(vs, es);
        sides = Structure.bipartiteSides(vs, es);
        completeBipartite = Structure.isCompleteBipartite(vs, es);

        blocks = Blocks.blocks(vs, es);
        nonseparable = Blocks.isNonseparable(vs, es);

        Vector<Vertex> vv = new Vector<Vertex>(vs);
        Vector<Edge> ev = new Vector<Edge>(es);
        tour = Traversals.hasTour(vs, es);
        eulerTrail = Traversals.eulerTrail(vv, ev) != null;
        eulerTour = Traversals.eulerTour(vv, ev) != null;
        hamiltonTooLarge = Traversals.hamiltonTooLarge(vv);
        hamiltonianPath = !hamiltonTooLarge && Traversals.hamiltonianPath(vv, ev) != null;
        hamiltonianCycle = !hamiltonTooLarge && Traversals.hamiltonianCycle(vv, ev) != null;

        chromatic = Colouring.chromaticNumber(vs, es);
        maximal = Matchings.maximal(vs, es);
        maximum = Matchings.maximum(vs, es);
        perfect = (maximum == null || vs.isEmpty()) ? null : maximum.size() * 2 == vs.size();
        this.proposer = vs.contains(proposer) ? proposer : (vs.isEmpty() ? null : vs.get(0));
        stable = Matchings.stable(vs, es, this.proposer);
        missingPreferences = PreferenceLists.missing(vs, es);
    }
}
