# ADR 0002 — Walks Are Edge Sequences

**Status:** Accepted

A walk is stored as a start vertex plus an ordered list of `Edge`s, not as a vertex sequence. Because the graph is mixed (ADR 0001), the same two vertices can be joined by both an undirected edge {a, b} and an arc (a, b), so a vertex sequence like "a, b" does not identify a walk, and trail/cycle classification depends on *which* edge was used. We chose this over forbidding mixed parallel edges, which would have been simpler but would have removed a legitimate mixed-graph configuration.

## Consequences

- Two walks/paths with the same vertex sequence but different edges are distinct: u -{u,v}-> v and u -(u,v)-> v are listed as two separate paths between u and v.
- The **vertex-disjoint width** in the pair box deliberately collapses paths with the same vertex sequence before computing, because internal vertex-disjointness (Menger / vertex connectivity) depends only on vertices. Do not "fix" this to use edge-aware paths.
