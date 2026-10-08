# Properties Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hand-painted Properties tab with real Swing components (Overview / Vertices / Matrices / Distributions sub-tabs), make every number on it match CONTEXT.md, and add saved preference lists so stable matching means something.

**Architecture:** Pure, tested classes compute everything from the vertex and edge lists — never from the vertices' neighbour vectors — (`GraphMatrices`, `Connectivity`, `Blocks`, `Structure`, `Colouring`, `Matchings`, `PreferenceLists`), and one immutable `PropertiesReport` gathers them. `SummaryText` and `PanelText` turn results into words. Thin Swing views (`PropertiesPanel`, `OverviewView`, `MatricesView`, `DegreeChartsView`, `PreferencesDialog`) only display a report. `Canvas` rebuilds the report only when the graph text (`GraphFile.write`, which now includes preference lists) or the proposing side changes. This is the `PairSummary` + `PanelText` + `SidePanel` pattern from the side-panel branch.

**Tech Stack:** Java 8+ Swing, JUnit 4.13.2, no build tool (javac from Git Bash).

**Spec:** CONTEXT.md (the glossary is the spec — every term on the tab is defined there), `docs/adr/0005-preference-lists-in-graph-files.md`, `docs/superpowers/plans/PROPERTIES-TAB-HANDOFF.md` (background), and the decisions recorded at the end of this plan.

---

## Build and test (Git Bash, repo root)

```bash
CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
TESTS="graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest \
 graphtheory.EdgeRegistryTest graphtheory.VertexNamesTest graphtheory.GraphFileTest graphtheory.LayoutTest \
 graphtheory.EditHistoryTest graphtheory.ToolsTest graphtheory.FileManagerTest graphtheory.ComponentsTest \
 graphtheory.EdgeShapesTest graphtheory.EdgeTest graphtheory.GraphRendererTest graphtheory.PairSummaryTest \
 graphtheory.PanelTextTest graphtheory.SidePanelTest graphtheory.GraphShapeTest graphtheory.DegreeDistributionTest"
rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -Djava.awt.headless=true -cp "out/test;$CP" org.junit.runner.JUnitCore $TESTS
MSYS_NO_PATHCONV=1 timeout 8 java -cp out/test graphtheory.Main; echo "exit $?"   # 124 + clean stderr = starts fine
```

`MSYS_NO_PATHCONV=1` is required. **Every task that adds a test class appends it to `TESTS`** (the list grows: `PreferenceListsTest`, `GraphMatricesTest`, `ConnectivityTest`, `BlocksTest`, `StructureTest`, `ColouringTest`, `MatchingsTest`, `PropertiesReportTest`, `SummaryTextTest`, `TableCopyTest`, `PreferenceEditorTest`, `PropertiesPanelTest`). To run one class, put just that class name where `$TESTS` is.

**Line endings:** older files are CRLF in the working copy. Use the Edit tool, not `sed -i`; after each task check `git diff --stat` shows only the lines you meant to change.

**Test helpers used throughout** (copy into each new test class that needs them):

```java
    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static Edge w(Edge e, int weight) { e.setWeight(weight); return e; }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }
```

(imports: `java.util.Arrays`, `java.util.List`, `java.util.Vector`, `org.junit.Test`, `static org.junit.Assert.*`). The new pure classes read only `Edge.vertex1/vertex2/directed/weight` and the vertex list, so tests never need to fill in neighbour vectors.

---

## File map

| File | Status | Responsibility |
|---|---|---|
| `src/graphtheory/Vertex.java` | modify | add `preferences` (null = no list) |
| `src/graphtheory/PreferenceLists.java` | create | neighbours for ranking; keep lists in step with edges; who lacks a list |
| `src/graphtheory/GraphFile.java` | modify | `prefer` lines, version 2 header (ADR 0005) |
| `src/graphtheory/GraphMatrices.java` | create | adjacency counts, distance (BFS), weighted distance (Dijkstra) |
| `src/graphtheory/Connectivity.java` | create | components, strong connectivity, κ/λ with minimum cuts by max-flow |
| `src/graphtheory/Blocks.java` | create | edge-aware bridges, blocks, nonseparable |
| `src/graphtheory/Structure.java` | create | simple, empty, complete, density, cyclic, forest/tree, star, bipartite |
| `src/graphtheory/Colouring.java` | create | chromatic number (self-loop aware) |
| `src/graphtheory/Matchings.java` | create | maximal, maximum, stable |
| `src/graphtheory/PropertiesReport.java` | create | everything the tab shows, computed once |
| `src/graphtheory/SummaryText.java` | create | the Overview summary's sections and lines, and its HTML |
| `src/graphtheory/PanelText.java` | modify | Vertices table columns, rows and the Neighbours notation |
| `src/graphtheory/TableCopy.java` | create | Ctrl+C as tab-separated values with headers |
| `src/graphtheory/PreferenceEditor.java` | create | working copy behind the preferences dialog |
| `src/graphtheory/PreferencesDialog.java` | create | the dialog |
| `src/graphtheory/OverviewView.java` | create | picture + summary + stable-matching controls |
| `src/graphtheory/MatricesView.java` | create | matrix selector, frozen headers |
| `src/graphtheory/DegreeChartsView.java` | create | distribution charts with tooltips |
| `src/graphtheory/PropertiesPanel.java` | create | the four sub-tabs; `display(report)` |
| `src/graphtheory/GraphProperties.java` | modify | static chart drawing + tooltip; delete everything the report replaced |
| `src/graphtheory/Edge.java` | modify | delete `blockId` |
| `src/graphtheory/Canvas.java` | modify | wiring, menu, keys, focus, remove old painting and `reloadVertexConnections` |
| `src/graphtheory/SidePanel.java` | modify | comment about Ctrl+C |
| `TODO.md` | modify | tick the items this lands |

---

### Task 0: Branch and docs

**Files:** `CONTEXT.md`, `docs/adr/0005-preference-lists-in-graph-files.md`, this plan (already written in the grilling session).

- [ ] **Step 1: Branch from fresh master**

```bash
git checkout master && git pull && git checkout -b feature/properties-tab
```

If `git pull` brought in changes to `CONTEXT.md`, merge them by hand, keeping both.

- [ ] **Step 2: Commit the docs**

```bash
git add CONTEXT.md docs/adr/0005-preference-lists-in-graph-files.md docs/superpowers/plans/2026-10-08-properties-tab.md
git commit -m "docs: glossary, ADR 0005 and plan for the Properties tab rebuild"
```

---

### Task 1: Stop Properties corrupting neighbour lists

`Canvas.computeProperties` calls `reloadVertexConnections`, which clears every vertex's `undirectedNeighbors` and refills it from the 0/1 adjacency matrix. Opening Properties therefore collapses parallel edges and turns each arc a→b into an undirected neighbour of a (degree goes up). It's private Canvas code, so the regression check is the probe in Task 18; this task just deletes it.

**Files:** Modify `src/graphtheory/Canvas.java` (`computeProperties` ~line 490, `reloadVertexConnections` ~line 1309)

- [ ] **Step 1: Delete the call and the method**

In `computeProperties`, change

```java
            int[][] matrix = gP.generateAdjacencyMatrix(vertexList, edgeList);
```
to
```java
            gP.generateAdjacencyMatrix(vertexList, edgeList);
```
and delete the line `reloadVertexConnections(matrix, vertexList);`. Delete the whole `private void reloadVertexConnections(int[][] aMatrix, Vector<Vertex> vList) { ... }` method.

- [ ] **Step 2: Build and run all tests** — all pass (247).

- [ ] **Step 3: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "fix: opening Properties no longer rewrites vertices' neighbour lists"
```

---

### Task 2: Preference lists on vertices

**Files:**
- Modify: `src/graphtheory/Vertex.java`
- Create: `src/graphtheory/PreferenceLists.java`
- Test: `test/graphtheory/PreferenceListsTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreferenceListsTest {

    private static Vertex v(String name) { return new Vertex(name, 0, 0); }
    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }
    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }
    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }
    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void neighbours_ignoreDirectionAndSelfAndRepeats_inFirstEdgeOrder() {
        List<Edge> edges = es(arc(c, a), und(a, b), und(a, b), und(a, a), arc(a, c));
        assertEquals(Arrays.asList(c, b), PreferenceLists.neighboursOf(a, edges));
    }

    @Test
    public void sync_newNeighbourJoinsTheEnd() {
        a.preferences = new Vector<Vertex>(Arrays.asList(c, b));
        PreferenceLists.sync(vs(a, b, c, d), es(und(a, b), und(a, c), und(a, d)));
        assertEquals(Arrays.asList(c, b, d), a.preferences);
    }

    @Test
    public void sync_lostNeighbourLeaves_andNoListStaysNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(c, b));
        PreferenceLists.sync(vs(a, b, c), es(und(a, b)));
        assertEquals(Arrays.asList(b), a.preferences);
        assertNull(b.preferences);
    }

    @Test
    public void sync_noNeighboursLeft_meansNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        PreferenceLists.sync(vs(a, b), es());
        assertNull(a.preferences);
    }

    @Test
    public void missing_listsVerticesWithNeighboursButNoList() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        assertEquals(Arrays.asList(b), PreferenceLists.missing(vs(a, b, d), es(und(a, b))));
    }

    @Test
    public void anyLists() {
        assertFalse(PreferenceLists.any(vs(a, b)));
        b.preferences = new Vector<Vertex>(Arrays.asList(a));
        assertTrue(PreferenceLists.any(vs(a, b)));
    }
}
```

- [ ] **Step 2: Run, expect compile failure** (`preferences`, `PreferenceLists` undefined).

- [ ] **Step 3: Implement**

In `Vertex.java`, after `public boolean isRoot;` add:

```java
    /**
     * This vertex's preference list (CONTEXT.md): its neighbours, most preferred first.
     * null = no list. PreferenceLists.sync keeps it naming exactly the neighbours.
     */
    public Vector<Vertex> preferences = null;
```

Create `src/graphtheory/PreferenceLists.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/** Preference lists (CONTEXT.md): each names exactly its vertex's neighbours other than itself. */
public final class PreferenceLists {

    private PreferenceLists() {}

    /** v's neighbours other than itself, ignoring direction, each once, in the order of their first edge. */
    public static List<Vertex> neighboursOf(Vertex v, List<Edge> edges) {
        List<Vertex> out = new ArrayList<Vertex>();
        for (Edge e : edges) {
            Vertex other = e.vertex1 == v ? e.vertex2 : e.vertex2 == v ? e.vertex1 : null;
            if (other != null && other != v && !out.contains(other)) out.add(other);
        }
        return out;
    }

    /**
     * Brings every list in step with the edges: non-neighbours leave, new neighbours join the
     * end, and a list with no neighbours left becomes no list. Idempotent.
     */
    public static void sync(List<Vertex> vertices, List<Edge> edges) {
        for (Vertex v : vertices) {
            if (v.preferences == null) continue;
            List<Vertex> nbrs = neighboursOf(v, edges);
            Vector<Vertex> kept = new Vector<Vertex>();
            for (Vertex p : v.preferences) if (nbrs.contains(p) && !kept.contains(p)) kept.add(p);
            for (Vertex n : nbrs) if (!kept.contains(n)) kept.add(n);
            v.preferences = kept.isEmpty() ? null : kept;
        }
    }

    /** Vertices that have a neighbour but no preference list, in vertex order. */
    public static List<Vertex> missing(List<Vertex> vertices, List<Edge> edges) {
        List<Vertex> out = new ArrayList<Vertex>();
        for (Vertex v : vertices) {
            if (v.preferences == null && !neighboursOf(v, edges).isEmpty()) out.add(v);
        }
        return out;
    }

    public static boolean any(List<Vertex> vertices) {
        for (Vertex v : vertices) if (v.preferences != null) return true;
        return false;
    }
}
```

- [ ] **Step 4: Add `graphtheory.PreferenceListsTest` to `TESTS`, run all** — pass.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/Vertex.java src/graphtheory/PreferenceLists.java test/graphtheory/PreferenceListsTest.java
git commit -m "feat: preference lists on vertices, kept in step with edges"
```

---

### Task 3: `prefer` lines in graph files (ADR 0005)

**Files:**
- Modify: `src/graphtheory/GraphFile.java`
- Test: `test/graphtheory/GraphFileTest.java` (line ~157 asserts the old version message)

- [ ] **Step 1: Write the failing tests** (append to `GraphFileTest`; it already has `file(...)`, `read(...)` and `assertRejected(message, lines...)`)

```java
    @Test
    public void write_withPreferences_isVersion2_withPreferLinesLast() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 10, 0), c = new Vertex("c", 20, 0);
        Vector<Vertex> vs = new Vector<Vertex>(java.util.Arrays.asList(a, b, c));
        Vector<Edge> es = new Vector<Edge>(java.util.Arrays.asList(new Edge(a, b, false), new Edge(c, a, true)));
        a.preferences = new Vector<Vertex>(java.util.Arrays.asList(c, b));
        assertEquals(file("graph-theory 2",
                          "vertex a 0 0", "vertex b 10 0", "vertex c 20 0",
                          "edge a b 1", "arc c a 1",
                          "prefer a c b"),
                     GraphFile.write(vs, es));
    }

    @Test
    public void roundTrip_preferences() throws Exception {
        String text = file("graph-theory 2",
                           "vertex a 0 0", "vertex b 10 0", "vertex c 20 0",
                           "edge a b 1", "edge a c 1",
                           "prefer a c b", "prefer b a");
        GraphFile.Data d = GraphFile.read(text);
        assertEquals(text, GraphFile.write(d.vertices, d.edges));
        assertNull(d.vertices.get(2).preferences);
    }

    @Test
    public void read_version1_stillReads() throws Exception {
        assertEquals(1, read("graph-theory 1", "vertex a").vertices.size());
    }

    @Test
    public void read_preferInVersion1_rejected() {
        assertRejected("Line 4: prefer lines need the header 'graph-theory 2'",
                "graph-theory 1", "vertex a", "vertex b", "prefer a b");
    }

    @Test
    public void read_preferBeforeAnEdge_rejected() {
        assertRejected("Line 5: vertex, edge and arc lines must come before prefer lines",
                "graph-theory 2", "vertex a", "vertex b", "prefer a b", "edge a b");
    }

    @Test
    public void read_preferMustNameExactlyTheNeighbours() {
        assertRejected("Line 7: 'a' must rank exactly its neighbours: b, c",
                "graph-theory 2", "vertex a", "vertex b", "vertex c", "edge a b", "edge c a", "prefer a b");
        assertRejected("Line 5: 'a' must rank exactly its neighbours: b",
                "graph-theory 2", "vertex a", "vertex b", "edge a b", "prefer a b b");
    }

    @Test
    public void read_secondPreferForSameVertex_rejected() {
        assertRejected("Line 6: 'a' already has a prefer line",
                "graph-theory 2", "vertex a", "vertex b", "edge a b", "prefer a b", "prefer a b");
    }

    @Test
    public void read_preferWithNoNames_rejected() {
        assertRejected("Line 4: expected 'prefer <vertex> <neighbour> ...'",
                "graph-theory 2", "vertex a", "vertex b", "prefer a");
    }
```

Also change the existing version test at line ~157 to:

```java
        assertRejected("Line 1: unsupported version (this app reads 'graph-theory 1' and 'graph-theory 2')", "graph-theory 3");
```

- [ ] **Step 2: Run `GraphFileTest`, expect failures.**

- [ ] **Step 3: Implement** in `GraphFile.java`:

Update the class comment's format block to add `graph-theory 2` and `prefer <vertex> <neighbour> ...   (version 2; after all vertex/edge/arc lines)`.

Replace the `HEADER` constant with:

```java
    public static final String HEADER = "graph-theory 1";
    /** Written instead of HEADER when some vertex has a preference list (ADR 0005). */
    public static final String HEADER_V2 = "graph-theory 2";
```

In `write`, change the first line to

