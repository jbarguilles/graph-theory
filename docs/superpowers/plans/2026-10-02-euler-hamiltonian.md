# Euler & Hamiltonian Walks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Euler trail/tour and Hamiltonian path/cycle checks respect arc direction and the glossary's small cases, and add four "Find …" menu items that load the walk found into the Build Walk tool.

**Architecture:** A new pure-logic class `Traversals` has one search function per concept. Each returns the edge-aware `Walk` it found, or `null`. The Graph Summary's Yes/No and the Find menu items both call these functions, so they can never disagree. Euler is exact with no cap: degree conditions + Hierholzer for purely undirected or purely directed graphs, and a max-flow orientation of the undirected edges followed by Hierholzer for mixed graphs (Task 1b). The Graph Summary caches its answers per graph change. Hamiltonian uses edge-aware backtracking (capped at 20 vertices). The searches work from the **edge list**, not the vertex neighbour lists. (`Canvas` rebuilds `undirectedNeighbors` from the adjacency matrix when Properties opens, so the edge list is the reliable source.)

**Tech Stack:** Java 17 (source-compatible with Java 7+, so no lambdas), Swing/AWT, JUnit 4.13.2.

**Read first:** `CONTEXT.md` (sections *Walk*, *Kinds of Walk*, *Euler Trail and Euler Tour*, *Hamiltonian Path and Hamiltonian Cycle*), `docs/adr/0002-edge-aware-walks.md` and `docs/adr/0003-traversal-checks-respect-direction.md`. The definitions there are the spec.

---

## How to build and test

There is no Ant/Maven on the PATH. Run from the repo root (`GraphTheory/`) in **Git Bash**:

```bash
CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -cp "out/test;$CP" org.junit.runner.JUnitCore \
     graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest
```

`MSYS_NO_PATHCONV=1` is required. Without it, Git Bash mangles the `;`-separated classpath and you get `ClassNotFoundException: org.hamcrest.SelfDescribing`. Leave `graphtheory.TraversalsTest` out of the list until Task 1 creates it. `out/` is git-ignored.

Baseline before starting: **47 tests pass.**

Launch the app (after compiling as above):

```bash
MSYS_NO_PATHCONV=1 java -cp out/test graphtheory.Main
```

---

## File Map

| File | Change |
|---|---|
| `src/graphtheory/Traversals.java` | **New.** Euler trail/tour and Hamiltonian path/cycle searches returning `Walk`s; size caps |
| `test/graphtheory/TraversalsTest.java` | **New.** Unit tests for `Traversals` |
| `src/graphtheory/GraphProperties.java` | Graph Summary calls `Traversals`; labels renamed; old direction-blind checks deleted |
| `src/graphtheory/Canvas.java` | Four "Find …" items in the Extras menu; `findTraversal` loads the result into the Build Walk tool |

---

## Task 0: Prerequisites

- [ ] **Step 1: Deal with the pre-existing `Canvas.java` change**

`git status` shows `src/graphtheory/Canvas.java` already modified before this plan: arrow, infinity and up/down characters replaced with `\u2192`, `\u221E`, `\u2191`/`\u2193`, `\u2190` escapes. It is unrelated to this feature. **Ask the user** whether to commit it on its own first. If they agree:

```bash
git add src/graphtheory/Canvas.java
git commit -m "fix: use unicode escapes for arrows and infinity in pair box

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Don't mix it into the feature commits below.

- [ ] **Step 2: Commit the docs that define this feature**

```bash
git add CONTEXT.md docs/adr/0003-traversal-checks-respect-direction.md docs/superpowers/plans/2026-10-02-euler-hamiltonian.md
git commit -m "docs: define Euler and Hamiltonian walks; ADR 0003; implementation plan

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 3: Confirm the baseline**

Run the build-and-test command (without `TraversalsTest`). Expected: `OK (47 tests)`.

---

## Task 1: Euler trail and Euler tour

**Files:**
- Create: `src/graphtheory/Traversals.java`
- Test: `test/graphtheory/TraversalsTest.java`

- [ ] **Step 1: Write the failing tests**

