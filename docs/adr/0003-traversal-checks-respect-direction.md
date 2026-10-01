# ADR 0003 — Euler and Hamiltonian Checks Respect Direction

**Status:** Accepted

Euler trail/tour and Hamiltonian path/cycle are defined as kinds of walk (CONTEXT.md), so their checks obey the walk rules: arcs are traversed only source → destination, and the result is an actual edge-aware `Walk` (ADR 0002), not a yes/no derived from neighbour sets. This deliberately departs from ADR 0001's habit of treating all edges as undirected (as cutpoint/bridge detection still does). We chose it over the simpler "ignore direction" approach because that approach reports "Yes" for graphs where no valid walk exists, contradicting the Build Walk tool.

## Consequences

- One search function per concept returns the walk it found (or none). The Graph Summary's Yes/No and the "Find …" menu items both call it, so they can never disagree.
- Purely undirected or purely directed graphs use degree conditions + Hierholzer (no size cap). Mixed graphs use exponential backtracking, capped (≈30 edges for Euler, 20 vertices for Hamiltonian), and show "> cap" beyond that.
- Small cases follow the glossary literally, not textbook convention: a self-loop or {a,b}+(b,a) can be a Hamiltonian cycle; an edgeless graph has an Euler trail (trivial walk) but no Euler tour.