```java
        StringBuilder sb = new StringBuilder(PreferenceLists.any(vertices) ? HEADER_V2 : HEADER).append('\n');
```

and before `return sb.toString();` add:

```java
        for (Vertex v : vertices) {
            if (v.preferences == null) continue;
            sb.append("prefer ").append(v.name);
            for (Vertex p : v.preferences) sb.append(' ').append(p.name);
            sb.append('\n');
        }
```

In `read`: add locals after `boolean sawHeader = false;`

```java
        int version = 0;
        boolean sawPrefer = false;
        List<String[]> prefers = new java.util.ArrayList<String[]>();
        List<Integer> preferLines = new java.util.ArrayList<Integer>();
```

Replace the header version check with:

```java
                if (t.length != 2 || !(t[1].equals("1") || t[1].equals("2"))) {
                    throw new FormatException(lineNo,
                            "unsupported version (this app reads '" + HEADER + "' and '" + HEADER_V2 + "')");
                }
                version = Integer.parseInt(t[1]);
                sawHeader = true;
```

Before the `vertex` branch's body and the `edge`/`arc` branch's body, reject them after a prefer line. Change those two branches and add a `prefer` branch:

```java
            } else if (t[0].equals("vertex") || t[0].equals("edge") || t[0].equals("arc")) {
                if (sawPrefer) {
                    throw new FormatException(lineNo, "vertex, edge and arc lines must come before prefer lines");
                }
                if (t[0].equals("vertex")) readVertex(t, lineNo, data, byName, rootLine);
                else readEdge(t, lineNo, data, byName);
            } else if (t[0].equals("prefer")) {
                if (version < 2) throw new FormatException(lineNo, "prefer lines need the header '" + HEADER_V2 + "'");
                if (t.length < 3) throw new FormatException(lineNo, "expected 'prefer <vertex> <neighbour> ...'");
                sawPrefer = true;
                prefers.add(t);
                preferLines.add(lineNo);
            } else {
                throw new FormatException(lineNo,
                        "unknown keyword '" + t[0] + "' (expected vertex, edge, arc or prefer)");
            }
```

In `GraphFileTest` (~line 162) change the expected message to `"Line 2: unknown keyword 'node' (expected vertex, edge, arc or prefer)"`.

After `checkRoots(data, rootLine);` add `readPreferences(prefers, preferLines, data, byName);` and the method:

```java
    /** Each prefer line must rank exactly its vertex's neighbours, once each (CONTEXT.md, Preference list). */
    private static void readPreferences(List<String[]> prefers, List<Integer> lines, Data data,
                                        Map<String, Vertex> byName) throws FormatException {
        for (int i = 0; i < prefers.size(); i++) {
            String[] t = prefers.get(i);
            int lineNo = lines.get(i);
            Vertex v = lookup(t[1], lineNo, byName);
            if (v.preferences != null) throw new FormatException(lineNo, "'" + v.name + "' already has a prefer line");
            Vector<Vertex> list = new Vector<Vertex>();
            for (int k = 2; k < t.length; k++) list.add(lookup(t[k], lineNo, byName));
            List<Vertex> nbrs = PreferenceLists.neighboursOf(v, data.edges);
            boolean exact = list.size() == nbrs.size() && new java.util.HashSet<Vertex>(list).size() == list.size()
                    && list.containsAll(nbrs);
            if (!exact) {
                StringBuilder names = new StringBuilder();
                for (Vertex n : nbrs) names.append(names.length() > 0 ? ", " : "").append(n.name);
                throw new FormatException(lineNo, "'" + v.name + "' must rank exactly its neighbours: " + names);
            }
            v.preferences = list;
        }
    }
```

- [ ] **Step 4: Run all tests** — pass.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/GraphFile.java test/graphtheory/GraphFileTest.java
git commit -m "feat(file): prefer lines and the version 2 header (ADR 0005)"
```

---

### Task 4: Adjacency and distance matrices

**Files:**
- Create: `src/graphtheory/GraphMatrices.java`
- Test: `test/graphtheory/GraphMatricesTest.java`

- [ ] **Step 1: Write the failing tests** (with the helpers above)

```java
public class GraphMatricesTest {
    // helpers v, und, arc, w, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void adjacency_countsEdges_rowToColumn() {
        int[][] m = GraphMatrices.adjacency(vs(a, b, c),
                es(und(a, b), und(a, b), arc(b, c), und(a, a), arc(c, c)));
        assertArrayEquals(new int[] {2, 2, 0}, m[0]);   // undirected loop counts 2
        assertArrayEquals(new int[] {2, 0, 1}, m[1]);
        assertArrayEquals(new int[] {0, 0, 1}, m[2]);   // directed loop counts 1
    }

    @Test
    public void adjacency_rowSumIsDegreePlusOutDegree() {
        // a: two undirected to b, loop (2) -> degree 4; arc a->c -> out 1
        int[][] m = GraphMatrices.adjacency(vs(a, b, c), es(und(a, b), und(a, b), und(a, a), arc(a, c)));
        assertEquals(5, m[0][0] + m[0][1] + m[0][2]);
    }

    @Test
    public void distances_countEdges_respectDirection() {
        int[][] d = GraphMatrices.distances(vs(a, b, c), es(w(arc(a, b), 9), und(b, c)));
        assertArrayEquals(new int[] {0, 1, 2}, d[0]);
        assertArrayEquals(new int[] {GraphMatrices.UNREACHABLE, 0, 1}, d[1]);
    }

    @Test
    public void weightedDistances_sumWeights() {
        int[][] d = GraphMatrices.weightedDistances(vs(a, b, c),
                es(w(und(a, b), 5), w(und(a, c), 1), w(und(c, b), 1)));
        assertEquals(2, d[0][1]);
        assertEquals(0, d[1][1]);
    }

    @Test
    public void weightedDistances_unreachable() {
        int[][] d = GraphMatrices.weightedDistances(vs(a, b), es(w(arc(b, a), 3)));
        assertEquals(GraphMatrices.UNREACHABLE, d[0][1]);
        assertEquals(3, d[1][0]);
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/GraphMatrices.java`:

```java
package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Properties tab's matrices (CONTEXT.md: Adjacency matrix, Distance, Weighted distance). */
public final class GraphMatrices {

    /** A distance entry for a pair with no walk between them. */
    public static final int UNREACHABLE = -1;

    private GraphMatrices() {}

    /** Entry (u, v) counts the edges along which you can leave u and arrive at v; an undirected loop counts 2. */
    public static int[][] adjacency(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = index(vs);
        int[][] m = new int[vs.size()][vs.size()];
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            if (e.directed) {
                m[i][j]++;
            } else if (i.equals(j)) {
                m[i][i] += 2;
            } else {
                m[i][j]++;
                m[j][i]++;
            }
        }
        return m;
    }

    /** Entry (u, v) = the fewest edges on a walk from u to v (respecting direction), or UNREACHABLE. */
    public static int[][] distances(List<Vertex> vs, List<Edge> es) {
        List<List<int[]>> steps = steps(vs, es);
        int n = vs.size();
        int[][] d = new int[n][];
        for (int s = 0; s < n; s++) {
            int[] row = new int[n];
            Arrays.fill(row, UNREACHABLE);
            row[s] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int[] st : steps.get(u)) {
                    if (row[st[0]] == UNREACHABLE) {
                        row[st[0]] = row[u] + 1;
                        queue.add(st[0]);
                    }
                }
            }
            d[s] = row;
        }
        return d;
    }

    /** Entry (u, v) = the smallest weight of a walk from u to v (respecting direction), or UNREACHABLE. */
    public static int[][] weightedDistances(List<Vertex> vs, List<Edge> es) {
        List<List<int[]>> steps = steps(vs, es);
        int n = vs.size();
        int[][] d = new int[n][];
        for (int s = 0; s < n; s++) {
            int[] row = new int[n];
            Arrays.fill(row, UNREACHABLE);
            boolean[] done = new boolean[n];
            row[s] = 0;
            for (int round = 0; round < n; round++) {
                int u = -1;
                for (int i = 0; i < n; i++) {
                    if (!done[i] && row[i] != UNREACHABLE && (u < 0 || row[i] < row[u])) u = i;
                }
                if (u < 0) break;
                done[u] = true;
                for (int[] st : steps.get(u)) {
                    int nd = row[u] + st[1];
                    if (row[st[0]] == UNREACHABLE || nd < row[st[0]]) row[st[0]] = nd;
                }
            }
            d[s] = row;
        }
        return d;
    }

    /** For each vertex index, the steps {to, weight} a walk can take from it. */
    private static List<List<int[]>> steps(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = index(vs);
        List<List<int[]>> steps = new ArrayList<List<int[]>>();
        for (int i = 0; i < vs.size(); i++) steps.add(new ArrayList<int[]>());
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == null || j == null) continue;
            steps.get(i).add(new int[] {j, e.weight});
            if (!e.directed) steps.get(j).add(new int[] {i, e.weight});
        }
        return steps;
    }

    static Map<Vertex, Integer> index(List<Vertex> vs) {
        Map<Vertex, Integer> idx = new HashMap<Vertex, Integer>();
        for (int i = 0; i < vs.size(); i++) idx.put(vs.get(i), i);
        return idx;
    }
}
```

- [ ] **Step 4: Add `graphtheory.GraphMatricesTest` to `TESTS`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: adjacency (edge counts), distance and weighted distance matrices`

---

### Task 5: Components, strong connectivity, κ and λ by max-flow

**Files:**
- Create: `src/graphtheory/Connectivity.java`
- Test: `test/graphtheory/ConnectivityTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class ConnectivityTest {
    // helpers v, und, arc, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void components_ignoreDirection_inVertexOrder() {
        List<List<Vertex>> cs = Connectivity.components(vs(a, b, c, d), es(arc(b, a), und(d, d)));
        assertEquals(Arrays.asList(Arrays.asList(a, b), Arrays.asList(c), Arrays.asList(d)), cs);
        assertFalse(Connectivity.isConnected(vs(a, b, c, d), es(arc(b, a))));
        assertFalse(Connectivity.isConnected(vs(), es()));
        assertTrue(Connectivity.isConnected(vs(a), es()));
    }

    @Test
    public void stronglyConnected_respectsDirection_singleVertexIsStrong() {
        assertTrue(Connectivity.isStronglyConnected(vs(a), es()));
        assertFalse(Connectivity.isStronglyConnected(vs(a, b), es(arc(a, b))));
        assertTrue(Connectivity.isStronglyConnected(vs(a, b, c), es(arc(a, b), und(b, c), arc(c, a))));
    }

    @Test
    public void edgeCut_parallelEdgesCountSeparately() {
        Connectivity.Cut<Edge> cut = Connectivity.minimumEdgeCut(vs(a, b), es(und(a, b), und(a, b)));
        assertEquals(2, cut.size);
        assertEquals(2, cut.members.size());
    }

    @Test
    public void edgeCut_pathHasBridge_arcsIgnoreDirection() {
        Edge bc = arc(c, b);
        Connectivity.Cut<Edge> cut = Connectivity.minimumEdgeCut(vs(a, b, c),
                es(und(a, b), und(a, b), bc));
        assertEquals(1, cut.size);
        assertEquals(Arrays.asList(bc), cut.members);
    }

    @Test
    public void edgeCut_disconnectedOrTiny_isZeroAndEmpty() {
        assertEquals(0, Connectivity.minimumEdgeCut(vs(a, b), es()).size);
        assertEquals(0, Connectivity.minimumEdgeCut(vs(a), es()).size);
        assertTrue(Connectivity.minimumEdgeCut(vs(a, b), es()).members.isEmpty());
    }

    @Test
    public void vertexCut_path_isTheMiddle() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c), es(und(a, b), arc(c, b)));
        assertEquals(1, cut.size);
        assertEquals(Arrays.asList(b), cut.members);
    }

    @Test
    public void vertexCut_cycleOfFour_isTwoOppositeVertices() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c, d),
                es(und(a, b), und(b, c), und(c, d), und(d, a)));
        assertEquals(2, cut.size);
        assertEquals(2, cut.members.size());
        assertFalse(cut.members.contains(a) && cut.members.contains(b));   // not adjacent
    }

    @Test
    public void vertexCut_complete_isNMinusOne_withNoCut() {
        Connectivity.Cut<Vertex> cut = Connectivity.minimumVertexCut(vs(a, b, c, d),
                es(und(a, b), und(a, c), und(a, d), arc(b, c), und(b, d), und(c, d), und(c, c)));
        assertEquals(3, cut.size);
        assertTrue(cut.members.isEmpty());
    }

    @Test
    public void vertexCut_disconnected_isZero() {
        assertEquals(0, Connectivity.minimumVertexCut(vs(a, b, c), es(und(a, b))).size);
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/Connectivity.java`:

