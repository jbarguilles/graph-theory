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

**Graph file** — a saved graph: its vertices (name, position, root) and edges (endpoints, direction, weight). It holds nothing derived or analytical — no cutpoints, coloring, walks or selected pair. Opening a file whose vertices lack positions arranges them, which counts as an unsaved change.

**Unsaved changes** — any edit to what a graph file holds, including moving a vertex. Analysis (coloring, building walks, selecting a pair, viewing Properties) is never an unsaved change.

**Minimum vertex cut** — a smallest set of vertices whose removal disconnects the graph; its size is the vertex connectivity.

**Minimum edge cut** — a smallest set of edges whose removal disconnects the graph; its size is the edge connectivity.

_Avoid_: "witness" for these — name the cut.

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

- **Graph window**: clicking a vertex shows its node properties in a fixed info box (degree, in-degree, out-degree, isolated, cutpoint, root).
- **Properties tab**: shows a table of node properties for all vertices, plus the adjacency matrix and distance matrix. It sits beside the **Graph** tab and is recomputed whenever it is opened.
- **Color coding on canvas**: root = green ring, cutpoint = orange ring, isolated = grey ring. A vertex is never both a cutpoint and isolated, but a root can be either, so it shows both rings at once (root outermost).
- **One meaning per colour**: blue = selection (lighter on hover), red = about to be removed, purple = bridge, teal = built walk, amber = selected pair's path, magenta dashed = minimum vertex/edge cut (Properties picture only), grey dashed = an edge being dragged out. Analysis results are highlighted, never shown by selecting things.
- **Parallel edges and self-loops on canvas**: every edge is drawn separately, so parallel edges and repeated self-loops are each visible and clickable on their own.
