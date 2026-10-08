# Properties Tab — Grilling Session Handoff

The next piece of work is rebuilding the **Properties tab**. Run a `/grill-with-docs` session on it first, then write a plan in `docs/superpowers/plans/`, then implement with subagents. That's how the canvas and side panel were done. This file is everything the grilling session needs to start cold.

**Start with:** `/grill-with-docs Rebuild the Properties tab as real Swing components` and point the session at this file.

---

## State of master (2026-10-08)

`master` is at `a206d1d`, pushed to `github-personal:jbarguilles/graph-theory`. 247 tests pass. Recent work this builds on:

| Work | Plan | What it changed for this tab |
|---|---|---|
| Canvas rendering | `2026-10-08-canvas-rendering.md` | `GraphRenderer` draws every view. The tab's thumbnail uses it, with the **minimum vertex/edge cut** dashed in magenta. |
| Side panel | `2026-10-08-side-panel.md` | The Graph tab has real Swing components: `SidePanel`, with wording in `PanelText` and pair analysis in `PairSummary`. This is the pattern to reuse here. |
| Degree distributions | commit `a206d1d` | `DegreeDistribution` (tested) is drawn as stacked charts at the bottom-left of this tab. |
| Dijkstra fix | commit `074a5ff` | `EdgeRegistry` keeps the cheapest parallel edge in each direction. |

Contributors: **jbarguilles** (owner; opens PRs as this gh account) and **rcoporto** (commits straight to master; wrote the degree distribution). Pull before branching.

---

## Read first

- `CONTEXT.md`: the glossary is the spec. In particular *Node Properties*, *Degree distribution*, *Graph* (Minimum vertex/edge cut), *Walk* (Length, **Distance**, **Weight of a walk**, **Weighted distance**, **Lightest path**), and *Display Conventions* (the Properties tab, colours).
- `docs/adr/0001-mixed-graph-model.md`: graphs mix directed and undirected edges, and every definition has to say what it means for arcs.
- `docs/adr/0002-edge-aware-walks.md`: parallel edges are distinct.
- `TODO.md`: three items land here (see *Agenda*).

---

## What the tab is today

Everything is hand-painted in one `JPanel.paintComponent` (`Canvas.buildPropertiesPanel`, around `Canvas.java:540`), using hard-coded coordinates and the `GraphProperties.draw*` methods. A separately hand-calculated preferred size sets the scroll height (`Canvas.refreshPropertiesScrollSize`). It's recomputed when the tab opens and after every edit made while it's open (`computeProperties`).

| Section | Where | Drawn by | Notes |
|---|---|---|---|
| Graph picture | top-left, 400×300 | `Canvas.drawThumbnail` → `GraphRenderer` | The minimum cut is dashed magenta. Already fine. |
| Node Properties table | left, below the picture | `GraphProperties.drawNodePropertiesTable` (`:173`) | Columns Name, Deg, In, Out, Isolated, Cut, Root, shown as `true`/`false`. |
| Adjacency List | left, below the table | `drawAdjacencyList` (`:202`) | Returns its height. |
| Degree distributions | left, bottom | `drawDegreeDistributions` (`:280`) | Already glossary-correct; it only needs a proper home. |
| Adjacency matrix | right column | `drawAdjacencyMatrix` (`:137`) | 0/1 only: parallel edges collapse to 1 and the self-loop value is unclear. Headers are **red**. Cells are 20px. |
| Distance matrix | right, below | `drawDistanceMatrix` (`:154`) | Filled by `VertexPair.getShortestDistance()`, i.e. Dijkstra over **weights** (`:80`). |
| Graph Summary | right, below | `drawGraphSummary` (`:1443`) | About 30 `label: value` lines. |

The summary currently shows these (`GraphProperties.java:1551–1581`):
- Order |V|, Size |E|, Magnitude |V|+|E|
- Connectivity κ(G), Edge connectivity λ(G)
- Connected, Components, Strongly connected
- Bipartite, Density |E|/maxE
- Tree, Star, Empty, Complete, Simple, Cyclic/Acyclic
- Blocks, Nontrivial blocks, Nonseparable, Block vertices
- Bridges (purple)
- Euler Trail, Euler Tour, Hamiltonian Path, Hamiltonian Cycle (cached per graph change, `:424`)
- Coloring (greedy)
- Maximal / Maximum / Perfect / Stable matching

---

## Known problems to fix

1. **The distance matrix contradicts the glossary.** CONTEXT.md says *Distance* counts edges, but this matrix sums weights. TODO.md: show a distance matrix (edge counts) and, on a weighted graph, a separate weighted distance matrix. `PairSummary` already computes both for one pair, but it enumerates every path, which is far too slow for all n² pairs. Use BFS for distance and Dijkstra (`EdgeRegistry`) for weighted distance.
2. **Nothing can be selected, copied, sorted or resized.** Matrix cells are 20px wide, so a 15-vertex graph is hard to read.
3. **Red headers** in both matrices (`:144`, `:161`). Red now means "about to be removed" (CONTEXT.md, One meaning per colour).
4. **Layout is coordinate arithmetic.** Section positions and the scroll height are calculated separately by hand. That's how the degree charts once needed a magic 240px.
5. **Debug output on stdout.** `displayContainers` (`:86`, called from `computeProperties`) prints `-a-b…` path dumps and `D<k>(G)=…` via `System.out` every time Properties is recomputed. It also runs the greedy vertex-disjoint search the side panel stopped trusting (TODO.md, *Internally vertex-disjoint paths, exactly*).
6. **Minimum vertex cut of a complete graph** (TODO.md). `vertexConnectivity` falls back to "the first n−1 vertices", so the picture marks a set that disconnects nothing.
7. **Summary terms aren't in the glossary.** Order, Size, Magnitude, Connected/Components/Strongly connected (for mixed graphs?), Bipartite, Density (what is maxE with arcs and loops?), Tree, Star, Empty, Complete, Simple, Cyclic, Block, Nonseparable, the four matchings, and greedy colouring are all undefined in CONTEXT.md. Many are ambiguous for mixed graphs or multigraphs.
8. **`true`/`false`** in the Node Properties table, while the side panel says `yes`/`no`.