```java
package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Connected, components, strongly connected, and the minimum vertex and edge cuts
 * (CONTEXT.md, Graph). Everything except strong connectivity ignores direction. The cuts
 * use max-flow (Menger), so they are exact and parallel edges count separately.
 */
public final class Connectivity {

    private Connectivity() {}

    /** A minimum cut: its size (κ or λ) and its members. Members are empty when no cut exists. */
    public static final class Cut<T> {
        public final int size;
        public final List<T> members;

        Cut(int size, List<T> members) {
            this.size = size;
            this.members = Collections.unmodifiableList(members);
        }
    }

    /** Maximal connected sets ignoring direction; each in vertex order, ordered by first vertex. */
    public static List<List<Vertex>> components(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[] parent = new int[vs.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (Edge e : es) {
            Integer i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i != null && j != null) parent[find(parent, i)] = find(parent, j);
        }
        List<List<Vertex>> out = new ArrayList<List<Vertex>>();
        Map<Integer, List<Vertex>> byRoot = new java.util.HashMap<Integer, List<Vertex>>();
        for (int i = 0; i < vs.size(); i++) {
            int r = find(parent, i);
            List<Vertex> comp = byRoot.get(r);
            if (comp == null) {
                comp = new ArrayList<Vertex>();
                byRoot.put(r, comp);
                out.add(comp);
            }
            comp.add(vs.get(i));
        }
        return out;
    }

    static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    public static boolean isConnected(List<Vertex> vs, List<Edge> es) {
        return !vs.isEmpty() && components(vs, es).size() == 1;
    }

    /** Every vertex reaches every other by a walk respecting direction. A single vertex is strong. */
    public static boolean isStronglyConnected(List<Vertex> vs, List<Edge> es) {
        if (vs.isEmpty()) return false;
        return reachesAll(vs, es, true) && reachesAll(vs, es, false);
    }

    private static boolean reachesAll(List<Vertex> vs, List<Edge> es, boolean forward) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[] seen = new boolean[vs.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        seen[0] = true;
        queue.add(0);
        while (!queue.isEmpty()) {
            Vertex u = vs.get(queue.poll());
            for (Edge e : es) {
                Vertex next = null;
                Vertex from = forward ? e.vertex1 : e.vertex2;
                Vertex to = forward ? e.vertex2 : e.vertex1;
                if (from == u) next = to;
                else if (!e.directed && to == u) next = from;
                Integer j = next == null ? null : idx.get(next);
                if (j != null && !seen[j]) {
                    seen[j] = true;
                    queue.add(j);
                }
            }
        }
        for (boolean s : seen) if (!s) return false;
        return true;
    }

    /** λ(G) and a minimum edge cut. 0 and empty for a disconnected graph or fewer than two vertices. */
    public static Cut<Edge> minimumEdgeCut(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isConnected(vs, es)) return new Cut<Edge>(0, new ArrayList<Edge>());
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[][] cap = new int[n][n];
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == j) continue;
            cap[i][j]++;
            cap[j][i]++;
        }
        int best = Integer.MAX_VALUE;
        List<Edge> bestCut = null;
        for (int t = 1; t < n; t++) {
            Flow f = new Flow(cap);
            int flow = f.maxFlow(0, t);
            if (flow < best) {
                best = flow;
                boolean[] side = f.sourceSide(0);
                bestCut = new ArrayList<Edge>();
                for (Edge e : es) {
                    int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
                    if (side[i] != side[j]) bestCut.add(e);
                }
            }
        }
        return new Cut<Edge>(best, bestCut);
    }

    /**
     * κ(G) and a minimum vertex cut. 0 and empty when disconnected or fewer than two vertices;
     * n − 1 and empty when every two vertices are adjacent (no vertex set disconnects the graph).
     */
    public static Cut<Vertex> minimumVertexCut(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isConnected(vs, es)) return new Cut<Vertex>(0, new ArrayList<Vertex>());
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[][] adj = new boolean[n][n];
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i != j) adj[i][j] = adj[j][i] = true;
        }
        int inf = n + 1;
        int best = n - 1;
        List<Vertex> bestCut = new ArrayList<Vertex>();
        for (int s = 0; s < n; s++) {
            for (int t = s + 1; t < n; t++) {
                if (adj[s][t]) continue;
                // Vertex v becomes in-node 2v and out-node 2v+1, joined with capacity 1 (s and t: unlimited).
                int[][] cap = new int[2 * n][2 * n];
                for (int v = 0; v < n; v++) cap[2 * v][2 * v + 1] = (v == s || v == t) ? inf : 1;
                for (int u = 0; u < n; u++) {
                    for (int v = 0; v < n; v++) if (adj[u][v]) cap[2 * u + 1][2 * v] = inf;
                }
                Flow f = new Flow(cap);
                int flow = f.maxFlow(2 * s + 1, 2 * t);
                if (flow < best || (flow == best && bestCut.isEmpty())) {
                    best = flow;
                    boolean[] side = f.sourceSide(2 * s + 1);
                    bestCut = new ArrayList<Vertex>();
                    for (int v = 0; v < n; v++) {
                        if (side[2 * v] && !side[2 * v + 1]) bestCut.add(vs.get(v));
                    }
                }
            }
        }
        return new Cut<Vertex>(best, bestCut);
    }

    /** Edmonds–Karp on a small capacity matrix. */
    private static final class Flow {
        private final int[][] residual;

        Flow(int[][] cap) {
            residual = new int[cap.length][];
            for (int i = 0; i < cap.length; i++) residual[i] = cap[i].clone();
        }

        int maxFlow(int s, int t) {
            int total = 0;
            int n = residual.length;
            while (true) {
                int[] prev = new int[n];
                Arrays.fill(prev, -1);
                prev[s] = s;
                ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
                queue.add(s);
                while (!queue.isEmpty() && prev[t] < 0) {
                    int u = queue.poll();
                    for (int v = 0; v < n; v++) {
                        if (prev[v] < 0 && residual[u][v] > 0) {
                            prev[v] = u;
                            queue.add(v);
                        }
                    }
                }
                if (prev[t] < 0) return total;
                int push = Integer.MAX_VALUE;
                for (int v = t; v != s; v = prev[v]) push = Math.min(push, residual[prev[v]][v]);
                for (int v = t; v != s; v = prev[v]) {
                    residual[prev[v]][v] -= push;
                    residual[v][prev[v]] += push;
                }
                total += push;
            }
        }

        /** After maxFlow: the nodes still reachable from s in the residual graph. */
        boolean[] sourceSide(int s) {
            boolean[] seen = new boolean[residual.length];
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            seen[s] = true;
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v = 0; v < residual.length; v++) {
                    if (!seen[v] && residual[u][v] > 0) {
                        seen[v] = true;
                        queue.add(v);
                    }
                }
            }
            return seen;
        }
    }
}
```

- [ ] **Step 4: Add `graphtheory.ConnectivityTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: exact κ and λ by max-flow; components and strong connectivity from edges`

---

### Task 6: Edge-aware bridges and blocks

**Files:**
- Create: `src/graphtheory/Blocks.java`
- Test: `test/graphtheory/BlocksTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class BlocksTest {
    // helpers v, und, arc, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d"), e = v("e");

    @Test
    public void parallelEdges_areNotBridges() {
        assertTrue(Blocks.bridges(vs(a, b), es(und(a, b), und(a, b))).isEmpty());
    }

    @Test
    public void path_everyEdgeIsABridge_arcsIgnoreDirection() {
        Edge ab = und(a, b), cb = arc(c, b);
        assertEquals(new java.util.HashSet<Edge>(Arrays.asList(ab, cb)),
                Blocks.bridges(vs(a, b, c), es(ab, cb)));
    }

    @Test
    public void selfLoop_isNeverABridge() {
        assertTrue(Blocks.bridges(vs(a), es(und(a, a))).isEmpty());
    }

    @Test
    public void bowtie_twoTriangleBlocks_sharingTheCutpoint() {
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c, d, e),
                es(und(a, b), und(b, c), und(c, a), und(c, d), und(d, e), und(e, c)));
        assertEquals(2, bs.size());
        assertEquals(Arrays.asList(a, b, c), bs.get(0).vertices);
        assertEquals(Arrays.asList(c, d, e), bs.get(1).vertices);
        assertEquals(3, bs.get(0).edges.size());
    }

    @Test
    public void parallelEdges_shareABlock_everyEdgeInExactlyOne() {
        Edge ab1 = und(a, b), ab2 = und(a, b), bc = und(b, c);
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c), es(ab1, ab2, bc));
        assertEquals(2, bs.size());
        int total = 0;
        for (Blocks.Block bl : bs) total += bl.edges.size();
        assertEquals(3, total);
        for (Blocks.Block bl : bs) {
            if (bl.edges.contains(ab1)) assertTrue(bl.edges.contains(ab2));
        }
    }

    @Test
    public void isolatedVertex_isABlockOnItsOwn_loopJoinsItsVertexBlock() {
        Edge loop = und(c, c);
        List<Blocks.Block> bs = Blocks.blocks(vs(a, b, c, d), es(und(a, b), loop));
        assertEquals(3, bs.size());
        assertEquals(Arrays.asList(a, b), bs.get(0).vertices);
        boolean found = false;
        for (Blocks.Block bl : bs) {
            if (bl.vertices.equals(Arrays.asList(c))) { assertEquals(Arrays.asList(loop), bl.edges); found = true; }
        }
        assertTrue(found);
    }

    @Test
    public void nonseparable() {
        assertTrue(Blocks.isNonseparable(vs(a), es()));
        assertTrue(Blocks.isNonseparable(vs(a, b), es(und(a, b))));
        assertFalse(Blocks.isNonseparable(vs(a, b, c), es(und(a, b), und(b, c))));
        assertTrue(Blocks.isNonseparable(vs(a, b, c), es(und(a, b), und(b, c), arc(c, a))));
        assertFalse(Blocks.isNonseparable(vs(a, b), es()));
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/Blocks.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bridges, blocks and nonseparability (CONTEXT.md), ignoring direction. Works on edges, not
 * neighbour sets, so parallel edges are distinct (ADR 0002): a parallel edge is never a bridge
 * and both copies land in the same block.
 */
public final class Blocks {

    private Blocks() {}

    public static final class Block {
        /** In vertex order. */
        public final List<Vertex> vertices;
        public final List<Edge> edges;

        Block(List<Vertex> vertices, List<Edge> edges) {
            this.vertices = Collections.unmodifiableList(vertices);
            this.edges = Collections.unmodifiableList(edges);
        }
    }

    public static Set<Edge> bridges(List<Vertex> vs, List<Edge> es) {
        Tarjan t = new Tarjan(vs, es);
        return t.bridges;
    }

    /** Every block; isolated vertices are blocks of their own, and self-loops join a block of their vertex. */
    public static List<Block> blocks(List<Vertex> vs, List<Edge> es) {
        Tarjan t = new Tarjan(vs, es);
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        List<List<Edge>> edgeSets = new ArrayList<List<Edge>>(t.blocks);
        // Self-loops: into the first block holding their vertex, else a block of their own vertex.
        for (Edge e : es) {
            if (e.vertex1 != e.vertex2 || !idx.containsKey(e.vertex1)) continue;
            List<Edge> home = null;
            for (List<Edge> set : edgeSets) {
                if (touches(set, e.vertex1)) { home = set; break; }
            }
            if (home == null) {
                home = new ArrayList<Edge>();
                edgeSets.add(home);
            }
            home.add(e);
        }
        List<Block> out = new ArrayList<Block>();
        Set<Vertex> covered = new HashSet<Vertex>();
        for (List<Edge> set : edgeSets) {
            List<Vertex> verts = new ArrayList<Vertex>();
            for (Vertex v : vs) if (touches(set, v)) verts.add(v);
            covered.addAll(verts);
            out.add(new Block(verts, set));
        }
        for (Vertex v : vs) {
            if (!covered.contains(v)) out.add(new Block(Arrays.asList(v), new ArrayList<Edge>()));
        }
        return out;
    }

    /** Connected with no cutpoint: a vertex lying in two blocks is a cutpoint. */
    public static boolean isNonseparable(List<Vertex> vs, List<Edge> es) {
        if (!Connectivity.isConnected(vs, es)) return false;
        Set<Vertex> seen = new HashSet<Vertex>();
        for (Block b : blocks(vs, es)) {
            for (Vertex v : b.vertices) if (!seen.add(v)) return false;
        }
        return true;
    }

    private static boolean touches(List<Edge> set, Vertex v) {
        for (Edge e : set) if (e.vertex1 == v || e.vertex2 == v) return true;
        return false;
    }

    /** Tarjan's DFS over edges (not neighbour vertices), skipping the parent edge, not the parent vertex. */
    private static final class Tarjan {
        final Set<Edge> bridges = new HashSet<Edge>();
        final List<List<Edge>> blocks = new ArrayList<List<Edge>>();
        private final List<Edge> es;
        private final List<List<int[]>> inc = new ArrayList<List<int[]>>();   // {edgeIndex, otherVertex}
        private final int[] disc, low;
        private final List<Edge> stack = new ArrayList<Edge>();
        private int timer = 0;

        Tarjan(List<Vertex> vs, List<Edge> es) {
            this.es = es;
            Map<Vertex, Integer> idx = GraphMatrices.index(vs);
            for (int i = 0; i < vs.size(); i++) inc.add(new ArrayList<int[]>());
            for (int k = 0; k < es.size(); k++) {
                Integer i = idx.get(es.get(k).vertex1), j = idx.get(es.get(k).vertex2);
                if (i == null || j == null || i.equals(j)) continue;
                inc.get(i).add(new int[] {k, j});
                inc.get(j).add(new int[] {k, i});
            }
            disc = new int[vs.size()];
            low = new int[vs.size()];
            Arrays.fill(disc, -1);
            for (int i = 0; i < vs.size(); i++) if (disc[i] < 0) dfs(i, -1);
        }

        private void dfs(int u, int parentEdge) {
            disc[u] = low[u] = timer++;
            for (int[] st : inc.get(u)) {
                int k = st[0], w = st[1];
                if (k == parentEdge) continue;
                if (disc[w] < 0) {
                    stack.add(es.get(k));
                    dfs(w, k);
                    low[u] = Math.min(low[u], low[w]);
                    if (low[w] > disc[u]) bridges.add(es.get(k));
                    if (low[w] >= disc[u]) {
                        List<Edge> block = new ArrayList<Edge>();
                        Edge top;
                        do {
                            top = stack.remove(stack.size() - 1);
                            block.add(0, top);
                        } while (top != es.get(k));
                        blocks.add(block);
                    }
                } else if (disc[w] < disc[u]) {
                    stack.add(es.get(k));
                    low[u] = Math.min(low[u], disc[w]);
                }
            }
        }
    }
}
```

Block order: the bowtie test expects the block containing `a` first. DFS from `a` finishes the inner block {c, d, e} first. So after the DFS, **sort `t.blocks` by the smallest vertex index each contains** before the self-loop step. Add this at the top of `blocks(...)`, after `edgeSets` is built:

```java
        final Map<Vertex, Integer> order = idx;
        Collections.sort(edgeSets, new java.util.Comparator<List<Edge>>() {
            public int compare(List<Edge> x, List<Edge> y) { return firstIndex(x, order) - firstIndex(y, order); }
        });
```

with

```java
    private static int firstIndex(List<Edge> set, Map<Vertex, Integer> idx) {
        int m = Integer.MAX_VALUE;
        for (Edge e : set) m = Math.min(m, Math.min(idx.get(e.vertex1), idx.get(e.vertex2)));
        return m;
    }
```

Ties (two blocks sharing their smallest vertex) keep DFS order. That's fine because the tests only fix the order where the smallest vertices differ.

- [ ] **Step 4: Add `graphtheory.BlocksTest`, run all** — pass.

- [ ] **Step 5: Use it for the canvas's purple bridges.** In `Canvas.recomputeGraphProperties` replace

```java
            gP.computeBridges(vertexList, edgeList);
            gP.computeBlocks(vertexList, edgeList);
```
with
```java
            java.util.Set<Edge> bridges = Blocks.bridges(vertexList, edgeList);
            for (Edge e : edgeList) e.isBridge = bridges.contains(e);
```

Delete `GraphProperties.computeBridges`, `dfsBridge`, `findEdgeIndex`, `computeBlocks`, `dfsBlock`, `findEdge`, `blockVertices`, `countNontrivialBlocks`, `hasMaximalBlocks`, `formatBlocks`, `isNonseparable` and the fields `blockCount` and `blockList`. Delete `Edge.blockId` and its comment. `drawGraphSummary` still uses some of these. Leave it compiling by deleting its blocks lines (`nontrivialCount`, `blocksStr`, `blockVerticesStr`, `nonsepStr` and the four summary strings that use them); Task 15 deletes the whole method. Build and run all tests: pass.

- [ ] **Step 6: Commit** — `fix: bridges and blocks treat parallel edges as distinct; isolated vertices are blocks`

---

### Task 7: Structure: simple, empty, complete, density, cyclic, forest, tree, star, bipartite

