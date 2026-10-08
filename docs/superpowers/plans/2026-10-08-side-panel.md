# Side Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the three info boxes painted over the Graph canvas (vertex, pair, walk) with a side panel of real Swing components beside the canvas, and fix the pair and walk tool behaviour that the panel exposes.

**Architecture:** Pure, tested classes compute what the panel says: `PairSummary` (a pair's adjacency, edge-aware paths, distance and weighted distance, computed once per change instead of on every repaint), `PanelText` (all wording), `GraphShape` (whether a found walk's graph has changed), plus `Walk.weight()` and `Walk.kindName()`. `SidePanel` is a thin Swing view with Selection, Pair and Walk sections. `Canvas.refresh()` fills a `SidePanel.Content` and hands it over. `SidePanel` only rebuilds a section when its text changes, so this is cheap on every mouse move.

**Tech Stack:** Java 17 (source-compatible with Java 7+, so no lambdas or `var` in new code), Swing/AWT, JUnit 4.13.2.

**Read first:** `CONTEXT.md`, in particular:
- *Walk*: Length, Distance, **Weight of a walk**, **Weighted distance**, **Lightest path**, Kinds of Walk.
- *Display Conventions*: Graph window, **Built and found walks**, One meaning per colour.

Also read `docs/adr/0002-edge-aware-walks.md`. Those definitions are the spec.

---

## Decisions this plan implements

| # | Decision |
|---|---|
| 1 | The panel is always visible, on the right of the Graph tab: `[palette | canvas | panel]`. It sits in a `JSplitPane` with a draggable divider and starts about 260px wide. The canvas keeps 800×600, so the window opens wider. Nothing is painted over the graph any more. |
| 2 | Sections are always in the order Selection, Pair, Walk. Each is shown only when it has content. With nothing to show, the panel shows a one-line hint. The panel scrolls if it gets too tall. |
| 3 | **Selection.** With one vertex selected, it lists name/value rows (Degree, In-Degree, Out-Degree, Isolated, Self-loop, Cutpoint, Root). With several selected, it shows a table with one row per vertex (Vertex, Deg, In, Out, flags). There is no "First" vertex and no edge section. |
| 4 | **Pair.** Facts: ordered pair, Adjacent, Reachable, Distance (edge count), Weighted distance (only on a weighted graph), number of paths. Below them is the **full edge-aware path list**, sorted by weight on a weighted graph and by length otherwise, with ties in search order. Each row shows its length, its weight (weighted graph only) and the tags `geodesic` / `lightest` (`lightest` only on a weighted graph). The vertex-sequence "simple paths" count and the greedy "vertex-disjoint width" are no longer shown. |
| 5 | Clicking a path row highlights that path in amber and switches to the Pair tool **without clearing the pair**. ↑/↓ still browse from the canvas, and the list selection follows them. |
| 6 | **Walk.** The heading is "Built walk", or e.g. "Euler tour (found)". The whole walk is shown wrapped and selectable, never truncated. Facts: Length, Weight (weighted graph only), Kind (the most specific of cycle / circuit / closed walk / path / trail / walk), then the yes/no grid. The walk message is a red line in this section. |
| 7 | A **found** walk is read-only. A left-click on a vertex starts a new built walk in its place, and a click on an edge says "Click a vertex to start a new walk". Backspace and right-click do nothing to it. Adding or removing a vertex or edge, or changing a weight, clears it; moving or renaming a vertex does not. |
| 8 | Choosing a tool never clears the pair or the walk. Esc clears what the active tool owns: the pair with the Pair tool, the walk with the Walk tool. The status-bar hints say so. |
| 9 | The pair is stored as two `Vertex` references, not list indices, so removing an earlier vertex can't move the pair onto other vertices. |

Out of scope: Properties as Swing tables; splitting the Properties distance matrix into distance and weighted distance; exact internally vertex-disjoint paths. All three are in `TODO.md`.

---

## How to build and test

There is no Ant/Maven on the PATH. Run from the repo root (`GraphTheory/`) in **Git Bash**:

```bash
CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -Djava.awt.headless=true -cp "out/test;$CP" org.junit.runner.JUnitCore \
     graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest \
     graphtheory.EdgeRegistryTest graphtheory.VertexNamesTest graphtheory.GraphFileTest graphtheory.LayoutTest \
     graphtheory.EditHistoryTest graphtheory.ToolsTest graphtheory.FileManagerTest graphtheory.ComponentsTest \
     graphtheory.EdgeShapesTest graphtheory.EdgeTest graphtheory.GraphRendererTest
```

`MSYS_NO_PATHCONV=1` is required. Without it, Git Bash mangles the `;`-separated classpath. **Add each new test class to the end of the list in the task that creates it** (`graphtheory.PairSummaryTest`, `graphtheory.PanelTextTest`, `graphtheory.SidePanelTest`, `graphtheory.GraphShapeTest`). A class listed before it exists fails with "Could not find class". `out/` is git-ignored.

Baseline before starting: **187 tests pass.** Each task states how many tests it adds.

Launch the app (after compiling as above). A timeout exit 124 with clean stderr means it started fine:

```bash
MSYS_NO_PATHCONV=1 timeout 8 java -cp out/test graphtheory.Main; echo "exit $?"
```

Line endings: the repo's working copies of existing files are CRLF. `sed -i` and some editors rewrite whole files as LF. Check `git diff --stat` shows only the lines you meant to change.

---

## File Map

| File | Change |
|---|---|
| `src/graphtheory/Walk.java` | `weight()`, `kindName()` |
| `src/graphtheory/PairSummary.java` | **New.** Adjacency, sorted edge-aware paths, distance, weighted distance, geodesic/lightest tests |
| `src/graphtheory/PanelText.java` | **New.** Every string the panel shows |
| `src/graphtheory/SidePanel.java` | **New.** The Swing panel (Selection / Pair / Walk sections) |
| `src/graphtheory/GraphShape.java` | **New.** Vertices, edges and weights by identity, for "has the graph a found walk came from changed?" |
| `src/graphtheory/Canvas.java` | Split pane + side panel; info boxes deleted; `PairSummary` replaces `pairPaths`; path-row clicks; tool/Esc rules; found walks; pair as vertex references |
| `src/graphtheory/Tools.java` | Pair and Walk hints mention the list, Esc and found walks |
| `test/graphtheory/WalkTest.java` | Tests for `weight` and `kindName` |
| `test/graphtheory/PairSummaryTest.java`, `PanelTextTest.java`, `SidePanelTest.java`, `GraphShapeTest.java` | **New** |

`Canvas.java` is about 1500 lines. Steps that touch it quote the code being replaced, because line numbers shift between tasks.

---