---

## Decisions already made that constrain this

- The **glossary is the spec**. Resolve terms in CONTEXT.md first, as they come up, with no implementation details in it.
- **One meaning per colour** (CONTEXT.md): blue = selection, red = removal, purple = bridge, teal = walk, amber = pair path, magenta dashed = minimum cut.
- **Distance** and **geodesic** count edges. **Weighted distance** and **lightest path** sum weights. Show the weighted versions only on a weighted graph (some weight ≠ 1).
- **Degree** counts undirected edges only. In- and out-degree are separate. There's no "total degree".
- **Analysis never changes the selection**, and viewing Properties is never an unsaved change.
- Pure logic lives in small tested classes and Swing views stay thin (`PairSummary` + `PanelText` + `SidePanel`). Views that refresh often must detect changes cheaply.

---

## Agenda for the grilling session (with recommended starting answers)

Ask one at a time, as before.

1. **Scope.** Recommend: the whole tab this round. The tab is one `paintComponent`, so a partial conversion leaves mixed layout code. Fix the K_n cut (6) and remove the stdout debug (5) along the way. Leave exact vertex-disjoint paths for later.
2. **Layout.** The candidates are one scrolling column of titled sections, two columns, or sub-tabs inside Properties (Overview / Matrices / Vertices / Distributions). Recommend: sub-tabs, because matrices for 20+ vertices need the whole width. Decide where the graph picture lives (on every sub-tab? only on Overview?).
3. **Matrices.**
   - Use `JTable`s with frozen row and column headers.
   - Adjacency entries should be **edge counts**, not 0/1, so parallel edges show. Decide what a self-loop counts as in the matrix: 1 or 2? Your course's convention decides, so ask, then put it in the glossary.
   - Directed cells: row → column for arcs; undirected edges are symmetric.
   - Distance matrix: edge counts, plus a weighted one only on weighted graphs. Show ∞ for unreachable.
   - Decide whether clicking a cell selects the pair on the Graph tab. Recommend no for now: analysis doesn't change the selection, and a cross-tab jump is surprising.
4. **Node Properties.** Recommend a sortable `JTable` with the same columns and words as the side panel (`PanelText.vertexProperties`), `yes`/`no`, and copyable.
5. **Summary.** This is the biggest glossary job. For each line: keep, rename or drop; define it in CONTEXT.md, including what it means for mixed graphs and multigraphs; and decide grouping (Size and order / Connectivity / Structure / Traversals / Colouring and matching). Expect to discover wrong definitions. Check each against the code before agreeing, as with Distance, where the code silently meant weighted.
6. **Degree distributions.** Keep the stacked charts, in their own section or sub-tab. Hover tooltips with exact counts are optional.
7. **Refresh and performance.** When does each section recompute? Today it's on tab open and after every edit. The Hamiltonian answers are capped and cached; check the matchings and connectivity on 15–20 vertices. A table model per section, updated only when its data changes (as in `SidePanel`).
8. **Copy and export.** Recommend Ctrl+C on any table copying tab-separated values (`JTable` does this). CSV export is optional; probably not this round.
9. **Focus and keys.** The side panel's text areas once stole keyboard focus from the canvas. Here the tab is separate, but check that switching back to Graph returns focus to the canvas.

---

## How the work runs (lessons from the last two branches)

- Branch off fresh `master`, commit the docs (CONTEXT.md, TODO.md, plan) first, then implement task by task with TDD. Use a spec review and then a quality review per task (Opus for reviews; Sonnet is fine for fully specified pure tasks), plus a final whole-branch review.
- **Build and test** (Git Bash, repo root):
  ```bash
  CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
  rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
    && MSYS_NO_PATHCONV=1 java -Djava.awt.headless=true -cp "out/test;$CP" org.junit.runner.JUnitCore \
       graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest \
       graphtheory.EdgeRegistryTest graphtheory.VertexNamesTest graphtheory.GraphFileTest graphtheory.LayoutTest \
       graphtheory.EditHistoryTest graphtheory.ToolsTest graphtheory.FileManagerTest graphtheory.ComponentsTest \
       graphtheory.EdgeShapesTest graphtheory.EdgeTest graphtheory.GraphRendererTest graphtheory.PairSummaryTest \
       graphtheory.PanelTextTest graphtheory.SidePanelTest graphtheory.GraphShapeTest graphtheory.DegreeDistributionTest
  MSYS_NO_PATHCONV=1 timeout 8 java -cp out/test graphtheory.Main; echo "exit $?"   # 124 + clean stderr = starts fine
  ```
  `MSYS_NO_PATHCONV=1` is required. Add new test classes to the list.
- **Line endings:** older files are CRLF in the working copy. `sed -i` and some tools rewrite whole files, so check `git diff --stat` shows only real lines.
- **Unit tests miss Swing wiring.** The focus bug was only caught by a non-headless probe that builds the real `Canvas`, drives it by reflection and posts real `KeyEvent`s. Budget a probe like that for the final review, and render views to PNG headlessly to look at layouts.
- **Repaint cost:** `Canvas.refresh()` runs on every mouse move. Anything it calls must skip work when nothing changed.
- Merge the way the owner prefers: no PR, merge to `master` and push.

## Out of scope

Zoom and pan; saving walks in graph files; exact internally vertex-disjoint paths (Menger). All are in TODO.md.