**Files:**
- Create: `src/graphtheory/Structure.java`
- Test: `test/graphtheory/StructureTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class StructureTest {
    // helpers v, und, arc, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void simple() {
        assertTrue(Structure.isSimple(vs(a, b), es(arc(a, b), arc(b, a))));
        assertFalse(Structure.isSimple(vs(a, b), es(und(a, b), arc(a, b))));
        assertFalse(Structure.isSimple(vs(a, b), es(arc(a, b), arc(a, b))));
        assertFalse(Structure.isSimple(vs(a), es(und(a, a))));
    }

    @Test
    public void empty_needsAVertex() {
        assertTrue(Structure.isEmpty(vs(a), es()));
        assertFalse(Structure.isEmpty(vs(), es()));
        assertFalse(Structure.isEmpty(vs(a, b), es(arc(a, b))));
    }

    @Test
    public void complete_mixedCanBeComplete_oneArcIsNot() {
        assertTrue(Structure.isComplete(vs(a, b, c), es(und(a, b), arc(a, c), arc(c, a), und(b, c))));
        assertFalse(Structure.isComplete(vs(a, b), es(arc(a, b))));
        assertTrue(Structure.isComplete(vs(a), es()));
        assertFalse(Structure.isComplete(vs(a, b), es(und(a, b), und(a, b))));
    }

    @Test
    public void density_orderedPairs_simpleOnly() {
        assertEquals(1.0, Structure.density(vs(a, b, c), es(und(a, b), arc(a, c), arc(c, a), und(b, c))), 1e-9);
        assertEquals(1.0 / 3, Structure.density(vs(a, b, c), es(und(a, b))), 1e-9);
        assertEquals(1.0 / 6, Structure.density(vs(a, b, c), es(arc(a, b))), 1e-9);
        assertNull(Structure.density(vs(a), es()));
        assertNull(Structure.density(vs(a, b), es(und(a, b), und(a, b))));
    }

    @Test
    public void cyclic_respectsDirection() {
        assertFalse(Structure.isCyclic(vs(a, b, c), es(arc(a, b), arc(b, c), arc(a, c))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(arc(a, b), arc(b, c), arc(c, a))));
        assertTrue(Structure.isCyclic(vs(a, b), es(und(a, b), und(a, b))));
        assertTrue(Structure.isCyclic(vs(a, b), es(und(a, b), arc(b, a))));
        assertTrue(Structure.isCyclic(vs(a), es(arc(a, a))));
        assertFalse(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))));
        assertTrue(Structure.isCyclic(vs(a, b, c), es(und(a, b), und(b, c), arc(c, a))));
        assertFalse(Structure.isCyclic(vs(a, b), es(arc(a, b), arc(a, b))));
    }

    @Test
    public void forestAndTree_ignoreDirection_countParallelEdges() {
        assertTrue(Structure.isTree(vs(a, b, c), es(arc(a, b), arc(b, c))));
        assertFalse(Structure.isForest(vs(a, b, c), es(arc(a, b), arc(b, c), arc(a, c))));
        assertFalse(Structure.isForest(vs(a, b), es(und(a, b), und(a, b))));
        assertFalse(Structure.isForest(vs(a), es(und(a, a))));
        assertTrue(Structure.isForest(vs(a, b, c), es(und(a, b))));
        assertFalse(Structure.isTree(vs(a, b, c), es(und(a, b))));
    }

    @Test
    public void star() {
        assertTrue(Structure.isStar(vs(a, b, c, d), es(und(a, b), arc(c, a), und(a, d))));
        assertFalse(Structure.isStar(vs(a, b, c, d), es(und(a, b), und(b, c), und(c, d))));
        assertFalse(Structure.isStar(vs(a, b), es(und(a, b))));
    }

    @Test
    public void bipartiteSides_firstVertexOnSideA_loopIsNotBipartite() {
        List<List<Vertex>> s = Structure.bipartiteSides(vs(a, b, c, d), es(und(b, a), arc(c, b)));
        assertEquals(Arrays.asList(a, c, d), s.get(0));
        assertEquals(Arrays.asList(b), s.get(1));
        assertNull(Structure.bipartiteSides(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))));
        assertNull(Structure.bipartiteSides(vs(a), es(und(a, a))));
        assertNotNull(Structure.bipartiteSides(vs(a), es()));
    }

    @Test
    public void completeBipartite_mustBeSimple() {
        assertTrue(Structure.isCompleteBipartite(vs(a, b, c), es(und(a, b), und(a, c))));
        assertFalse(Structure.isCompleteBipartite(vs(a, b, c, d), es(und(a, b), und(a, b), und(a, d), und(c, d))));
        assertFalse(Structure.isCompleteBipartite(vs(a), es()));
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/Structure.java`:

```java
package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Whole-graph structure (CONTEXT.md, Graph). Each method says whether it respects direction. */
public final class Structure {

    private Structure() {}

    /** No loops, and no two edges on one pair except the opposite arcs (a, b) and (b, a). */
    public static boolean isSimple(List<Vertex> vs, List<Edge> es) {
        Map<String, List<Edge>> byPair = new HashMap<String, List<Edge>>();
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        for (Edge e : es) {
            if (e.vertex1 == e.vertex2) return false;
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            String key = Math.min(i, j) + "," + Math.max(i, j);
            List<Edge> on = byPair.get(key);
            if (on == null) byPair.put(key, on = new ArrayList<Edge>());
            on.add(e);
        }
        for (List<Edge> on : byPair.values()) {
            if (on.size() == 1) continue;
            if (on.size() > 2) return false;
            Edge x = on.get(0), y = on.get(1);
            if (!(x.directed && y.directed && x.vertex1 == y.vertex2)) return false;
        }
        return true;
    }

    public static boolean isEmpty(List<Vertex> vs, List<Edge> es) {
        return !vs.isEmpty() && es.isEmpty();
    }

    /** Simple, and every two vertices joined both ways (an undirected edge, or both arcs). */
    public static boolean isComplete(List<Vertex> vs, List<Edge> es) {
        if (vs.isEmpty() || !isSimple(vs, es)) return false;
        boolean[][] step = steps(vs, es);
        for (int i = 0; i < vs.size(); i++) {
            for (int j = 0; j < vs.size(); j++) if (i != j && !step[i][j]) return false;
        }
        return true;
    }

    /** Fraction of ordered pairs (u, v), u ≠ v, with one edge leading u → v. null unless simple with n ≥ 2. */
    public static Double density(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n < 2 || !isSimple(vs, es)) return null;
        boolean[][] step = steps(vs, es);
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) if (i != j && step[i][j]) count++;
        }
        return (double) count / (n * (n - 1));
    }

    /**
     * Contains a cycle, respecting direction (CONTEXT.md, Cyclic). A loop is a cycle; two
     * distinct edges that go u → v and v → u are a cycle of length 2. With neither, a longer
     * cycle exists exactly when an arc lies inside a strongly connected part, or the
     * undirected edges close a cycle.
     */
    public static boolean isCyclic(List<Vertex> vs, List<Edge> es) {
        for (Edge e : es) if (e.vertex1 == e.vertex2) return true;
        for (int i = 0; i < es.size(); i++) {
            for (int j = 0; j < es.size(); j++) {
                if (i == j) continue;
                Edge x = es.get(i), y = es.get(j);
                if (goes(x, x.vertex1, x.vertex2) && goes(y, x.vertex2, x.vertex1)) return true;
            }
        }
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int n = vs.size();
        boolean[][] reach = steps(vs, es);
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (!reach[i][k]) continue;
                for (int j = 0; j < n; j++) if (reach[k][j]) reach[i][j] = true;
            }
        }
        for (Edge e : es) {
            if (e.directed && reach[idx.get(e.vertex2)][idx.get(e.vertex1)]) return true;
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (Edge e : es) {
            if (e.directed) continue;
            int x = Connectivity.find(parent, idx.get(e.vertex1)), y = Connectivity.find(parent, idx.get(e.vertex2));
            if (x == y) return true;
            parent[x] = y;
        }
        return false;
    }

    /** No cycle ignoring direction, counting parallel edges and loops. */
    public static boolean isForest(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int[] parent = new int[vs.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (Edge e : es) {
            int x = Connectivity.find(parent, idx.get(e.vertex1)), y = Connectivity.find(parent, idx.get(e.vertex2));
            if (x == y) return false;
            parent[x] = y;
        }
        return true;
    }

    public static boolean isTree(List<Vertex> vs, List<Edge> es) {
        return isForest(vs, es) && Connectivity.isConnected(vs, es);
    }

    /** A tree with at least three vertices, one of them adjacent to all the others. */
    public static boolean isStar(List<Vertex> vs, List<Edge> es) {
        if (vs.size() < 3 || !isTree(vs, es)) return false;
        for (Vertex c : vs) {
            if (PreferenceLists.neighboursOf(c, es).size() == vs.size() - 1) return true;
        }
        return false;
    }

    /**
     * The two sides of a bipartition, ignoring direction, or null if there is none. Each
     * component puts its first vertex (in vertex order) on side A; both lists are in vertex order.
     */
    public static List<List<Vertex>> bipartiteSides(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        int n = vs.size();
        List<List<Integer>> adj = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<Integer>());
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == j) return null;
            adj.get(i).add(j);
            adj.get(j).add(i);
        }
        int[] colour = new int[n];
        java.util.Arrays.fill(colour, -1);
        for (int s = 0; s < n; s++) {
            if (colour[s] >= 0) continue;
            colour[s] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int w : adj.get(u)) {
                    if (colour[w] < 0) {
                        colour[w] = 1 - colour[u];
                        queue.add(w);
                    } else if (colour[w] == colour[u]) {
                        return null;
                    }
                }
            }
        }
        List<List<Vertex>> sides = new ArrayList<List<Vertex>>();
        sides.add(new ArrayList<Vertex>());
        sides.add(new ArrayList<Vertex>());
        for (int i = 0; i < n; i++) sides.get(colour[i]).add(vs.get(i));
        return sides;
    }

    /** Simple, bipartite with two non-empty sides, every cross pair adjacent. */
    public static boolean isCompleteBipartite(List<Vertex> vs, List<Edge> es) {
        List<List<Vertex>> sides = bipartiteSides(vs, es);
        if (sides == null || sides.get(0).isEmpty() || sides.get(1).isEmpty() || !isSimple(vs, es)) return false;
        for (Vertex x : sides.get(0)) {
            Set<Vertex> nbrs = new HashSet<Vertex>(PreferenceLists.neighboursOf(x, es));
            if (!nbrs.containsAll(sides.get(1))) return false;
        }
        return true;
    }

    /** step[i][j]: some edge leads from vertex i to vertex j. */
    private static boolean[][] steps(List<Vertex> vs, List<Edge> es) {
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[][] step = new boolean[vs.size()][vs.size()];
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            step[i][j] = true;
            if (!e.directed) step[j][i] = true;
        }
        return step;
    }

    /** Edge e can be crossed from x to y. */
    private static boolean goes(Edge e, Vertex x, Vertex y) {
        if (e.vertex1 == x && e.vertex2 == y) return true;
        return !e.directed && e.vertex1 == y && e.vertex2 == x;
    }
}
```

- [ ] **Step 4: Add `graphtheory.StructureTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: glossary-exact simple, complete, density, cyclic, forest, star and bipartite`

---

### Task 8: Chromatic number with self-loops

**Files:**
- Create: `src/graphtheory/Colouring.java`
- Test: `test/graphtheory/ColouringTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class ColouringTest {
    // helpers v, und, arc, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void triangleNeedsThree_arcsCountAsAdjacent() {
        assertEquals(3, Colouring.chromaticNumber(vs(a, b, c), es(und(a, b), arc(b, c), arc(a, c))));
    }

    @Test
    public void noEdgesIsOne_noVerticesIsZero() {
        assertEquals(1, Colouring.chromaticNumber(vs(a, b), es()));
        assertEquals(0, Colouring.chromaticNumber(vs(), es()));
    }

    @Test
    public void selfLoop_hasNoProperColouring() {
        assertEquals(Colouring.NO_PROPER_COLOURING, Colouring.chromaticNumber(vs(a, b), es(und(a, b), arc(b, b))));
    }

    @Test
    public void tooLarge() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i <= Colouring.CAP; i++) many.add(v("v" + i));
        assertEquals(Colouring.TOO_LARGE, Colouring.chromaticNumber(many, es()));
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/Colouring.java`:

```java
package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Chromatic number (CONTEXT.md, Proper colouring), ignoring direction. */
public final class Colouring {

    /** Exact search gives up above this many vertices. */
    public static final int CAP = 15;
    public static final int NO_PROPER_COLOURING = -1;
    public static final int TOO_LARGE = -2;

    private Colouring() {}

    public static int chromaticNumber(List<Vertex> vs, List<Edge> es) {
        int n = vs.size();
        if (n == 0) return 0;
        for (Edge e : es) if (e.vertex1 == e.vertex2) return NO_PROPER_COLOURING;
        if (n > CAP) return TOO_LARGE;
        if (es.isEmpty()) return 1;
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        boolean[][] adj = new boolean[n][n];
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            adj[i][j] = adj[j][i] = true;
        }
        int[] colour = new int[n];
        for (int k = 1; k < n; k++) {
            Arrays.fill(colour, -1);
            if (canColour(0, k, colour, adj)) return k;
        }
        return n;
    }

    private static boolean canColour(int v, int k, int[] colour, boolean[][] adj) {
        if (v == colour.length) return true;
        for (int c = 0; c < k; c++) {
            boolean clash = false;
            for (int u = 0; u < v; u++) {
                if (adj[v][u] && colour[u] == c) { clash = true; break; }
            }
            if (clash) continue;
            colour[v] = c;
            if (canColour(v + 1, k, colour, adj)) return true;
            colour[v] = -1;
        }
        return false;
    }
}
```

- [ ] **Step 4: Add `graphtheory.ColouringTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: chromatic number that knows a self-loop has no proper colouring`

---

### Task 9: Maximal, maximum and stable matchings

**Files:**
- Create: `src/graphtheory/Matchings.java`
- Test: `test/graphtheory/MatchingsTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class MatchingsTest {
    // helpers v, und, arc, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    private static Vector<Vertex> prefs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    @Test
    public void maximal_greedyInEdgeOrder_skipsLoops_usesArcs() {
        Edge bc = arc(b, c);
        assertEquals(Arrays.asList(bc), Matchings.maximal(vs(a, b, c, d), es(und(a, a), bc, und(a, b), und(c, d))));
    }

    @Test
    public void maximum_beatsGreedyOnAPath() {
        // a-b-c-d: greedy taking b-c first gets 1; maximum is 2 (a-b, c-d)
        List<Edge> m = Matchings.maximum(vs(a, b, c, d), es(und(b, c), und(a, b), arc(d, c)));
        assertEquals(2, m.size());
    }

    @Test
    public void maximum_oddCycleLeavesOneOut() {
        assertEquals(1, Matchings.maximum(vs(a, b, c), es(und(a, b), und(b, c), und(c, a))).size());
    }

    @Test
    public void maximum_tooLarge_isNull() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i <= Matchings.MAXIMUM_VERTEX_CAP; i++) many.add(v("v" + i));
        assertNull(Matchings.maximum(many, es()));
    }

    @Test
    public void stable_proposingSideGetsItsBest() {
        // Sides {a, c} and {b, d}; everyone adjacent across.
        List<Edge> edges = es(und(a, b), und(a, d), und(c, b), und(c, d));
        // Each side's first choices conflict, so the proposing side decides the result.
        a.preferences = prefs(b, d);
        c.preferences = prefs(d, b);
        b.preferences = prefs(c, a);
        d.preferences = prefs(a, c);
        List<Edge> fromA = Matchings.stable(vs(a, b, c, d), edges, a);
        assertTrue(joins(fromA, a, b) && joins(fromA, c, d));
        List<Edge> fromB = Matchings.stable(vs(a, b, c, d), edges, b);
        assertTrue(joins(fromB, c, b) && joins(fromB, a, d));
    }

    @Test
    public void stable_needsBipartiteAndLists() {
        List<Edge> edges = es(und(a, b));
        assertNull(Matchings.stable(vs(a, b), edges, a));   // no lists
        a.preferences = prefs(b);
        b.preferences = prefs(a);
        assertEquals(1, Matchings.stable(vs(a, b), edges, null).size());
        assertNull(Matchings.stable(vs(a, b, c), es(und(a, b), und(b, c), und(c, a)), a));
    }

    private static boolean joins(List<Edge> m, Vertex x, Vertex y) {
        for (Edge e : m) {
            if ((e.vertex1 == x && e.vertex2 == y) || (e.vertex1 == y && e.vertex2 == x)) return true;
        }
        return false;
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/Matchings.java`:

```java
package graphtheory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Matchings (CONTEXT.md), ignoring direction; a self-loop is never in a matching. */
public final class Matchings {

    /** Exact maximum matching gives up above this many vertices. */
    public static final int MAXIMUM_VERTEX_CAP = 20;

    private Matchings() {}

    /** A maximal matching: edges taken greedily in edge order. */
    public static List<Edge> maximal(List<Vertex> vs, List<Edge> es) {
        List<Vertex> used = new ArrayList<Vertex>();
        List<Edge> out = new ArrayList<Edge>();
        for (Edge e : es) {
            if (e.vertex1 == e.vertex2 || used.contains(e.vertex1) || used.contains(e.vertex2)) continue;
            out.add(e);
            used.add(e.vertex1);
            used.add(e.vertex2);
        }
        return out;
    }

    /** A maximum matching, or null above MAXIMUM_VERTEX_CAP vertices. */
    public static List<Edge> maximum(List<Vertex> vs, List<Edge> es) {
        final int n = vs.size();
        if (n > MAXIMUM_VERTEX_CAP) return null;
        Map<Vertex, Integer> idx = GraphMatrices.index(vs);
        final Edge[][] join = new Edge[n][n];
        for (Edge e : es) {
            int i = idx.get(e.vertex1), j = idx.get(e.vertex2);
            if (i == j || join[i][j] != null) continue;
            join[i][j] = join[j][i] = e;
        }
        final int[] memo = new int[1 << n];
        Arrays.fill(memo, -1);
        int full = (1 << n) - 1;
        best(full, join, memo);
        List<Edge> out = new ArrayList<Edge>();
        int mask = full;
        while (mask != 0) {
            int i = Integer.numberOfTrailingZeros(mask);
            int rest = mask & ~(1 << i);
            if (best(mask, join, memo) == best(rest, join, memo)) {
                mask = rest;
                continue;
            }
            for (int j = 0; j < n; j++) {
                if (join[i][j] == null || (rest & (1 << j)) == 0) continue;
                int after = rest & ~(1 << j);
                if (best(mask, join, memo) == 1 + best(after, join, memo)) {
                    out.add(join[i][j]);
                    mask = after;
                    break;
                }
            }
        }
        return out;
    }

    /** The largest matching among the vertices in mask. */
    private static int best(int mask, Edge[][] join, int[] memo) {
        if (mask == 0) return 0;
        if (memo[mask] >= 0) return memo[mask];
        int i = Integer.numberOfTrailingZeros(mask);
        int rest = mask & ~(1 << i);
        int r = best(rest, join, memo);
        for (int j = 0; j < join.length; j++) {
            if (join[i][j] != null && (rest & (1 << j)) != 0) {
                r = Math.max(r, 1 + best(rest & ~(1 << j), join, memo));
            }
        }
        return memo[mask] = r;
    }

    /**
     * The stable matching found by Gale–Shapley with the side containing 'proposer' proposing
     * (side A if proposer is null or not in the graph). null when the graph is not bipartite or
     * some vertex with a neighbour has no preference list.
     */
    public static List<Edge> stable(List<Vertex> vs, List<Edge> es, Vertex proposer) {
        List<List<Vertex>> sides = Structure.bipartiteSides(vs, es);
        if (sides == null || !PreferenceLists.missing(vs, es).isEmpty()) return null;
        List<Vertex> proposers = sides.get(1).contains(proposer) ? sides.get(1) : sides.get(0);

        Map<Vertex, Vertex> partner = new HashMap<Vertex, Vertex>();
        Map<Vertex, Integer> next = new HashMap<Vertex, Integer>();
        ArrayDeque<Vertex> free = new ArrayDeque<Vertex>();
        for (Vertex p : proposers) {
            next.put(p, 0);
            if (p.preferences != null) free.add(p);
        }
        while (!free.isEmpty()) {
            Vertex p = free.poll();
            int k = next.get(p);
            if (k >= p.preferences.size()) continue;
            next.put(p, k + 1);
            Vertex r = p.preferences.get(k);
            Vertex current = partner.get(r);
            if (current == null) {
                partner.put(r, p);
                partner.put(p, r);
            } else if (r.preferences.indexOf(p) < r.preferences.indexOf(current)) {
                partner.put(r, p);
                partner.put(p, r);
                partner.remove(current);
                free.add(current);
            } else {
                free.add(p);
            }
        }
        List<Edge> out = new ArrayList<Edge>();
        for (Vertex p : proposers) {
            Vertex r = partner.get(p);
            if (r == null) continue;
            for (Edge e : es) {
                if ((e.vertex1 == p && e.vertex2 == r) || (e.vertex1 == r && e.vertex2 == p)) {
                    out.add(e);
                    break;
                }
            }
        }
        return out;
    }
}
```

- [ ] **Step 4: Add `graphtheory.MatchingsTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: exact maximum matching for any graph; stable matching from real preference lists`

---

### Task 10: `PropertiesReport`: everything, computed once

**Files:**
- Create: `src/graphtheory/PropertiesReport.java`
- Test: `test/graphtheory/PropertiesReportTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class PropertiesReportTest {
    // helpers v, und, arc, w, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    @Test
    public void weightedOnlyWhenSomeWeightIsNotOne() {
        assertNull(new PropertiesReport(vs(a, b), es(und(a, b)), null).weightedDistances);
        assertNotNull(new PropertiesReport(vs(a, b), es(w(und(a, b), 4)), null).weightedDistances);
    }

    @Test
    public void strongConnectivityOnlyWithArcs() {
        assertNull(new PropertiesReport(vs(a, b), es(und(a, b)), null).stronglyConnected);
        assertEquals(Boolean.FALSE, new PropertiesReport(vs(a, b), es(arc(a, b)), null).stronglyConnected);
    }

    @Test
    public void perfectFollowsMaximum() {
        PropertiesReport r = new PropertiesReport(vs(a, b, c), es(und(a, b), und(b, c)), null);
        assertEquals(1, r.maximum.size());
        assertEquals(Boolean.FALSE, r.perfect);
    }

    @Test
    public void proposerOutsideGraph_fallsBackToSideA() {
        PropertiesReport r = new PropertiesReport(vs(a, b), es(und(a, b)), v("zz"));
        assertSame(a, r.proposer);
    }

    @Test
    public void emptyGraph_doesNotThrow() {
        PropertiesReport r = new PropertiesReport(vs(), es(), null);
        assertEquals(0, r.vertexCut.size);
        assertTrue(r.blocks.isEmpty());
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/PropertiesReport.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Everything the Properties tab shows, computed once from the graph (and the proposing side
 * for stable matching). Immutable; Canvas builds a new one when the graph text changes.
 */
public final class PropertiesReport {

    public final List<Vertex> vertices;
    public final List<Edge> edges;
    public final boolean weighted, hasArcs;

    public final int[][] adjacency, distances;
    /** null unless the graph is weighted. */
    public final int[][] weightedDistances;

    public final List<List<Vertex>> components;
    public final boolean connected;
    /** null when the graph has no arcs. */
    public final Boolean stronglyConnected;
    public final Connectivity.Cut<Vertex> vertexCut;
    public final Connectivity.Cut<Edge> edgeCut;
    public final Set<Edge> bridges;

    public final boolean simple, empty, complete, cyclic, forest, tree, star, completeBipartite;
    /** null when undefined (fewer than two vertices, or not simple). */
    public final Double density;
    /** null when not bipartite. */
    public final List<List<Vertex>> sides;

    public final List<Blocks.Block> blocks;
    public final boolean nonseparable;

    public final boolean hamiltonTooLarge;
    public final boolean eulerTrail, eulerTour, hamiltonianPath, hamiltonianCycle;

    /** Colouring.chromaticNumber's result, including its NO_PROPER_COLOURING / TOO_LARGE codes. */
    public final int chromatic;
    public final List<Edge> maximal;
    /** null above Matchings.MAXIMUM_VERTEX_CAP. */
    public final List<Edge> maximum;
    /** null when maximum is null or there are no vertices. */
    public final Boolean perfect;
    /** The vertex whose side proposes (null with no vertices). */
    public final Vertex proposer;
    /** null when not bipartite or lists are missing. */
    public final List<Edge> stable;
    public final List<Vertex> missingPreferences;

    public PropertiesReport(List<Vertex> vs, List<Edge> es, Vertex proposer) {
        vertices = Collections.unmodifiableList(new ArrayList<Vertex>(vs));
        edges = Collections.unmodifiableList(new ArrayList<Edge>(es));
        weighted = Edge.isWeighted(es);
        boolean arcs = false;
        for (Edge e : es) arcs |= e.directed;
        hasArcs = arcs;

        adjacency = GraphMatrices.adjacency(vs, es);
        distances = GraphMatrices.distances(vs, es);
        weightedDistances = weighted ? GraphMatrices.weightedDistances(vs, es) : null;

        components = Connectivity.components(vs, es);
        connected = Connectivity.isConnected(vs, es);
        stronglyConnected = hasArcs ? Connectivity.isStronglyConnected(vs, es) : null;
        vertexCut = Connectivity.minimumVertexCut(vs, es);
        edgeCut = Connectivity.minimumEdgeCut(vs, es);
        bridges = Blocks.bridges(vs, es);

        simple = Structure.isSimple(vs, es);
        empty = Structure.isEmpty(vs, es);
        complete = Structure.isComplete(vs, es);
        density = Structure.density(vs, es);
        cyclic = Structure.isCyclic(vs, es);
        forest = Structure.isForest(vs, es);
        tree = Structure.isTree(vs, es);
        star = Structure.isStar(vs, es);
        sides = Structure.bipartiteSides(vs, es);
        completeBipartite = Structure.isCompleteBipartite(vs, es);

        blocks = Blocks.blocks(vs, es);
        nonseparable = Blocks.isNonseparable(vs, es);

        Vector<Vertex> vv = new Vector<Vertex>(vs);
        Vector<Edge> ev = new Vector<Edge>(es);
        eulerTrail = Traversals.eulerTrail(vv, ev) != null;
        eulerTour = Traversals.eulerTour(vv, ev) != null;
        hamiltonTooLarge = Traversals.hamiltonTooLarge(vv);
        hamiltonianPath = !hamiltonTooLarge && Traversals.hamiltonianPath(vv, ev) != null;
        hamiltonianCycle = !hamiltonTooLarge && Traversals.hamiltonianCycle(vv, ev) != null;

        chromatic = Colouring.chromaticNumber(vs, es);
        maximal = Matchings.maximal(vs, es);
        maximum = Matchings.maximum(vs, es);
        perfect = (maximum == null || vs.isEmpty()) ? null : maximum.size() * 2 == vs.size();
        this.proposer = vs.contains(proposer) ? proposer : (vs.isEmpty() ? null : vs.get(0));
        stable = Matchings.stable(vs, es, this.proposer);
        missingPreferences = PreferenceLists.missing(vs, es);
    }
}
```

Note: the `proposer` fallback picks `vs.get(0)`, which is always on side A because each component's first vertex is colour 0.

- [ ] **Step 4: Add `graphtheory.PropertiesReportTest`, run all** — pass.

- [ ] **Step 5: Timing check.** Add this test, which fails if a 20-vertex, 60-edge graph takes over 1 s. The budget is 200 ms; 1 s keeps it stable on a slow machine.

```java
    @Test(timeout = 1000)
    public void twentyVerticesSixtyEdges_isFast() {
        List<Vertex> many = new Vector<Vertex>();
        for (int i = 0; i < 20; i++) many.add(v("v" + i));
        List<Edge> edges = new Vector<Edge>();
        java.util.Random rnd = new java.util.Random(7);
        while (edges.size() < 60) {
            Vertex x = many.get(rnd.nextInt(20)), y = many.get(rnd.nextInt(20));
            edges.add(rnd.nextBoolean() ? und(x, y) : arc(x, y));
        }
        new PropertiesReport(many, edges, null);
    }
```

If it times out, profile which part is slow and report it rather than raising the timeout. The Hamiltonian search is capped at 20, so a dense graph may be the cause; the report's job is to say so.

- [ ] **Step 6: Commit** — `feat: PropertiesReport computes the whole tab once`

---

### Task 11: Summary text and Vertices table text

**Files:**
- Create: `src/graphtheory/SummaryText.java`
- Modify: `src/graphtheory/PanelText.java`
- Test: `test/graphtheory/SummaryTextTest.java`, `test/graphtheory/PanelTextTest.java`

- [ ] **Step 1: Write the failing tests**

`SummaryTextTest`:

```java
public class SummaryTextTest {
    // helpers v, und, arc, w, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    private static String line(List<SummaryText.Section> ss, String label) {
        for (SummaryText.Section s : ss) for (String[] l : s.lines) if (l[0].equals(label)) return l[1];
        return null;
    }

    private static List<String> titles(List<SummaryText.Section> ss) {
        List<String> t = new java.util.ArrayList<String>();
        for (SummaryText.Section s : ss) t.add(s.title);
        return t;
    }

    @Test
    public void sixSectionsInOrder() {
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a), es(), null));
        assertEquals(Arrays.asList("Size and order", "Connectivity", "Structure", "Blocks",
                "Traversals", "Colouring and matching"), titles(ss));
    }

    @Test
    public void edgeSet_showsWeightsOnlyWhenWeighted_neverTruncates() {
        List<Edge> many = new Vector<Edge>();
        for (int i = 0; i < 8; i++) many.add(und(a, b));
        assertEquals("{a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}, {a, b}",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), many, null)), "E"));
        assertEquals("{a, b}:3, (b, a):1",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), es(w(und(a, b), 3), arc(b, a)), null)), "E"));
    }

    @Test
    public void strongConnectivityLineOnlyWithArcs() {
        assertNull(line(SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b)), null)), "Strongly connected"));
        assertEquals("no", line(SummaryText.sections(new PropertiesReport(vs(a, b), es(arc(a, b)), null)), "Strongly connected"));
    }

    @Test
    public void completeGraph_saysThereIsNoVertexCut() {
        String k = line(SummaryText.sections(new PropertiesReport(vs(a, b, c),
                es(und(a, b), und(b, c), und(c, a)), null)), "Vertex connectivity \u03BA(G)");
        assertEquals("2 (no vertex cut: every two vertices are adjacent)", k);
    }

    @Test
    public void reasonsInBrackets() {
        List<SummaryText.Section> ss = SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b), und(a, b)), null));
        assertEquals("\u2014 (not simple)", line(ss, "Density"));
        assertEquals("\u2014 (no preference list: a, b)", line(ss, "Stable matching"));
        List<SummaryText.Section> loop = SummaryText.sections(new PropertiesReport(vs(a), es(und(a, a)), null));
        assertEquals("\u2014 (self-loop)", line(loop, "Chromatic number \u03C7(G)"));
        assertEquals("no (self-loop)", line(loop, "Bipartite"));
    }

    @Test
    public void tree_namesItsRoot() {
        a.isRoot = true;
        assertEquals("rooted tree (root a)",
                line(SummaryText.sections(new PropertiesReport(vs(a, b), es(und(a, b)), null)), "Tree"));
    }

    @Test
    public void blocksLine() {
        assertEquals("2 (0 nontrivial): {a, b}, {c}",
                line(SummaryText.sections(new PropertiesReport(vs(a, b, c), es(und(a, b)), null)), "Blocks"));
    }

    @Test
    public void html_escapesNothingSurprising_andHasHeadings() {
        String html = SummaryText.html(SummaryText.sections(new PropertiesReport(vs(a), es(), null)));
        assertTrue(html.contains("<h3>Size and order</h3>"));
        assertTrue(html.contains("Order"));
    }
}
```

Append to `PanelTextTest`:

```java
    @Test
    public void neighbours_notation() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0),
               d = new Vertex("d", 0, 0), e = new Vertex("e", 0, 0);
        java.util.List<Vertex> vs = java.util.Arrays.asList(a, b, c, d, e);
        java.util.List<Edge> es = java.util.Arrays.asList(new Edge(a, b, false), new Edge(b, a, false),
                new Edge(a, c, false), new Edge(a, d, true), new Edge(e, a, true), new Edge(a, a, true));
        assertEquals("b \u00d72, c, \u2192a, \u2192d, \u2190e", PanelText.neighbours(a, vs, es));
    }

    @Test
    public void neighbours_isolatedIsBlank_undirectedLoopIsOwnName() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0);
        java.util.List<Vertex> vs = java.util.Arrays.asList(a, b);
        assertEquals("", PanelText.neighbours(b, vs, java.util.Arrays.asList(new Edge(a, a, false))));
        assertEquals("a", PanelText.neighbours(a, vs, java.util.Arrays.asList(new Edge(a, a, false))));
    }

    @Test
    public void verticesRow_matchesColumns() {
        Vertex a = new Vertex("a", 0, 0);
        Object[] row = PanelText.verticesRow(a, java.util.Arrays.asList(a), new java.util.Vector<Edge>());
        assertEquals(PanelText.VERTICES_COLUMNS.length, row.length);
        assertEquals("a", row[0]);
        assertEquals(Integer.valueOf(0), row[1]);
        assertEquals("yes", row[4]);
    }
```

