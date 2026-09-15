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

## Display Conventions

- **Graph window**: clicking a vertex shows its node properties in a fixed info box (degree, in-degree, out-degree, isolated, cutpoint, root).
- **Properties window**: shows a table of node properties for all vertices, plus the adjacency matrix and distance matrix.
- **Color coding on canvas**: root = green ring, cutpoint = orange ring, isolated = grey ring. Cutpoint color persists from the last Properties window computation.
