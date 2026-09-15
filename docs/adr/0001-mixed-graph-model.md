# ADR 0001 — Mixed Graph Model

**Status:** Accepted

## Context

The project needs to support both directed edges (with in/out degree) and undirected edges (with plain degree). The question was whether to enforce a single mode for the whole graph or allow both edge types simultaneously.

## Decision

Support mixed graphs — a single graph may contain both directed and undirected edges at the same time. Direction is a property of each individual `Edge`, not of the graph.

## Consequences

- `Edge` carries a `directed` boolean flag
- `Vertex` maintains three separate neighbor lists: `undirectedNeighbors`, `inNeighbors`, `outNeighbors`
- Degree, in-degree, and out-degree are always three distinct values per vertex
- Algorithms that require a purely undirected graph (e.g., cutpoint detection) treat all edges as undirected
