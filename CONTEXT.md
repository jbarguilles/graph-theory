# Graph Theory Project — Domain Glossary

## Vertex (Node)

A point in the graph. Has a name, a canvas location, and three neighbor lists:

- **undirectedNeighbors** — vertices connected by undirected edges
- **inNeighbors** — vertices connected by directed edges pointing *into* this vertex
- **outNeighbors** — vertices connected by directed edges pointing *out of* this vertex

**Name** — a short label identifying the vertex: 1–4 characters from letters, digits and `_`, unique within the graph (case-sensitive, so `a` and `A` differ). New vertices are named `0`, `1`, `2`, … (lowest unused number); the user may rename them.

### Node Properties

**Degree** — count of undirected edges incident to the vertex. A self-loop contributes 2.

**In-Degree** — count of directed edges pointing into the vertex. A directed self-loop contributes 1.

**Out-Degree** — count of directed edges pointing out of the vertex. A directed self-loop contributes 1.

**Isolated** — a vertex with no incident edges of any kind (degree = 0, in-degree = 0, out-degree = 0). Determined by `isIsolated()` method; not stored as a flag.

**Cutpoint** (articulation point) — a vertex whose removal increases the number of connected components. Computed using Tarjan's DFS algorithm, treating all edges as undirected. Stored as `isCutpoint` on the vertex. Recomputed whenever the graph changes.

