# TODO

- [ ] **Zoom and pan on the Graph canvas.** `GraphRenderer` already draws in graph coordinates under a transform (the thumbnail and Induced Subgraph use `GraphRenderer.fit`); the canvas would need a view transform and every mouse handler converted from screen to graph coordinates.
- [ ] **Properties view as real Swing tables (UI phase 2).** Currently painted pixel-by-pixel; can't select, copy or sort, and matrix cells are 20px wide.
- [ ] **Save walks in graph files.** Graph files currently store only the graph (vertices, edges, weights, roots). Consider optionally saving the built walk (e.g. a found Euler tour) so it reopens with the file. Walk steps must reference specific edges, not just vertex pairs, since parallel edges exist (ADR 0002).
- [ ] **Dijkstra with parallel edges.** `EdgeRegistry` keeps one weight per direction (first undirected edge, or last arc), not the minimum, so with parallel edges of different weights the weighted distance can be too long. Take the minimum per direction.
