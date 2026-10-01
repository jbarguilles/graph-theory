# ADR 0003 — Euler and Hamiltonian Checks Respect Direction

**Status:** Accepted

Euler trail/tour and Hamiltonian path/cycle are defined as kinds of walk (CONTEXT.md), so their checks obey the walk rules: arcs are traversed only source → destination, and the result is an actual edge-aware `Walk` (ADR 0002), not a yes/no derived from neighbour sets. This deliberately departs from ADR 0001's habit of treating all edges as undirected (as cutpoint/bridge detection still does). We chose it over the simpler "ignore direction" approach because that approach reports "Yes" for graphs where no valid walk exists, contradicting the Build Walk tool.

## Consequences

- One search function per concept returns the walk it found (or none). The Graph Summary's Yes/No and the "Find …" menu items both call it, so they can never disagree.
- Euler search is exact and polynomial for every graph, with no size cap: purely undirected or purely directed graphs use degree conditions + Hierholzer; mixed graphs first orient their undirected edges with a max-flow so every vertex balances, then run Hierholzer. Backtracking with a 30-edge cap was tried first and rejected: graphs of ~22 edges that pass the parity test but have no Euler trail took minutes.
- Hamiltonian search is a bitmask dynamic program over vertex subsets (about 2ⁿ·n steps), capped at 20 vertices ("> 20 vertices" beyond that) to bound memory. Plain backtracking was tried first and rejected: K11 plus one isolated vertex took over a minute. With n ≥ 3 the choice among parallel edges never matters, so the DP works on vertices and picks concrete edges afterwards; n ≤ 2 is handled by edge-aware special cases (a self-loop; two distinct edges). The Graph Summary caches its answers and recomputes them only after the graph changes, not on every repaint.
- Small cases follow the glossary literally, not textbook convention: a self-loop or {a,b}+(b,a) can be a Hamiltonian cycle; an edgeless graph has an Euler trail (trivial walk) but no Euler tour.