**Root** — a user-designated vertex within a rooted tree or forest. At most one root per connected component. Stored as `isRoot` on the vertex. When a new edge joins two components that each have a root, the root of the component the edge starts from (the drag's first vertex) stays; the other stops being a root.

**Self-loop** — an edge whose two endpoints are the same vertex. A self-loop contributes 2 to the vertex's degree (undirected) or 1 each to in-degree and out-degree (directed).

**Degree distribution** — for each k, the fraction of vertices whose degree is k. The **in-degree distribution** and **out-degree distribution** are the same for in-degree and out-degree. A graph with undirected edges, or with no edges, has a degree distribution; a graph with directed edges has in-degree and out-degree distributions; a mixed graph has all three.

_Avoid_: one distribution of "total degree" (degree + in-degree + out-degree); it mixes edge kinds and is not a term here.

---

## Edge (Arc)

A connection between two vertices. Every edge is either **directed** or **undirected**; the same graph may contain both kinds (mixed graph).

- **Directed edge** — has a source (`vertex1`) and a destination (`vertex2`). Written as an ordered pair (a, b).
- **Undirected edge** — has no source/destination distinction. Written as a set {a, b}.
- **Self-loop** — an edge where `vertex1 == vertex2`.

**Weight** — a non-negative whole number on every edge; 1 unless the user changes it.

**Weighted graph** — a graph in which at least one edge has a weight other than 1. Only a weighted graph shows weights on the canvas, and then it shows all of them.

---

## Graph

A set of vertices and edges. This project supports **mixed graphs** — graphs containing both directed and undirected edges simultaneously.

**Order** — the number of vertices, |V|.

**Size** — the number of edges, |E|. Every edge counts once: each parallel edge, each self-loop, each arc.

**Magnitude** — order + size, |V| + |E|.

**Simple graph** — no self-loops, and no two edges joining the same pair of vertices, in any combination of directions — except that the two opposite arcs (a, b) and (b, a) may both be present.

**Empty graph** — a graph with at least one vertex and no edges.

**Complete graph** — a simple graph in which every two vertices are joined both ways: by an undirected edge, or by both arcs (a, b) and (b, a). A mixed graph can be complete.

**Density** — for a simple graph only: the fraction of ordered pairs of distinct vertices (u, v) such that one edge leads from u to v. An undirected edge counts for both (u, v) and (v, u), so for a graph with only undirected edges this is |E| / (n(n−1)/2). A graph is complete exactly when its density is 1. Undefined for graphs with fewer than two vertices or that are not simple.

**Cyclic / Acyclic** — a graph is cyclic if it contains a **Cycle** (see Walk), respecting direction and counting parallel edges and self-loops. So {a, b} twice is cyclic (a cycle of length 2), and so is {a, b} with (b, a); the arcs (a, b), (b, c), (a, c) are acyclic.

**Forest** — a graph with no cycle when direction is ignored (arcs treated as undirected edges), still counting parallel edges and self-loops. **Tree** — a connected forest. A forest describes the shape of the graph, so the directed path (a, b), (b, c) is a tree.

Because of the difference, a graph can be acyclic without being a forest: (a, b), (b, c), (a, c) has no cycle, but ignoring direction it is a triangle.

**Rooted tree** — a tree with one root. **Rooted forest** — a forest each of whose components has a root.

**Star** — a tree with at least three vertices in which one vertex (the centre) is adjacent to all the others.

**Bridge** — an edge whose removal increases the number of components (ignoring direction). A parallel edge is never a bridge (its copy still joins the pair), and neither is a self-loop.

**Nonseparable** — connected with no cutpoint. A single vertex, and two vertices joined by one edge, are nonseparable.

**Block** — a maximal nonseparable subgraph (ignoring direction). Every edge lies in exactly one block; parallel edges lie in the same block; an isolated vertex is a block on its own; a self-loop lies in a block containing its vertex and does not change which vertices form the blocks. A **nontrivial block** has at least three vertices.

**Proper colouring** — a colour for each vertex such that adjacent vertices (ignoring direction) get different colours. A graph with a self-loop has no proper colouring. **Chromatic number** χ(G) — the fewest colours a proper colouring needs. A **greedy colouring** is a proper colouring found by colouring vertices one at a time; it may use more than χ(G) colours.

**Bipartite** — the vertices split into two sets so that every edge (ignoring direction) joins the two sets; equivalently, χ(G) ≤ 2. A single vertex is bipartite; a graph with a self-loop is not. **Complete bipartite** K_{m,k} — a simple bipartite graph with sides of m and k vertices in which every vertex of one side is adjacent to every vertex of the other.

**Matching** — a set of edges, no two sharing an endpoint, ignoring direction; a self-loop is never in a matching. Parallel edges are interchangeable in a matching.

- **Maximal matching** — a matching that no further edge can be added to. There are usually many; which one is shown depends on the order edges are tried.
- **Maximum matching** — a matching with the most edges possible. Every maximum matching is maximal, not the reverse.
- **Perfect matching** — a matching that covers every vertex; it exists exactly when a maximum matching covers every vertex.

**Preference list** — a vertex's ranking of its neighbours other than itself (ignoring direction), most preferred first, each neighbour once however many edges join them. A vertex either has no preference list or has one naming exactly those neighbours: when it gains a new neighbour, that neighbour joins the end of its list; when it loses its last edge to a neighbour, that neighbour leaves the list; a vertex left with no neighbours has no list. Preference lists are part of the graph — saved in the graph file, and changing one is an unsaved change.

**Stable matching** — for a bipartite graph whose vertices all have preference lists: a matching with no **blocking pair**, i.e. two adjacent vertices, not matched to each other, who each are either unmatched or prefer the other to their current partner. One side **proposes** (Gale–Shapley); the result is the best stable matching for the proposing side, so choosing the other side can give a different stable matching. Some vertices may be left unmatched.

**Graph file** — a saved graph: its vertices (name, position, root, preference list) and edges (endpoints, direction, weight). It holds nothing derived or analytical — no cutpoints, coloring, walks or selected pair. Opening a file whose vertices lack positions arranges them, which counts as an unsaved change.

**Unsaved changes** — any edit to what a graph file holds, including moving a vertex. Analysis (coloring, building walks, selecting a pair, viewing Properties) is never an unsaved change.

**Connected** — every two vertices are joined by a walk when direction is ignored (arcs treated as undirected edges). A graph with no vertices is neither connected nor disconnected.

**Component** — a maximal connected set of vertices, again ignoring direction.

**Strongly connected** — every vertex reaches every other by a walk that respects direction. A single vertex is strongly connected (the trivial walk). Only meaningful when the graph has arcs; without them it is the same as connected.

**Minimum vertex cut** — a smallest set of vertices whose removal disconnects the graph (ignoring direction); its size is the **vertex connectivity** κ(G). A disconnected graph has κ = 0. If no set of vertices disconnects the graph (every two vertices are adjacent), κ = n − 1 by convention and there is no minimum vertex cut.

**Minimum edge cut** — a smallest set of edges whose removal disconnects the graph (ignoring direction); its size is the **edge connectivity** λ(G). Parallel edges are separate edges, so each must be removed. A disconnected graph has λ = 0.

_Avoid_: "witness" for these — name the cut.

**Adjacency matrix** — entry (u, v) counts the edges along which you can leave u and arrive at v. An undirected edge {u, v} adds 1 to both (u, v) and (v, u); an arc (u, v) adds 1 to (u, v) only (read row → column). An undirected self-loop at u adds 2 to (u, u); a directed self-loop adds 1. So parallel edges show as counts above 1, and for every vertex the row sum is degree + out-degree and the column sum is degree + in-degree.

_Avoid_: a 0/1 adjacency matrix — it hides parallel edges.

---

## Walk

An alternating sequence of vertices and edges v₀, e₁, v₁, e₂, …, eₖ, vₖ, where each edge eᵢ joins vᵢ₋₁ to vᵢ. A walk is defined by its **edges**, not only its vertices, because a mixed graph can have more than one edge between the same pair of vertices (e.g. {a, b} and (a, b)).

- An **undirected edge** can be traversed in either direction.
- A **directed edge** can only be traversed from its source to its destination.
- A **self-loop** is one step from a vertex back to itself.

**Length** — the number of edges in the walk (k), counting repeats.

_Avoid_: using a vertex sequence alone as a walk; it is ambiguous when parallel edges exist.

Two walks with the same vertex sequence but different edges are **different walks** (and likewise different trails/paths).

**Trivial walk** — a single vertex, length 0. It counts as a walk, trail and path, but is **not** closed.

### Kinds of Walk

Each is a restriction of the one before it:

- **Trail** — a walk with no repeated edge.
- **Path** — a walk with no repeated vertex. Every path is a trail.
- **Closed walk** — a walk of length ≥ 1 with v₀ = vₖ.
- **Circuit** — a closed walk that is also a trail.
- **Cycle** — a circuit whose only repeated vertex is v₀ = vₖ.

Edge cases:
- A self-loop traversed once is a cycle of length 1.
- a → b → a using the same undirected edge twice is not a trail, so not a cycle. Going out on {a, b} and back on the arc (b, a) is a cycle of length 2.

**Distance** (geodesic distance) — from u to v, the length of the shortest walk from u to v. It belongs to a pair of vertices, not to a walk. A shortest u–v path is a **geodesic**; there may be more than one.

_Avoid_: "distance" for the length of a particular walk — use **Length**.

**Weight of a walk** — the sum of the weights of its edges, counting repeats. Not the same as its length unless every weight is 1.

**Weighted distance** — from u to v, the smallest weight of a walk from u to v. A u–v path of that weight is a **lightest path**. In an unweighted graph the weighted distance equals the distance, and every geodesic is a lightest path; in a weighted graph they can differ.

_Avoid_: "distance" or "geodesic" when the weight sum is meant — use **Weighted distance** and **Lightest path**.

_Avoid_: "Tour" for a closed trail — use **Circuit**. "Tour" is reserved for an **Euler tour**.

### Euler Trail and Euler Tour

- **Euler trail** — a trail that uses every edge of the graph exactly once.
- **Euler tour** — an Euler trail that is closed (a circuit using every edge exactly once).

Like any walk, it must respect direction: a directed edge is crossed only from its source to its destination, and an undirected edge may be crossed either way (but still only once). Isolated vertices do not matter; every edge must be reachable from the trail.

In a graph with no edges, the trivial walk is an Euler trail but not an Euler tour (it is not closed). A graph with no vertices has neither.

_Avoid_: "Euler path" — an Euler trail may repeat vertices, so it is generally not a **Path**. "Euler circuit" is an acceptable synonym for Euler tour.

### Hamiltonian Path and Hamiltonian Cycle

- **Hamiltonian path** — a path that visits every vertex of the graph.
- **Hamiltonian cycle** — a cycle that visits every vertex of the graph.

They follow the walk rules exactly, including direction, so there is no minimum number of vertices:
- A single vertex with no edges has a Hamiltonian path (the trivial walk) but no Hamiltonian cycle.
- A single vertex with a self-loop has a Hamiltonian cycle of length 1.
- Two vertices joined by {a, b} and (b, a) have a Hamiltonian cycle of length 2. Joined by {a, b} alone, they do not.

---

## Display Conventions

- **Graph window**: a side panel beside the canvas describes, in this order, whatever currently exists: the selected vertices (their node properties), the selected pair (adjacency, reachability, distance, its paths), and the walk. Nothing is drawn over the graph.
- **Built and found walks**: a walk is either built click by click, or found by an Euler or Hamiltonian search. A found walk is read-only: clicking starts a new built walk in its place, and adding or removing a vertex or edge, or changing a weight, clears it (moving or renaming a vertex does not).
- **Properties tab**: sits beside the **Graph** tab and always describes the current graph. Four views: **Overview** (the graph picture with its minimum vertex and edge cuts, and the summary of graph properties), **Vertices** (node properties and neighbours of every vertex), **Matrices** (adjacency matrix, distance matrix, and on a weighted graph the weighted distance matrix — one at a time; ∞ = unreachable), and **Distributions** (the degree distributions). Nothing on it changes the selection on the Graph tab. Choosing the proposing side for a stable matching is analysis; editing preference lists is an edit.
- **Color coding on canvas**: root = green ring, cutpoint = orange ring, isolated = grey ring. A vertex is never both a cutpoint and isolated, but a root can be either, so it shows both rings at once (root outermost).
- **One meaning per colour**: blue = selection (lighter on hover), red = about to be removed, purple = bridge, teal = the walk (one being built, or a found Euler trail/tour or Hamiltonian path/cycle), amber = selected pair's path, magenta dashed = minimum vertex/edge cut (Properties picture only), grey dashed = an edge being dragged out. Analysis results are highlighted, never shown by selecting things.
- **Parallel edges and self-loops on canvas**: every edge is drawn separately, so parallel edges and repeated self-loops are each visible and clickable on their own.