- [ ] **Step 2: Run both, expect compile failure.**

- [ ] **Step 3: Implement.** In `PanelText.java` add (next to `VERTEX_COLUMNS`):

```java
    /** The Properties tab's Vertices table: the side panel's labels plus Neighbours. */
    public static final String[] VERTICES_COLUMNS = { "Name", "Degree", "In-Degree", "Out-Degree",
            "Isolated", "Self-loop", "Cutpoint", "Root", "Neighbours" };

    /** One Vertices row; degrees are Integers so the column sorts as numbers. */
    public static Object[] verticesRow(Vertex v, List<Vertex> vs, List<Edge> es) {
        return new Object[] { v.name, v.degree(), v.inDegree(), v.outDegree(), yesNo(v.isIsolated()),
                yesNo(v.hasSelfLoop()), yesNo(v.isCutpoint), yesNo(v.isRoot), neighbours(v, vs, es) };
    }

    /**
     * Undirected neighbours, then "→x" for arcs out, then "←x" for arcs in, each in vertex
     * order, "×k" for k parallel edges. A loop shows the vertex's own name ("→a" if directed).
     */
    public static String neighbours(Vertex v, List<Vertex> vs, List<Edge> es) {
        List<String> parts = new ArrayList<String>();
        for (int pass = 0; pass < 3; pass++) {
            for (Vertex w : vs) {
                int k = 0;
                for (Edge e : es) {
                    if (pass == 0 && !e.directed
                            && ((e.vertex1 == v && e.vertex2 == w) || (e.vertex2 == v && e.vertex1 == w))) k++;
                    if (pass == 1 && e.directed && e.vertex1 == v && e.vertex2 == w) k++;
                    if (pass == 2 && e.directed && e.vertex2 == v && e.vertex1 == w && w != v) k++;
                }
                if (k == 0) continue;
                String prefix = pass == 1 ? "\u2192" : pass == 2 ? "\u2190" : "";
                parts.add(prefix + w.name + (k > 1 ? " \u00d7" + k : ""));
            }
        }
        return join(parts, ", ");
    }
```

Check the test's expected order, `"b ×2, c, →a, →d, ←e"`: pass 0 gives b ×2 and c, pass 1 gives →a (the loop, since a comes first in vertex order) and →d, pass 2 gives ←e. The undirected-loop case counts the loop once (`e.vertex1 == v && e.vertex2 == w` with w == v), so it reads `a`.

Create `src/graphtheory/SummaryText.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The Overview summary: titled sections of (label, value) lines (CONTEXT.md terms; yes/no). */
public final class SummaryText {

    public static final String NONE = "\u2014";

    private SummaryText() {}

    public static final class Section {
        public final String title;
        /** Each {label, value}. */
        public final List<String[]> lines = new ArrayList<String[]>();

        Section(String title) { this.title = title; }

        void add(String label, String value) { lines.add(new String[] { label, value }); }
    }

    public static List<Section> sections(PropertiesReport r) {
        int n = r.vertices.size();
        boolean none = n == 0;
        List<Section> out = new ArrayList<Section>();

        Section size = new Section("Size and order");
        size.add("Order", "" + n);
        size.add("Size", "" + r.edges.size());
        size.add("Magnitude", "" + (n + r.edges.size()));
        size.add("V", vertexSet(r.vertices));
        size.add("E", edges(r.edges, r.weighted));
        out.add(size);

        Section con = new Section("Connectivity");
        con.add("Connected", none ? NONE : r.connected ? "yes" : "no (" + r.components.size() + " components)");
        con.add("Components", none ? NONE : sets(r.components));
        if (r.stronglyConnected != null) con.add("Strongly connected", yesNo(r.stronglyConnected));
        String kappa = "" + r.vertexCut.size;
        if (!r.vertexCut.members.isEmpty()) kappa += " (minimum vertex cut " + vertexSet(r.vertexCut.members) + ")";
        else if (r.connected && n > 1) kappa += " (no vertex cut: every two vertices are adjacent)";
        con.add("Vertex connectivity \u03BA(G)", none ? NONE : kappa);
        String lambda = "" + r.edgeCut.size;
        if (!r.edgeCut.members.isEmpty()) lambda += " (minimum edge cut " + edges(r.edgeCut.members, false) + ")";
        con.add("Edge connectivity \u03BB(G)", none ? NONE : lambda);
        List<Edge> bridges = new ArrayList<Edge>();
        for (Edge e : r.edges) if (r.bridges.contains(e)) bridges.add(e);
        con.add("Bridges", bridges.isEmpty() ? "0" : bridges.size() + ": " + edges(bridges, false));
        out.add(con);

        Section st = new Section("Structure");
        st.add("Simple", none ? NONE : yesNo(r.simple));
        st.add("Empty", none ? NONE : yesNo(r.empty));
        st.add("Complete", none ? NONE : yesNo(r.complete));
        st.add("Density", r.density != null ? String.format("%.2f", r.density)
                : NONE + (n < 2 ? " (fewer than two vertices)" : " (not simple)"));
        st.add("Cyclic", none ? NONE : r.cyclic ? "cyclic" : "acyclic");
        st.add("Tree", none ? NONE : treeLine(r));
        st.add("Star", none ? NONE : yesNo(r.star));
        st.add("Bipartite", none ? NONE : bipartiteLine(r));
        out.add(st);

        Section bl = new Section("Blocks");
        bl.add("Nonseparable", none ? NONE : yesNo(r.nonseparable));
        int nontrivial = 0;
        List<List<Vertex>> blockSets = new ArrayList<List<Vertex>>();
        for (Blocks.Block b : r.blocks) {
            if (b.vertices.size() >= 3) nontrivial++;
            blockSets.add(b.vertices);
        }
        bl.add("Blocks", none ? NONE : r.blocks.size() + " (" + nontrivial + " nontrivial): " + sets(blockSets));
        out.add(bl);

        Section tr = new Section("Traversals");
        String big = NONE + " (more than " + Traversals.HAMILTON_VERTEX_CAP + " vertices)";
        tr.add("Euler trail", yesNo(r.eulerTrail));
        tr.add("Euler tour", yesNo(r.eulerTour));
        tr.add("Hamiltonian path", r.hamiltonTooLarge ? big : yesNo(r.hamiltonianPath));
        tr.add("Hamiltonian cycle", r.hamiltonTooLarge ? big : yesNo(r.hamiltonianCycle));
        out.add(tr);

        Section cm = new Section("Colouring and matching");
        cm.add("Chromatic number \u03C7(G)", none ? NONE
                : r.chromatic == Colouring.NO_PROPER_COLOURING ? NONE + " (self-loop)"
                : r.chromatic == Colouring.TOO_LARGE ? NONE + " (more than " + Colouring.CAP + " vertices)"
                : "" + r.chromatic);
        cm.add("Maximal matching", none ? NONE : matching(r.maximal));
        String tooMany = NONE + " (more than " + Matchings.MAXIMUM_VERTEX_CAP + " vertices)";
        cm.add("Maximum matching", none ? NONE : r.maximum == null ? tooMany : matching(r.maximum));
        cm.add("Perfect matching", none ? NONE : r.maximum == null ? tooMany : yesNo(r.perfect));
        cm.add("Stable matching", stableLine(r));
        out.add(cm);
        return out;
    }

    /** The summary as HTML for a read-only, selectable JEditorPane. Names are [A-Za-z0-9_], so no escaping is needed. */
    public static String html(List<Section> sections) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif'>");
        for (Section s : sections) {
            sb.append("<h3>").append(s.title).append("</h3><table>");
            for (String[] l : s.lines) {
                sb.append("<tr><td valign='top'><b>").append(l[0]).append("</b></td><td>")
                  .append(l[1]).append("</td></tr>");
            }
            sb.append("</table>");
        }
        return sb.append("</body></html>").toString();
    }

    private static String treeLine(PropertiesReport r) {
        List<String> roots = new ArrayList<String>();
        for (Vertex v : r.vertices) if (v.isRoot) roots.add(v.name);
        if (r.tree) return roots.size() == 1 ? "rooted tree (root " + roots.get(0) + ")" : "tree";
        if (!r.forest) return "no";
        String kind = roots.size() == r.components.size() ? "rooted forest" : "forest";
        String trees = r.components.size() + " trees";
        return kind + " (" + trees + (roots.isEmpty() ? "" : "; roots " + PanelText.join(roots, ", ")) + ")";
    }

    private static String bipartiteLine(PropertiesReport r) {
        if (r.sides == null) {
            for (Edge e : r.edges) if (e.vertex1 == e.vertex2) return "no (self-loop)";
            return "no";
        }
        String k = r.completeBipartite
                ? "K_{" + r.sides.get(0).size() + "," + r.sides.get(1).size() + "}, " : "";
        return "yes (" + k + "sides " + vertexSet(r.sides.get(0)) + " and " + vertexSet(r.sides.get(1)) + ")";
    }

    private static String stableLine(PropertiesReport r) {
        if (r.vertices.isEmpty()) return NONE;
        if (r.sides == null) return NONE + " (not bipartite)";
        if (!r.missingPreferences.isEmpty()) {
            List<String> names = new ArrayList<String>();
            for (Vertex v : r.missingPreferences) names.add(v.name);
            return NONE + " (no preference list: " + PanelText.join(names, ", ") + ")";
        }
        List<Vertex> side = r.sides.get(1).contains(r.proposer) ? r.sides.get(1) : r.sides.get(0);
        return matching(r.stable) + " (side " + vertexSet(side) + " proposes)";
    }

    private static String matching(List<Edge> m) {
        return m.isEmpty() ? "0" : m.size() + ": " + edges(m, false);
    }

    static String vertexSet(List<Vertex> vs) {
        List<String> names = new ArrayList<String>();
        for (Vertex v : vs) names.add(v.name);
        return "{" + PanelText.join(names, ", ") + "}";
    }

    private static String sets(List<List<Vertex>> sets) {
        List<String> parts = new ArrayList<String>();
        for (List<Vertex> s : sets) parts.add(vertexSet(s));
        return PanelText.join(parts, ", ");
    }

    /** {a, b} for an edge, (a, b) for an arc, ":w" after each when weighted. */
    static String edges(List<Edge> es, boolean weighted) {
        List<String> parts = new ArrayList<String>();
        for (Edge e : es) {
            String s = e.directed ? "(" + e.vertex1.name + ", " + e.vertex2.name + ")"
                                  : "{" + e.vertex1.name + ", " + e.vertex2.name + "}";
            parts.add(weighted ? s + ":" + e.weight : s);
        }
        return PanelText.join(parts, ", ");
    }

    private static String yesNo(boolean b) { return PanelText.yesNo(b); }
}
```

With no vertices, `V` reads `{}` and `E` is blank. That's intended: the E line is the bare edge list, as the test expects.

- [ ] **Step 4: Add `graphtheory.SummaryTextTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: summary sections and Vertices table text from the report`

---

### Task 12: Copy as tab-separated values

**Files:**
- Create: `src/graphtheory/TableCopy.java`
- Test: `test/graphtheory/TableCopyTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import javax.swing.JTable;
import org.junit.Test;
import static org.junit.Assert.*;

public class TableCopyTest {

    @Test
    public void tsv_withColumnAndRowHeaders() {
        assertEquals("\tb\tc\na\t1\t\u221E\n",
                TableCopy.tsv(new String[] {"b", "c"}, new String[] {"a"}, new String[][] {{"1", "\u221E"}}));
    }

    @Test
    public void tsv_withoutRowHeaders() {
        assertEquals("Name\tDegree\na\t2\n",
                TableCopy.tsv(new String[] {"Name", "Degree"}, null, new String[][] {{"a", "2"}}));
    }

    @Test
    public void selection_copiesOnlySelectedCells_inViewOrder() {
        JTable t = new JTable(new Object[][] {{"a", 1, "x"}, {"b", 2, "y"}}, new Object[] {"N", "D", "Z"});
        t.setCellSelectionEnabled(true);
        t.changeSelection(1, 0, false, false);
        t.changeSelection(1, 1, false, true);
        assertEquals("N\tD\nb\t2\n", TableCopy.selection(t, null));
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/TableCopy.java`:

```java
package graphtheory;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

/** Ctrl+C on a Properties table copies the selected cells as tab-separated values, with headers. */
public final class TableCopy {

    private TableCopy() {}

    /** Header row (blank corner cell when rowNames is given), then one line per row. */
    public static String tsv(String[] columnNames, String[] rowNames, String[][] cells) {
        StringBuilder sb = new StringBuilder();
        if (rowNames != null) sb.append('\t');
        sb.append(String.join("\t", columnNames)).append('\n');
        for (int r = 0; r < cells.length; r++) {
            if (rowNames != null) sb.append(rowNames[r]).append('\t');
            sb.append(String.join("\t", cells[r])).append('\n');
        }
        return sb.toString();
    }

    /** The table's selected cells; rowNames[viewRow] labels each row, or null for none. */
    public static String selection(JTable t, String[] rowNames) {
        int[] rows = t.getSelectedRows(), cols = t.getSelectedColumns();
        String[] colNames = new String[cols.length];
        for (int c = 0; c < cols.length; c++) colNames[c] = t.getColumnName(cols[c]);
        String[] names = rowNames == null ? null : new String[rows.length];
        String[][] cells = new String[rows.length][cols.length];
        for (int r = 0; r < rows.length; r++) {
            if (names != null) names[r] = rowNames[rows[r]];
            for (int c = 0; c < cols.length; c++) cells[r][c] = String.valueOf(t.getValueAt(rows[r], cols[c]));
        }
        return tsv(colNames, names, cells);
    }

    /** Makes Ctrl+C on t copy selection(t, rowNames). rowNames may be replaced later via the array. */
    public static void install(final JTable t, final String[][] rowNamesHolder) {
        t.setTransferHandler(new TransferHandler() {
            @Override public int getSourceActions(JComponent c) { return COPY; }

            @Override protected Transferable createTransferable(JComponent c) {
                return new StringSelection(selection(t, rowNamesHolder == null ? null : rowNamesHolder[0]));
            }
        });
    }
}
```

`rowNamesHolder` is a one-element array, so `MatricesView` can swap the names when the graph changes without reinstalling the handler.

- [ ] **Step 4: Add `graphtheory.TableCopyTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: copy table selections as tab-separated values with headers`

---

### Task 13: The Properties views

**Files:**
- Create: `src/graphtheory/OverviewView.java`, `src/graphtheory/MatricesView.java`, `src/graphtheory/DegreeChartsView.java`, `src/graphtheory/PropertiesPanel.java`
- Modify: `src/graphtheory/GraphProperties.java` (chart drawing becomes static, plus tooltip)
- Test: `test/graphtheory/PropertiesPanelTest.java`, `test/graphtheory/GraphPropertiesTest.java`

- [ ] **Step 1: Write the failing tests**

`PropertiesPanelTest` (headless-safe: no frames are shown):