Create `test/graphtheory/TraversalsTest.java`:

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class TraversalsTest {

    private final Vertex a = new Vertex("a", 0, 0);
    private final Vertex b = new Vertex("b", 0, 0);
    private final Vertex c = new Vertex("c", 0, 0);
    private final Vertex d = new Vertex("d", 0, 0);

    private final Vector<Vertex> vList = new Vector<Vertex>();
    private final Vector<Edge> eList = new Vector<Edge>();

    private void vertices(Vertex... vs) { for (Vertex v : vs) vList.add(v); }
    private Edge und(Vertex x, Vertex y) { Edge e = new Edge(x, y, false); eList.add(e); return e; }
    private Edge arc(Vertex x, Vertex y) { Edge e = new Edge(x, y, true);  eList.add(e); return e; }

    /** w is a trail that uses every edge of eList exactly once. */
    private void assertEulerTrail(Walk w) {
        assertNotNull(w);
        assertTrue(w.isTrail());
        assertEquals(eList.size(), w.length());
        for (Edge e : eList) assertTrue(w.uses(e));
    }

    // ---------- Euler ----------

    @Test
    public void noVertices_noEulerTrailOrTour() {
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void edgeless_trivialWalkIsEulerTrail_butNotTour() {
        vertices(a, b);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertNotNull(w);
        assertEquals(0, w.length());
        assertSame(a, w.start());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void undirectedTriangle_hasEulerTour() {
        vertices(a, b, c);
        und(a, b); und(b, c); und(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
        assertEulerTrail(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void undirectedPath_trailStartsAtFirstOddVertex_noTour() {
        vertices(a, b, c);
        und(a, b); und(b, c);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(a, w.start());
        assertSame(c, w.end());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void star_fourOddVertices_noEulerTrail() {
        vertices(d, a, b, c);
        und(d, a); und(d, b); und(d, c);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void twoSeparateEdges_notConnected_noEulerTrail() {
        vertices(a, b, c, d);
        und(a, b); und(c, d);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void isolatedVertex_doesNotBlockEulerTour() {
        vertices(d, a, b, c);
        und(a, b); und(b, c); und(c, a);
        assertEulerTrail(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void undirectedSelfLoop_isEulerTourOfLengthOne() {
        vertices(a);
        und(a, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void directedCycle_hasEulerTour() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); arc(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void directedPath_trailStartsAtSource_noTour() {
        vertices(c, b, a);
        arc(a, b); arc(b, c);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(a, w.start());
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void twoArcsIntoSameVertex_noEulerTrail() {
        // Undirected degrees are 0, so the old check wrongly said "Yes".
        vertices(a, b, c);
        arc(a, b); arc(c, b);
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void mixedTriangleWithExtraArc_hasTrail_noTour() {
        vertices(a, b, c);
        und(a, b); und(b, c); und(c, a); arc(a, b);
        assertEulerTrail(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void mixedCycleFollowingArcs_hasEulerTour() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); und(c, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void mixedEvenDegrees_butArcsTrapAtB_noTrailOrTour() {
        // Every vertex has even total degree, but b has no way out.
        vertices(a, b, c);
        arc(a, b); arc(c, b); und(a, c);
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test
    public void mixedOverEdgeCap_isTooLarge_butPureUndirectedIsNot() {
        Vertex[] vs = new Vertex[40];
        for (int i = 0; i < 40; i++) { vs[i] = new Vertex("v" + i, 0, 0); vList.add(vs[i]); }
        for (int i = 0; i < 40; i++) und(vs[i], vs[(i + 1) % 40]);   // 40-edge cycle
        assertFalse(Traversals.eulerTooLarge(eList));
        assertEulerTrail(Traversals.eulerTour(vList, eList));

        arc(vs[0], vs[1]);
        assertTrue(Traversals.eulerTooLarge(eList));
        assertNull(Traversals.eulerTrail(vList, eList));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run the build-and-test command with `graphtheory.TraversalsTest` added.
Expected: compilation fails with `cannot find symbol ... Traversals`.

- [ ] **Step 3: Implement the Euler searches**

Create `src/graphtheory/Traversals.java`:

```java
package graphtheory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Searches for Euler trails/tours and Hamiltonian paths/cycles.
 * Each search returns the Walk it found, or null if none exists (or the
 * graph is over the size cap). Arcs are only traversed source -> destination.
 * See CONTEXT.md and docs/adr/0003-traversal-checks-respect-direction.md.
 */
public class Traversals {

    /** Mixed graphs need exponential search for Euler; above this many edges we don't try. */
    public static final int EULER_MIXED_EDGE_CAP = 30;
    /** Hamiltonian search is exponential; above this many vertices we don't try. */
    public static final int HAMILTON_VERTEX_CAP = 20;

    // ---------- Euler ----------

    /** True if the graph is mixed and has more edges than the Euler search will try. */
    public static boolean eulerTooLarge(Vector<Edge> eList) {
        return isMixed(eList) && eList.size() > EULER_MIXED_EDGE_CAP;
    }

    /** A trail using every edge exactly once (open or closed), or null. */
    public static Walk eulerTrail(Vector<Vertex> vList, Vector<Edge> eList) {
        return euler(vList, eList, false);
    }

    /** A closed trail using every edge exactly once, or null. */
    public static Walk eulerTour(Vector<Vertex> vList, Vector<Edge> eList) {
        return euler(vList, eList, true);
    }

    private static Walk euler(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        if (vList.isEmpty()) return null;
        // The trivial walk uses all zero edges, but it is not closed.
        if (eList.isEmpty()) return closed ? null : new Walk(vList.firstElement());
        if (!edgesConnected(eList)) return null;

        if (isMixed(eList)) return eulerMixed(vList, eList, closed);
        Vertex start = eList.firstElement().directed
                ? directedEulerStart(vList, eList, closed)
                : undirectedEulerStart(vList, eList, closed);
        return start == null ? null : hierholzer(start, eList);
    }

    private static boolean isMixed(Vector<Edge> eList) {
        boolean anyDirected = false, anyUndirected = false;
        for (Edge e : eList) {
            if (e.directed) anyDirected = true; else anyUndirected = true;
        }
        return anyDirected && anyUndirected;
    }

    /** Number of edge-ends at each vertex, ignoring direction (a self-loop counts 2). */
    private static Map<Vertex, Integer> endCounts(Vector<Edge> eList) {
        Map<Vertex, Integer> ends = new HashMap<Vertex, Integer>();
        for (Edge e : eList) {
            add(ends, e.vertex1, 1);
            add(ends, e.vertex2, 1);
        }
        return ends;
    }

    /** Vertices with an odd number of edge-ends, in vList order. */
    private static List<Vertex> oddVertices(Vector<Vertex> vList, Vector<Edge> eList) {
        Map<Vertex, Integer> ends = endCounts(eList);
        List<Vertex> odd = new Vector<Vertex>();
        for (Vertex v : vList) {
            if (get(ends, v) % 2 != 0) odd.add(v);
        }
        return odd;
    }

    /** Undirected graph: all degrees even (any start), or exactly two odd (start at one) for an open trail. */
    private static Vertex undirectedEulerStart(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        List<Vertex> odd = oddVertices(vList, eList);
        if (odd.isEmpty()) return eList.firstElement().vertex1;
        if (!closed && odd.size() == 2) return odd.get(0);
        return null;
    }

    /** Directed graph: out = in everywhere, or (open trail only) one vertex +1 (the start) and one -1. */
    private static Vertex directedEulerStart(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        Map<Vertex, Integer> net = new HashMap<Vertex, Integer>();   // out-degree - in-degree
        for (Edge e : eList) {
            add(net, e.vertex1, 1);
            add(net, e.vertex2, -1);
        }
        Vertex plus = null;
        int plusCount = 0, minusCount = 0;
        for (Vertex v : vList) {
            int n = get(net, v);
            if (n == 0) continue;
            if (n == 1) { plus = v; plusCount++; }
            else if (n == -1) minusCount++;
            else return null;
        }
        if (plusCount == 0 && minusCount == 0) return eList.firstElement().vertex1;
        if (!closed && plusCount == 1 && minusCount == 1) return plus;
        return null;
    }

    /**
     * Hierholzer's algorithm from 'start'. Assumes the degree conditions hold
     * and the graph is purely undirected or purely directed.
     */
    private static Walk hierholzer(Vertex start, Vector<Edge> eList) {
        boolean[] used = new boolean[eList.size()];
        Vector<Vertex> vStack = new Vector<Vertex>();
        Vector<Edge> eStack = new Vector<Edge>();   // eStack[i] = edge used to reach vStack[i]; null for start
        LinkedList<Vertex> vOut = new LinkedList<Vertex>();
        LinkedList<Edge> eOut = new LinkedList<Edge>();

        vStack.add(start);
        eStack.add(null);
        while (!vStack.isEmpty()) {
            Vertex v = vStack.lastElement();
            int i = nextUnusedEdge(v, eList, used);
            if (i >= 0) {
                used[i] = true;
                vStack.add(Walk.otherEnd(eList.get(i), v));
                eStack.add(eList.get(i));
            } else {
                vOut.addFirst(vStack.remove(vStack.size() - 1));
                Edge e = eStack.remove(eStack.size() - 1);
                if (e != null) eOut.addFirst(e);
            }
        }
        if (eOut.size() != eList.size()) return null;

        Walk w = new Walk(vOut.getFirst());
        for (Edge e : eOut) w.extend(e);
        return w;
    }

    private static int nextUnusedEdge(Vertex from, Vector<Edge> eList, boolean[] used) {
        for (int i = 0; i < eList.size(); i++) {
            if (!used[i] && Walk.canTraverse(eList.get(i), from)) return i;
        }
        return -1;
    }

    /** Mixed graph: backtracking over edges, after a parity check that ignores direction. */
    private static Walk eulerMixed(Vector<Vertex> vList, Vector<Edge> eList, boolean closed) {
        if (eList.size() > EULER_MIXED_EDGE_CAP) return null;

        List<Vertex> odd = oddVertices(vList, eList);
        List<Vertex> starts;
        if (closed) {
            if (!odd.isEmpty()) return null;
            starts = new Vector<Vertex>();
            starts.add(eList.firstElement().vertex1);   // a tour passes every edge's ends
        } else if (odd.size() == 2) {
            starts = odd;
        } else if (odd.isEmpty()) {
            Map<Vertex, Integer> ends = endCounts(eList);
            starts = new Vector<Vertex>();
            for (Vertex v : vList) {
                if (get(ends, v) > 0) starts.add(v);
            }
        } else {
            return null;
        }

        for (Vertex s : starts) {
            Walk w = new Walk(s);
            if (extendEuler(w, eList, new boolean[eList.size()], eList.size(), closed)) return w;
        }
        return null;
    }

    private static boolean extendEuler(Walk w, Vector<Edge> eList, boolean[] used,
                                       int remaining, boolean closed) {
        if (remaining == 0) return !closed || w.end() == w.start();
        Vertex at = w.end();
        for (int i = 0; i < eList.size(); i++) {
            Edge e = eList.get(i);
            if (used[i] || !Walk.canTraverse(e, at)) continue;
            used[i] = true;
            w.extend(e);
            if (extendEuler(w, eList, used, remaining - 1, closed)) return true;
            w.undo();
            used[i] = false;
        }
        return false;
    }

    /** True if every edge lies in one connected piece (direction ignored). */
    private static boolean edgesConnected(Vector<Edge> eList) {
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        for (Edge e : eList) union(parent, e.vertex1, e.vertex2);
        Vertex root = find(parent, eList.firstElement().vertex1);
        for (Edge e : eList) {
            if (find(parent, e.vertex1) != root) return false;
        }
        return true;
    }

    private static Vertex find(Map<Vertex, Vertex> parent, Vertex v) {
        Vertex p = parent.get(v);
        if (p == null) { parent.put(v, v); return v; }
        if (p == v) return v;
        Vertex root = find(parent, p);
        parent.put(v, root);
        return root;
    }

    private static void union(Map<Vertex, Vertex> parent, Vertex x, Vertex y) {
        Vertex rx = find(parent, x), ry = find(parent, y);
        if (rx != ry) parent.put(rx, ry);
    }

    private static int get(Map<Vertex, Integer> m, Vertex v) {
        Integer n = m.get(v);
        return n == null ? 0 : n;
    }

    private static void add(Map<Vertex, Integer> m, Vertex v, int delta) {
        m.put(v, get(m, v) + delta);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run the build-and-test command with `graphtheory.TraversalsTest`.
Expected: `OK (62 tests)`, i.e. 47 + 15.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Traversals.java test/graphtheory/TraversalsTest.java
git commit -m "feat: direction-aware Euler trail and tour search returning walks

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 1b: Exact mixed-graph Euler search (replaces the capped backtracking)

> **Added after review.** Task 1's mixed-graph backtracking gave correct answers: it was fuzzed against brute force on 200,000 random graphs with 0 mismatches. But it hangs on graphs that pass the parity test and have no trail. Seven triangles sharing a hub plus two arcs `h→x` (23 edges) took 9 s, and K7 minus an edge plus two arcs (22 edges) took over 120 s. The user chose the exact polynomial method. ADR 0003 is updated accordingly.

**Files:**
- Modify: `src/graphtheory/Traversals.java`
- Modify: `test/graphtheory/TraversalsTest.java`

**Design (implement exactly this):**

1. **Remove** `EULER_MIXED_EDGE_CAP`, `eulerTooLarge`, `extendEuler` and the backtracking body of `eulerMixed`. Euler search no longer has a cap. Keep the `euler(...)` entry flow: no vertices → null; no edges → trivial trail / no tour; not connected → null.
2. **Generalise `hierholzer`** to `hierholzer(Vertex start, Vector<Edge> eList, Vertex[] tail)`. `tail[i]` is the only vertex edge i may be left from, or `null` if it may be left from either end. Replace `Walk.canTraverse(e, v)` in `nextUnusedEdge` with that rule: `tail[i] == null ? (e.vertex1 == v || e.vertex2 == v) : tail[i] == v`. Purely undirected and purely directed graphs pass `tail[i] = e.directed ? e.vertex1 : null`; add a small helper for this. Still build the result with `Walk.extend`, which re-checks the real direction rules.
3. **Mixed Euler tour** (private `mixedTour(eList)`, for a connected mixed edge list where every vertex has an even number of edge-ends):
   - For each vertex v, `need(v) = ends(v)/2 − loops(v) − arcsOut(v)`. Here `ends` counts edge-ends ignoring direction, so a self-loop counts 2. `loops` counts self-loops of either kind. `arcsOut` counts non-loop arcs leaving v. If any `need(v) < 0`, return null.
   - Every non-loop undirected edge must be assigned to the endpoint it will leave from, with exactly `need(v)` edges assigned to each v. Solve this as a bipartite b-matching (unit-capacity max-flow) using augmenting paths:
     - For each such edge, DFS for an augmenting path. An edge can take an endpoint with spare capacity directly, or take an endpoint whose assigned edges include one that can be moved to its other endpoint, recursively, with a visited set per augmentation.
     - If any edge can't be assigned, return null.
   - Build `tail`: arcs get `vertex1`, assigned undirected edges get their assigned endpoint, and self-loops get their vertex. Run `hierholzer(eList.firstElement().vertex1, eList, tail)`.
4. **Mixed Euler trail** (`closed == false`):
   - 0 odd vertices: return `mixedTour(eList)`. Any closed trail is a trail, and an open Euler trail can't exist when every count is even.
   - Exactly 2 odd vertices x, y: add a **virtual** undirected `new Edge(x, y, false)` to a copy of `eList` and run `mixedTour` on the copy. If that returns a walk, cut it at the virtual edge: the trail starts at the vertex right after the virtual edge and follows the remaining edges cyclically, back to the vertex right before it. Rebuild it with `new Walk(...)` + `extend` so the virtual edge never appears in the result.
   - Otherwise return null.
5. Leave the pure undirected and pure directed paths (degree tests + Hierholzer) as they are, except for passing `tail`.
6. Fix comments as needed: one start suffices for a closed trail because a closed trail can be rotated. Javadoc on `eulerTrail`/`eulerTour`: "exact for any size; returns null only when none exists".

**Tests (TDD: write first, then implement):**
- Delete `mixedOverEdgeCap_isTooLarge_butPureUndirectedIsNot`.
- Add:

```java
    @Test(timeout = 2000)
    public void windmillWithTwoTrappingArcs_noEulerTrail_fast() {
        // 9 triangles sharing hub h, plus arcs h->x twice: every vertex has
        // even edge-ends, but x can't be left. Backtracking took minutes here.
        Vertex h = new Vertex("h", 0, 0), x = new Vertex("x", 0, 0);
        vList.add(h); vList.add(x);
        for (int i = 0; i < 9; i++) {
            Vertex p = new Vertex("p" + i, 0, 0), q = new Vertex("q" + i, 0, 0);
            vList.add(p); vList.add(q);
            und(h, p); und(p, q); und(q, h);
        }
        arc(h, x); arc(h, x);
        assertNull(Traversals.eulerTrail(vList, eList));
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test(timeout = 2000)
    public void k7MinusEdgePlusTwoArcs_noEulerTour_fast() {
        Vertex[] k = new Vertex[7];
        for (int i = 0; i < 7; i++) { k[i] = new Vertex("k" + i, 0, 0); vList.add(k[i]); }
        for (int i = 0; i < 7; i++)
            for (int j = i + 1; j < 7; j++)
                if (!(i == 0 && j == 1)) und(k[i], k[j]);
        Vertex x = new Vertex("x", 0, 0);
        vList.add(x);
        arc(x, k[0]); arc(x, k[1]);
        assertNull(Traversals.eulerTour(vList, eList));
    }

    @Test(timeout = 2000)
    public void largeMixedCycle_hasEulerTour_noCap() {
        Vertex[] vs = new Vertex[60];
        for (int i = 0; i < 60; i++) { vs[i] = new Vertex("v" + i, 0, 0); vList.add(vs[i]); }
        for (int i = 0; i < 60; i++) {
            if (i % 2 == 0) arc(vs[i], vs[(i + 1) % 60]); else und(vs[i], vs[(i + 1) % 60]);
        }
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void mixedTrail_mustStartAtSecondOddVertex() {
        // Odd vertices are a (first in vList) and c; the arc forces c -> b -> a.
        vertices(a, b, c);
        arc(c, b); und(b, a);
        Walk w = Traversals.eulerTrail(vList, eList);
        assertEulerTrail(w);
        assertSame(c, w.start());
        assertSame(a, w.end());
    }

    @Test
    public void mixedUndirectedEdgesMustBeOrientedConsistently() {
        // Square with arcs (a,b), (c,d) and undirected {b,c}, {d,a}.
        // The tour a->b->c->d->a needs {b,c} as b->c and {d,a} as d->a.
        vertices(a, b, c, d);
        arc(a, b); und(b, c); arc(c, d); und(d, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void parallelUndirectedEdges_tourOfLengthTwo() {
        vertices(a, b);
        und(a, b); und(a, b);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void parallelArcsSameWay_noTour_trailNeedsBalance() {
        vertices(a, b);
        arc(a, b); arc(a, b);
        assertNull(Traversals.eulerTour(vList, eList));
        assertNull(Traversals.eulerTrail(vList, eList));
    }

    @Test
    public void directedSelfLoop_isEulerTourOfLengthOne() {
        vertices(a);
        arc(a, a);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }

    @Test
    public void balancedDirectedGraph_eulerTrailIsClosed() {
        vertices(a, b, c);
        arc(a, b); arc(b, c); arc(c, a);
        assertTrue(Traversals.eulerTrail(vList, eList).isCircuit());
    }

    @Test
    public void mixedWithSelfLoops_hasEulerTour() {
        vertices(a, b);
        arc(a, b); und(b, a); und(a, a); arc(b, b);
        Walk w = Traversals.eulerTour(vList, eList);
        assertEulerTrail(w);
        assertTrue(w.isCircuit());
    }
```

**Verification beyond JUnit:** A reviewer left a brute-force fuzz harness at `C:\Users\Jade\AppData\Local\Temp\claude\C--Users-Jade-Documents-Jade-4th-Year--Graph-Theory-v0-5--Graph-Theory-v0-5-GraphTheory\06f6e680-b554-4b2d-9faf-548e1c37e365\scratchpad\graphtheory\Fuzz.java`. It compares `Traversals` against exhaustive search on random small graphs. Adapt it if it references the removed `eulerTooLarge`, run it against the new code (at least 200,000 graphs), and report the mismatch count, which must be 0. Also run `Perf.java` and `Perf2.java` from the same folder; each must finish in well under a second. Don't commit the harness.

**Expected:** `OK (71 tests)`, i.e. 62 − 1 + 10.

**Commit:**

```bash
git add src/graphtheory/Traversals.java test/graphtheory/TraversalsTest.java
git commit -m "feat: exact polynomial Euler search for mixed graphs via edge orientation flow

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 2: Hamiltonian path and Hamiltonian cycle

**Files:**
- Modify: `src/graphtheory/Traversals.java` (append before the private helpers at the bottom)
- Test: `test/graphtheory/TraversalsTest.java` (append inside the class)

- [ ] **Step 1: Write the failing tests**

Append to `TraversalsTest`, before the final `}`:

```java
    // ---------- Hamiltonian ----------

    /** w is a path through every vertex of vList. */
    private void assertHamiltonianPath(Walk w) {
        assertNotNull(w);
        assertTrue(w.isPath());
        assertEquals(vList.size(), w.vertices().size());
    }

    /** w is a cycle through every vertex of vList. */
    private void assertHamiltonianCycle(Walk w) {
        assertNotNull(w);
        assertTrue(w.isCycle());
        assertEquals(vList.size(), w.length());
        for (Vertex v : vList) assertTrue(w.visits(v));
    }

    @Test
    public void noVertices_noHamiltonianPathOrCycle() {
        assertNull(Traversals.hamiltonianPath(vList, eList));
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void singleVertex_trivialHamiltonianPath_noCycle() {
        vertices(a);
        Walk w = Traversals.hamiltonianPath(vList, eList);
        assertHamiltonianPath(w);
        assertEquals(0, w.length());
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void selfLoop_isHamiltonianCycleOfLengthOne() {
        vertices(a);
        und(a, a);
        assertHamiltonianCycle(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void singleUndirectedEdge_pathButNoCycle() {
        vertices(a, b);
        und(a, b);
        assertHamiltonianPath(Traversals.hamiltonianPath(vList, eList));
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void undirectedEdgePlusReverseArc_isHamiltonianCycleOfLengthTwo() {
        vertices(a, b);
        und(a, b); arc(b, a);
        assertHamiltonianCycle(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void undirectedEdgePlusSameWayArc_cycleNeedsArcFirst() {
        // Going out on {a,b} leaves no way back; going out on (a,b) and back on {a,b} works.
        vertices(a, b);
        und(a, b); arc(a, b);
        assertHamiltonianCycle(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void directedPath_hamiltonianPathFromSource_noCycle() {
        vertices(c, b, a);
        arc(a, b); arc(b, c);
        Walk w = Traversals.hamiltonianPath(vList, eList);
        assertHamiltonianPath(w);
        assertSame(a, w.start());
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void arcsOutOfMiddle_noHamiltonianPath() {
        // Ignoring direction this is the path a-b-c, but from b you can't reach both.
        vertices(a, b, c);
        arc(b, a); arc(b, c);
        assertNull(Traversals.hamiltonianPath(vList, eList));
    }

    @Test
    public void undirectedSquare_hasHamiltonianCycle() {
        vertices(a, b, c, d);
        und(a, b); und(b, c); und(c, d); und(d, a);
        assertHamiltonianCycle(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void star_noHamiltonianPath() {
        vertices(d, a, b, c);
        und(d, a); und(d, b); und(d, c);
        assertNull(Traversals.hamiltonianPath(vList, eList));
    }

    @Test
    public void overVertexCap_isTooLarge_andReturnsNull() {
        Vertex prev = null;
        for (int i = 0; i < 21; i++) {
            Vertex v = new Vertex("v" + i, 0, 0);
            vList.add(v);
            if (prev != null) und(prev, v);
            prev = v;
        }
        assertTrue(Traversals.hamiltonTooLarge(vList));
        assertNull(Traversals.hamiltonianPath(vList, eList));
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run the build-and-test command.
Expected: compilation fails with `cannot find symbol ... hamiltonianPath`.

- [ ] **Step 3: Implement the Hamiltonian searches**

In `src/graphtheory/Traversals.java`, insert this block directly above the `/** True if every edge lies in one connected piece (direction ignored). */` comment:

```java
    // ---------- Hamiltonian ----------

    public static boolean hamiltonTooLarge(Vector<Vertex> vList) {
        return vList.size() > HAMILTON_VERTEX_CAP;
    }

    /** A path visiting every vertex, or null. */
    public static Walk hamiltonianPath(Vector<Vertex> vList, Vector<Edge> eList) {
        if (vList.isEmpty() || hamiltonTooLarge(vList)) return null;
        for (Vertex s : vList) {
            Walk w = new Walk(s);
            if (extendHamiltonian(w, vList.size(), eList, false)) return w;
        }
        return null;
    }

    /** A cycle visiting every vertex, or null. A cycle passes every vertex, so starting at the first is enough. */
    public static Walk hamiltonianCycle(Vector<Vertex> vList, Vector<Edge> eList) {
        if (vList.isEmpty() || hamiltonTooLarge(vList)) return null;
        Walk w = new Walk(vList.firstElement());
        return extendHamiltonian(w, vList.size(), eList, true) ? w : null;
    }

    /**
     * Backtracking over edges, not neighbour sets: which edge is used matters
     * for closing a cycle (e.g. {a,b} + (a,b) only closes if (a,b) goes first).
     */
    private static boolean extendHamiltonian(Walk w, int n, Vector<Edge> eList, boolean closed) {
        Vertex at = w.end();
        if (w.vertices().size() == n) {
            if (!closed) return true;
            for (Edge e : eList) {
                if (Walk.canTraverse(e, at) && Walk.otherEnd(e, at) == w.start() && !w.uses(e)) {
                    w.extend(e);
                    return true;
                }
            }
            return false;
        }
        for (Edge e : eList) {
            if (!Walk.canTraverse(e, at) || w.visits(Walk.otherEnd(e, at))) continue;
            w.extend(e);
            if (extendHamiltonian(w, n, eList, closed)) return true;
            w.undo();
        }
        return false;
    }

```

- [ ] **Step 4: Run the tests to verify they pass**

Run the build-and-test command.
Expected: `OK (82 tests)`, i.e. 71 + 11.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Traversals.java test/graphtheory/TraversalsTest.java
git commit -m "feat: direction-aware Hamiltonian path and cycle search returning walks

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 2b: Bitmask DP Hamiltonian search (replaces the backtracking)

> **Added after review.** Task 2's backtracking was correct: fuzzed against brute force on 200,000 graphs with 0 mismatches. But it explodes well under the 20-vertex cap. K11 + 1 isolated vertex took 73 s for a path, and K7,8 took over 90 s. ADR 0003 is updated.

**Files:** Modify `src/graphtheory/Traversals.java` and `test/graphtheory/TraversalsTest.java`.

**Design:**
- Keep the public API exactly as it is: `hamiltonTooLarge`, `hamiltonianPath` and `hamiltonianCycle` return a `Walk` or null. Add javadoc to `hamiltonTooLarge` saying the searches return null above the cap.
- n = 0 → null. **n = 1 and n = 2 keep the current edge-aware logic.** It's simplest to keep `extendHamiltonian` for n ≤ 2 only, which is trivially fast. This covers the self-loop 1-cycle, and the 2-cycle that needs two different edges, e.g. {a,b}+(a,b) has to go out on the arc.
- n ≥ 3: index the vertices in `vList` 0..n-1. Ignore edge endpoints not in `vList` and self-loops. `out[i]` = bitmask of the vertices j ≠ i reachable from i in one step (`Walk.canTraverse`).
  - `reach[mask]` = bitmask of the vertices v such that some path visits exactly the set `mask` and ends at v.
  - **Path:** seed `reach[1<<i] = 1<<i` for every i. Iterate the masks in increasing order. For each v in `reach[mask]` and each w in `out[v] & ~mask`, set bit w in `reach[mask | 1<<w]`. If `reach[full] != 0`, pick the lowest end v. Rebuild the vertices backwards: the previous vertex is any u in `reach[mask ^ (1<<v)]` with bit v in `out[u]`.
  - **Cycle:** same, but seed only vertex 0. A cycle can be rotated to start at any vertex, so vertex 0 is enough. Success means some v in `reach[full]` with bit 0 in `out[v]`.
  - **Turn the vertex sequence into a Walk** by choosing, for each step, the first edge from `Walk.edgesBetween(from, to, eList)`. For n ≥ 3 a path or cycle never reuses an edge, so any choice works. Close the cycle the same way.
  - Memory: `int[1 << n]`, 4 MB at n = 20. Fine.
- Fixes the review's Minor 3 for free: vertices not in `vList` are never stepped onto.

**Tests** (add; keep all existing ones):

```java
    private void complete(Vertex[] k) {
        for (int i = 0; i < k.length; i++)
            for (int j = i + 1; j < k.length; j++) und(k[i], k[j]);
    }

    @Test(timeout = 2000)
    public void k11PlusIsolatedVertex_noHamiltonianPathOrCycle_fast() {
        Vertex[] k = new Vertex[11];
        for (int i = 0; i < 11; i++) { k[i] = new Vertex("k" + i, 0, 0); vList.add(k[i]); }
        complete(k);
        vList.add(new Vertex("lonely", 0, 0));
        assertNull(Traversals.hamiltonianPath(vList, eList));
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test(timeout = 2000)
    public void k7_8_hasHamiltonianPath_noCycle_fast() {
        Vertex[] left = new Vertex[7], right = new Vertex[8];
        for (int i = 0; i < 7; i++) { left[i] = new Vertex("l" + i, 0, 0); vList.add(left[i]); }
        for (int i = 0; i < 8; i++) { right[i] = new Vertex("r" + i, 0, 0); vList.add(right[i]); }
        for (Vertex l : left) for (Vertex r : right) und(l, r);
        assertHamiltonianPath(Traversals.hamiltonianPath(vList, eList));
        assertNull(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test(timeout = 2000)
    public void k20_hasHamiltonianCycle_atCap_fast() {
        Vertex[] k = new Vertex[20];
        for (int i = 0; i < 20; i++) { k[i] = new Vertex("k" + i, 0, 0); vList.add(k[i]); }
        complete(k);
        assertHamiltonianCycle(Traversals.hamiltonianCycle(vList, eList));
    }

    @Test
    public void directedCycleOfFour_followsArcs() {
        vertices(a, b, c, d);
        arc(a, b); arc(b, c); arc(c, d); arc(d, a); arc(a, c);
        Walk w = Traversals.hamiltonianCycle(vList, eList);
        assertHamiltonianCycle(w);
    }
```

Also add a `for (Vertex v : vList) assertTrue(w.visits(v));` loop to `assertHamiltonianPath`.

**Verification beyond JUnit:** run the reviewer's brute-force harness `HamFuzz.java` (scratchpad `graphtheory\` folder, the same folder as Task 1b's Fuzz.java) against the new code for at least 200,000 graphs; mismatches must be 0. Run `HamPerf.java` on the cases from the review. Use an in-process timeout. **Never run `taskkill` on java.exe**, because it kills the user's other Java programs. Don't commit the harness.

**Expected:** `OK (88 tests)`, i.e. 84 + 4.

**Commit:**

```bash
git add src/graphtheory/Traversals.java test/graphtheory/TraversalsTest.java
git commit -m "perf: bitmask DP Hamiltonian search, milliseconds up to the 20-vertex cap

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 3: Graph Summary uses `Traversals`

**Files:**
- Modify: `src/graphtheory/GraphProperties.java` (lines ~223-293 removed; `drawGraphSummary` ~301-350)

There are no drawing tests in this project; verify by compiling, running the tests and doing the manual check in Task 5.

- [ ] **Step 1: Delete the old direction-blind checks**

In `GraphProperties.java`, delete these whole blocks. Nothing else calls them; confirm with the grep in Step 4.
- The `// ---- Connectivity (uses all edge types) ----` section: `isConnected` and `dfsConnected`.
- The `// ---- Euler conditions ----` section: `hasEulerCircuit` and `hasEulerPath`.
- The `// ---- Hamiltonian path / cycle (backtracking, capped at 20 vertices) ----` section: `hasHamiltonianPath`, `hamiltonianPathDFS`, `hasHamiltonianCycle` and `hamiltonianCycleDFS`.

Keep `getAllNeighborIndices`. The bridge code still uses it.

- [ ] **Step 2: Compute the summary values with `Traversals`**

In `drawGraphSummary`, replace:

```java
    boolean eulerCircuit = hasEulerCircuit(vList);
    boolean eulerPath    = hasEulerPath(vList);
    boolean hamPath      = vList.size() <= 20 && hasHamiltonianPath(vList);
    boolean hamCycle     = vList.size() <= 20 && hasHamiltonianCycle(vList);
    boolean tooLarge     = vList.size() > 20;
```

with:

```java
    ensureTraversalSummary(vList, eList);
    String eulerTrail = cachedEulerTrail;
    String eulerTour  = cachedEulerTour;
    String hamPath    = cachedHamPath;
    String hamCycle   = cachedHamCycle;
```

Then add the cache directly **above** `drawGraphSummary` (above its javadoc). The Hamiltonian search is exponential, and the Properties window repaints often, so the answers are computed once per graph change. The computation is lazy, so editing in the Graph window never pays for it.

```java
    // ---- Euler / Hamiltonian answers for the summary, cached per graph change ----
    // Same searches as the Find menu items, so the two never disagree (ADR 0003).

    private boolean traversalSummaryValid = false;
    private String cachedEulerTrail, cachedEulerTour, cachedHamPath, cachedHamCycle;

    /** Canvas calls this whenever vertices or edges are added or removed. */
    public void invalidateTraversalSummary() {
        traversalSummaryValid = false;
    }

    private void ensureTraversalSummary(Vector<Vertex> vList, Vector<Edge> eList) {
        if (traversalSummaryValid) return;
        cachedEulerTrail = yesNo(Traversals.eulerTrail(vList, eList) != null);
        cachedEulerTour  = yesNo(Traversals.eulerTour(vList, eList) != null);
        if (Traversals.hamiltonTooLarge(vList)) {
            cachedHamPath = cachedHamCycle = "> " + Traversals.HAMILTON_VERTEX_CAP + " vertices";
        } else {
            cachedHamPath  = yesNo(Traversals.hamiltonianPath(vList, eList) != null);
            cachedHamCycle = yesNo(Traversals.hamiltonianCycle(vList, eList) != null);
        }
        traversalSummaryValid = true;
    }
```

Finally, in `src/graphtheory/Canvas.java`, make `markGraphDirty()` invalidate the cache. Every vertex/edge add or remove, Remove All, and file load already goes through it:

```java
    private void markGraphDirty() {
        graphDirty = true;
        gP.invalidateTraversalSummary();
        refreshPairPaths();
    }
```

(so `git add` in Step 6 must include `src/graphtheory/Canvas.java` too).

- [ ] **Step 3: Rename the summary lines**

In the `lines` array of `drawGraphSummary`, replace:

```java
        "Euler Circuit: " + (eulerCircuit ? "Yes" : "No"),
        "Euler Path (Trail): " + (eulerPath ? "Yes" : "No"),
        "Hamiltonian Path: " + (tooLarge ? ">20 vertices" : (hamPath ? "Yes" : "No")),
        "Hamiltonian Cycle: " + (tooLarge ? ">20 vertices" : (hamCycle ? "Yes" : "No")),
```

with:

```java
        "Euler Trail: " + eulerTrail,
        "Euler Tour: " + eulerTour,
        "Hamiltonian Path: " + hamPath,
        "Hamiltonian Cycle: " + hamCycle,
```

Then add this helper directly after the closing `}` of `drawGraphSummary`:

```java
    private static String yesNo(boolean b) {
        return b ? "Yes" : "No";
    }
```

- [ ] **Step 4: Check nothing references the deleted methods**

Use the Grep tool (or `grep -rn`) for `hasEuler|hasHamilton|hamiltonianPathDFS|hamiltonianCycleDFS|isConnected\(` in `src` and `test`.
Expected: no matches.

- [ ] **Step 5: Compile and run the tests**

Run the build-and-test command.
Expected: `OK (88 tests)`.

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/GraphProperties.java src/graphtheory/Canvas.java
git commit -m "fix: graph summary Euler/Hamiltonian lines respect arc direction; rename to Euler Trail/Tour

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 4: "Find …" menu items

**Files:**
- Modify: `src/graphtheory/Canvas.java`: menu construction (~line 125, after "Mark as Root"); `MenuListener.actionPerformed` (~line 621); new method next to `handleWalkClick` (~line 310)

- [ ] **Step 1: Add the four menu items to the Extras menu**

In the constructor, find:

```java
        item = new JMenuItem("Mark as Root");
        item.addActionListener(new MenuListener());
        menuOptions2.add(item);
```

and add directly after it:

```java
        menuOptions2.addSeparator();
        for (String find : new String[] {"Find Euler Trail", "Find Euler Tour",
                                         "Find Hamiltonian Path", "Find Hamiltonian Cycle"}) {
            item = new JMenuItem(find);
            item.addActionListener(new MenuListener());
            menuOptions2.add(item);
        }
```

- [ ] **Step 2: Route the commands**

In `MenuListener.actionPerformed`, find:

```java
            } else if (command.equals("Mark as Root")) {
```

and insert directly before it:

```java
            } else if (command.startsWith("Find ")) {
                findTraversal(command.substring("Find ".length()));
```

The listener already clears `walkMessage` at the top and calls `refresh()` at the end, so nothing else is needed there.

- [ ] **Step 3: Add `findTraversal`**

Add this method directly after `handleWalkClick`:

```java
    /**
     * Extras > Find …: switches to the Build Walk tool on the Graph window and
     * loads the walk found, or says none exists. kind is e.g. "Euler Tour".
     */
    private void findTraversal(String kind) {
        selectedTool = 7;
        selectedWindow = 0;
        clearWalk();
        if (kind.startsWith("Hamiltonian") && Traversals.hamiltonTooLarge(vertexList)) {
            walkMessage = "Too large to search (> " + Traversals.HAMILTON_VERTEX_CAP + " vertices)";
            return;
        }
        if (kind.equals("Euler Trail")) {
            currentWalk = Traversals.eulerTrail(vertexList, edgeList);
        } else if (kind.equals("Euler Tour")) {
            currentWalk = Traversals.eulerTour(vertexList, edgeList);
        } else if (kind.equals("Hamiltonian Path")) {
            currentWalk = Traversals.hamiltonianPath(vertexList, edgeList);
        } else {
            currentWalk = Traversals.hamiltonianCycle(vertexList, edgeList);
        }
        if (currentWalk == null) walkMessage = "No " + kind + " exists";
    }
```

- [ ] **Step 4: Compile and run the tests**

Run the build-and-test command.
Expected: `OK (88 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "feat: Find Euler/Hamiltonian menu items load the walk into Build Walk

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 5: Manual verification

Launch the app (see *How to build and test*). The GUI can't be unit tested, so check each of these:

- [ ] **Undirected triangle a, b, c:** *Extras > Find Euler Tour* switches to the Graph window. Three teal edges labelled `#1`–`#3`; walk box says Length 3, Circuit yes. *Window > Properties*: Summary shows `Euler Trail: Yes`, `Euler Tour: Yes`, `Hamiltonian Cycle: Yes`.
- [ ] **Add an arc a → b to the triangle:** *Find Euler Tour* shows "No Euler Tour exists" in red. *Find Euler Trail* shows a 4-edge walk starting at a or b, Trail yes, Circuit no. Summary: `Euler Trail: Yes`, `Euler Tour: No`.
- [ ] **Arcs only, a → b and c → b:** *Find Euler Trail* says "No Euler Trail exists" (the old summary wrongly said Yes). *Find Hamiltonian Path* finds nothing either.
- [ ] **Single vertex with a self-loop:** *Find Hamiltonian Cycle* gives a walk of length 1, Cycle yes.
- [ ] **Two vertices with {a,b} and arc (b,a):** *Find Hamiltonian Cycle* gives a walk of length 2, Cycle yes.
- [ ] **Edgeless graph (two vertices, no edges):** *Find Euler Trail* shows the trivial walk at the first vertex (Length 0). *Find Euler Tour* says "No Euler Tour exists".
- [ ] **Found walk is editable:** after any Find, Backspace removes the last step and Esc clears it, as with a hand-built walk.
- [ ] **Caps:** with 21 vertices, *Find Hamiltonian Path* says "Too large to search (> 20 vertices)" and the Summary shows `> 20 vertices`.