## Task 0: Branch and baseline

The branch `feature/side-panel` already exists and is checked out. `CONTEXT.md` and `TODO.md` have the design's glossary and TODO changes, not yet committed.

- [ ] **Step 1: Commit the docs that define this feature**

```bash
git add CONTEXT.md TODO.md docs/superpowers/plans/2026-10-08-side-panel.md
git commit -m "docs: weighted distance, lightest path, built/found walks, side panel; plan

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 2: Confirm the baseline**

Run the build-and-test command. Expected: `OK (187 tests)`.

---

## Task 1: Walk weight and kind

**Files:**
- Modify: `src/graphtheory/Walk.java`
- Test: `test/graphtheory/WalkTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `test/graphtheory/WalkTest.java`, before the final `}`:

```java
    private static Walk walk(Vertex start, Edge... steps) {
        Walk w = new Walk(start);
        for (Edge e : steps) assertTrue("can't take " + e.vertex1.name + e.vertex2.name, w.extend(e));
        return w;
    }

    @Test
    public void weight_sumsEdgeWeights_countingRepeats() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        Edge ab = new Edge(a, b, false);
        ab.setWeight(4);
        assertEquals(8, walk(a, ab, ab).weight());
    }

    @Test
    public void weight_trivialWalk_isZero() {
        assertEquals(0, new Walk(new Vertex("a", 0, 0)).weight());
    }

    @Test
    public void kindName_trivialWalk_isPath() {
        assertEquals("path", new Walk(new Vertex("a", 0, 0)).kindName());
    }

    @Test
    public void kindName_openNoRepeatedVertex_isPath() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        assertEquals("path", walk(a, new Edge(a, b, false)).kindName());
    }

    @Test
    public void kindName_openRepeatedVertexNoRepeatedEdge_isTrail() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0), d = new Vertex("d", 0, 0);
        Edge ab = new Edge(a, b, false), bc = new Edge(b, c, false), ca = new Edge(c, a, false), ad = new Edge(a, d, false);
        assertEquals("trail", walk(a, ab, bc, ca, ad).kindName());
    }

    @Test
    public void kindName_openRepeatedEdge_isWalk() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0);
        Edge ab = new Edge(a, b, false), ac = new Edge(a, c, false);
        assertEquals("walk", walk(a, ab, ab, ac).kindName());
    }

    @Test
    public void kindName_backAndForthOnOneEdge_isClosedWalk() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        Edge ab = new Edge(a, b, false);
        assertEquals("closed walk", walk(a, ab, ab).kindName());
    }

    @Test
    public void kindName_figureEight_isCircuit() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0),
               d = new Vertex("d", 0, 0), f = new Vertex("f", 0, 0);
        Edge ab = new Edge(a, b, false), bc = new Edge(b, c, false), ca = new Edge(c, a, false),
             ad = new Edge(a, d, false), df = new Edge(d, f, false), fa = new Edge(f, a, false);
        assertEquals("circuit", walk(a, ab, bc, ca, ad, df, fa).kindName());
    }

    @Test
    public void kindName_undirectedThenArcBack_isCycle() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        assertEquals("cycle", walk(a, new Edge(a, b, false), new Edge(b, a, true)).kindName());
    }
```

If `WalkTest` already has a helper named `walk(Vertex, Edge...)`, name this one `walkOf` and update the calls.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: compile error `cannot find symbol: method weight()`.

- [ ] **Step 3: Implement**

In `src/graphtheory/Walk.java`, add after `isCycle()`:

```java
    /** Sum of its edges' weights, counting repeats (CONTEXT.md, Weight of a walk). */
    public int weight() {
        int w = 0;
        for (Edge e : edges) w += e.weight;
        return w;
    }

    /**
     * The most specific kind it is (CONTEXT.md, Kinds of Walk): "cycle", "circuit" or
     * "closed walk" when closed, otherwise "path", "trail" or "walk".
     */
    public String kindName() {
        if (isClosed()) return isCycle() ? "cycle" : isCircuit() ? "circuit" : "closed walk";
        return isPath() ? "path" : isTrail() ? "trail" : "walk";
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: 9 more tests, `OK (196 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Walk.java test/graphtheory/WalkTest.java
git commit -m "feat: Walk.weight and Walk.kindName (most specific kind of walk)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 2: PairSummary

**Files:**
- Create: `src/graphtheory/PairSummary.java`
- Test: `test/graphtheory/PairSummaryTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/PairSummaryTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PairSummaryTest {

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);
    private final Vertex m = new Vertex("m", 0, 0);

    private static Edge und(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, false);
        e.setWeight(w);
        return e;
    }

    private static Edge arc(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, true);
        e.setWeight(w);
        return e;
    }

    private static Vector<Edge> edges(Edge... es) {
        return new Vector<Edge>(Arrays.asList(es));
    }

    @Test
    public void undirectedEdge_isAdjacentAndReachable() {
        PairSummary s = new PairSummary(u, v, edges(und(u, v, 1)));
        assertTrue(s.adjacent);
        assertTrue(s.reachable());
        assertEquals(1, s.distance());
    }

    @Test
    public void arcTheOtherWay_isNeitherAdjacentNorReachable() {
        PairSummary s = new PairSummary(u, v, edges(arc(v, u, 1)));
        assertFalse(s.adjacent);
        assertFalse(s.reachable());
        assertEquals(-1, s.distance());
        assertEquals(-1, s.weightedDistance());
        assertTrue(s.paths.isEmpty());
    }

    @Test
    public void parallelEdges_areSeparatePaths() {
        PairSummary s = new PairSummary(u, v, edges(und(u, v, 1), arc(u, v, 1)));
        assertEquals(2, s.paths.size());
    }

    @Test
    public void weighted_distanceCountsEdges_weightedDistanceSumsWeights() {
        Edge direct = und(u, v, 10);
        PairSummary s = new PairSummary(u, v, edges(direct, und(u, m, 1), und(m, v, 1)));
        assertTrue(s.weighted);
        assertEquals(1, s.distance());
        assertEquals(2, s.weightedDistance());
    }

    @Test
    public void weighted_pathsSortedLightestFirst_andTaggedSeparately() {
        Edge direct = und(u, v, 10);
        PairSummary s = new PairSummary(u, v, edges(direct, und(u, m, 1), und(m, v, 1)));
        Walk lightest = s.paths.get(0);
        Walk shortest = s.paths.get(1);
        assertEquals(2, lightest.length());
        assertTrue(s.isLightest(lightest));
        assertFalse(s.isGeodesic(lightest));
        assertTrue(s.isGeodesic(shortest));
        assertFalse(s.isLightest(shortest));
    }

    @Test
    public void unweighted_pathsSortedShortestFirst_distancesAgree() {
        PairSummary s = new PairSummary(u, v, edges(und(u, m, 1), und(m, v, 1), und(u, v, 1)));
        assertFalse(s.weighted);
        assertEquals(1, s.paths.get(0).length());
        assertEquals(2, s.paths.get(1).length());
        assertEquals(s.distance(), s.weightedDistance());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.PairSummaryTest` to the list. Expected: compile error `cannot find symbol: class PairSummary`.

- [ ] **Step 3: Implement**

Create `src/graphtheory/PairSummary.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * What the side panel says about an ordered pair (from, to): adjacency, every
 * from–to path as an edge-aware walk (ADR 0002), distance and weighted distance
 * (CONTEXT.md). Computed once when the pair or the graph changes, not on repaint.
 */
public final class PairSummary {

    public final Vertex from;
    public final Vertex to;
    /** Some edge can be crossed in one step from 'from' to 'to'. */
    public final boolean adjacent;
    /** The graph is weighted (some edge weight is not 1). */
    public final boolean weighted;
    /** Every from–to path: lightest first on a weighted graph, else shortest first; ties keep search order. */
    public final List<Walk> paths;

    private final int distance;
    private final int weightedDistance;

    public PairSummary(Vertex from, Vertex to, Vector<Edge> edges) {
        this.from = from;
        this.to = to;
        this.weighted = Edge.isWeighted(edges);
        this.adjacent = !Walk.edgesBetween(from, to, edges).isEmpty();

        // generateEdgePaths is already shortest first; a stable sort by weight keeps that among ties.
        List<Walk> ps = new ArrayList<Walk>(new VertexPair(from, to).generateEdgePaths(edges));
        if (weighted) {
            Collections.sort(ps, new Comparator<Walk>() {
                public int compare(Walk p, Walk q) {
                    return p.weight() - q.weight();
                }
            });
        }
        this.paths = Collections.unmodifiableList(ps);

        // Every shortest or lightest walk can be shortened to a path, so the path list decides both.
        int d = -1, wd = -1;
        for (Walk p : ps) {
            if (d < 0 || p.length() < d) d = p.length();
            if (wd < 0 || p.weight() < wd) wd = p.weight();
        }
        distance = d;
        weightedDistance = wd;
    }

    public boolean reachable() {
        return !paths.isEmpty();
    }

    /** Edges on a shortest from–to walk (CONTEXT.md, Distance), or -1 if unreachable. */
    public int distance() {
        return distance;
    }

    /** Smallest weight of a from–to walk (CONTEXT.md, Weighted distance), or -1 if unreachable. */
    public int weightedDistance() {
        return weightedDistance;
    }

    /** p is a geodesic: no from–to walk has fewer edges. */
    public boolean isGeodesic(Walk p) {
        return p.length() == distance;
    }

    /** p is a lightest path: no from–to walk weighs less. */
    public boolean isLightest(Walk p) {
        return p.weight() == weightedDistance;
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: 6 more tests, `OK (202 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/PairSummary.java test/graphtheory/PairSummaryTest.java
git commit -m "feat: PairSummary (adjacency, edge-aware paths, distance, weighted distance)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 3: PanelText

All of the panel's wording, kept out of Swing so it can be tested.

**Files:**
- Create: `src/graphtheory/PanelText.java`
- Test: `test/graphtheory/PanelTextTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/PanelTextTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PanelTextTest {

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);
    private final Vertex m = new Vertex("m", 0, 0);

    private static Edge und(Vertex x, Vertex y, int w) {
        Edge e = new Edge(x, y, false);
        e.setWeight(w);
        return e;
    }

    @Test
    public void vertexProperties_useGlossaryNames() {
        u.isRoot = true;
        String[][] rows = PanelText.vertexProperties(u);
        assertEquals("Name", rows[0][0]);
        assertEquals("u", rows[0][1]);
        assertEquals("Degree", rows[1][0]);
        assertEquals("Root", rows[7][0]);
        assertEquals("yes", rows[7][1]);
    }

    @Test
    public void flags_listOnlyWhatHolds() {
        u.isRoot = true;
        u.isCutpoint = true;
        assertEquals("root, cutpoint, isolated", PanelText.flags(u));
        assertEquals("isolated", PanelText.flags(v));
    }

    @Test
    public void pairFacts_unweighted_noWeightedDistanceLine() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(und(u, v, 1))));
        assertEquals(Arrays.asList("Ordered pair: (u, v)", "Adjacent: yes", "Reachable: yes",
                "Distance: 1", "Paths: 1"), Arrays.asList(PanelText.pairFacts(s)));
    }

    @Test
    public void pairFacts_unreachable_infinity() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>());
        assertTrue(Arrays.asList(PanelText.pairFacts(s)).contains("Distance: \u221E"));
    }

    @Test
    public void pairFacts_weighted_bothDistances() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(
                und(u, v, 10), und(u, m, 1), und(m, v, 1))));
        java.util.List<String> facts = Arrays.asList(PanelText.pairFacts(s));
        assertTrue(facts.contains("Distance: 1"));
        assertTrue(facts.contains("Weighted distance: 2"));
    }

    @Test
    public void pathRow_weighted_showsWeightAndTags() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(
                und(u, v, 10), und(u, m, 1), und(m, v, 1))));
        assertEquals("1. u -{u,m}-> m -{m,v}-> v   len 2 \u00b7 weight 2   lightest",
                PanelText.pathRow(0, s.paths.get(0), s));
        assertEquals("2. u -{u,v}-> v   len 1 \u00b7 weight 10   geodesic",
                PanelText.pathRow(1, s.paths.get(1), s));
    }

    @Test
    public void pathRow_unweighted_noWeightNoLightest() {
        PairSummary s = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(und(u, v, 1))));
        assertEquals("1. u -{u,v}-> v   len 1   geodesic", PanelText.pathRow(0, s.paths.get(0), s));
    }

    @Test
    public void walkHeading_builtOrFound() {
        assertEquals("Built walk", PanelText.walkHeading(null));
        assertEquals("Euler tour (found)", PanelText.walkHeading("Euler Tour"));
        assertEquals("Hamiltonian cycle (found)", PanelText.walkHeading("Hamiltonian Cycle"));
    }

    @Test
    public void walkFacts_weightOnlyWhenWeighted() {
        Edge uv = und(u, v, 3);
        Walk w = new Walk(u);
        w.extend(uv);
        assertEquals(Arrays.asList("Length: 1", "Kind: path", "Trail: yes   Path: yes",
                "Closed: no   Circuit: no   Cycle: no"), Arrays.asList(PanelText.walkFacts(w, false)));
        assertEquals("Weight: 3", PanelText.walkFacts(w, true)[1]);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.PanelTextTest` to the list. Expected: compile error `cannot find symbol: class PanelText`.

- [ ] **Step 3: Implement**

Create `src/graphtheory/PanelText.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.List;

/** Everything the side panel says, kept out of Swing so it can be tested. Terms follow CONTEXT.md. */
public final class PanelText {

    public static final String INFINITY = "\u221E";

    /** Column names of the table shown when several vertices are selected. */
    public static final String[] VERTEX_COLUMNS = { "Vertex", "Deg", "In", "Out", "" };

    private PanelText() {}

    /** Name/value rows describing one selected vertex (CONTEXT.md, Node Properties). */
    public static String[][] vertexProperties(Vertex v) {
        return new String[][] {
            { "Name",       v.name },
            { "Degree",     String.valueOf(v.degree()) },
            { "In-Degree",  String.valueOf(v.inDegree()) },
            { "Out-Degree", String.valueOf(v.outDegree()) },
            { "Isolated",   yesNo(v.isIsolated()) },
            { "Self-loop",  yesNo(v.hasSelfLoop()) },
            { "Cutpoint",   yesNo(v.isCutpoint) },
            { "Root",       yesNo(v.isRoot) },
        };
    }

    /** One row of the several-vertices table, matching VERTEX_COLUMNS. */
    public static Object[] vertexRow(Vertex v) {
        return new Object[] { v.name, v.degree(), v.inDegree(), v.outDegree(), flags(v) };
    }

    /** The properties that hold, e.g. "root, cutpoint". Empty if none. */
    public static String flags(Vertex v) {
        List<String> fs = new ArrayList<String>();
        if (v.isRoot)        fs.add("root");
        if (v.isCutpoint)    fs.add("cutpoint");
        if (v.isIsolated())  fs.add("isolated");
        if (v.hasSelfLoop()) fs.add("self-loop");
        return join(fs, ", ");
    }

    /** The pair's fact lines, shown above its path list. */
    public static String[] pairFacts(PairSummary s) {
        List<String> lines = new ArrayList<String>();
        lines.add("Ordered pair: (" + s.from.name + ", " + s.to.name + ")");
        lines.add("Adjacent: " + yesNo(s.adjacent));
        lines.add("Reachable: " + yesNo(s.reachable()));
        lines.add("Distance: " + orInfinity(s.distance()));
        if (s.weighted) lines.add("Weighted distance: " + orInfinity(s.weightedDistance()));
        lines.add("Paths: " + s.paths.size());
        return lines.toArray(new String[0]);
    }

    /** One row of the path list, e.g. "1. u -{u,v}-> v   len 1 · weight 3   geodesic, lightest". */
    public static String pathRow(int index, Walk p, PairSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append(index + 1).append(". ").append(p).append("   len ").append(p.length());
        if (s.weighted) sb.append(" \u00b7 weight ").append(p.weight());
        List<String> tags = new ArrayList<String>();
        if (s.isGeodesic(p)) tags.add("geodesic");
        if (s.weighted && s.isLightest(p)) tags.add("lightest");
        if (!tags.isEmpty()) sb.append("   ").append(join(tags, ", "));
        return sb.toString();
    }

    /** "Built walk", or for a Find result e.g. "Euler tour (found)". foundKind is the Find command's kind, e.g. "Euler Tour". */
    public static String walkHeading(String foundKind) {
        if (foundKind == null) return "Built walk";
        return foundKind.charAt(0) + foundKind.substring(1).toLowerCase() + " (found)";
    }

    /** The walk's fact lines, shown under the walk itself. */
    public static String[] walkFacts(Walk w, boolean weighted) {
        List<String> lines = new ArrayList<String>();
        lines.add("Length: " + w.length());
        if (weighted) lines.add("Weight: " + w.weight());
        lines.add("Kind: " + w.kindName());
        lines.add("Trail: " + yesNo(w.isTrail()) + "   Path: " + yesNo(w.isPath()));
        lines.add("Closed: " + yesNo(w.isClosed()) + "   Circuit: " + yesNo(w.isCircuit())
                + "   Cycle: " + yesNo(w.isCycle()));
        return lines.toArray(new String[0]);
    }

    static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private static String orInfinity(int n) {
        return n < 0 ? INFINITY : String.valueOf(n);
    }

    static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: 9 more tests, `OK (211 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/PanelText.java test/graphtheory/PanelTextTest.java
git commit -m "feat: PanelText, the side panel's wording in glossary terms

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 4: SidePanel

A thin Swing view. Lightweight Swing components work headless, so the tests construct it directly.

**Files:**
- Create: `src/graphtheory/SidePanel.java`
- Test: `test/graphtheory/SidePanelTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/SidePanelTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidePanelTest {

    private int chosen = -2;

    private SidePanel panel() {
        return new SidePanel(new SidePanel.Listener() {
            public void pathChosen(int index) {
                chosen = index;
            }
        });
    }

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);

    private PairSummary pairWithTwoPaths() {
        return new PairSummary(u, v, new Vector<Edge>(Arrays.asList(new Edge(u, v, false), new Edge(u, v, true))));
    }

    @Test
    public void nothingToShow_onlyTheHint() {
        SidePanel p = panel();
        p.display(new SidePanel.Content());
        assertTrue(p.hint.isVisible());
        assertFalse(p.selection.isVisible());
        assertFalse(p.pair.isVisible());
        assertFalse(p.walk.isVisible());
    }

    @Test
    public void oneVertex_propertyRows() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.selected = Arrays.asList(u);
        p.display(c);
        assertTrue(p.selection.isVisible());
        assertFalse(p.hint.isVisible());
        assertEquals(8, p.selectionTable.getRowCount());
        assertEquals(2, p.selectionTable.getColumnCount());
    }

    @Test
    public void severalVertices_oneRowEach() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.selected = Arrays.asList(u, v);
        p.display(c);
        assertEquals(2, p.selectionTable.getRowCount());
        assertEquals(PanelText.VERTEX_COLUMNS.length, p.selectionTable.getColumnCount());
    }

    @Test
    public void pair_listsEveryPath_andSelectsTheGivenRowWithoutCallingBack() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        c.pathIndex = 1;
        p.display(c);
        assertTrue(p.pair.isVisible());
        assertEquals(2, p.pathList.getModel().getSize());
        assertEquals(1, p.pathList.getSelectedIndex());
        assertEquals(-2, chosen);
    }

    @Test
    public void clickingAPathRow_tellsTheListener() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        p.display(c);
        p.pathList.setSelectedIndex(1);
        assertEquals(1, chosen);
    }

    @Test
    public void walk_headingTextAndMessage() {
        SidePanel p = panel();
        Edge uv = new Edge(u, v, false);
        Walk w = new Walk(u);
        w.extend(uv);
        SidePanel.Content c = new SidePanel.Content();
        c.walk = w;
        c.foundKind = "Euler Trail";
        c.walkMessage = "No edge from v to u";
        p.display(c);
        assertTrue(p.walk.isVisible());
        assertEquals("Euler trail (found)", p.walkHeading.getText());
        assertEquals(w.toString(), p.walkText.getText());
        assertTrue(p.walkMessage.isVisible());
    }

    @Test
    public void messageWithoutWalk_stillShowsWalkSection() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.walkMessage = "No Euler Tour exists";
        p.display(c);
        assertTrue(p.walk.isVisible());
        assertFalse(p.walkText.isVisible());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.SidePanelTest` to the list. Expected: compile error `cannot find symbol: class SidePanel`.

- [ ] **Step 3: Implement**

Create `src/graphtheory/SidePanel.java`:

```java
package graphtheory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * The Graph tab's right-hand panel (CONTEXT.md, Display Conventions): Selection, Pair
 * and Walk sections in that order, each shown only when it has something to say.
 * A section is only rebuilt when its text changes, so display() is cheap enough to
 * call on every refresh.
 */
public class SidePanel extends JPanel implements Scrollable {

    public static final int WIDTH = 260;

    public interface Listener {
        /** The user clicked row 'index' of the pair's path list. */
        void pathChosen(int index);
    }

    /** What the panel shows. Canvas fills one in on every refresh. */
    public static class Content {
        public List<Vertex> selected = Collections.emptyList();
        /** null = no pair. */
        public PairSummary pair;
        /** Highlighted row of the path list, or -1. */
        public int pathIndex = -1;
        /** null = no walk. */
        public Walk walk;
        /** The Find command's kind (e.g. "Euler Tour") when the walk is a found walk, else null. */
        public String foundKind;
        /** Red line in the Walk section, or null. */
        public String walkMessage;
        public boolean weighted;
    }

    private final Listener listener;
    private boolean updating;
    private String selectionKey, pairKey, walkKey;

    // Package-private for SidePanelTest.
    final JLabel hint = new JLabel("<html>Select a vertex, pick a pair, or build a walk.</html>");
    final JPanel selection = section("Selection");
    final JTable selectionTable = new JTable() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JPanel pair = section("Pair");
    final JTextArea pairFacts = textArea();
    final DefaultListModel<String> pathModel = new DefaultListModel<String>();
    final JList<String> pathList = new JList<String>(pathModel);
    final JPanel walk = section("Walk");
    final JLabel walkHeading = new JLabel();
    final JTextArea walkText = textArea();
    final JTextArea walkFacts = textArea();
    final JLabel walkMessage = new JLabel();

    public SidePanel(Listener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        hint.setForeground(Color.GRAY);
        add(left(hint));

        selectionTable.setFocusable(false);
        selectionTable.setRowSelectionAllowed(false);
        selection.add(left(selectionTable.getTableHeader()));
        selection.add(left(selectionTable));
        add(selection);

        pair.add(left(pairFacts));
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pathList.setVisibleRowCount(8);
        pathList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!updating && !e.getValueIsAdjusting() && pathList.getSelectedIndex() >= 0) {
                    SidePanel.this.listener.pathChosen(pathList.getSelectedIndex());
                }
            }
        });
        pair.add(left(new JScrollPane(pathList)));
        add(pair);

        walkHeading.setFont(walkHeading.getFont().deriveFont(Font.BOLD));
        walkMessage.setForeground(new Color(200, 0, 0));
        walk.add(left(walkHeading));
        walk.add(left(walkText));
        walk.add(left(walkFacts));
        walk.add(left(walkMessage));
        add(walk);

        add(Box.createVerticalGlue());
        display(new Content());
    }

    public void display(Content c) {
        updating = true;
        try {
            boolean changed = showSelection(c) | showPair(c) | showWalk(c);
            hint.setVisible(!selection.isVisible() && !pair.isVisible() && !walk.isVisible());
            if (changed) {
                revalidate();
                repaint();
            }
        } finally {
            updating = false;
        }
    }

    private boolean showSelection(Content c) {
        StringBuilder key = new StringBuilder();
        for (Vertex v : c.selected) {
            for (Object o : PanelText.vertexRow(v)) key.append(o).append('\u0000');
            key.append('\n');
        }
        if (key.toString().equals(selectionKey)) return false;
        selectionKey = key.toString();

        selection.setVisible(!c.selected.isEmpty());
        if (c.selected.size() == 1) {
            selectionTable.setModel(new DefaultTableModel(
                    PanelText.vertexProperties(c.selected.get(0)), new String[] { "Property", "Value" }));
        } else if (c.selected.size() > 1) {
            Object[][] rows = new Object[c.selected.size()][];
            for (int i = 0; i < rows.length; i++) rows[i] = PanelText.vertexRow(c.selected.get(i));
            selectionTable.setModel(new DefaultTableModel(rows, PanelText.VERTEX_COLUMNS));
        }
        return true;
    }

    private boolean showPair(Content c) {
        String facts = c.pair == null ? "" : PanelText.join(java.util.Arrays.asList(PanelText.pairFacts(c.pair)), "\n");
        StringBuilder rows = new StringBuilder();
        if (c.pair != null) {
            for (int i = 0; i < c.pair.paths.size(); i++) {
                rows.append(PanelText.pathRow(i, c.pair.paths.get(i), c.pair)).append('\n');
            }
        }
        String key = facts + '\u0000' + rows + '\u0000' + c.pathIndex;
        if (key.equals(pairKey)) return false;
        String oldRows = pairKey == null ? null : pairKey.substring(pairKey.indexOf('\u0000') + 1, pairKey.lastIndexOf('\u0000'));
        pairKey = key;

        pair.setVisible(c.pair != null);
        pairFacts.setText(facts);
        if (!rows.toString().equals(oldRows)) {
            pathModel.clear();
            if (c.pair != null) {
                for (int i = 0; i < c.pair.paths.size(); i++) {
                    pathModel.addElement(PanelText.pathRow(i, c.pair.paths.get(i), c.pair));
                }
            }
        }
        if (c.pathIndex >= 0 && c.pathIndex < pathModel.getSize()) {
            pathList.setSelectedIndex(c.pathIndex);
            pathList.ensureIndexIsVisible(c.pathIndex);
        } else {
            pathList.clearSelection();
        }
        return true;
    }

    private boolean showWalk(Content c) {
        String key = (c.walk == null ? "" : c.walk.toString() + '\u0000'
                + PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted)), "\n"))
                + '\u0000' + c.foundKind + '\u0000' + c.walkMessage;
        if (key.equals(walkKey)) return false;
        walkKey = key;

        walk.setVisible(c.walk != null || c.walkMessage != null);
        walkHeading.setVisible(c.walk != null);
        walkText.setVisible(c.walk != null);
        walkFacts.setVisible(c.walk != null);
        if (c.walk != null) {
            walkHeading.setText(PanelText.walkHeading(c.foundKind));
            walkText.setText(c.walk.toString());
            walkFacts.setText(PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted)), "\n"));
        }
        walkMessage.setVisible(c.walkMessage != null);
        walkMessage.setText(c.walkMessage == null ? "" : "<html>" + escape(c.walkMessage) + "</html>");
        return true;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JPanel section(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JTextArea textArea() {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        t.setFont(new JLabel().getFont());
        return t;
    }

    private static <T extends javax.swing.JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // Scrollable: track the viewport's width so text wraps instead of scrolling sideways.

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(WIDTH, getPreferredSize().height);
    }

    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
        return 16;
    }

    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
        return Math.max(16, r.height - 16);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
```

`left(...)` takes `javax.swing.JComponent`. `JTableHeader`, `JScrollPane`, `JTable`, `JLabel` and `JTextArea` all qualify.

- [ ] **Step 4: Run the tests to verify they pass**

Expected: 7 more tests, `OK (218 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/SidePanel.java test/graphtheory/SidePanelTest.java
git commit -m "feat: SidePanel with Selection, Pair and Walk sections

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 5: Canvas uses the side panel

The panel goes in. The info boxes and the per-repaint pair analysis go out.

**Files:**
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Fields**

Replace:

```java
    // Paths for the selected pair (Tools.PAIR), browsed one at a time
    private Vector<Walk> pairPaths = null;
    private int selectedPathIndex = 0;
    private static final Color PATH_COLOR = new Color(220, 160, 0);
    private static final int PATH_ROWS = 6;
```

with:

```java
    // The selected pair (Tools.PAIR): its paths and distances, and the path shown in amber
    private PairSummary pairSummary = null;
    private int selectedPathIndex = 0;
    private static final Color PATH_COLOR = new Color(220, 160, 0);

    // The Find command (e.g. "Euler Tour") that produced currentWalk, or null for a built walk
    private String foundKind = null;

    private SidePanel sidePanel;
```

If the comment line above `pairPaths` differs, keep the fields as shown and drop the old ones. Then add `import java.util.ArrayList;` and `import java.util.List;` next to the other `java.util` imports. The single-type `java.util.List` import wins over `java.awt.*`'s `List`.

- [ ] **Step 2: Put the panel in a split pane**

In the constructor, replace:

```java
        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.add(palette, BorderLayout.WEST);
        graphPanel.add(canvas, BorderLayout.CENTER);
```

with:

```java
        sidePanel = new SidePanel(new SidePanel.Listener() {
            public void pathChosen(int index) {
                choosePath(index);
            }
        });
        JScrollPane sideScroll = new JScrollPane(sidePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setPreferredSize(new Dimension(SidePanel.WIDTH, height));
        sideScroll.setBorder(BorderFactory.createEmptyBorder());
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, sideScroll);
        split.setResizeWeight(1.0);   // a bigger window widens the canvas; the panel keeps its width
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());

        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.add(palette, BorderLayout.WEST);
        graphPanel.add(split, BorderLayout.CENTER);
```

- [ ] **Step 3: Compute the pair summary once per change**

Replace `refreshPairPaths()`:

```java
    /** Recomputes the selected pair's path list and vertex-disjoint width. */
    private void refreshPairPaths() {
        selectedPathIndex = 0;
        if (currentPairVP == null) {
            pairPaths = null;
            return;
        }
        currentPairVP.generateVertexDisjointPaths();
        pairPaths = currentPairVP.generateEdgePaths(edgeList);
    }
```

with:

```java
    /** Recomputes the selected pair's summary (paths, distances) after the pair or the graph changes. */
    private void refreshPairPaths() {
        selectedPathIndex = 0;
        pairSummary = currentPairVP == null ? null
                : new PairSummary(currentPairVP.vertex1, currentPairVP.vertex2, edgeList);
    }
```

Replace every remaining `pairPaths = null;` with `pairSummary = null;`. There are three: in `selectTool`, in `replaceGraph`, and in the Pair tool's click case.

- [ ] **Step 4: Keys and highlight use the summary**

In `installKeyBindings`, replace the `pathPrev` condition:

```java
                if (selectedTool == 6 && selectedWindow == 0
                        && pairPaths != null && selectedPathIndex > 0) {
```

with:

```java
                if (selectedTool == 6 && selectedWindow == 0
                        && pairSummary != null && selectedPathIndex > 0) {
```

and the `pathNext` condition:

```java
                if (selectedTool == 6 && selectedWindow == 0 && pairPaths != null
                        && selectedPathIndex < pairPaths.size() - 1) {
```

with:

```java
                if (selectedTool == 6 && selectedWindow == 0 && pairSummary != null
                        && selectedPathIndex < pairSummary.paths.size() - 1) {
```

In `applyHighlights`, replace:

```java
        if (selectedTool == 6 && pairPaths != null && !pairPaths.isEmpty()) {
            for (Edge ed : pairPaths.get(selectedPathIndex).edges()) {
```

with:

```java
        if (selectedTool == 6 && pairSummary != null && !pairSummary.paths.isEmpty()) {
            for (Edge ed : pairSummary.paths.get(selectedPathIndex).edges()) {
```

- [ ] **Step 5: Path-row clicks**

Add below `refreshPairPaths()`:

```java
    /** A row of the side panel's path list was clicked: show that path, with the Pair tool, keeping the pair. */
    private void choosePath(int index) {
        if (pairSummary == null || index < 0 || index >= pairSummary.paths.size()) return;
        selectedPathIndex = index;
        if (selectedTool != Tools.PAIR) {
            // Not selectTool(): that runs the tool-switch logic; here only the active tool changes.
            selectedTool = Tools.PAIR;
            palette.setSelectedTool(Tools.PAIR);
        }
        refresh();
    }
```

`ToolPalette.setSelectedTool` only calls `setSelected` on the button. It doesn't fire the button's `ActionListener`, so it doesn't call back into `selectTool`.

- [ ] **Step 6: Feed the panel on every refresh**

In `refresh()`, add `updateSidePanel();` right after `applyHighlights();`. Add this method below `applyHighlights()`:

```java
    /** Hands the side panel what currently exists: selection, pair, walk. */
    private void updateSidePanel() {
        if (sidePanel == null) return;
        SidePanel.Content c = new SidePanel.Content();
        List<Vertex> selected = new ArrayList<Vertex>();
        for (Vertex v : vertexList) {
            if (v.wasClicked) selected.add(v);
        }
        c.selected = selected;
        c.pair = pairSummary;
        c.pathIndex = selectedTool == Tools.PAIR && pairSummary != null && !pairSummary.paths.isEmpty()
                ? selectedPathIndex : -1;
        c.walk = currentWalk;
        c.foundKind = foundKind;
        c.walkMessage = walkMessage;
        c.weighted = Edge.isWeighted(edgeList);
        sidePanel.display(c);
    }
```

- [ ] **Step 7: Delete the info boxes**

Delete the methods `drawInfoBox`, `drawPairInfoBox` and `drawWalkInfoBox`. Also delete the helpers `truncate` and `yesNo` if nothing else uses them (`grep -n "truncate(\|yesNo(" src/graphtheory/Canvas.java`).

In `CanvasPane.paintComponent`, delete these lines:

```java

            g2.setStroke(new BasicStroke(1f));
            g2.setFont(getFont());
            drawInfoBox(g2);
            drawPairInfoBox(g2);
            drawWalkInfoBox(g2);
```

so the method ends with `drawWalkMarkers(g2);`.

Then check nothing is left:

```bash
grep -n "pairPaths\|PATH_ROWS\|drawInfoBox\|drawPairInfoBox\|drawWalkInfoBox\|generateVertexDisjointPaths" src/graphtheory/Canvas.java
```

Expected: no output. (`VertexPair.generateVertexDisjointPaths` stays, because `GraphProperties.displayContainers` still uses it.)

- [ ] **Step 8: Build, test, launch**

Expected: `OK (218 tests)`; the launch command exits 124 with clean stderr.

- [ ] **Step 9: Check it by hand**

1. The panel sits right of the canvas and the divider drags. Making the window wider widens the canvas, not the panel.
2. With nothing selected, the panel shows the hint. Selecting one vertex shows its property rows. Selecting several (Grab tool with Ctrl or Shift, whichever the app uses) shows one table row each.
3. Pick a pair with parallel edges. The Pair section lists every path. Clicking a row draws it in amber. ↑/↓ move the list selection.
4. With a weight set to something other than 1, Weighted distance appears and the list is sorted lightest first, with `geodesic` and `lightest` tags.
5. Build a walk. The Walk section shows the whole walk wrapped, plus Length, Kind and the yes/no grid. A bad click shows a red message there.
6. Nothing is drawn over the graph any more.

- [ ] **Step 10: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "feat: side panel replaces the info boxes painted over the canvas

The pair's paths and distances are computed once per change (PairSummary)
instead of on every repaint.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 6: Tool rules, found walks, pair by reference

**Files:**
- Create: `src/graphtheory/GraphShape.java`
- Modify: `src/graphtheory/Canvas.java`, `src/graphtheory/Tools.java`
- Test: `test/graphtheory/GraphShapeTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/GraphShapeTest.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphShapeTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Edge ab = new Edge(a, b, false);
    private final Vector<Vertex> vs = new Vector<Vertex>(Arrays.asList(a, b));
    private final Vector<Edge> es = new Vector<Edge>(Arrays.asList(ab));

    @Test
    public void unchanged_matches() {
        assertTrue(GraphShape.of(vs, es).matches(vs, es));
    }

    @Test
    public void movingOrRenaming_stillMatches() {
        GraphShape s = GraphShape.of(vs, es);
        a.location.x = 300;
        b.name = "z";
        assertTrue(s.matches(vs, es));
    }

    @Test
    public void weightChanged_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        ab.setWeight(5);
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void edgeAddedOrRemoved_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        es.add(new Edge(b, a, true));
        assertFalse(s.matches(vs, es));
        es.remove(1);
        es.remove(0);
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void vertexAdded_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        vs.add(new Vertex("c", 0, 0));
        assertFalse(s.matches(vs, es));
    }

    @Test
    public void sameShapeButNewObjects_doesNotMatch() {
        GraphShape s = GraphShape.of(vs, es);
        List<Edge> copy = Arrays.asList(new Edge(a, b, false));
        assertFalse(s.matches(vs, copy));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Add `graphtheory.GraphShapeTest` to the list. Expected: compile error `cannot find symbol: class GraphShape`.

- [ ] **Step 3: Implement GraphShape**

Create `src/graphtheory/GraphShape.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.List;

/**
 * Which vertices and edges a graph has (by identity) and the edges' weights.
 * Positions and names are not part of it, so moving or renaming a vertex keeps
 * the shape. Used to drop a found walk once the graph it was found in changes
 * (CONTEXT.md, Built and found walks).
 */
public final class GraphShape {

    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private final int[] weights;

    private GraphShape(List<Vertex> vs, List<Edge> es) {
        vertices = new ArrayList<Vertex>(vs);
        edges = new ArrayList<Edge>(es);
        weights = new int[es.size()];
        for (int i = 0; i < weights.length; i++) weights[i] = es.get(i).weight;
    }

    public static GraphShape of(List<Vertex> vs, List<Edge> es) {
        return new GraphShape(vs, es);
    }

    /** Same vertex and edge objects, in the same order, with the same weights. */
    public boolean matches(List<Vertex> vs, List<Edge> es) {
        if (vs.size() != vertices.size() || es.size() != edges.size()) return false;
        for (int i = 0; i < vertices.size(); i++) if (vs.get(i) != vertices.get(i)) return false;
        for (int i = 0; i < edges.size(); i++) {
            if (es.get(i) != edges.get(i) || es.get(i).weight != weights[i]) return false;
        }
        return true;
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Expected: 6 more tests, `OK (224 tests)`.

- [ ] **Step 5: Choosing a tool no longer clears**

In `selectTool`, delete:

```java
        if (tool == Tools.PAIR) {
            pairedVertex1Index = -1;
            pairedVertex2Index = -1;
            currentPairVP = null;
            pairSummary = null;
        } else if (tool == Tools.WALK) {
            clearWalk();
        }
```

- [ ] **Step 6: Store the pair as vertex references**

Replace the fields:

```java
    private int pairedVertex1Index = -1;
    private int pairedVertex2Index = -1;
```

with:

```java
    // The pair being picked with the Pair tool: first vertex, then second (null = not picked yet)
    private Vertex pairFirst = null;
    private Vertex pairSecond = null;
```

In `replaceGraph`, replace

```java
        pairedVertex1Index = -1;
        pairedVertex2Index = -1;
```

with

```java
        pairFirst = null;
        pairSecond = null;
```

Replace the Pair tool's click case body:

```java
                    case 6: {
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                int idx = vertexList.indexOf(v);
                                ...
                            }
                        }
                        refresh();
                        break;
                    }
```

with:

```java
                    case 6: {
                        Vertex v = vertexAt(e.getX(), e.getY());
                        if (v != null) {
                            if (pairFirst == null) {
                                pairFirst = v;
                                v.wasClicked = true;
                            } else if (pairSecond == null && v != pairFirst) {
                                pairSecond = v;
                                v.wasClicked = true;
                                currentPairVP = new VertexPair(pairFirst, pairSecond);
                                refreshPairPaths();
                            } else {
                                clearPair();
                                pairFirst = v;
                                v.wasClicked = true;
                            }
                        }
                        refresh();
                        break;
                    }
```

In the Remove tool's vertex branch, replace:

```java
                            if (pairedVertex1Index >= 0
                                    && vertexList.get(pairedVertex1Index) == victim) {
                                pairedVertex1Index = -1;
                            }
                            if (pairedVertex2Index >= 0
                                    && vertexList.get(pairedVertex2Index) == victim) {
                                pairedVertex2Index = -1;
                            }
                            if (currentPairVP != null
                                    && (currentPairVP.vertex1 == victim || currentPairVP.vertex2 == victim)) {
                                currentPairVP = null;
                            }
```

with:

```java
                            if (victim == pairFirst || victim == pairSecond) clearPair();
```

Add below `clearWalk()`:

```java
    /** Forgets the pair (Esc with the Pair tool, or picking a new first vertex) and unselects its vertices. */
    private void clearPair() {
        if (pairFirst != null) pairFirst.wasClicked = false;
        if (pairSecond != null) pairSecond.wasClicked = false;
        pairFirst = null;
        pairSecond = null;
        currentPairVP = null;
        pairSummary = null;
        selectedPathIndex = 0;
    }
```

Check: `grep -n "pairedVertex" src/graphtheory/Canvas.java` gives no output.

- [ ] **Step 7: Esc clears what the active tool owns**

In `installKeyBindings`, replace:

```java
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "walkClear");
        am.put("walkClear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 9 && selectedWindow == 0) { clearWalk(); refresh(); }
            }
        });
```

with:

```java
        // Esc clears what the active tool owns: the walk (Walk tool) or the pair (Pair tool).
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearToolState");
        am.put("clearToolState", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedWindow != 0) return;
                if (selectedTool == Tools.WALK) {
                    clearWalk();
                } else if (selectedTool == Tools.PAIR) {
                    clearPair();
                } else {
                    return;
                }
                refresh();
            }
        });
```

- [ ] **Step 8: Found walks are read-only and dropped when the graph changes**

Add a field below `foundKind`:

```java
    // The graph a found walk was found in; when it changes the found walk is dropped
    private GraphShape foundShape = null;
```

Replace `clearWalk()`:

```java
    private void clearWalk() {
        currentWalk = null;
        walkMessage = null;
    }
```

with:

```java
    private void clearWalk() {
        currentWalk = null;
        walkMessage = null;
        foundKind = null;
        foundShape = null;
    }
```

At the end of `findTraversal`, replace:

```java
        if (currentWalk == null) walkMessage = "No " + kind + " exists";
```

with:

```java
        if (currentWalk == null) {
            walkMessage = "No " + kind + " exists";
        } else {
            foundKind = kind;
            foundShape = GraphShape.of(vertexList, edgeList);
        }
```

At the top of `undoWalkStep()`, before `walkMessage = null;`, add:

```java
        if (foundKind != null) return;   // a found walk is read-only
```

In `handleWalkClick`, after the line `if (hitV == null && hitE == null) return;`, add:

```java
        if (foundKind != null) {
            // A found walk is read-only: clicking a vertex starts a new built walk in its place.
            if (hitV == null) {
                walkMessage = "Click a vertex to start a new walk";
                return;
            }
            clearWalk();
        }
```

In `afterEdit`, inside `if (!before.equals(snapshot())) {`, add as the first line:

```java
            if (foundShape != null && !foundShape.matches(vertexList, edgeList)) clearWalk();
```

Check that every graph edit goes through `afterEdit`: adding or removing vertices and edges, the weight dialog, rename, Remove All and menu actions. Grep for `markGraphDirty()` call sites outside `afterEdit`/`replaceGraph`. Each must be inside a handler that ends in `afterEdit(before)` (`mouseClicked`, `mouseReleased`, the menu listener). If you find one that isn't, add the same `foundShape` check there and say so in your report.

- [ ] **Step 9: Hints**

In `src/graphtheory/Tools.java`, `hint(...)`, replace the PAIR and WALK lines with:

```java
            case PAIR:   return "Click two vertices to inspect the ordered pair. Up/Down or the list on the right browse its paths. Esc clears the pair.";
            case WALK:   return "Click a vertex to start, then vertices or edges to extend. Backspace or right-click undoes a step, Esc clears. A found walk is read-only: click a vertex to start a new one.";
```

- [ ] **Step 10: Build, test, launch**

Expected: `OK (224 tests)`; the launch exits 124 with clean stderr.

- [ ] **Step 11: Check it by hand**

1. Pick a pair, switch to Grab, switch back to Pair. The pair and its list are still there. Esc with the Pair tool clears them.
2. Build a walk, switch tools and back. The walk is still there. Esc with the Walk tool clears it.
3. Pick a pair, then with Remove delete a vertex that was created *before* both pair vertices. The pair still names the same two vertices.
4. Find Euler Tour. The heading says "Euler tour (found)". Backspace does nothing. Clicking an edge says "Click a vertex to start a new walk". Clicking a vertex starts a new "Built walk".
5. Find again, then drag a vertex. The found walk stays. Add an edge or change a weight, and it disappears.
6. Clicking a path row while another tool is active switches to Pair and keeps the pair.

- [ ] **Step 12: Commit**

```bash
git add src/graphtheory/GraphShape.java src/graphtheory/Canvas.java src/graphtheory/Tools.java test/graphtheory/GraphShapeTest.java
git commit -m "feat: tools keep the pair and walk, Esc clears; found walks are read-only

- choosing a tool never clears; Esc clears the Pair tool's pair or the Walk tool's walk
- a found walk can't be extended or undone, a vertex click starts a new built walk,
  and changing vertices, edges or weights drops it (GraphShape)
- the pair is kept as two vertices, not list indices, so removals can't shift it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 7: Wrap-up

- [ ] **Step 1: Full test run and launch**

Expected: `OK (224 tests)`; the launch exits 124 with clean stderr.

- [ ] **Step 2: Finish the branch**

Use superpowers:finishing-a-development-branch. PRs are opened as `jbarguilles`.