```java
public class PropertiesPanelTest {
    // helpers v, und, arc, w, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    private PropertiesPanel panel() {
        return new PropertiesPanel(new PropertiesPanel.Listener() {
            public void proposerChosen(Vertex p) {}
            public void editPreferences() {}
        });
    }

    @Test
    public void fourSubTabs() {
        PropertiesPanel p = panel();
        assertEquals(4, p.getTabCount());
        assertEquals(Arrays.asList("Overview", "Vertices", "Matrices", "Distributions"),
                Arrays.asList(p.getTitleAt(0), p.getTitleAt(1), p.getTitleAt(2), p.getTitleAt(3)));
    }

    @Test
    public void verticesTable_oneRowPerVertex_sortsNumbersAsNumbers() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(a, c)), null));
        assertEquals(3, p.verticesTable.getRowCount());
        assertEquals(Integer.class, p.verticesTable.getModel().getColumnClass(1));
    }

    @Test
    public void matrices_weightedOptionOnlyOnWeightedGraph() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        assertEquals(2, p.matrices.kind.getItemCount());
        p.display(new PropertiesReport(vs(a, b), es(w(und(a, b), 3)), null));
        assertEquals(3, p.matrices.kind.getItemCount());
    }

    @Test
    public void matrices_unreachableIsInfinity_rowHeadersAreNames() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(arc(a, b)), null));
        p.matrices.kind.setSelectedIndex(1);   // Distance
        assertEquals(PanelText.INFINITY, p.matrices.table.getValueAt(1, 0));
        assertEquals("b", p.matrices.rowHeader.getValueAt(1, 0));
    }

    @Test
    public void sameReport_isNotRedisplayed() {
        PropertiesPanel p = panel();
        PropertiesReport r = new PropertiesReport(vs(a), es(), null);
        p.display(r);
        javax.swing.table.TableModel before = p.verticesTable.getModel();
        p.display(r);
        assertSame(before, p.verticesTable.getModel());
    }

    @Test
    public void sideSelector_listsBothSides_onlyWhenBipartite() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        assertEquals(2, p.overview.side.getItemCount());
        assertTrue(p.overview.side.isEnabled());
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(b, c), und(c, a)), null));
        assertFalse(p.overview.side.isEnabled());
    }
}
```

Append to `GraphPropertiesTest`:

