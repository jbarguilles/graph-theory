# Node Properties — Spec Review Handoff

All 11 implementation tasks are complete and committed. Spec compliance reviews were skipped during implementation and need to be done as a dedicated pass.

---

## Commit Range to Review

| SHA | Task(s) | Description |
|---|---|---|
| `723300e` | Task 1 | Rename `connectedVertices` → `undirectedNeighbors` |
| `99baa5d` | Task 1 fix | Stray `connectedVertices` reference in commented-out Canvas code |
| `1103ef4` | Task 2 | `directed` flag + `isSelfLoop()` on Edge; arrowheads + self-loop drawing |
| `fd139c8` | Tasks 3 & 4 | JUnit test infra; `inNeighbors`/`outNeighbors`/`degree()`/`isIsolated()` on Vertex |
| `0ec054c` | Tasks 5 & 6 | Self-loops allowed; directed edge tool (Ctrl+D); Mark as Root menu |
| `d711441` | Tasks 7 & 8 | Tarjan's cutpoint algorithm; directed adjacency matrix; wire into Properties window |
| `a438f43` | Tasks 9–11 | Node properties table (Properties window); info box (Graph window); color rings |

**Base (pre-feature):** `c352f40`
**Head (all features):** `a438f43`

Full diff: `git diff c352f40..a438f43`

---

## What to Verify Per Task

### Task 1 — Rename
- [ ] No remaining `connectedVertices` anywhere in `src/` (including commented-out code)
- [ ] No remaining `addVertex(` calls anywhere in `src/`
- [ ] `addUndirectedNeighbor` is used consistently in Canvas.java and FileManager.java

### Task 2 — Edge model
- [ ] `Edge` constructor requires 3 args everywhere it's called (Canvas, FileManager)
- [ ] `isSelfLoop()` uses reference equality (`vertex1 == vertex2`)
- [ ] Self-loop draws a small circle, not a line
- [ ] Directed edge draws an arrowhead at `vertex2`
- [ ] `hasIntersection` returns false for self-loops

### Tasks 3 & 4 — Vertex degree model
- [ ] `Vertex` has `inNeighbors`, `outNeighbors`, `isCutpoint`, `isRoot` fields
- [ ] Constructor initialises all three Vector lists
- [ ] `degree()` counts undirected edges; self-loop adds 2
- [ ] `inDegree()` and `outDegree()` are simple `.size()` of their lists
- [ ] `getDegree()` returns sum of all three (used by comparators)
- [ ] `isIsolated()` returns true only when all three lists are empty
- [ ] `connectedToVertex(v)` checks all three lists
- [ ] 13 JUnit tests in VertexTest.java match the spec cases

### Tasks 5 & 6 — Canvas interaction
- [ ] `v != parentV` guard removed from `case 2` in `mouseReleased`
- [ ] `case 5` in `mouseReleased` creates directed Edge with `parentV → v`, updates `outNeighbors`/`inNeighbors`
- [ ] `case 5` also present in `mousePressed` and `mouseDragged` for drag feedback
- [ ] "Add Directed Edge" menu item in Tools menu with Ctrl+D accelerator
- [ ] "Mark as Root" menu item in Extras menu; toggles `isRoot` on `wasClicked` vertex

### Tasks 7 & 8 — Cutpoints
- [ ] `computeCutpoints` uses Tarjan's DFS, ignores direction (treats all edges as undirected)
- [ ] Self-loops skipped (`if (v == u) continue`) to avoid false cutpoint detection
- [ ] `getAllNeighborIndices` merges all three neighbor lists into a deduplicated set
- [ ] `generateAdjacencyMatrix` sets `matrix[i][j]=1` only (not mirrored) for directed edges
- [ ] `computeCutpoints` is called in the Properties window handler after `generateDistanceMatrix`
- [ ] 5 JUnit tests in GraphPropertiesTest.java match spec cases

### Tasks 9–11 — Display
- [ ] `drawNodePropertiesTable` renders in bottom-left area (below minimap) at `y = height/2 + 70`
- [ ] Table shows 7 columns: Name, Deg, In, Out, Isolated, Cut, Root
- [ ] `drawInfoBox` shown only when a vertex has `wasClicked = true`
- [ ] Info box appears in Graph window (`case 0`), not Properties window
- [ ] Info box shows all 6 node properties for the clicked vertex
- [ ] Color ring priority: `isRoot` (green) > `isCutpoint` (orange) > `isIsolated` (grey)
- [ ] Color ring drawn **before** the existing vertex fill (so it appears behind)

---

## Known Limitations (by design, not bugs)

- File save/load (`FileManager`) does not persist `directed` edges or `isRoot`. Loaded graphs treat all edges as undirected. Intentional — file format update is a separate plan.
- Cutpoint highlight in Graph window persists from the last Properties window open. If the graph changes after Properties was opened, cutpoint colors may be stale until Properties is reopened.
- JUnit tests cannot be run from command line — no JUnit jar on classpath. Run via NetBeans: right-click test file > Test File.

---

## Plan Reference

Full implementation plan: `docs/superpowers/plans/2026-09-15-node-properties.md`
Domain glossary: `CONTEXT.md`
Architecture decision: `docs/adr/0001-mixed-graph-model.md`
