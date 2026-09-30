# Graph Theory Project — Domain Glossary

## Vertex (Node)

A point in the graph. Has a name, a canvas location, and three neighbor lists:
- **undirectedNeighbors** — vertices connected by undirected edges
- **inNeighbors** — vertices connected by directed edges pointing *into* this vertex
- **outNeighbors** — vertices connected by directed edges pointing *out of* this vertex

### Node Properties

**Degree** — count of undirected edges incident to the vertex. A self-loop contributes 2.

**In-Degree** — count of directed edges pointing into the vertex. A directed self-loop contributes 1.

**Out-Degree** — count of directed edges pointing out of the vertex. A directed self-loop contributes 1.

**Isolated** — a vertex with no incident edges of any kind (degree = 0, in-degree = 0, out-degree = 0). Determined by `isIsolated()` method; not stored as a flag.

**Cutpoint** (articulation point) — a vertex whose removal increases the number of connected components. Computed using Tarjan's DFS algorithm, treating all edges as undirected. Stored as `isCutpoint` on the vertex. Recomputed only when the Properties window is opened.

**Root** — a user-designated vertex within a rooted tree or forest. At most one root per connected component. Stored as `isRoot` on the vertex.

**Self-loop** — an edge whose two endpoints are the same vertex. A self-loop contributes 2 to the vertex's degree (undirected) or 1 each to in-degree and out-degree (directed).

---

## Edge (Arc)

A connection between two vertices. Every edge is either **directed** or **undirected**; the same graph may contain both kinds (mixed graph).

- **Directed edge** — has a source (`vertex1`) and a destination (`vertex2`). Written as an ordered pair (a, b).
- **Undirected edge** — has no source/destination distinction. Written as a set {a, b}.
- **Self-loop** — an edge where `vertex1 == vertex2`.

---

## Graph

A set of vertices and edges. This project supports **mixed graphs** — graphs containing both directed and undirected edges simultaneously.

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

_Avoid_: "Tour" for a closed trail — use **Circuit**. "Tour" is reserved for an Euler tour (a closed walk using every edge).

---

## Display Conventions

- **Graph window**: clicking a vertex shows its node properties in a fixed info box (degree, in-degree, out-degree, isolated, cutpoint, root).
- **Properties window**: shows a table of node properties for all vertices, plus the adjacency matrix and distance matrix.
- **Color coding on canvas**: root = green ring, cutpoint = orange ring, isolated = grey ring. Cutpoint color persists from the last Properties window computation.