```java
    @Test
    public void distributionTooltip_namesTheDegreeAndCount() {
        Vertex a = new Vertex("a", 0, 0), b = new Vertex("b", 0, 0), c = new Vertex("c", 0, 0);
        Vector<Vertex> vs = new Vector<Vertex>(java.util.Arrays.asList(a, b, c));
        Vector<Edge> es = new Vector<Edge>(java.util.Arrays.asList(new Edge(a, b, false)));
        // One chart (degree). Slots 0 and 1; plotW 200 -> slot width 100. Plot starts at x 60, y 22.
        assertEquals("degree 1: 2 vertices (0.67)", GraphProperties.distributionTooltip(vs, es, 0, 0, 200, 60 + 150, 100));
        assertEquals("degree 0: 1 vertex (0.33)", GraphProperties.distributionTooltip(vs, es, 0, 0, 200, 60 + 10, 100));
        assertNull(GraphProperties.distributionTooltip(vs, es, 0, 0, 200, 5, 100));
    }
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Make chart drawing static and add the tooltip.** In `GraphProperties.java`, change `public int drawDegreeDistributions(` to `public static int drawDegreeDistributions(` and `private void drawDistributionChart(` to `private static void drawDistributionChart(`. Neither reads instance fields. The existing test calls them through an instance, which still compiles. Below them add:

```java
    /** Tooltip for the charts drawn at (x, y) with plotW: "degree 3: 4 vertices (0.40)", or null off the bars' columns. */
    public static String distributionTooltip(Vector<Vertex> vList, Vector<Edge> eList, int x, int y, int plotW,
                                             int mx, int my) {
        int n = vList.size();
        if (n == 0 || my < y) return null;
        java.util.List<DegreeDistribution.Kind> kinds = DegreeDistribution.kindsFor(eList);
        int perChart = DIST_TOP_PAD + DIST_PLOT_H + DIST_BOTTOM_PAD + DIST_GAP;
        int i = (my - y) / perChart;
        if (i >= kinds.size()) return null;
        int plotY = y + i * perChart + DIST_TOP_PAD;
        int plotX = x + DIST_LEFT_PAD;
        if (my < plotY || my > plotY + DIST_PLOT_H || mx < plotX || mx >= plotX + plotW) return null;
        DegreeDistribution.Kind kind = kinds.get(i);
        int[] counts = DegreeDistribution.counts(vList, kind);
        int d = (int) ((mx - plotX) / ((double) plotW / counts.length));
        if (d < 0 || d >= counts.length) return null;
        String label = kind == DegreeDistribution.Kind.DEGREE ? "degree"
                : kind == DegreeDistribution.Kind.IN_DEGREE ? "in-degree" : "out-degree";
        return label + " " + d + ": " + counts[d] + (counts[d] == 1 ? " vertex" : " vertices")
                + String.format(" (%.2f)", (double) counts[d] / n);
    }
```

- [ ] **Step 4: Create the views.**

`src/graphtheory/DegreeChartsView.java`:

```java
package graphtheory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.JComponent;

/** The Distributions sub-tab: stacked degree charts, with exact counts on hover. */
public class DegreeChartsView extends JComponent {

    static final int PLOT_W = 360, X = 20, Y = 20;

    private Vector<Vertex> vs = new Vector<Vertex>();
    private Vector<Edge> es = new Vector<Edge>();

    public DegreeChartsView() {
        setToolTipText("");   // registers with the ToolTipManager; getToolTipText(MouseEvent) supplies the text
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    public void show(PropertiesReport r) {
        vs = new Vector<Vertex>(r.vertices);
        es = new Vector<Edge>(r.edges);
        setPreferredSize(new Dimension(X + 60 + PLOT_W + 40, Y + GraphProperties.degreeDistributionsHeight(es)));
        revalidate();
        repaint();
    }

    @Override public String getToolTipText(MouseEvent e) {
        return GraphProperties.distributionTooltip(vs, es, X, Y, PLOT_W, e.getX(), e.getY());
    }

    @Override protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        GraphProperties.drawDegreeDistributions(g, vs, es, X, Y, PLOT_W);
    }
}
```

`src/graphtheory/MatricesView.java`:

```java
package graphtheory;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

/** The Matrices sub-tab: one matrix at a time, names frozen on both edges, ∞ = unreachable. */
public class MatricesView extends JPanel {

    static final String ADJACENCY = "Adjacency", DISTANCE = "Distance", WEIGHTED = "Weighted distance";

    final JComboBox<String> kind = new JComboBox<String>();
    final JLabel caption = new JLabel(" ");
    final JTable table = new JTable();
    final JTable rowHeader = new JTable();
    private final String[][] rowNames = { new String[0] };
    private PropertiesReport report;
    private boolean updating;

    public MatricesView() {
        super(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(8, 4));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(kind, BorderLayout.WEST);
        top.add(caption, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);
        rowHeader.setFocusable(false);
        rowHeader.setRowSelectionAllowed(false);
        rowHeader.setBackground(UIManager.getColor("TableHeader.background"));
        TableCopy.install(table, rowNames);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setRowHeaderView(rowHeader);
        add(scroll, BorderLayout.CENTER);

        kind.addActionListener(e -> { if (!updating) showSelected(); });
    }

    public void show(PropertiesReport r) {
        report = r;
        updating = true;
        Object was = kind.getSelectedItem();
        kind.removeAllItems();
        kind.addItem(ADJACENCY);
        kind.addItem(DISTANCE);
        if (r.weightedDistances != null) kind.addItem(WEIGHTED);
        kind.setSelectedItem(was != null && (r.weightedDistances != null || !WEIGHTED.equals(was)) ? was : ADJACENCY);
        updating = false;
        showSelected();
    }

    private void showSelected() {
        if (report == null) return;
        Object k = kind.getSelectedItem();
        final int[][] m = DISTANCE.equals(k) ? report.distances
                : WEIGHTED.equals(k) ? report.weightedDistances : report.adjacency;
        caption.setText(DISTANCE.equals(k) ? "Fewest edges on a walk from row to column. \u221E = unreachable."
                : WEIGHTED.equals(k) ? "Smallest weight of a walk from row to column. \u221E = unreachable."
                : "Edges from row to column. An undirected self-loop counts 2.");
        final String[] names = new String[report.vertices.size()];
        for (int i = 0; i < names.length; i++) names[i] = report.vertices.get(i).name;
        rowNames[0] = names;

        table.setModel(new AbstractTableModel() {
            public int getRowCount() { return names.length; }
            public int getColumnCount() { return names.length; }
            public String getColumnName(int c) { return names[c]; }
            public Object getValueAt(int r, int c) {
                return m[r][c] == GraphMatrices.UNREACHABLE ? PanelText.INFINITY : String.valueOf(m[r][c]);
            }
        });
        rowHeader.setModel(new AbstractTableModel() {
            public int getRowCount() { return names.length; }
            public int getColumnCount() { return 1; }
            public Object getValueAt(int r, int c) { return names[r]; }
        });

        FontMetrics fm = table.getFontMetrics(table.getFont());
        int widest = fm.stringWidth(PanelText.INFINITY);
        for (int i = 0; i < names.length; i++) {
            widest = Math.max(widest, fm.stringWidth(names[i]));
            for (int j = 0; j < names.length; j++) widest = Math.max(widest, fm.stringWidth(String.valueOf(m[i][j])));
        }
        int colW = widest + 16;
        for (int c = 0; c < table.getColumnCount(); c++) table.getColumnModel().getColumn(c).setPreferredWidth(colW);
        rowHeader.getColumnModel().getColumn(0).setPreferredWidth(colW);
        rowHeader.setPreferredScrollableViewportSize(new java.awt.Dimension(colW, 0));
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
}
```

`src/graphtheory/OverviewView.java`:

```java
package graphtheory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** The Overview sub-tab: the graph with its minimum cuts, the summary, and the stable-matching controls. */
public class OverviewView extends JPanel {

    static final int THUMB_W = 400, THUMB_H = 300;

    final JEditorPane summary = new JEditorPane("text/html", "");
    final JComboBox<String> side = new JComboBox<String>();
    final JButton editPreferences = new JButton("Edit preferences\u2026");
    private final Thumbnail thumbnail = new Thumbnail();
    private List<List<Vertex>> sides;
    private boolean updating;

    public OverviewView(final PropertiesPanel.Listener listener) {
        super(new BorderLayout(8, 8));
        summary.setEditable(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Stable matching \u2014 proposing side:"));
        controls.add(side);
        controls.add(editPreferences);

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(summary), BorderLayout.CENTER);
        right.add(controls, BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout());
        left.add(thumbnail, BorderLayout.NORTH);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        side.addActionListener(e -> {
            int i = side.getSelectedIndex();
            if (!updating && sides != null && i >= 0 && !sides.get(i).isEmpty()) listener.proposerChosen(sides.get(i).get(0));
        });
        editPreferences.addActionListener(e -> listener.editPreferences());
    }

    public void show(PropertiesReport r) {
        thumbnail.report = r;
        thumbnail.repaint();
        summary.setText(SummaryText.html(SummaryText.sections(r)));
        summary.setCaretPosition(0);

        updating = true;
        side.removeAllItems();
        sides = r.sides;
        boolean usable = sides != null && !sides.get(0).isEmpty() && !sides.get(1).isEmpty();
        if (sides != null) {
            for (List<Vertex> s : sides) side.addItem(SummaryText.vertexSet(s));
            side.setSelectedIndex(sides.get(1).contains(r.proposer) ? 1 : 0);
        }
        side.setEnabled(usable);
        editPreferences.setEnabled(!r.vertices.isEmpty());
        updating = false;
    }

    /** The graph fitted into a box, with the minimum vertex and edge cuts dashed in magenta. */
    private static final class Thumbnail extends JComponent {
        PropertiesReport report;

        Thumbnail() { setPreferredSize(new Dimension(THUMB_W, THUMB_H)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (report != null) {
                    g.transform(GraphRenderer.fit(report.vertices, getWidth(), getHeight(), 40));
                    GraphRenderer.Options o = new GraphRenderer.Options();
                    o.cutVertices = new HashSet<Vertex>(report.vertexCut.members);
                    o.cutEdges = new HashSet<Edge>(report.edgeCut.members);
                    GraphRenderer.paint(g, report.vertices, report.edges, o);
                }
            } finally {
                g.dispose();
            }
            g0.setColor(Color.BLACK);
            g0.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
```

`src/graphtheory/PropertiesPanel.java`:

```java
package graphtheory;

import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The Properties tab (CONTEXT.md, Display Conventions): Overview, Vertices, Matrices and
 * Distributions. display() does nothing when given the report it already shows.
 */
public class PropertiesPanel extends JTabbedPane {

    public interface Listener {
        /** The user picked the side containing p to propose. */
        void proposerChosen(Vertex p);

        void editPreferences();
    }

    final OverviewView overview;
    final JTable verticesTable = new JTable();
    final MatricesView matrices = new MatricesView();
    final DegreeChartsView charts = new DegreeChartsView();
    private PropertiesReport shown;

    public PropertiesPanel(Listener listener) {
        overview = new OverviewView(listener);
        verticesTable.setAutoCreateRowSorter(true);
        verticesTable.setCellSelectionEnabled(true);
        TableCopy.install(verticesTable, null);
        addTab("Overview", overview);
        addTab("Vertices", new JScrollPane(verticesTable));
        addTab("Matrices", matrices);
        addTab("Distributions", new JScrollPane(charts));
        setPreferredSize(new Dimension(800, 600));
    }

    public void display(PropertiesReport r) {
        if (r == shown) return;
        shown = r;
        overview.show(r);
        Object[][] rows = new Object[r.vertices.size()][];
        for (int i = 0; i < rows.length; i++) rows[i] = PanelText.verticesRow(r.vertices.get(i), r.vertices, r.edges);
        verticesTable.setModel(new DefaultTableModel(rows, PanelText.VERTICES_COLUMNS) {
            @Override public Class<?> getColumnClass(int c) { return c >= 1 && c <= 3 ? Integer.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        matrices.show(r);
        charts.show(r);
    }
}
```

The `JEditorPane` handles Ctrl+C itself when it has focus and text is selected. `JTable`s handle it through `TableCopy`. Both are `WHEN_FOCUSED` bindings, so they run before menu accelerators.

- [ ] **Step 5: Add `graphtheory.PropertiesPanelTest`, run all** — pass.

- [ ] **Step 6: Render each sub-tab to PNG headlessly and look at it.** Write a throwaway class in the scratchpad (not committed) that builds a `PropertiesPanel` for a 15-vertex mixed weighted graph with parallel edges, calls `display`, sizes it to 1100×750, selects each tab, does `doLayout()` recursively and `printAll` into a `BufferedImage`, and writes `overview.png`, `vertices.png`, `matrices.png`, `distributions.png`. Read the PNGs. Check: headers grey, no red; matrix columns wide enough; summary wraps; nothing cut off with `...`.

- [ ] **Step 7: Commit** — `feat: Properties sub-tabs as Swing components (Overview, Vertices, Matrices, Distributions)`

---

### Task 14: Preference editor and dialog

**Files:**
- Create: `src/graphtheory/PreferenceEditor.java`, `src/graphtheory/PreferencesDialog.java`
- Test: `test/graphtheory/PreferenceEditorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
public class PreferenceEditorTest {
    // helpers v, und, vs, es

    private final Vertex a = v("a"), b = v("b"), c = v("c"), d = v("d");

    @Test
    public void onlyVerticesWithNeighbours_areListed() {
        PreferenceEditor ed = new PreferenceEditor(vs(a, b, c, d), es(und(a, b), und(a, c)));
        assertEquals(Arrays.asList(a, b, c), ed.vertices());
    }

    @Test
    public void create_reorder_apply() {
        List<Vertex> all = vs(a, b, c);
        PreferenceEditor ed = new PreferenceEditor(all, es(und(a, b), und(a, c)));
        assertNull(ed.listOf(a));
        ed.create(a);
        assertEquals(Arrays.asList(b, c), ed.listOf(a));
        assertEquals(0, ed.moveUp(a, 1));
        assertEquals(Arrays.asList(c, b), ed.listOf(a));
        assertTrue(ed.changed());
        assertNull(a.preferences);   // nothing touches the graph before apply
        ed.apply();
        assertEquals(Arrays.asList(c, b), a.preferences);
    }

    @Test
    public void clear_andNoChange() {
        a.preferences = new Vector<Vertex>(Arrays.asList(b));
        PreferenceEditor ed = new PreferenceEditor(vs(a, b), es(und(a, b)));
        assertFalse(ed.changed());
        assertEquals(0, ed.moveUp(a, 0));
        assertFalse(ed.changed());
        ed.clear(a);
        assertTrue(ed.changed());
        ed.apply();
        assertNull(a.preferences);
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** `src/graphtheory/PreferenceEditor.java`:

```java
package graphtheory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/** A working copy of the preference lists; apply() writes it into the vertices in one go. */
public class PreferenceEditor {

    private final List<Vertex> vertices = new ArrayList<Vertex>();
    private final List<Edge> edges;
    private final Map<Vertex, List<Vertex>> lists = new HashMap<Vertex, List<Vertex>>();
    private final Map<Vertex, List<Vertex>> original = new HashMap<Vertex, List<Vertex>>();

    public PreferenceEditor(List<Vertex> vs, List<Edge> es) {
        edges = es;
        for (Vertex v : vs) {
            if (PreferenceLists.neighboursOf(v, es).isEmpty()) continue;
            vertices.add(v);
            List<Vertex> copy = v.preferences == null ? null : new ArrayList<Vertex>(v.preferences);
            lists.put(v, copy);
            original.put(v, copy == null ? null : new ArrayList<Vertex>(copy));
        }
    }

    /** Vertices that can have a list (those with a neighbour), in vertex order. */
    public List<Vertex> vertices() { return vertices; }

    /** null = no list. */
    public List<Vertex> listOf(Vertex v) { return lists.get(v); }

    /** A list of v's neighbours in the order of their first edge. */
    public void create(Vertex v) { lists.put(v, new ArrayList<Vertex>(PreferenceLists.neighboursOf(v, edges))); }

    public void clear(Vertex v) { lists.put(v, null); }

    /** Moves entry i up one place; returns its new index. */
    public int moveUp(Vertex v, int i) { return swap(v, i, i - 1); }

    /** Moves entry i down one place; returns its new index. */
    public int moveDown(Vertex v, int i) { return swap(v, i, i + 1); }

    private int swap(Vertex v, int i, int j) {
        List<Vertex> l = lists.get(v);
        if (l == null || j < 0 || j >= l.size()) return i;
        Vertex t = l.get(i);
        l.set(i, l.get(j));
        l.set(j, t);
        return j;
    }

    public boolean changed() {
        for (Vertex v : vertices) {
            List<Vertex> now = lists.get(v), was = original.get(v);
            if (now == null ? was != null : !now.equals(was)) return true;
        }
        return false;
    }

    public void apply() {
        for (Vertex v : vertices) {
            List<Vertex> l = lists.get(v);
            v.preferences = l == null ? null : new Vector<Vertex>(l);
        }
    }
}
```

`src/graphtheory/PreferencesDialog.java`:

```java
package graphtheory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** Edit ▸ Preference Lists…: create, reorder or clear each vertex's preference list. */
public class PreferencesDialog extends JDialog {

    private final PreferenceEditor editor;
    private final JList<Vertex> vertexList;
    private final DefaultListModel<Vertex> ranking = new DefaultListModel<Vertex>();
    private final JList<Vertex> rankingList = new JList<Vertex>(ranking);
    private final JButton create = new JButton("Create list"), clear = new JButton("Clear list");
    private final JButton up = new JButton("Up"), down = new JButton("Down");
    private boolean ok;

    /** Shows the dialog modally; true if the user pressed OK and changed something (the editor is then applied by the caller). */
    public static boolean edit(Window owner, PreferenceEditor editor) {
        PreferencesDialog d = new PreferencesDialog(owner, editor);
        d.setVisible(true);
        return d.ok && editor.changed();
    }

    private PreferencesDialog(Window owner, PreferenceEditor editor) {
        super(owner, "Preference Lists", ModalityType.APPLICATION_MODAL);
        this.editor = editor;
        DefaultListModel<Vertex> vm = new DefaultListModel<Vertex>();
        for (Vertex v : editor.vertices()) vm.addElement(v);
        vertexList = new JList<Vertex>(vm);
        vertexList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vertexList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean sel, boolean f) {
                Vertex v = (Vertex) value;
                String mark = PreferencesDialog.this.editor.listOf(v) == null ? "  (no list)" : "  \u2713";
                return super.getListCellRendererComponent(l, v.name + mark, i, sel, f);
            }
        });
        rankingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rankingList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean sel, boolean f) {
                return super.getListCellRendererComponent(l, (i + 1) + ". " + ((Vertex) value).name, i, sel, f);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(create);
        buttons.add(clear);
        buttons.add(up);
        buttons.add(down);
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.add(new JLabel("Most preferred first"), BorderLayout.NORTH);
        right.add(new JScrollPane(rankingList), BorderLayout.CENTER);
        right.add(buttons, BorderLayout.EAST);

        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centre.add(new JScrollPane(vertexList));
        centre.add(right);

        JButton okButton = new JButton("OK"), cancel = new JButton("Cancel");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(okButton);
        bottom.add(cancel);

        getContentPane().add(centre, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        vertexList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showRanking(0); });
        create.addActionListener(e -> { editor.create(selected()); showRanking(0); vertexList.repaint(); });
        clear.addActionListener(e -> { editor.clear(selected()); showRanking(-1); vertexList.repaint(); });
        up.addActionListener(e -> showRanking(editor.moveUp(selected(), rankingList.getSelectedIndex())));
        down.addActionListener(e -> showRanking(editor.moveDown(selected(), rankingList.getSelectedIndex())));
        okButton.addActionListener(e -> { ok = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        if (!editor.vertices().isEmpty()) vertexList.setSelectedIndex(0);
        showRanking(0);
        setSize(480, 360);
        setLocationRelativeTo(owner);
    }

    private Vertex selected() { return vertexList.getSelectedValue(); }

    private void showRanking(int select) {
        ranking.clear();
        Vertex v = selected();
        List<Vertex> l = v == null ? null : editor.listOf(v);
        if (l != null) for (Vertex p : l) ranking.addElement(p);
        if (select >= 0 && select < ranking.size()) rankingList.setSelectedIndex(select);
        create.setEnabled(v != null && l == null);
        clear.setEnabled(l != null);
        up.setEnabled(l != null);
        down.setEnabled(l != null);
    }
}
```

- [ ] **Step 4: Add `graphtheory.PreferenceEditorTest`, run all** — pass.

- [ ] **Step 5: Commit** — `feat: preference lists dialog`

---

### Task 15: Wire it into Canvas; remove the old painting

**Files:** Modify `src/graphtheory/Canvas.java`, `src/graphtheory/GraphProperties.java`, `src/graphtheory/SidePanel.java`

- [ ] **Step 1: Fields.** Replace `private JScrollPane propertiesScroll;` and `private JPanel propertiesContent;` with:

```java
    private PropertiesPanel propertiesPanel;
    // The report the Properties tab shows, and the graph text + proposer it was built from.
    private PropertiesReport report;
    private String reportKey;
    // A vertex on the side that proposes for stable matching (analysis; not saved).
    private Vertex proposer;
```

Delete the `THUMB_W`, `THUMB_H` and `DIST_PLOT_W` constants.

- [ ] **Step 2: Snapshots keep lists in step.** Change `snapshot()` to:

```java
    /** The graph as .graph text: the unit of undo, saving and "unsaved changes". Brings preference lists in step first. */
    private String snapshot() {
        PreferenceLists.sync(vertexList, edgeList);
        return GraphFile.write(vertexList, edgeList);
    }
```

- [ ] **Step 3: Build the tab.** In the constructor replace

```java
        buildPropertiesPanel();
        // Without this the tab would take the (huge) preferred size of the properties content.
        propertiesScroll.setPreferredSize(new Dimension(width, height));
```
with
```java
        propertiesPanel = new PropertiesPanel(new PropertiesPanel.Listener() {
            public void proposerChosen(Vertex p) {
                proposer = p;
                computeProperties();
            }

            public void editPreferences() {
                editPreferenceLists();
            }
        });
        propertiesPanel.setPreferredSize(new Dimension(width, height));
```
and `tabs.addTab("Properties", propertiesScroll);` with `tabs.addTab("Properties", propertiesPanel);`.

- [ ] **Step 4: Recompute only on change.** Replace `computeProperties()` with:

```java
    /** Rebuilds the Properties report if the graph or the proposing side changed since the last one. */
    private void computeProperties() {
        recomputeGraphProperties();   // cutpoints, read by the Vertices table
        if (proposer != null && !vertexList.contains(proposer)) proposer = null;
        String key = snapshot() + "\u0000" + (proposer == null ? "" : proposer.name);
        if (!key.equals(reportKey)) {
            report = new PropertiesReport(vertexList, edgeList, proposer);
            reportKey = key;
        }
        propertiesPanel.display(report);
    }
```

Delete `drawThumbnail`, `buildPropertiesPanel` and `refreshPropertiesScrollSize`. In `refresh()` delete the `if (propertiesContent != null) { propertiesContent.repaint(); }` block.

- [ ] **Step 5: Preferences dialog, undoable.** Add:

```java
    private void editPreferenceLists() {
        PreferenceLists.sync(vertexList, edgeList);
        PreferenceEditor editor = new PreferenceEditor(vertexList, edgeList);
        if (editor.vertices().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Add some edges first: a preference list ranks a vertex's neighbours.",
                    "Preference Lists", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!PreferencesDialog.edit(frame, editor)) return;
        String before = snapshot();
        editor.apply();
        afterEdit(before);
        refresh();
    }
```

In `buildMenuBar`, after `addItem(edit, "Remove All", null);` add:

```java
        edit.addSeparator();
        addItem(edit, "Preference Lists...", null);
```

In `MenuListener.actionPerformed`, add a branch: `} else if (command.equals("Preference Lists...")) { editPreferenceLists();`.

- [ ] **Step 6: Ctrl+Shift+C for greedy colouring, with hints.** Change the accelerator:

```java
        addItem(extras, "Show Greedy Coloring", KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
```

In `updateStatus()`, change the Properties hint to:

```java
                : "Ctrl+C copies the selected cells \u00b7 Ctrl+Shift+C colours the graph greedily \u00b7 switch to the Graph tab to edit.");
```

and the Graph-tab hint to `Tools.hint(selectedTool) + "  (Ctrl+Shift+C: greedy colouring)"`. In `SidePanel.java` (comment at ~line 129) change "Ctrl+A/C/V/X fall through to the menu accelerators (Add Vertex, Greedy Coloring, ...)" to "Ctrl+A/V/X fall through to the menu accelerators (Add Vertex, ...); Ctrl+C does nothing here".

- [ ] **Step 7: Focus returns to the canvas.** In `onTabChanged()` add, after `refresh();`:

```java
        if (selectedWindow == 0) canvas.requestFocusInWindow();
```

- [ ] **Step 8: Delete what the report replaced.** In `GraphProperties.java` delete: `adjacencyMatrix`/`weightedAdjacencyMatrix`/`distanceMatrix`/`vpList` fields and `generateAdjacencyMatrix`, `generateDistanceMatrix`, `displayContainers` (the stdout dump), `descendingWidthComparator`, `drawAdjacencyMatrix`, `drawDistanceMatrix`, `drawNodePropertiesTable`, `drawAdjacencyList`, `drawGraphSummary` and every helper only it used (`ensureTraversalSummary`, `invalidateTraversalSummary` and its cache fields, the connectivity/strong-connectivity/simple/bipartite/density/acyclic/forest/tree/star/rooted/empty/complete/complete-bipartite methods, `chromaticNumber`/`canColor`/`areAdjacent`, `formatColoring`, `formatComponents`, all matching methods, `vertexConnectivity`/`edgeConnectivity` and their helpers, `minVertexCut`/`minEdgeCut`/the connectivity value fields, and the summary `yesNo`). **Keep** `computeCutpoints` (+ `dfsAP`, `getAllNeighborIndices`), `greedyColoring`, `clearColoring`, `allNeighborsForColoring`, `vertexColors`, and the degree-distribution drawing. In Canvas remove `gP.invalidateTraversalSummary();` from `markGraphDirty()`. Compile. `javac` errors list anything still referenced, so delete or redirect each one. Remove now-unused imports.

- [ ] **Step 9: Build, run all tests, start the app** (exit 124, clean stderr).

- [ ] **Step 10: Check the diff.** `git diff --stat` shows only intended files, and no whole-file CRLF churn.

- [ ] **Step 11: Commit** — `feat: Canvas shows the new Properties tab; preference dialog; Ctrl+Shift+C; old painting removed`

---

### Task 16: TODO.md

- [ ] **Step 1:** In `TODO.md`, delete the items for **Properties view as real Swing tables**, **Minimum vertex cut of a complete graph** and **Distance matrix: distance vs weighted distance**. They're done. The repo's TODO keeps only open items, so check how earlier branches handled finished ones (`git log -p TODO.md`) and follow that. Leave zoom/pan, saving walks and Menger.

- [ ] **Step 2: Commit** — `docs: TODO items finished by the Properties tab`

---

### Task 17: Final review and probe

- [ ] **Step 1: Whole-branch review** (Opus) against this plan and CONTEXT.md.

- [ ] **Step 2: Non-headless probe** (scratchpad, not committed). Build the real `Canvas` (`new Canvas(...)` as `Main` does), drive it by reflection on the EDT, and post real `KeyEvent`s with `Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(...)`. Check:
  1. Build a graph with an arc a→b and two parallel {b, c}. Record each vertex's `degree()`, open Properties, switch back, and confirm the degrees are unchanged (Task 1 regression).
  2. On Properties, focus the Matrices table, select two cells, post Ctrl+C. Confirm the clipboard holds TSV with headers and that **no vertex got a `colorId`** (greedy colouring did not run).
  3. Post Ctrl+Shift+C. Vertices now have colours.
  4. Switch to Graph. `canvas.isFocusOwner()` is true, and Backspace/Esc reach the canvas.
  5. Open the preferences dialog via reflection, create a list, OK. Then: undo restores no list, `isModified()` is true after OK, and the file text has `graph-theory 2` and a `prefer` line.
  6. K₄: the Overview thumbnail's `report.vertexCut.members` is empty and the summary says "no vertex cut".

- [ ] **Step 3:** Fix anything found (with a test where possible), re-run everything, then merge the owner's way: `git checkout master && git pull && git merge --no-ff feature/properties-tab && git push`. No PR.

---

## Decisions recorded in the grilling session (2026-10-08)

1. **Scope:** the whole tab. Fix the K_n cut, delete the stdout debug, defer Menger.
2. **Layout:** sub-tabs Overview / Vertices / Matrices / Distributions; the picture is only on Overview.
3. **Vertices:** one sortable table, side-panel labels, yes/no, Self-loop column, Neighbours as `b ×2, c, →d, ←e` (blank for isolated).
4. **Adjacency matrix:** edge counts, row → column, undirected loop 2, directed loop 1 (the course's convention).
5. **Matrices:** a selector shows one at a time; frozen grey headers; weighted distance only on weighted graphs; clicking selects nothing on the Graph tab; equal column widths.
6. **Summary:** Magnitude kept; E lists every edge, with weights on a weighted graph; Connected/κ/λ ignore direction, only Strongly connected respects it; Density only for simple graphs (ordered-pair definition), no Sparse/Dense label; Cyclic respects direction, Forest/Tree ignore it; Star by adjacency; blocks edge-aware with isolated vertices as blocks; χ none with a self-loop; greedy colouring line dropped; all four matchings kept with honest definitions; six sections; no truncation; reasons in brackets.
7. **Stable matching:** preference lists are part of the graph (ADR 0005), rank exactly the neighbours, follow edge edits, edited in a dialog (one undoable edit); the user picks the proposing side (analysis, not saved).
8. **Refresh:** a pure report keyed on graph text + proposer; nothing computed in paint; κ/λ by max-flow; target under 200 ms for 20 vertices and 60 edges.
9. **Keys:** TSV copy with headers; greedy colouring moves to Ctrl+Shift+C, shown in the menu and both status hints; switching to Graph focuses the canvas. No CSV export this round.
