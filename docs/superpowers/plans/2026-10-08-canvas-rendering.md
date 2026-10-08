# Canvas Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the canvas truthful (every parallel edge and self-loop visible and clickable, every vertex property visible at once, arrowheads not buried, analysis never touching selection) and then give it a cleaner look (anti-aliasing, outlined vertices with centred names, one meaning per colour, weights only on weighted graphs, resizable window with a dot grid). All three views draw through one renderer.

**Architecture:** Two new classes hold all drawing logic. `EdgeShapes` is pure geometry: for a list of edges it decides where each one goes (parallel edges fan out around the straight line, self-loops nest above their vertex) and answers "which edge is nearest this point". `GraphRenderer` paints vertices and edges onto any `Graphics2D` using `EdgeShapes`. The Graph canvas, the Properties thumbnail and the Induced Subgraph window all call it, with an `AffineTransform` to fit the graph where needed. `Vertex.draw`, `Edge.draw` and `Edge.hasIntersection` are deleted; the canvas stops using an off-screen image and paints directly in `paintComponent`.

**Tech Stack:** Java 17 (source-compatible with Java 7+, so no lambdas or `var`), Swing/AWT (`java.awt.geom`), JUnit 4.13.2.

**Read first:** `CONTEXT.md`. The sections *Display Conventions*, *Edge* (Weight, Weighted graph) and *Graph* (Minimum vertex cut, Minimum edge cut) are the spec for this plan. Also `docs/adr/0002-edge-aware-walks.md`: parallel edges are real, distinct edges, which is why they must each be visible.

---

## Decisions this plan implements

| # | Decision |
|---|---|
| 1 | Non-loop edges are grouped by **unordered vertex pair**, whatever their direction. A group of *n* edges gives edge *i* (in `edgeList` order) the offset `(i − (n−1)/2) × FAN_SPACING`, along a perpendicular fixed for the pair (worked out from the endpoint with the smaller name to the other). One edge is straight. Antiparallel arcs are just a group of two. |
| 2 | Self-loops on one vertex **nest**: all touch the same low point inside the vertex, radius grows 7px per loop, so the tops are 14px apart. Ordered by `edgeList`. |
| 3 | Picking an edge chooses the **nearest** edge within tolerance, not the first one in the list. |
| 4 | Rings: inner ring = cutpoint (orange) or isolated (grey); outer ring = root (green). Remove-hover (red) replaces both. |
| 5 | One meaning per colour: blue = selection, lighter blue = hover, red = remove-hover only, grey dashed = drag preview, purple = bridge, teal = walk, amber = pair path. |
| 6 | The minimum vertex/edge cut never sets `wasClicked`. It is drawn only in the Properties thumbnail: vertex cut as dashed rings, edge cut as dashed edges, in one magenta "cut" colour. Fields renamed from `witness*` to `minVertexCut` / `minEdgeCut`. |
| 7 | Vertices: 36px (radius 18) white or palette-coloured circle, 2px #333 outline, bold sans-serif name centred, 14pt shrinking to fit 4 characters. Selected: 3px blue outline + soft glow. Hovered: 3px light-blue outline. |
| 8 | Edges: 1.5px #444; any coloured state 3px. Filled arrowheads with the tip on the vertex outline, along the curve's tangent. |
| 9 | Weights shown only on a **weighted graph** (some edge weight ≠ 1), then all of them, in a white rounded box at the curve's real midpoint. Walk step labels go in the same box, in the highlight colour. |
| 10 | Window resizable; Auto Arrange and opening a file use the current canvas size; nothing is moved on resize. Faint dot grid, no snapping. |
| 11 | One `GraphRenderer` for canvas, Properties thumbnail and Induced Subgraph window. |

Out of scope: zoom/pan, Properties as Swing tables (both in `TODO.md`).

---

## How to build and test

There is no Ant/Maven on the PATH. Run from the repo root (`GraphTheory/`) in **Git Bash**:

```bash
CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -cp "out/test;$CP" org.junit.runner.JUnitCore \
     graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest \
     graphtheory.EdgeRegistryTest graphtheory.VertexNamesTest graphtheory.GraphFileTest graphtheory.LayoutTest \
     graphtheory.EditHistoryTest graphtheory.ToolsTest graphtheory.FileManagerTest graphtheory.ComponentsTest
```

`MSYS_NO_PATHCONV=1` is required. Without it, Git Bash mangles the `;`-separated classpath and you get `ClassNotFoundException: org.hamcrest.SelfDescribing`. **Add each new test class to the end of the list in the task that creates it** (`graphtheory.EdgeShapesTest`, `graphtheory.EdgeTest`, `graphtheory.GraphRendererTest`). Listing a class before it exists fails with "Could not find class". `out/` is git-ignored.

Baseline before starting: **140 tests pass.** The expected total after each task is stated in that task.

Launch the app (after compiling as above):

```bash
MSYS_NO_PATHCONV=1 java -cp out/test graphtheory.Main
```

The renderer tests draw into a `BufferedImage`, which works without a display.

---

## File Map

| File | Change |
|---|---|
| `src/graphtheory/EdgeShapes.java` | **New.** Fan offsets, loop nesting, curve/loop geometry, distance and nearest-edge picking |
| `src/graphtheory/GraphRenderer.java` | **New.** Paints vertices, edges, labels, rings, cut overlay and grid; `fit` transform; colour constants |
| `src/graphtheory/Vertex.java` | `RADIUS = 18` used by the hit-test; `draw`, `size1`, `size2` deleted |
| `src/graphtheory/Edge.java` | `isWeighted(List<Edge>)` added; `draw`, `hasIntersection`, `hasReverseArc` and drawing helpers deleted |
| `src/graphtheory/Layout.java` | `MARGIN` 25 → 30 so the outer root ring stays on screen |
| `src/graphtheory/GraphProperties.java` | `witnessVertices`/`witnessEdges` renamed `minVertexCut`/`minEdgeCut` |
| `src/graphtheory/Canvas.java` | Paints through `GraphRenderer` in `paintComponent`; nearest-edge picking; drag preview; cut no longer selects; thumbnail; induced subgraph; resizable; grid |
| `test/graphtheory/EdgeShapesTest.java` | **New** |
| `test/graphtheory/EdgeTest.java` | **New** |
| `test/graphtheory/GraphRendererTest.java` | **New** |
| `test/graphtheory/VertexTest.java`, `test/graphtheory/LayoutTest.java` | Updated for radius 18 and margin 30 |
| `TODO.md` | Resizable item done; zoom/pan item added |

`Canvas.java` is about 1560 lines. Steps that touch it name the method and quote the code being replaced, because line numbers shift between tasks.

---

## Task 0: Branch and baseline

- [ ] **Step 1: Create the feature branch**

```bash
git checkout master && git pull && git checkout -b feature/canvas-rendering
```

- [ ] **Step 2: Commit the docs that define this feature**

```bash
git add CONTEXT.md docs/superpowers/plans/2026-10-08-canvas-rendering.md
git commit -m "docs: display conventions, weighted graph, minimum cuts; canvas rendering plan

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 3: Confirm the baseline**

Run the build-and-test command. Expected: `OK (140 tests)`.

---

## Task 1: One vertex radius

Vertices are drawn 40px wide but edges stop 15px from the centre, so arrowheads are buried under the vertex border. From now on there is one radius, `Vertex.RADIUS = 18`, used for the hit-test, where edges stop, and (later) drawing.

**Files:**
- Modify: `src/graphtheory/Vertex.java`
- Modify: `src/graphtheory/Layout.java`
- Test: `test/graphtheory/VertexTest.java`, `test/graphtheory/LayoutTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `test/graphtheory/VertexTest.java`, before the final `}`:

```java
    @Test
    public void hasIntersection_insideRadius_true() {
        Vertex v = new Vertex("v", 100, 100);
        assertTrue(v.hasIntersection(100 + Vertex.RADIUS, 100));
    }

    @Test
    public void hasIntersection_justOutsideRadius_false() {
        Vertex v = new Vertex("v", 100, 100);
        assertFalse(v.hasIntersection(100 + Vertex.RADIUS + 1, 100));
    }

    @Test
    public void radius_is18() {
        assertEquals(18, Vertex.RADIUS);
    }
```

In `test/graphtheory/LayoutTest.java`, `clampInto_pullsOutsideVerticesToTheMargin`, change both `assertEquals(25, …)` to `assertEquals(30, …)`.

- [ ] **Step 2: Run the tests to verify they fail**

Run the build-and-test command. Expected: compile error `cannot find symbol: variable RADIUS`.

- [ ] **Step 3: Implement**

In `src/graphtheory/Vertex.java`, add below `public boolean isRoot;`:

```java
    /** Radius of a vertex on the canvas. Hit-testing, edge endpoints and drawing all use it. */
    public static final int RADIUS = 18;
```

Replace `hasIntersection`:

```java
    public boolean hasIntersection(int x, int y) {
        return Math.hypot(x - location.x, y - location.y) <= RADIUS;
    }
```

In `src/graphtheory/Layout.java` replace:

```java
    /** Distance kept from the canvas edge: vertex radius (20) plus its property ring. */
    public static final int MARGIN = 25;
```

with:

```java
    /** Distance kept from the canvas edge: vertex radius plus its outer (root) ring. */
    public static final int MARGIN = Vertex.RADIUS + 12;
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: `OK (143 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Vertex.java src/graphtheory/Layout.java test/graphtheory/VertexTest.java test/graphtheory/LayoutTest.java
git commit -m "refactor: one vertex radius (18) for hit-testing, layout margin and drawing

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 2: EdgeShapes, fanned parallel edges

**Files:**
- Create: `src/graphtheory/EdgeShapes.java`
- Test: `test/graphtheory/EdgeShapesTest.java`

Coordinates used in the tests: `a` at (0, 0), `b` at (100, 0). The pair's perpendicular, from `a` (smaller name) to `b`, is (0, 1), so a positive offset bends an edge downward (larger y).

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/EdgeShapesTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeShapesTest {

    private static final double EPS = 1e-9;

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 100, 0);
    private final Vertex c = new Vertex("c", 0, 100);

    private static EdgeShapes shapes(Edge... es) {
        return EdgeShapes.of(Arrays.asList(es));
    }

    @Test
    public void singleEdge_isStraight() {
        Edge e = new Edge(a, b, false);
        double[] cv = shapes(e).curve(e);
        assertEquals(50, cv[2], EPS);
        assertEquals(0, cv[3], EPS);
    }

    @Test
    public void singleEdge_endpointsOnVertexOutlines() {
        Edge e = new Edge(a, b, true);
        double[] cv = shapes(e).curve(e);
        assertEquals(Vertex.RADIUS, cv[0], EPS);
        assertEquals(100 - Vertex.RADIUS, cv[4], EPS);
    }

    @Test
    public void twoParallel_fanSymmetrically() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        assertEquals(-EdgeShapes.FAN_SPACING / 2, s.curve(e1)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING / 2, s.curve(e2)[3], EPS);
    }

    @Test
    public void threeParallel_middleIsStraight() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, true);
        Edge e3 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2, e3);
        assertEquals(-EdgeShapes.FAN_SPACING, s.curve(e1)[3], EPS);
        assertEquals(0, s.curve(e2)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING, s.curve(e3)[3], EPS);
    }

    @Test
    public void mixedDirections_shareOneFan_withPairPerpendicular() {
        Edge undirected = new Edge(a, b, false);
        Edge reversedArc = new Edge(b, a, true);
        EdgeShapes s = shapes(undirected, reversedArc);
        // The reversed arc still bends to +y: the perpendicular belongs to the pair, not the edge.
        assertEquals(-EdgeShapes.FAN_SPACING / 2, s.curve(undirected)[3], EPS);
        assertEquals(EdgeShapes.FAN_SPACING / 2, s.curve(reversedArc)[3], EPS);
    }

    @Test
    public void reversedArc_startsAtItsOwnSource() {
        Edge arc = new Edge(b, a, true);
        double[] cv = shapes(arc).curve(arc);
        assertEquals(100 - Vertex.RADIUS, cv[0], EPS);
        assertEquals(Vertex.RADIUS, cv[4], EPS);
    }

    @Test
    public void curvedEdge_endpointsStillOnOutlines() {
        Edge e1 = new Edge(a, b, true);
        Edge e2 = new Edge(a, b, true);
        double[] cv = shapes(e1, e2).curve(e2);
        assertEquals(Vertex.RADIUS, Math.hypot(cv[0] - a.location.x, cv[1] - a.location.y), EPS);
        assertEquals(Vertex.RADIUS, Math.hypot(cv[4] - b.location.x, cv[5] - b.location.y), EPS);
    }

    @Test
    public void differentPairs_doNotFan() {
        Edge ab = new Edge(a, b, false);
        Edge ac = new Edge(a, c, false);
        EdgeShapes s = shapes(ab, ac);
        assertEquals(0, s.curve(ab)[3], EPS);
        assertEquals(0, s.curve(ac)[2], EPS);
    }

    @Test
    public void parallelMidpoints_fartherApartThanTwoTolerances() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        double[] m1 = EdgeShapes.at(s.curve(e1), 0.5);
        double[] m2 = EdgeShapes.at(s.curve(e2), 0.5);
        assertTrue(Math.hypot(m1[0] - m2[0], m1[1] - m2[1]) > 2 * EdgeShapes.TOLERANCE);
    }

    @Test
    public void nearest_picksTheParallelEdgeClickedOn() {
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(a, b, false);
        EdgeShapes s = shapes(e1, e2);
        double[] m2 = EdgeShapes.at(s.curve(e2), 0.5);
        assertSame(e2, s.nearest(m2[0], m2[1]));
        double[] m1 = EdgeShapes.at(s.curve(e1), 0.5);
        assertSame(e1, s.nearest(m1[0], m1[1]));
    }

    @Test
    public void nearest_farFromEverything_null() {
        Edge e = new Edge(a, b, false);
        assertNull(shapes(e).nearest(50, 60));
    }

    @Test
    public void edgeNotInList_isStraight() {
        Edge listed = new Edge(a, b, false);
        Edge other = new Edge(a, b, false);
        assertEquals(0, shapes(listed).curve(other)[3], EPS);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.EdgeShapesTest` to the end of the test list and run. Expected: compile error `cannot find symbol: class EdgeShapes`.

- [ ] **Step 3: Implement**

Create `src/graphtheory/EdgeShapes.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each edge is drawn. Parallel edges between the same two vertices fan
 * out symmetrically and self-loops on one vertex nest above it, so every edge
 * can be seen and clicked on its own (CONTEXT.md, Display Conventions).
 * Drawing and hit-testing both use this, so what you see is what you click.
 */
public final class EdgeShapes {

    /** Gap between neighbouring control points in a fan; the curves' midpoints end up half this apart. */
    public static final double FAN_SPACING = 28;
    /** How close (pixels) a click must be to an edge to hit it. */
    public static final double TOLERANCE = 6.0;

    private static final int SAMPLES = 32;

    private final List<Edge> edges;
    private final Map<Edge, Double> fanOffset = new IdentityHashMap<Edge, Double>();

    private EdgeShapes(List<Edge> edges) {
        this.edges = edges;
    }

    /** Lays out the given edges. Order within a fan follows the list. */
    public static EdgeShapes of(List<Edge> edges) {
        EdgeShapes s = new EdgeShapes(edges);
        Map<String, List<Edge>> pairs = new LinkedHashMap<String, List<Edge>>();
        for (Edge e : edges) {
            if (e.isSelfLoop()) continue;
            String key = low(e).name + "\u0000" + high(e).name;
            List<Edge> group = pairs.get(key);
            if (group == null) {
                group = new ArrayList<Edge>();
                pairs.put(key, group);
            }
            group.add(e);
        }
        for (List<Edge> group : pairs.values()) {
            int n = group.size();
            for (int i = 0; i < n; i++) {
                s.fanOffset.put(group.get(i), (i - (n - 1) / 2.0) * FAN_SPACING);
            }
        }
        return s;
    }

    /** The endpoint with the smaller name; the pair's perpendicular is worked out from it. */
    private static Vertex low(Edge e) {
        return e.vertex1.name.compareTo(e.vertex2.name) <= 0 ? e.vertex1 : e.vertex2;
    }

    private static Vertex high(Edge e) {
        return low(e) == e.vertex1 ? e.vertex2 : e.vertex1;
    }

    /**
     * A non-loop edge as a quadratic curve {startX, startY, ctrlX, ctrlY, endX, endY}.
     * Start is on vertex1's outline and end on vertex2's, both pointing at the control point,
     * so an arrowhead at the end sits on the outline along the curve's tangent.
     */
    public double[] curve(Edge e) {
        Vertex a = low(e), b = high(e);
        double dx = b.location.x - a.location.x;
        double dy = b.location.y - a.location.y;
        double len = Math.hypot(dx, dy);
        double px = len == 0 ? 0 : -dy / len;
        double py = len == 0 ? -1 : dx / len;
        Double off = fanOffset.get(e);
        double o = off == null ? 0 : off;
        double cx = (a.location.x + b.location.x) / 2.0 + px * o;
        double cy = (a.location.y + b.location.y) / 2.0 + py * o;
        double[] s = towards(e.vertex1, cx, cy);
        double[] t = towards(e.vertex2, cx, cy);
        return new double[] { s[0], s[1], cx, cy, t[0], t[1] };
    }

    /** The point on v's outline in the direction of (x, y). */
    private static double[] towards(Vertex v, double x, double y) {
        double dx = x - v.location.x, dy = y - v.location.y;
        double len = Math.hypot(dx, dy);
        if (len == 0) return new double[] { v.location.x, v.location.y };
        return new double[] { v.location.x + dx / len * Vertex.RADIUS,
                              v.location.y + dy / len * Vertex.RADIUS };
    }

    /** The point at parameter t (0..1) on a curve from curve(). */
    public static double[] at(double[] c, double t) {
        double mt = 1 - t;
        return new double[] {
            mt * mt * c[0] + 2 * mt * t * c[2] + t * t * c[4],
            mt * mt * c[1] + 2 * mt * t * c[3] + t * t * c[5]
        };
    }

    /** Distance from (x, y) to the drawn edge. */
    public double distance(Edge e, double x, double y) {
        double[] c = curve(e);
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= SAMPLES; i++) {
            double[] p = at(c, i / (double) SAMPLES);
            best = Math.min(best, Math.hypot(x - p[0], y - p[1]));
        }
        return best;
    }

    /** The edge closest to (x, y) if it is within TOLERANCE, else null. Ties go to the earlier edge. */
    public Edge nearest(double x, double y) {
        Edge best = null;
        double bestD = TOLERANCE;
        for (Edge e : edges) {
            double d = distance(e, x, y);
            if (d < bestD || (best == null && d == bestD)) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }
}
```

(Self-loops are handled in Task 3; until then `distance` treats them as a degenerate curve.)

- [ ] **Step 4: Run the tests to verify they pass**

Expected: `OK (155 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/EdgeShapes.java test/graphtheory/EdgeShapesTest.java
git commit -m "feat: EdgeShapes fans parallel edges per vertex pair and picks the nearest edge

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 3: EdgeShapes, nested self-loops

Every loop on a vertex touches the same low point, 4px inside the vertex outline (hidden under the vertex). Loop *i* has radius `12 + 7i`, so each loop's top is 14px above the previous one.

**Files:**
- Modify: `src/graphtheory/EdgeShapes.java`
- Test: `test/graphtheory/EdgeShapesTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `EdgeShapesTest`, before the final `}`:

```java
    @Test
    public void firstLoop_sitsAboveVertex() {
        Vertex v = new Vertex("v", 100, 100);
        Edge loop = new Edge(v, v, false);
        double[] l = shapes(loop).loop(loop);
        assertEquals(100, l[0], EPS);
        assertEquals(EdgeShapes.LOOP_RADIUS, l[2], EPS);
        // lowest point is just inside the vertex outline
        assertEquals(100 - Vertex.RADIUS + 4, l[1] + l[2], EPS);
    }

    @Test
    public void loops_nestWithTopsFourteenApart() {
        Vertex v = new Vertex("v", 100, 100);
        Edge l1 = new Edge(v, v, false);
        Edge l2 = new Edge(v, v, true);
        EdgeShapes s = shapes(l1, l2);
        double top1 = s.loop(l1)[1] - s.loop(l1)[2];
        double top2 = s.loop(l2)[1] - s.loop(l2)[2];
        assertEquals(14, top1 - top2, EPS);
    }

    @Test
    public void loops_onDifferentVertices_countSeparately() {
        Vertex v = new Vertex("v", 100, 100);
        Vertex w = new Vertex("w", 300, 100);
        Edge lv = new Edge(v, v, false);
        Edge lw = new Edge(w, w, false);
        EdgeShapes s = shapes(lv, lw);
        assertEquals(EdgeShapes.LOOP_RADIUS, s.loop(lw)[2], EPS);
    }

    @Test
    public void nearest_picksTheNestedLoopClickedOn() {
        Vertex v = new Vertex("v", 100, 100);
        Edge l1 = new Edge(v, v, false);
        Edge l2 = new Edge(v, v, false);
        EdgeShapes s = shapes(l1, l2);
        double[] o = s.loop(l2);
        assertSame(l2, s.nearest(o[0], o[1] - o[2]));
        double[] i = s.loop(l1);
        assertSame(l1, s.nearest(i[0], i[1] - i[2]));
    }

    @Test
    public void loopAndEdge_sameList_bothLaidOut() {
        Edge ab = new Edge(a, b, false);
        Edge loop = new Edge(a, a, false);
        EdgeShapes s = shapes(ab, loop);
        assertEquals(0, s.curve(ab)[3], EPS);
        assertEquals(EdgeShapes.LOOP_RADIUS, s.loop(loop)[2], EPS);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: compile error `cannot find symbol: method loop(Edge)`.

- [ ] **Step 3: Implement**

In `EdgeShapes.java`, add below `TOLERANCE`:

```java
    /** Radius of the first self-loop on a vertex. */
    public static final double LOOP_RADIUS = 12;
    /** Each further loop on the same vertex is this much bigger in radius, so its top is twice this higher. */
    public static final double LOOP_STEP = 7;
    /** How far every loop's lowest point sits inside the vertex outline (hidden under the vertex). */
    private static final double LOOP_SINK = 4;
```

Add a field below `fanOffset`:

```java
    private final Map<Edge, Integer> loopIndex = new IdentityHashMap<Edge, Integer>();
```

In `of`, replace `if (e.isSelfLoop()) continue;` with:

```java
            if (e.isSelfLoop()) {
                Integer count = loopsOn.get(e.vertex1);
                int i = count == null ? 0 : count;
                s.loopIndex.put(e, i);
                loopsOn.put(e.vertex1, i + 1);
                continue;
            }
```

and declare, just above the `for` loop in `of`:

```java
        Map<Vertex, Integer> loopsOn = new IdentityHashMap<Vertex, Integer>();
```

Add the method below `curve`:

```java
    /**
     * A self-loop as a circle {centreX, centreY, radius}. All loops on a vertex share
     * the same lowest point, so bigger ones nest around smaller ones.
     */
    public double[] loop(Edge e) {
        Integer i = loopIndex.get(e);
        double r = LOOP_RADIUS + LOOP_STEP * (i == null ? 0 : i);
        double bottom = e.vertex1.location.y - Vertex.RADIUS + LOOP_SINK;
        return new double[] { e.vertex1.location.x, bottom - r, r };
    }
```

At the top of `distance`, add:

```java
        if (e.isSelfLoop()) {
            double[] l = loop(e);
            return Math.abs(Math.hypot(x - l[0], y - l[1]) - l[2]);
        }
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: `OK (160 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/EdgeShapes.java test/graphtheory/EdgeShapesTest.java
git commit -m "feat: nest self-loops on the same vertex so each is visible and clickable

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 4: Weighted graph

**Files:**
- Modify: `src/graphtheory/Edge.java`
- Test: `test/graphtheory/EdgeTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/EdgeTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 100, 0);

    @Test
    public void isWeighted_noEdges_false() {
        assertFalse(Edge.isWeighted(Collections.<Edge>emptyList()));
    }

    @Test
    public void isWeighted_allWeightOne_false() {
        assertFalse(Edge.isWeighted(Arrays.asList(new Edge(a, b, false), new Edge(b, a, true))));
    }

    @Test
    public void isWeighted_oneEdgeNotOne_true() {
        Edge heavy = new Edge(a, b, true);
        heavy.setWeight(5);
        assertTrue(Edge.isWeighted(Arrays.asList(new Edge(a, b, false), heavy)));
    }

    @Test
    public void isWeighted_weightZero_true() {
        Edge free = new Edge(a, b, false);
        free.setWeight(0);
        assertTrue(Edge.isWeighted(Arrays.asList(free)));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.EdgeTest` to the test list. Expected: compile error `cannot find symbol: method isWeighted`.

- [ ] **Step 3: Implement**

In `src/graphtheory/Edge.java`, add `import java.util.List;` with the imports and add below `isSelfLoop()`:

```java
    /** A weighted graph has at least one edge whose weight is not 1 (CONTEXT.md). */
    public static boolean isWeighted(List<Edge> edges) {
        for (Edge e : edges) {
            if (e.weight != 1) return true;
        }
        return false;
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: `OK (164 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Edge.java test/graphtheory/EdgeTest.java
git commit -m "feat: Edge.isWeighted, true when some edge weight is not 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 5: GraphRenderer

The renderer draws in graph coordinates; callers apply any transform first. It exists alongside the old `draw` methods until Task 6 switches the canvas over.

**Files:**
- Create: `src/graphtheory/GraphRenderer.java`
- Test: `test/graphtheory/GraphRendererTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/GraphRendererTest.java`:

```java
package graphtheory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphRendererTest {

    private static BufferedImage blank() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        return img;
    }

    private static Color pixel(BufferedImage img, int x, int y) {
        return new Color(img.getRGB(x, y));
    }

    private static void paint(BufferedImage img, List<Vertex> vs, List<Edge> es, GraphRenderer.Options o) {
        Graphics2D g = img.createGraphics();
        GraphRenderer.paint(g, vs, es, o);
        g.dispose();
    }

    @Test
    public void rootCutpoint_showsBothRings() {
        Vertex v = new Vertex("v", 100, 100);
        v.isRoot = true;
        v.isCutpoint = true;
        BufferedImage img = blank();
        paint(img, Arrays.asList(v), Collections.<Edge>emptyList(), new GraphRenderer.Options());
        // cutpoint ring is centred on radius + 4, root ring on radius + 8, both 2px wide
        Color inner = pixel(img, 100 + Vertex.RADIUS + 3, 100);
        Color outer = pixel(img, 100 + Vertex.RADIUS + 7, 100);
        assertTrue("inner ring orange, was " + inner, inner.getRed() > 200 && inner.getBlue() < 100);
        assertTrue("outer ring green, was " + outer,
                outer.getGreen() > outer.getRed() + 50 && outer.getGreen() > outer.getBlue() + 50);
    }

    @Test
    public void removeHover_replacesRings_onlyWithInteraction() {
        Vertex v = new Vertex("v", 100, 100);
        v.isRoot = true;
        v.removeHover = true;
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        BufferedImage img = blank();
        paint(img, Arrays.asList(v), Collections.<Edge>emptyList(), o);
        Color root = pixel(img, 100 + Vertex.RADIUS + 7, 100);
        assertFalse("no green root ring while remove-hovered",
                root.getGreen() > root.getRed() + 50);
    }

    @Test
    public void edgeColor_highlightBeatsBridge() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        e.isBridge = true;
        e.highlight = Color.CYAN;
        assertEquals(Color.CYAN, GraphRenderer.edgeColor(e, new GraphRenderer.Options()));
    }

    @Test
    public void edgeColor_removeHoverIgnoredWithoutInteraction() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        e.removeHover = true;
        assertEquals(GraphRenderer.EDGE, GraphRenderer.edgeColor(e, new GraphRenderer.Options()));
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        assertEquals(GraphRenderer.REMOVE, GraphRenderer.edgeColor(e, o));
    }

    @Test
    public void edgeColor_cutEdge() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.cutEdges = new HashSet<Edge>(Arrays.asList(e));
        assertEquals(GraphRenderer.CUT, GraphRenderer.edgeColor(e, o));
    }

    @Test
    public void nameFont_shortName_isFullSize() {
        Graphics2D g = blank().createGraphics();
        assertEquals(GraphRenderer.NAME_MAX, GraphRenderer.nameFont(g, "1").getSize());
    }

    @Test
    public void nameFont_fourChars_fitsInsideVertex() {
        Graphics2D g = blank().createGraphics();
        Font f = GraphRenderer.nameFont(g, "WWWW");
        assertTrue(g.getFontMetrics(f).stringWidth("WWWW") <= 2 * Vertex.RADIUS - 8
                || f.getSize() == GraphRenderer.NAME_MIN);
    }

    @Test
    public void fit_singleVertex_centred() {
        AffineTransform t = GraphRenderer.fit(Arrays.asList(new Vertex("a", 10, 10)), 200, 100, 40);
        Point2D p = t.transform(new Point2D.Double(10, 10), null);
        assertEquals(100, p.getX(), 1e-9);
        assertEquals(50, p.getY(), 1e-9);
    }

    @Test
    public void fit_largeGraph_scaledIntoMargins() {
        List<Vertex> vs = Arrays.asList(new Vertex("a", 0, 0), new Vertex("b", 1000, 0));
        AffineTransform t = GraphRenderer.fit(vs, 200, 200, 50);
        assertEquals(50, t.transform(new Point2D.Double(0, 0), null).getX(), 1e-9);
        assertEquals(150, t.transform(new Point2D.Double(1000, 0), null).getX(), 1e-9);
    }

    @Test
    public void fit_smallGraph_neverScaledUp() {
        List<Vertex> vs = Arrays.asList(new Vertex("a", 0, 0), new Vertex("b", 10, 0));
        assertEquals(1.0, GraphRenderer.fit(vs, 200, 200, 50).getScaleX(), 1e-9);
    }

    @Test
    public void paint_everyKindOfEdge_doesNotThrow() {
        Vertex a = new Vertex("a", 50, 100);
        Vertex b = new Vertex("bcde", 150, 100);
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(b, a, true);
        e2.setWeight(3);
        e2.stepLabel = "#1,2";
        e2.highlight = Color.ORANGE;
        Edge loop1 = new Edge(a, a, true);
        Edge loop2 = new Edge(a, a, false);
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        o.cutVertices = new HashSet<Vertex>(Arrays.asList(b));
        o.cutEdges = new HashSet<Edge>(Arrays.asList(e1));
        paint(blank(), Arrays.asList(a, b), Arrays.asList(e1, e2, loop1, loop2), o);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.GraphRendererTest` to the test list. Expected: compile error `cannot find symbol: class GraphRenderer`.

- [ ] **Step 3: Implement**

Create `src/graphtheory/GraphRenderer.java`:

```java
package graphtheory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Draws a graph. The Graph canvas, the Properties thumbnail and the Induced
 * Subgraph window all use it. Draws in graph coordinates; apply a transform
 * (see fit) first to scale or move. Colours follow CONTEXT.md, Display Conventions.
 */
public final class GraphRenderer {

    public static final Color OUTLINE  = new Color(0x33, 0x33, 0x33);
    public static final Color EDGE     = new Color(0x44, 0x44, 0x44);
    public static final Color SELECT   = new Color(30, 100, 220);
    public static final Color HOVER    = new Color(120, 170, 240);
    public static final Color REMOVE   = new Color(220, 0, 0);
    public static final Color BRIDGE   = new Color(150, 0, 200);
    public static final Color ROOT     = new Color(0, 170, 0);
    public static final Color CUTPOINT = new Color(255, 140, 0);
    public static final Color ISOLATED = Color.GRAY;
    public static final Color CUT      = new Color(200, 0, 120);

    private static final Color GLOW         = new Color(30, 100, 220, 70);
    private static final Color LABEL_TEXT   = new Color(80, 80, 80);
    private static final Color LABEL_BORDER = new Color(200, 200, 200);
    private static final Font  LABEL_FONT   = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    public static final int NAME_MAX = 14;
    public static final int NAME_MIN = 9;

    /** What to draw besides the graph itself. */
    public static class Options {
        /** Show selection, hover and remove-hover (the Graph canvas only). */
        public boolean interaction;
        /** Minimum vertex cut, drawn as dashed rings. */
        public Set<Vertex> cutVertices = Collections.emptySet();
        /** Minimum edge cut, drawn as dashed edges. */
        public Set<Edge> cutEdges = Collections.emptySet();
    }

    private GraphRenderer() {}

    public static void paint(Graphics2D g, List<Vertex> vertices, List<Edge> edges, Options o) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();

        EdgeShapes shapes = EdgeShapes.of(edges);
        boolean weighted = Edge.isWeighted(edges);
        for (Edge e : edges) drawEdge(g, shapes, e, o);
        for (Edge e : edges) drawEdgeLabel(g, shapes, e, weighted);
        for (Vertex v : vertices) drawVertex(g, v, o);

        g.setStroke(oldStroke);
        g.setFont(oldFont);
    }

    /** Scales (never up) and centres the vertices' bounding box in a w×h area, keeping margin free. */
    public static AffineTransform fit(List<Vertex> vs, int w, int h, int margin) {
        AffineTransform t = new AffineTransform();
        if (vs.isEmpty()) return t;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Vertex v : vs) {
            minX = Math.min(minX, v.location.x);
            minY = Math.min(minY, v.location.y);
            maxX = Math.max(maxX, v.location.x);
            maxY = Math.max(maxY, v.location.y);
        }
        double sw = Math.max(1, maxX - minX);
        double sh = Math.max(1, maxY - minY);
        double s = Math.min(1.0, Math.min((w - 2.0 * margin) / sw, (h - 2.0 * margin) / sh));
        t.translate(w / 2.0 - (minX + maxX) / 2.0 * s, h / 2.0 - (minY + maxY) / 2.0 * s);
        t.scale(s, s);
        return t;
    }

    /** One colour per meaning; the first state that applies wins. */
    static Color edgeColor(Edge e, Options o) {
        if (o.interaction && e.removeHover) return REMOVE;
        if (o.interaction && e.wasClicked)  return SELECT;
        if (o.interaction && e.wasFocused)  return HOVER;
        if (o.cutEdges.contains(e))         return CUT;
        if (e.highlight != null)            return e.highlight;
        if (e.isBridge)                     return BRIDGE;
        return EDGE;
    }

    private static void drawEdge(Graphics2D g, EdgeShapes shapes, Edge e, Options o) {
        Color c = edgeColor(e, o);
        float width = c == EDGE ? 1.5f : 3f;
        g.setColor(c);
        g.setStroke(c == CUT ? dashed(width) : new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (e.isSelfLoop()) {
            double[] l = shapes.loop(e);
            g.draw(circle(l[0], l[1], l[2]));
            // clockwise, at the top of the loop
            if (e.directed) arrowhead(g, l[0], l[1] - l[2], 0.0, 9);
        } else {
            double[] cv = shapes.curve(e);
            g.draw(new QuadCurve2D.Double(cv[0], cv[1], cv[2], cv[3], cv[4], cv[5]));
            if (e.directed) arrowhead(g, cv[4], cv[5], Math.atan2(cv[5] - cv[3], cv[4] - cv[2]), 12);
        }
    }

    private static void arrowhead(Graphics2D g, double tipX, double tipY, double angle, double size) {
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - size * Math.cos(angle - Math.PI / 7), tipY - size * Math.sin(angle - Math.PI / 7));
        head.lineTo(tipX - size * Math.cos(angle + Math.PI / 7), tipY - size * Math.sin(angle + Math.PI / 7));
        head.closePath();
        g.fill(head);
    }

    /** Weight (on a weighted graph) and walk step numbers, in one box on the edge's midpoint. */
    private static void drawEdgeLabel(Graphics2D g, EdgeShapes shapes, Edge e, boolean weighted) {
        String weight = weighted ? String.valueOf(e.weight) : "";
        String step = e.stepLabel == null ? "" : e.stepLabel;
        if (weight.isEmpty() && step.isEmpty()) return;
        String first = weight.isEmpty() || step.isEmpty() ? weight : weight + " ";

        double x, y;
        if (e.isSelfLoop()) {
            double[] l = shapes.loop(e);
            x = l[0] + l[2] * 0.71 + 12;
            y = l[1] - l[2] * 0.71;
        } else {
            double[] p = EdgeShapes.at(shapes.curve(e), 0.5);
            x = p[0];
            y = p[1];
        }

        g.setFont(LABEL_FONT);
        FontMetrics fm = g.getFontMetrics();
        double bw = fm.stringWidth(first + step) + 8;
        double bh = fm.getAscent() + 4;
        double bx = x - bw / 2, by = y - bh / 2;
        RoundRectangle2D box = new RoundRectangle2D.Double(bx, by, bw, bh, 6, 6);
        g.setColor(Color.WHITE);
        g.fill(box);
        g.setStroke(new BasicStroke(1f));
        g.setColor(LABEL_BORDER);
        g.draw(box);

        float tx = (float) (bx + 4);
        float ty = (float) (by + 2 + fm.getAscent() - fm.getDescent() / 2.0);
        g.setColor(LABEL_TEXT);
        g.drawString(first, tx, ty);
        if (!step.isEmpty()) {
            g.setColor(e.highlight != null ? e.highlight : Color.BLACK);
            g.drawString(step, tx + fm.stringWidth(first), ty);
        }
    }

    private static void drawVertex(Graphics2D g, Vertex v, Options o) {
        double x = v.location.x, y = v.location.y, r = Vertex.RADIUS;
        boolean selected = o.interaction && v.wasClicked;
        boolean hovered = o.interaction && v.wasFocused && !selected;

        if (selected) {
            g.setColor(GLOW);
            g.setStroke(new BasicStroke(6f));
            g.draw(circle(x, y, r + 2));
        }

        g.setColor(v.colorId >= 0 ? Vertex.PALETTE[v.colorId % Vertex.PALETTE.length] : Color.WHITE);
        g.fill(circle(x, y, r));
        g.setColor(selected ? SELECT : hovered ? HOVER : OUTLINE);
        g.setStroke(new BasicStroke(selected || hovered ? 3f : 2f));
        g.draw(circle(x, y, r));

        if (o.interaction && v.removeHover) {
            ring(g, x, y, r + 6, REMOVE, new BasicStroke(3f));
        } else {
            if (v.isCutpoint)       ring(g, x, y, r + 4, CUTPOINT, new BasicStroke(2f));
            else if (v.isIsolated()) ring(g, x, y, r + 4, ISOLATED, new BasicStroke(2f));
            if (v.isRoot)           ring(g, x, y, r + 8, ROOT, new BasicStroke(2f));
            if (o.cutVertices.contains(v)) ring(g, x, y, r + 12, CUT, dashed(2f));
        }

        drawName(g, v.name, x, y);
    }

    private static void ring(Graphics2D g, double x, double y, double r, Color c, Stroke s) {
        g.setColor(c);
        g.setStroke(s);
        g.draw(circle(x, y, r));
    }

    /** The bold name font, shrunk from NAME_MAX until the name fits inside the vertex. */
    static Font nameFont(Graphics2D g, String name) {
        for (int size = NAME_MAX; size > NAME_MIN; size--) {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, size);
            if (g.getFontMetrics(f).stringWidth(name) <= 2 * Vertex.RADIUS - 8) return f;
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, NAME_MIN);
    }

    private static void drawName(Graphics2D g, String name, double x, double y) {
        g.setFont(nameFont(g, name));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.BLACK);
        g.drawString(name,
                (float) (x - fm.stringWidth(name) / 2.0),
                (float) (y + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private static Shape circle(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
    }

    private static BasicStroke dashed(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10f, new float[] { 6f, 4f }, 0f);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: `OK (175 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/GraphRenderer.java test/graphtheory/GraphRendererTest.java
git commit -m "feat: GraphRenderer draws vertices, fanned edges, nested loops, rings and labels

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 6: The Graph canvas draws through GraphRenderer

The canvas stops drawing into a fixed off-screen image and paints directly in `paintComponent`. Edge picking uses `EdgeShapes.nearest`. The old drawing code in `Vertex` and `Edge` is deleted.

**Files:**
- Modify: `src/graphtheory/Canvas.java`
- Modify: `src/graphtheory/Vertex.java`
- Modify: `src/graphtheory/Edge.java`

- [ ] **Step 1: Remove the off-screen image fields**

In `Canvas`, delete these two field lines:

```java
    private Graphics2D graphic;
```
```java
    private Image canvasImage;
```

and add, below `private String pressBefore = null;`:

```java
    // Where the mouse is while dragging out a new edge (Add/Directed Edge tools); null otherwise.
    private Point dragPoint = null;
```

- [ ] **Step 2: Simplify `refresh()`**

Replace the body of `refresh()`:

```java
    public void refresh() {
        recomputeGraphProperties();
        EdgeRegistry.rebuild(edgeList);
        applyHighlights();
        erase();
        for (Edge e : edgeList) {
            e.draw(graphic);
        }
        for (Vertex v : vertexList) {
            v.draw(graphic);
        }
        drawWalkMarkers(graphic);
        canvas.repaint();
```

with:

```java
    public void refresh() {
        recomputeGraphProperties();
        EdgeRegistry.rebuild(edgeList);
        applyHighlights();
        canvas.repaint();
```

(leave the rest of the method, from `if (propertiesContent != null)` on, as it is).

- [ ] **Step 3: Remove the off-screen helpers**

Replace `setVisible` with:

```java
    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
```

Delete the methods `erase()`, `erase(int x, int y, int x1, int y2)`, `drawString(String text, int x, int y, float size)` and `drawLine(int x1, int y1, int x2, int y2)`. Check nothing else calls them:

```bash
grep -n "erase(\|drawLine(\|canvasImage\|graphic\b" src/graphtheory/Canvas.java
```

Expected after Steps 4–5: only the `buildPropertiesPanel` thumbnail line `canvasImage.getScaledInstance(...)` remains. Task 7 replaces it. Until then, change that line and the `draw3DRect` line under it to:

```java
                g2.setColor(Color.BLACK);
                g2.drawRect(10, 10, width / 2, height / 2);
```

- [ ] **Step 4: Stop drawing in the click handler**

In `InputListener.handleClick`, `case 1:`, delete the line:

```java
                        v.draw(graphic);
```

- [ ] **Step 5: Drag preview as state**

In `InputListener.mouseDragged`, replace:

```java
                    case 2:
                    case 5: {
                        refresh();
                        Vertex from = pressedVertex();
                        if (from != null) {
                            graphic.setColor(Color.RED);
                            drawLine(from.location.x, from.location.y, e.getX(), e.getY());
                        }
                        canvas.repaint();
                        return;
                    }
```

with:

```java
                    case 2:
                    case 5: {
                        dragPoint = pressedVertex() != null ? e.getPoint() : null;
                        refresh();
                        return;
                    }
```

In `InputListener.mouseReleased`, add as the first line of the method:

```java
            dragPoint = null;
```

Add this method to `Canvas`, just above `drawWalkMarkers`:

```java
    /** Grey dashed line from the pressed vertex to the mouse while dragging out an edge. */
    private void drawDragPreview(Graphics2D g) {
        if (dragPoint == null || clickedVertexIndex < 0 || clickedVertexIndex >= vertexList.size()) return;
        Vertex from = vertexList.get(clickedVertexIndex);
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10f, new float[] { 6f, 4f }, 0f));
        g.drawLine(from.location.x, from.location.y, dragPoint.x, dragPoint.y);
        g.setStroke(new BasicStroke(1f));
    }
```

- [ ] **Step 6: Paint in `CanvasPane.paintComponent`**

Replace the whole `CanvasPane` class:

```java
    private class CanvasPane extends JPanel {

        public void paint(Graphics g) {
            switch (selectedWindow) {
                ...
            }
        }
    }
```

with:

```java
    private class CanvasPane extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(backgroundColour);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (selectedWindow != 0) return;

            GraphRenderer.Options o = new GraphRenderer.Options();
            o.interaction = true;
            GraphRenderer.paint(g2, vertexList, edgeList, o);
            drawDragPreview(g2);
            drawWalkMarkers(g2);

            g2.setStroke(new BasicStroke(1f));
            g2.setFont(getFont());
            drawInfoBox(g2);
            drawPairInfoBox(g2);
            drawWalkInfoBox(g2);
        }
    }
```

- [ ] **Step 7: Pick the nearest edge everywhere**

Replace the body of `edgeAt`:

```java
    private Edge edgeAt(int x, int y) {
        return EdgeShapes.of(edgeList).nearest(x, y);
    }
```

In `handleWalkClick`, replace:

```java
        Edge hitE = null;
        if (hitV == null) {
            for (Edge ed : edgeList) {
                if (ed.hasIntersection(x, y)) { hitE = ed; break; }
            }
        }
```

with:

```java
        Edge hitE = hitV == null ? edgeAt(x, y) : null;
```

In `handleClick`'s remove-tool case, replace:

```java
                        Edge edgeVictim = null;
                        for (Edge ed : edgeList) {
                            if (ed.hasIntersection(e.getX(), e.getY())) {
                                edgeVictim = ed;
                                break;
                            }
                        }
```

with:

```java
                        Edge edgeVictim = edgeAt(e.getX(), e.getY());
```

In `updateHover`, replace:

```java
        for (Edge d : edgeList) {
            boolean hit = (hoveredVertex == null) && d.hasIntersection(mx, my);
```

with:

```java
        Edge hoveredEdge = hoveredVertex == null ? edgeAt(mx, my) : null;
        for (Edge d : edgeList) {
            boolean hit = (d == hoveredEdge);
```

Then confirm no caller is left:

```bash
grep -n "\.hasIntersection" src/graphtheory/Canvas.java | grep -v "v\.hasIntersection"
```

Expected: no output.

- [ ] **Step 8: Delete the old drawing code**

In `src/graphtheory/Vertex.java`: delete the whole `draw(Graphics g)` method, the fields `private int size1 = 30;` and `private int size2 = 40;`, and `import java.awt.Graphics;`.

In `src/graphtheory/Edge.java`: delete `VERTEX_RADIUS`, `ARROW_SIZE`, `TOLERANCE`, `CURVE_OFFSET`, and the methods `hasReverseArc`, `draw`, `drawStepLabel`, `drawQuadCurve`, both `drawArrowhead` overloads and `hasIntersection`. Delete the imports `BasicStroke`, `Graphics`, `Graphics2D`, `Polygon`, `Stroke` (keep `Color` and `List`). Update the two field comments:

```java
    // Set by Canvas when the Remove Tool hovers over this edge.
    public boolean removeHover = false;

    // Set by Canvas before drawing when this edge is on the built walk or the
    // highlighted pair path. null = not highlighted / no label.
    public Color highlight = null;
    public String stepLabel = null;
```

These stay as they are (GraphRenderer reads them).

- [ ] **Step 9: Build and run the tests**

Expected: compiles; `OK (175 tests)`.

- [ ] **Step 10: Check it by hand**

Launch the app and check:
1. Vertices are smooth white circles with a thin outline and a centred name; a 4-character name (rename one to `abcd`) fits inside.
2. Two undirected edges between the same pair, plus an arc either way, fan out as three or four separate curves. Hovering each lights up only that one; Remove Tool removes only the hovered one.
3. Three self-loops on one vertex nest; each can be hovered on its own.
4. Arrowhead tips touch the vertex outline.
5. A vertex that is both root and cutpoint shows orange inside green.
6. Dragging out an edge shows a grey dashed line; hovering is light blue; selecting is blue with a glow; Remove Tool hover is red.
7. With all weights 1 no numbers show; set one weight to 5 and all weights appear in boxes on the curves.
8. Build a walk across parallel edges: the step numbers sit in the box on the right curve.

- [ ] **Step 11: Commit**

```bash
git add src/graphtheory/Canvas.java src/graphtheory/Vertex.java src/graphtheory/Edge.java
git commit -m "feat: canvas paints through GraphRenderer and picks the nearest edge

Parallel edges and nested self-loops are each visible and clickable,
rings combine, arrowheads touch the outline, red only means remove.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 7: Minimum cut in the Properties thumbnail, never as selection

Opening the Properties tab currently clears the selection and marks the minimum cut as selected. It should only highlight the cut, in the thumbnail.

**Files:**
- Modify: `src/graphtheory/GraphProperties.java`
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Rename the fields**

```bash
sed -i 's/witnessVertices/minVertexCut/g; s/witnessEdges/minEdgeCut/g' src/graphtheory/GraphProperties.java src/graphtheory/Canvas.java
grep -rn "witness" src test
```

Expected: no output from `grep`.

- [ ] **Step 2: Stop `computeProperties` touching the selection**

In `Canvas.computeProperties()`, delete these lines:

```java
            for (Vertex v : vertexList) v.wasClicked = false;
            for (Edge ed : edgeList)    ed.wasClicked = false;

            for (Vertex v : gP.minVertexCut) v.wasClicked = true;
            for (Edge ed : gP.minEdgeCut)     ed.wasClicked = true;

```

- [ ] **Step 3: Draw the thumbnail through the renderer**

Add `import java.util.HashSet;` to `Canvas.java`. Add constants below `PATH_ROWS`:

```java
    // Size of the graph picture at the top left of the Properties tab.
    private static final int THUMB_W = 400;
    private static final int THUMB_H = 300;
```

Add this method above `buildPropertiesPanel`:

```java
    /** The graph fitted into a box, with the minimum vertex and edge cuts marked. */
    private void drawThumbnail(Graphics2D g, int x, int y, int w, int h) {
        Graphics2D t = (Graphics2D) g.create();
        try {
            t.clipRect(x, y, w, h);
            t.translate(x, y);
            t.setColor(backgroundColour);
            t.fillRect(0, 0, w, h);
            t.transform(GraphRenderer.fit(vertexList, w, h, 40));
            GraphRenderer.Options o = new GraphRenderer.Options();
            o.cutVertices = new HashSet<Vertex>(gP.minVertexCut);
            o.cutEdges = new HashSet<Edge>(gP.minEdgeCut);
            GraphRenderer.paint(t, vertexList, edgeList, o);
        } finally {
            t.dispose();
        }
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);
    }
```

In `buildPropertiesPanel`'s `paintComponent`, replace:

```java
                g2.setColor(Color.BLACK);
                g2.drawRect(10, 10, width / 2, height / 2);

                int rightX = width / 2 + 60;
```

with:

```java
                drawThumbnail(g2, 10, 10, THUMB_W, THUMB_H);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(getFont());

                int rightX = THUMB_W + 60;
```

and replace `int nodeY = height / 2 + 90;` with `int nodeY = THUMB_H + 90;`.

In `refreshPropertiesScrollSize`, replace `+ height / 2 + 20` with `+ THUMB_H + 20` and `width / 2 + 60 + 700` with `THUMB_W + 60 + 700`.

- [ ] **Step 4: Build and run the tests**

Expected: `OK (175 tests)`.

- [ ] **Step 5: Check it by hand**

1. Draw a path a–b–c and select `a`. Open Properties: the thumbnail shows `b` with a dashed magenta ring and one edge dashed magenta. Go back to Graph: `a` is still the only selected vertex and nothing is magenta.
2. The thumbnail uses the new look (fanned edges, rings) and fits large graphs in the box.

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/GraphProperties.java src/graphtheory/Canvas.java
git commit -m "fix: viewing Properties no longer replaces the selection with the minimum cut

The cut is shown only in the Properties thumbnail, as dashed rings/edges.
Rename witnessVertices/witnessEdges to minVertexCut/minEdgeCut.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 8: Induced Subgraph window uses the renderer

**Files:**
- Modify: `src/graphtheory/Canvas.java` (`showSubgraphWindow`)

- [ ] **Step 1: Replace the hand-written drawing**

In `showSubgraphWindow`, replace the whole body of the panel's `paintComponent`, from `super.paintComponent(g);` down to its closing `}`, with:

```java
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (sV.isEmpty()) return;
                g2.transform(GraphRenderer.fit(sV, getWidth(), getHeight(), 50));
                GraphRenderer.paint(g2, sV, sE, new GraphRenderer.Options());
```

- [ ] **Step 2: Build and run the tests**

Expected: `OK (175 tests)`.

- [ ] **Step 3: Check it by hand**

Select three vertices joined by an arc, a pair of parallel edges and a self-loop, then Extras → Induced Subgraph. The window shows arrowheads, the fan and the loop, in the new style.

- [ ] **Step 4: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "feat: Induced Subgraph window draws with GraphRenderer (arrows, fans, loops)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 9: Resizable window and dot grid

**Files:**
- Modify: `src/graphtheory/GraphRenderer.java`
- Modify: `src/graphtheory/Canvas.java`
- Test: `test/graphtheory/GraphRendererTest.java`

- [ ] **Step 1: Write the failing test**

Add to `GraphRendererTest`, before the final `}`:

```java
    @Test
    public void paintGrid_dotsEveryGridStep() {
        BufferedImage img = blank();
        Graphics2D g = img.createGraphics();
        GraphRenderer.paintGrid(g, 200, 200);
        g.dispose();
        assertEquals(GraphRenderer.GRID, pixel(img, GraphRenderer.GRID_STEP, GraphRenderer.GRID_STEP));
        assertEquals(Color.WHITE, pixel(img, GraphRenderer.GRID_STEP + 10, GraphRenderer.GRID_STEP + 10));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Expected: compile error `cannot find symbol: method paintGrid`.

- [ ] **Step 3: Implement the grid**

In `GraphRenderer`, add below `CUT`:

```java
    public static final Color GRID = new Color(225, 225, 225);
    public static final int GRID_STEP = 20;
```

and add below `paint`:

```java
    /** Faint dots every GRID_STEP pixels, behind the graph. Purely visual; nothing snaps to it. */
    public static void paintGrid(Graphics2D g, int w, int h) {
        g.setColor(GRID);
        for (int x = GRID_STEP; x < w; x += GRID_STEP) {
            for (int y = GRID_STEP; y < h; y += GRID_STEP) {
                g.fillRect(x - 1, y - 1, 2, 2);
            }
        }
    }
```

In `Canvas.CanvasPane.paintComponent`, add after `if (selectedWindow != 0) return;`:

```java
            GraphRenderer.paintGrid(g2, getWidth(), getHeight());
```

- [ ] **Step 4: Make the window resizable**

In the `Canvas` constructor, change `frame.setResizable(false);` to `frame.setResizable(true);`.

Add these methods below `edgeAt`:

```java
    /** The Graph canvas's current size; before it is laid out, the size it was created with. */
    private int canvasWidth() {
        return canvas.getWidth() > 0 ? canvas.getWidth() : width;
    }

    private int canvasHeight() {
        return canvas.getHeight() > 0 ? canvas.getHeight() : height;
    }
```

In `openGraph`, replace:

```java
        Layout.arrangeOnCircle(d.unplaced, width, height);
        Layout.clampInto(d.vertices, width, height);
```

with:

```java
        Layout.arrangeOnCircle(d.unplaced, canvasWidth(), canvasHeight());
        Layout.clampInto(d.vertices, canvasWidth(), canvasHeight());
```

In `arrangeVertices`, replace `Layout.arrangeOnCircle(vertexList, width, height);` with:

```java
        Layout.arrangeOnCircle(vertexList, canvasWidth(), canvasHeight());
```

In `drawWalkInfoBox`, replace `int x = 10, y = 420, w = 360, h = 95;` with:

```java
        int x = 10, y = canvasHeight() - 180, w = 360, h = 95;
```

(180 keeps it exactly where it was at the original 600px height.)

Resizing moves no vertices, so it never creates unsaved changes (CONTEXT.md, *Unsaved changes*).

- [ ] **Step 5: Build and run the tests**

Expected: `OK (176 tests)`.

- [ ] **Step 6: Check it by hand**

1. Drag the window bigger: the canvas grows, the grid fills it, the walk box stays near the bottom.
2. Auto Arrange in a big window centres the circle in the big canvas.
3. Shrink the window so some vertices are off-screen: the title shows no unsaved-changes mark; enlarge again and they are where they were.
4. Opening a file with unplaced vertices arranges them in the current canvas.

- [ ] **Step 7: Commit**

```bash
git add src/graphtheory/GraphRenderer.java src/graphtheory/Canvas.java test/graphtheory/GraphRendererTest.java
git commit -m "feat: resizable window with a faint dot grid; layout uses the current canvas size

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 10: Docs and wrap-up

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Update TODO.md**

Delete the item that starts `- [ ] **Resizable window / canvas (UI phase 2).**`. Add at the top of the list:

```markdown
- [ ] **Zoom and pan on the Graph canvas.** `GraphRenderer` already draws in graph coordinates under a transform (the thumbnail and Induced Subgraph use `GraphRenderer.fit`); the canvas would need a view transform and every mouse handler converted from screen to graph coordinates.
```

- [ ] **Step 2: Full test run**

Expected: `OK (176 tests)`.

- [ ] **Step 3: Commit**

```bash
git add TODO.md
git commit -m "docs: resizable canvas done; zoom/pan noted for later

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 4: Finish the branch**

Use superpowers:finishing-a-development-branch. PRs are opened as `jbarguilles`.
