# File I/O and UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the lossy save/load with the `.graph` format, add New/Open/Save/Save As with an unsaved-changes prompt, snapshot undo/redo, vertex renaming, a left tool palette, a status bar and Graph | Properties tabs.

**Architecture:** Pure-logic classes hold everything testable. `GraphFile` reads and writes the format (ADR 0004). `VertexNames` holds the naming rules, `Layout` places and clamps vertices, `EditHistory` stores undo snapshots, and `Tools` is the tool table. `Canvas` wires them to Swing. A **snapshot** is the graph written as `.graph` text. It is the single unit for undo ("the text before the edit"), unsaved changes ("current text ≠ saved text") and saving. Every edit is wrapped as `before = snapshot(); …edit…; afterEdit(before)`. `afterEdit` records undo and updates the title only if the text changed.

**Tech Stack:** Java 17 (source-compatible with Java 7+, so no lambdas or `var`), Swing/AWT, JUnit 4.13.2.

**Read first:** `CONTEXT.md` (sections *Vertex → Name*, *Graph → Graph file* and *Unsaved changes*), `docs/adr/0001-mixed-graph-model.md`, `docs/adr/0002-edge-aware-walks.md` and `docs/adr/0004-graph-file-format.md`. Those definitions are the spec.

---

## How to build and test

There is no Ant/Maven on the PATH. Run from the repo root (`GraphTheory/`) in **Git Bash**:

```bash
CP="C:/Users/Jade/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar;C:/Users/Jade/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
rm -rf out/test && javac -encoding UTF-8 -d out/test -cp "$CP" src/graphtheory/*.java test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -cp "out/test;$CP" org.junit.runner.JUnitCore \
     graphtheory.VertexTest graphtheory.GraphPropertiesTest graphtheory.WalkTest graphtheory.VertexPairTest graphtheory.TraversalsTest \
     graphtheory.EdgeRegistryTest graphtheory.VertexNamesTest graphtheory.GraphFileTest graphtheory.LayoutTest \
     graphtheory.EditHistoryTest graphtheory.ToolsTest graphtheory.FileManagerTest
```

`MSYS_NO_PATHCONV=1` is required. Without it, Git Bash mangles the `;`-separated classpath and you get `ClassNotFoundException: org.hamcrest.SelfDescribing`. **Leave a test class out of the list until the task that creates it**, or JUnitCore fails with "Could not find class". `out/` is git-ignored.

Baseline before starting: **88 tests pass.** The expected total after each task is stated in that task.

Launch the app (after compiling as above):

```bash
MSYS_NO_PATHCONV=1 java -cp out/test graphtheory.Main
```

---

## File Map

| File | Change |
|---|---|
| `src/graphtheory/EdgeRegistry.java` | Weights keyed by `Vertex` identity instead of `name + "->" + name` |
| `src/graphtheory/VertexNames.java` | **New.** Name rules (1–4 of `[A-Za-z0-9_]`, unique), rename check, next free name |
| `src/graphtheory/GraphFile.java` | **New.** Read and write the `.graph` format; line-numbered `FormatException` |
| `src/graphtheory/Layout.java` | **New.** Circle arrangement (from `Canvas.arrangeVertices`) and clamping into the canvas |
| `src/graphtheory/EditHistory.java` | **New.** Bounded undo/redo stacks of snapshot strings |
| `src/graphtheory/Tools.java` | **New.** Tool ids (the existing `selectedTool` numbers), labels, shortcuts, status-bar hints |
| `src/graphtheory/ToolPalette.java` | **New.** Left column of toggle buttons, one per tool |
| `src/graphtheory/FileManager.java` | **Rewritten.** File dialogs (`.graph` filter, overwrite check) and UTF-8 disk read/write |
| `src/graphtheory/Canvas.java` | Window layout (palette, tabs, status bar), menus, file commands, undo, rename |
| `src/graphtheory/Main.java` | App name "Graph Theory Visualizer" |
| `test/graphtheory/*Test.java` | **New:** `EdgeRegistryTest`, `VertexNamesTest`, `GraphFileTest`, `LayoutTest`, `EditHistoryTest`, `ToolsTest`, `FileManagerTest` |

`Canvas.java` is about 1350 lines. Steps that touch it name the method or `case` to change and quote the code being replaced, because line numbers shift between tasks.

---

## Task 0: Branch and baseline

- [ ] **Step 1: Create the feature branch**

```bash
git checkout master && git pull && git checkout -b feature/file-io-ux
```

- [ ] **Step 2: Commit the docs that define this feature**

```bash
git add CONTEXT.md TODO.md docs/adr/0004-graph-file-format.md docs/superpowers/plans/2026-10-05-file-io-and-ux.md
git commit -m "docs: vertex names, graph files, unsaved changes; ADR 0004; file I/O + UX plan

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 3: Confirm the baseline**

Run the build-and-test command, listing only the five existing test classes. Expected: `OK (88 tests)`.

---

## Task 1: EdgeRegistry keyed by vertex, not name

The registry builds keys as `a.name + "->" + b.name`. Two vertices with the same name share weights, and names containing `->` collide (`"a->"+"b"` = `"a"+"->b"`). Renaming makes duplicate names possible during an edit, so key by the `Vertex` object. `Vertex` doesn't override `equals`/`hashCode`, so a `HashMap` compares by identity.

**Files:**
- Modify: `src/graphtheory/EdgeRegistry.java` (whole file)
- Test: `test/graphtheory/EdgeRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class EdgeRegistryTest {

    private static Edge edge(Vertex a, Vertex b, boolean directed, int weight) {
        Edge e = new Edge(a, b, directed);
        e.weight = weight;
        return e;
    }

    @Test
    public void verticesWithTheSameName_keepTheirOwnWeights() {
        Vertex x1 = new Vertex("x", 0, 0);
        Vertex x2 = new Vertex("x", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(x1, b, true, 5));
        es.add(edge(x2, b, true, 9));
        EdgeRegistry.rebuild(es);
        assertEquals(5, EdgeRegistry.weightOf(x1, b));
        assertEquals(9, EdgeRegistry.weightOf(x2, b));
    }

    @Test
    public void arrowInsideNames_doesNotCollide() {
        Vertex p = new Vertex("a->", 0, 0);
        Vertex q = new Vertex("b", 0, 0);
        Vertex r = new Vertex("a", 0, 0);
        Vertex s = new Vertex("->b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(p, q, true, 5));
        EdgeRegistry.rebuild(es);
        assertEquals(-1, EdgeRegistry.weightOf(r, s));
    }

    @Test
    public void undirectedEdge_givesBothDirections_butAnEarlierArcKeepsPriority() {
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        Vector<Edge> es = new Vector<Edge>();
        es.add(edge(a, b, true, 3));
        es.add(edge(a, b, false, 7));
        EdgeRegistry.rebuild(es);
        assertEquals(3, EdgeRegistry.weightOf(a, b));
        assertEquals(7, EdgeRegistry.weightOf(b, a));
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Run the build-and-test command with `graphtheory.EdgeRegistryTest` added. Expected: `verticesWithTheSameName…` and `arrowInsideNames…` fail. The third test already passes and pins down the behaviour that must not change.

- [ ] **Step 3: Rewrite `EdgeRegistry`**

```java
package graphtheory;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Static lookup table mapping (u, v) → weight for the current graph.
 * Rebuilt by Canvas.refresh() on every redraw, so it is always in sync
 * with the current edgeList. Used by VertexPair for weighted Dijkstra.
 * Keyed by Vertex identity, so names never matter.
 */
public class EdgeRegistry {

    private static Map<Vertex, Map<Vertex, Integer>> weights = new HashMap<Vertex, Map<Vertex, Integer>>();

    /** Rebuild the lookup from the current edge list. */
    public static void rebuild(Vector<Edge> edgeList) {
        weights.clear();
        if (edgeList == null) return;
        for (Edge e : edgeList) {
            if (e == null || e.vertex1 == null || e.vertex2 == null) continue;

            if (e.directed) {
                // Directed arc: only u → v gets the weight.
                put(e.vertex1, e.vertex2, e.weight);
            } else {
                // Undirected: both u → v and v → u get the same weight.
                // A previously-registered directed arc keeps priority.
                if (!has(e.vertex1, e.vertex2)) put(e.vertex1, e.vertex2, e.weight);
                if (!has(e.vertex2, e.vertex1)) put(e.vertex2, e.vertex1, e.weight);
            }
        }
    }

    /** Weight of u → v, or -1 if there is no such edge. */
    public static int weightOf(Vertex u, Vertex v) {
        if (u == null || v == null) return -1;
        Map<Vertex, Integer> out = weights.get(u);
        Integer w = out == null ? null : out.get(v);
        return w == null ? -1 : w;
    }

    private static boolean has(Vertex u, Vertex v) {
        Map<Vertex, Integer> out = weights.get(u);
        return out != null && out.containsKey(v);
    }

    private static void put(Vertex u, Vertex v, int w) {
        Map<Vertex, Integer> out = weights.get(u);
        if (out == null) {
            out = new HashMap<Vertex, Integer>();
            weights.put(u, out);
        }
        out.put(v, w);
    }
}
```

- [ ] **Step 4: Run all tests**

Expected: `OK (91 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/EdgeRegistry.java test/graphtheory/EdgeRegistryTest.java
git commit -m "fix: key edge weights by vertex identity, not name

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 2: Vertex name rules

**Files:**
- Create: `src/graphtheory/VertexNames.java`
- Modify: `src/graphtheory/Canvas.java` (`nextAvailableVertexName` and its caller in `mouseClicked` `case 1`)
- Test: `test/graphtheory/VertexNamesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class VertexNamesTest {

    private static Vector<Vertex> named(String... names) {
        Vector<Vertex> vs = new Vector<Vertex>();
        for (String n : names) vs.add(new Vertex(n, 0, 0));
        return vs;
    }

    @Test
    public void isValid_acceptsShortLabels() {
        for (String n : new String[] {"a", "v1", "u_2", "ABCD", "0", "_"}) {
            assertTrue(n, VertexNames.isValid(n));
        }
    }

    @Test
    public void isValid_rejectsEverythingElse() {
        for (String n : new String[] {"", "abcde", "a b", "a-b", "a#", "\u00e9", " a"}) {
            assertFalse(n, VertexNames.isValid(n));
        }
        assertFalse(VertexNames.isValid(null));
    }

    @Test
    public void checkRename_acceptsAFreeName() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(0), "c", vs));
    }

    @Test
    public void checkRename_allowsKeepingTheSameName() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(0), "a", vs));
    }

    @Test
    public void checkRename_rejectsANameInUse() {
        Vector<Vertex> vs = named("a", "b");
        assertEquals("'b' is already used by another vertex.",
                     VertexNames.checkRename(vs.get(0), "b", vs));
    }

    @Test
    public void checkRename_isCaseSensitive() {
        Vector<Vertex> vs = named("a", "b");
        assertNull(VertexNames.checkRename(vs.get(1), "A", vs));
    }

    @Test
    public void checkRename_rejectsAnInvalidName() {
        Vector<Vertex> vs = named("a");
        assertEquals("Names are 1-4 letters, digits or _.",
                     VertexNames.checkRename(vs.get(0), "toolong", vs));
    }

    @Test
    public void nextFree_fillsTheLowestGap() {
        assertEquals("2", VertexNames.nextFree(named("0", "1", "3", "x")));
    }

    @Test
    public void nextFree_emptyGraphStartsAtZero() {
        assertEquals("0", VertexNames.nextFree(named()));
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: VertexNames`.

- [ ] **Step 3: Create `VertexNames`**

```java
package graphtheory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** The rules for vertex names (CONTEXT.md, Vertex → Name). */
public final class VertexNames {

    /** Human-readable rule, used in dialogs and file errors. */
    public static final String RULE = "1-4 letters, digits or _";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{1,4}");

    private VertexNames() {}

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    /** Why v can't be renamed to newName, or null if it can. */
    public static String checkRename(Vertex v, String newName, List<Vertex> all) {
        if (!isValid(newName)) return "Names are " + RULE + ".";
        for (Vertex other : all) {
            if (other != v && other.name.equals(newName)) {
                return "'" + newName + "' is already used by another vertex.";
            }
        }
        return null;
    }

    /** The lowest unused number, as a name: "0", "1", … */
    public static String nextFree(List<Vertex> all) {
        Set<String> used = new HashSet<String>();
        for (Vertex v : all) used.add(v.name);
        int i = 0;
        while (used.contains("" + i)) i++;
        return "" + i;
    }
}
```

- [ ] **Step 4: Use it in `Canvas`**

Delete the whole `private String nextAvailableVertexName() { … }` method. In `InputListener.mouseClicked`, `case 1`, replace

```java
                        String name = nextAvailableVertexName();
```

with

```java
                        String name = VertexNames.nextFree(vertexList);
```

- [ ] **Step 5: Run all tests**

Expected: `OK (100 tests)`.

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/VertexNames.java src/graphtheory/Canvas.java test/graphtheory/VertexNamesTest.java
git commit -m "feat: vertex name rules (1-4 chars, unique)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 3: GraphFile, writing and reading valid files

Format (ADR 0004):

```
graph-theory 1
vertex <name> [<x> <y>] [root]
edge <a> <b> [<weight>]      # undirected
arc  <a> <b> [<weight>]      # directed a → b
```

`#` starts a comment. Blank lines are ignored. `write` always produces the full form: coordinates on every vertex and a weight on every edge. `read` builds the neighbour lists exactly as the editor does (`Canvas` `mouseReleased`, cases 2 and 5). For an undirected edge, each end gets the other once, and a self-loop is added once. For an arc, `a.outNeighbors += b` and `b.inNeighbors += a`.

**Files:**
- Create: `src/graphtheory/GraphFile.java`
- Test: `test/graphtheory/GraphFileTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class GraphFileTest {

    static String file(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    static GraphFile.Data read(String... lines) throws GraphFile.FormatException {
        return GraphFile.read(file(lines));
    }

    @Test
    public void write_emptyGraph_isJustTheHeader() {
        assertEquals("graph-theory 1\n", GraphFile.write(new Vector<Vertex>(), new Vector<Edge>()));
    }

    @Test
    public void write_alwaysUsesTheFullForm() {
        Vertex a = new Vertex("a", 120, 200);
        a.isRoot = true;
        Vertex b = new Vertex("b", 300, 200);
        Vector<Vertex> vs = new Vector<Vertex>();
        vs.add(a);
        vs.add(b);
        Edge ab = new Edge(a, b, false);
        Edge ba = new Edge(b, a, true);
        ba.weight = 3;
        Vector<Edge> es = new Vector<Edge>();
        es.add(ab);
        es.add(ba);
        assertEquals(file("graph-theory 1",
                          "vertex a 120 200 root",
                          "vertex b 300 200",
                          "edge a b 1",
                          "arc b a 3"),
                     GraphFile.write(vs, es));
    }

    @Test
    public void roundTrip_mixedGraphWithParallelEdgesAndSelfLoops() throws Exception {
        String text = file("graph-theory 1",
                           "vertex a 120 200 root",
                           "vertex b 300 200",
                           "vertex c 210 340",
                           "edge a b 1",
                           "edge a b 5",
                           "arc a b 2",
                           "arc b c 3",
                           "edge c c 1",
                           "arc a a 4");
        GraphFile.Data d = GraphFile.read(text);
        assertEquals(text, GraphFile.write(d.vertices, d.edges));
        assertTrue(d.unplaced.isEmpty());
    }

    @Test
    public void read_buildsNeighbourListsLikeTheEditor() throws Exception {
        GraphFile.Data d = read("graph-theory 1",
                                "vertex a 0 0",
                                "vertex b 0 0",
                                "edge a b",
                                "arc b a",
                                "edge a a",
                                "arc b b");
        Vertex a = d.vertices.get(0);
        Vertex b = d.vertices.get(1);
        assertEquals(3, a.degree());      // {a,b} + undirected self-loop (2)
        assertEquals(1, a.inDegree());    // (b,a)
        assertEquals(0, a.outDegree());
        assertEquals(1, b.degree());
        assertEquals(1, b.inDegree());    // (b,b)
        assertEquals(2, b.outDegree());   // (b,a), (b,b)
    }

    @Test
    public void read_handWrittenForm_defaultsWeightAndListsUnplacedVertices() throws Exception {
        GraphFile.Data d = read("# a small example",
                                "graph-theory 1",
                                "",
                                "vertex a        # no position",
                                "vertex b 10 20",
                                "vertex c",
                                "edge a b",
                                "arc c a 7");
        assertEquals(3, d.vertices.size());
        assertEquals(2, d.unplaced.size());
        assertSame(d.vertices.get(0), d.unplaced.get(0));
        assertSame(d.vertices.get(2), d.unplaced.get(1));
        assertEquals(10, d.vertices.get(1).location.x);
        assertEquals(20, d.vertices.get(1).location.y);
        assertFalse(d.edges.get(0).directed);
        assertEquals(1, d.edges.get(0).weight);
        assertTrue(d.edges.get(1).directed);
        assertEquals(7, d.edges.get(1).weight);
    }

    @Test
    public void read_acceptsWindowsLineEndings() throws Exception {
        GraphFile.Data d = GraphFile.read("graph-theory 1\r\nvertex a 1 2\r\n");
        assertEquals("a", d.vertices.get(0).name);
        assertEquals(2, d.vertices.get(0).location.y);
    }

    @Test
    public void read_rootWithAndWithoutPosition() throws Exception {
        GraphFile.Data d = read("graph-theory 1", "vertex a root", "vertex b 5 6 root");
        assertTrue(d.vertices.get(0).isRoot);
        assertTrue(d.vertices.get(1).isRoot);   // separate components: no edge between them
        assertEquals(1, d.unplaced.size());
    }

    @Test
    public void read_aVertexMayBeNamedRoot() throws Exception {
        GraphFile.Data d = read("graph-theory 1", "vertex root");
        assertEquals("root", d.vertices.get(0).name);
        assertFalse(d.vertices.get(0).isRoot);
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: GraphFile`.

- [ ] **Step 3: Create `GraphFile`**

```java
package graphtheory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Reads and writes the .graph text format (ADR 0004):
 *
 *   graph-theory 1
 *   vertex <name> [<x> <y>] [root]
 *   edge <a> <b> [<weight>]      undirected
 *   arc  <a> <b> [<weight>]      directed a → b
 *
 * '#' starts a comment; blank lines are ignored.
 */
public final class GraphFile {

    public static final String HEADER = "graph-theory 1";

    private GraphFile() {}

    /** A problem in a graph file; the message starts with "Line N: ". */
    public static class FormatException extends Exception {
        public final int line;

        public FormatException(int line, String message) {
            super("Line " + line + ": " + message);
            this.line = line;
        }
    }

    /** A graph read from a file. unplaced = vertices declared without coordinates (left at 0,0). */
    public static class Data {
        public final Vector<Vertex> vertices = new Vector<Vertex>();
        public final Vector<Edge> edges = new Vector<Edge>();
        public final Vector<Vertex> unplaced = new Vector<Vertex>();
    }

    /** The full form: every vertex with coordinates, every edge with its weight. */
    public static String write(List<Vertex> vertices, List<Edge> edges) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (Vertex v : vertices) {
            sb.append("vertex ").append(v.name)
              .append(' ').append(v.location.x)
              .append(' ').append(v.location.y);
            if (v.isRoot) sb.append(" root");
            sb.append('\n');
        }
        for (Edge e : edges) {
            sb.append(e.directed ? "arc " : "edge ")
              .append(e.vertex1.name).append(' ')
              .append(e.vertex2.name).append(' ')
              .append(e.weight).append('\n');
        }
        return sb.toString();
    }

    public static Data read(String text) throws FormatException {
        Data data = new Data();
        Map<String, Vertex> byName = new HashMap<String, Vertex>();
        Map<Vertex, Integer> rootLine = new HashMap<Vertex, Integer>();
        String[] lines = text.split("\r?\n", -1);
        boolean sawHeader = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = lines[i];
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] t = line.split("\\s+");

            if (!sawHeader) {
                if (!t[0].equals("graph-theory")) {
                    throw new FormatException(lineNo, "not a graph file (expected '" + HEADER + "')");
                }
                if (t.length != 2 || !t[1].equals("1")) {
                    throw new FormatException(lineNo, "unsupported version (this app reads '" + HEADER + "')");
                }
                sawHeader = true;
            } else if (t[0].equals("vertex")) {
                readVertex(t, lineNo, data, byName, rootLine);
            } else if (t[0].equals("edge") || t[0].equals("arc")) {
                readEdge(t, lineNo, data, byName);
            } else {
                throw new FormatException(lineNo,
                        "unknown keyword '" + t[0] + "' (expected vertex, edge or arc)");
            }
        }
        if (!sawHeader) throw new FormatException(1, "empty file (expected '" + HEADER + "')");
        checkRoots(data, rootLine);
        return data;
    }

    private static void readVertex(String[] t, int lineNo, Data data,
                                   Map<String, Vertex> byName, Map<Vertex, Integer> rootLine)
            throws FormatException {
        int n = t.length;
        boolean root = n > 2 && t[n - 1].equals("root");
        if (root) n--;
        if (n != 2 && n != 4) {
            throw new FormatException(lineNo, "expected 'vertex <name> [<x> <y>] [root]'");
        }
        String name = t[1];
        if (!VertexNames.isValid(name)) {
            throw new FormatException(lineNo,
                    "invalid vertex name '" + name + "' (" + VertexNames.RULE + ")");
        }
        if (byName.containsKey(name)) {
            throw new FormatException(lineNo, "vertex '" + name + "' is already declared");
        }
        Vertex v;
        if (n == 4) {
            v = new Vertex(name, parseInt(t[2], lineNo), parseInt(t[3], lineNo));
        } else {
            v = new Vertex(name, 0, 0);
            data.unplaced.add(v);
        }
        v.isRoot = root;
        if (root) rootLine.put(v, lineNo);
        byName.put(name, v);
        data.vertices.add(v);
    }

    private static void readEdge(String[] t, int lineNo, Data data, Map<String, Vertex> byName)
            throws FormatException {
        if (t.length != 3 && t.length != 4) {
            throw new FormatException(lineNo, "expected '" + t[0] + " <from> <to> [<weight>]'");
        }
        Vertex a = lookup(t[1], lineNo, byName);
        Vertex b = lookup(t[2], lineNo, byName);
        boolean directed = t[0].equals("arc");
        Edge e = new Edge(a, b, directed);
        if (t.length == 4) {
            int w = parseInt(t[3], lineNo);
            if (w < 0) throw new FormatException(lineNo, "weight must not be negative");
            e.weight = w;
        }
        if (directed) {
            a.outNeighbors.add(b);
            b.inNeighbors.add(a);
        } else {
            a.addUndirectedNeighbor(b);
            if (a != b) b.addUndirectedNeighbor(a);
        }
        data.edges.add(e);
    }

    private static Vertex lookup(String name, int lineNo, Map<String, Vertex> byName)
            throws FormatException {
        Vertex v = byName.get(name);
        if (v == null) {
            throw new FormatException(lineNo,
                    "unknown vertex '" + name + "' (declare it with a vertex line first)");
        }
        return v;
    }

    private static int parseInt(String s, int lineNo) throws FormatException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new FormatException(lineNo, "'" + s + "' is not a whole number");
        }
    }

    /** At most one root per connected component (edges taken as undirected). */
    private static void checkRoots(Data data, Map<Vertex, Integer> rootLine) throws FormatException {
        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        for (Vertex v : data.vertices) parent.put(v, v);
        for (Edge e : data.edges) parent.put(find(parent, e.vertex1), find(parent, e.vertex2));

        Map<Vertex, Vertex> rootOfComponent = new HashMap<Vertex, Vertex>();
        for (Vertex v : data.vertices) {
            if (!v.isRoot) continue;
            Vertex component = find(parent, v);
            Vertex earlier = rootOfComponent.get(component);
            if (earlier != null) {
                throw new FormatException(rootLine.get(v), "'" + v.name
                        + "' is a second root in the same component as '" + earlier.name + "'");
            }
            rootOfComponent.put(component, v);
        }
    }

    private static Vertex find(Map<Vertex, Vertex> parent, Vertex v) {
        while (parent.get(v) != v) v = parent.get(v);
        return v;
    }
}
```

- [ ] **Step 4: Run all tests**

Run with `graphtheory.GraphFileTest` added. Expected: `OK (108 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/GraphFile.java test/graphtheory/GraphFileTest.java
git commit -m "feat: .graph file format reader and writer (ADR 0004)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 4: GraphFile, rejecting invalid files

The reader from Task 3 already throws these errors. This task checks every message, because these strings are what users see in the "Couldn't open" dialog.

**Files:**
- Modify: `test/graphtheory/GraphFileTest.java`

- [ ] **Step 1: Add the error tests**

Add this helper and these tests to `GraphFileTest`:

```java
    static void assertRejected(String expectedMessage, String... lines) {
        try {
            read(lines);
            fail("expected FormatException: " + expectedMessage);
        } catch (GraphFile.FormatException ex) {
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void rejects_emptyFile() {
        assertRejected("Line 1: empty file (expected 'graph-theory 1')", "", "# nothing here");
    }

    @Test
    public void rejects_missingHeader() {
        assertRejected("Line 1: not a graph file (expected 'graph-theory 1')", "vertex a");
    }

    @Test
    public void rejects_otherVersion() {
        assertRejected("Line 1: unsupported version (this app reads 'graph-theory 1')", "graph-theory 2");
    }

    @Test
    public void rejects_unknownKeyword() {
        assertRejected("Line 2: unknown keyword 'node' (expected vertex, edge or arc)",
                       "graph-theory 1", "node a");
    }

    @Test
    public void rejects_unknownVertex() {
        assertRejected("Line 3: unknown vertex 'q' (declare it with a vertex line first)",
                       "graph-theory 1", "vertex a", "edge a q");
    }

    @Test
    public void rejects_duplicateVertex() {
        assertRejected("Line 3: vertex 'a' is already declared",
                       "graph-theory 1", "vertex a", "vertex a 1 1");
    }

    @Test
    public void rejects_invalidName() {
        assertRejected("Line 2: invalid vertex name 'abcde' (1-4 letters, digits or _)",
                       "graph-theory 1", "vertex abcde");
    }

    @Test
    public void rejects_negativeWeight() {
        assertRejected("Line 3: weight must not be negative",
                       "graph-theory 1", "vertex a", "edge a a -1");
    }

    @Test
    public void rejects_nonNumber() {
        assertRejected("Line 2: 'x' is not a whole number", "graph-theory 1", "vertex a x 5");
    }

    @Test
    public void rejects_vertexWithOneCoordinate() {
        assertRejected("Line 2: expected 'vertex <name> [<x> <y>] [root]'",
                       "graph-theory 1", "vertex a 5");
    }

    @Test
    public void rejects_edgeWithOneEnd() {
        assertRejected("Line 3: expected 'arc <from> <to> [<weight>]'",
                       "graph-theory 1", "vertex a", "arc a");
    }

    @Test
    public void rejects_twoRootsInOneComponent() {
        assertRejected("Line 4: 'c' is a second root in the same component as 'a'",
                       "graph-theory 1",
                       "vertex a root",
                       "vertex b",
                       "vertex c root",
                       "edge a b",
                       "arc b c");
    }
```

- [ ] **Step 2: Run all tests**

Expected: `OK (120 tests)`. If one fails, fix `GraphFile` so the message matches the test. Don't change the test, since the test is the spec for the user-facing text.

- [ ] **Step 3: Commit**

```bash
git add test/graphtheory/GraphFileTest.java
git commit -m "test: graph file error messages

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 5: Layout (circle arrangement and clamping)

**Files:**
- Create: `src/graphtheory/Layout.java`
- Modify: `src/graphtheory/Canvas.java` (`arrangeVertices`)
- Test: `test/graphtheory/LayoutTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class LayoutTest {

    private static Vector<Vertex> at(int... xy) {
        Vector<Vertex> vs = new Vector<Vertex>();
        for (int i = 0; i < xy.length; i += 2) vs.add(new Vertex("v" + i, xy[i], xy[i + 1]));
        return vs;
    }

    @Test
    public void clampInto_pullsOutsideVerticesToTheMargin() {
        Vector<Vertex> vs = at(-50, 5000, 900, -3);
        Layout.clampInto(vs, 800, 600);
        assertEquals(25, vs.get(0).location.x);
        assertEquals(575, vs.get(0).location.y);
        assertEquals(775, vs.get(1).location.x);
        assertEquals(25, vs.get(1).location.y);
    }

    @Test
    public void clampInto_leavesInsideVerticesAlone() {
        Vector<Vertex> vs = at(400, 300);
        Layout.clampInto(vs, 800, 600);
        assertEquals(400, vs.get(0).location.x);
        assertEquals(300, vs.get(0).location.y);
    }

    @Test
    public void arrangeOnCircle_spacesVerticesEvenlyAroundTheCentre() {
        Vector<Vertex> vs = at(0, 0, 0, 0, 0, 0, 0, 0);
        Layout.arrangeOnCircle(vs, 800, 600);   // centre (400,300), radius 600/5 = 120
        assertEquals(520, vs.get(0).location.x);
        assertEquals(300, vs.get(0).location.y);
        assertEquals(400, vs.get(1).location.x);
        assertEquals(420, vs.get(1).location.y);
        assertEquals(280, vs.get(2).location.x);
        assertEquals(300, vs.get(2).location.y);
        assertEquals(400, vs.get(3).location.x);
        assertEquals(180, vs.get(3).location.y);
    }

    @Test
    public void arrangeOnCircle_emptyListIsANoOp() {
        Layout.arrangeOnCircle(new Vector<Vertex>(), 800, 600);
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: Layout`.

- [ ] **Step 3: Create `Layout`**

```java
package graphtheory;

import java.util.List;

/** Places vertices on the canvas. */
public final class Layout {

    /** Distance kept from the canvas edge: vertex radius (20) plus its property ring. */
    public static final int MARGIN = 25;

    private Layout() {}

    /** Evenly around a circle of radius height/5 at the canvas centre (Extras > Auto Arrange). */
    public static void arrangeOnCircle(List<Vertex> vs, int width, int height) {
        if (vs.isEmpty()) return;
        double radius = height / 5.0;
        double centreX = width / 2.0;
        double centreY = height / 2.0;
        for (int i = 0; i < vs.size(); i++) {
            double angle = 2 * Math.PI * i / vs.size();
            vs.get(i).location.x = (int) Math.round(centreX + Math.cos(angle) * radius);
            vs.get(i).location.y = (int) Math.round(centreY + Math.sin(angle) * radius);
        }
    }

    /** Moves any vertex outside the canvas (less MARGIN) to the nearest point inside it. */
    public static void clampInto(List<Vertex> vs, int width, int height) {
        for (Vertex v : vs) {
            v.location.x = Math.max(MARGIN, Math.min(width - MARGIN, v.location.x));
            v.location.y = Math.max(MARGIN, Math.min(height - MARGIN, v.location.y));
        }
    }
}
```

- [ ] **Step 4: Use it in `Canvas`**

Replace the whole body of `private void arrangeVertices()` so the method reads:

```java
    private void arrangeVertices() {
        Layout.arrangeOnCircle(vertexList, width, height);
    }
```

The old body stepped by `360 / n` whole degrees, so positions now differ by a pixel or two for some `n`. That's intended.

- [ ] **Step 5: Run all tests**

Run with `graphtheory.LayoutTest` added. Expected: `OK (124 tests)`.

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/Layout.java src/graphtheory/Canvas.java test/graphtheory/LayoutTest.java
git commit -m "feat: Layout helper for auto-arrange and clamping

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 6: EditHistory

**Files:**
- Create: `src/graphtheory/EditHistory.java`
- Test: `test/graphtheory/EditHistoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditHistoryTest {

    @Test
    public void fresh_hasNothingToUndoOrRedo() {
        EditHistory h = new EditHistory(50);
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
        assertNull(h.undo("now"));
        assertNull(h.redo("now"));
    }

    @Test
    public void undoThenRedo_walksBackAndForth() {
        EditHistory h = new EditHistory(50);
        h.record("A");                       // the graph was A, now it is B
        assertEquals("A", h.undo("B"));
        assertTrue(h.canRedo());
        assertEquals("B", h.redo("A"));
        assertFalse(h.canRedo());
        assertTrue(h.canUndo());
    }

    @Test
    public void aNewEdit_clearsRedo() {
        EditHistory h = new EditHistory(50);
        h.record("A");
        h.undo("B");
        h.record("A");                       // edited A into C instead
        assertFalse(h.canRedo());
    }

    @Test
    public void oldestSnapshotsAreDroppedPastTheLimit() {
        EditHistory h = new EditHistory(2);
        h.record("1");
        h.record("2");
        h.record("3");
        assertEquals("3", h.undo("4"));
        assertEquals("2", h.undo("3"));
        assertNull(h.undo("2"));
    }

    @Test
    public void clear_forgetsEverything() {
        EditHistory h = new EditHistory(50);
        h.record("A");
        h.undo("B");
        h.record("C");
        h.clear();
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: EditHistory`.

- [ ] **Step 3: Create `EditHistory`**

```java
package graphtheory;

import java.util.ArrayDeque;

/**
 * Undo/redo as stacks of graph snapshots (.graph text). The caller records
 * the snapshot from before each edit; undo/redo hand back the snapshot to
 * restore and take the current one in exchange.
 */
public class EditHistory {

    private final int limit;
    private final ArrayDeque<String> undo = new ArrayDeque<String>();
    private final ArrayDeque<String> redo = new ArrayDeque<String>();

    public EditHistory(int limit) {
        this.limit = limit;
    }

    /** Call after an edit with the snapshot from before it. */
    public void record(String before) {
        push(undo, before);
        redo.clear();
    }

    public boolean canUndo() { return !undo.isEmpty(); }

    public boolean canRedo() { return !redo.isEmpty(); }

    /** The snapshot to restore, or null if there is nothing to undo. */
    public String undo(String current) {
        if (undo.isEmpty()) return null;
        push(redo, current);
        return undo.pop();
    }

    /** The snapshot to restore, or null if there is nothing to redo. */
    public String redo(String current) {
        if (redo.isEmpty()) return null;
        push(undo, current);
        return redo.pop();
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }

    private void push(ArrayDeque<String> stack, String snapshot) {
        stack.push(snapshot);
        if (stack.size() > limit) stack.removeLast();
    }
}
```

- [ ] **Step 4: Run all tests**

Run with `graphtheory.EditHistoryTest` added. Expected: `OK (129 tests)`.

- [ ] **Step 5: Commit**

```bash
git add src/graphtheory/EditHistory.java test/graphtheory/EditHistoryTest.java
git commit -m "feat: EditHistory snapshot stacks for undo/redo

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 7: Tools table and ToolPalette

The tool ids stay the existing `selectedTool` numbers, so none of the `switch (selectedTool)` cases in `Canvas` change.

**Files:**
- Create: `src/graphtheory/Tools.java`, `src/graphtheory/ToolPalette.java`
- Test: `test/graphtheory/ToolsTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class ToolsTest {

    @Test
    public void everyToolHasLabelsAHintAndAShortcut() {
        for (int tool : Tools.ORDER) {
            assertFalse("short " + tool, Tools.shortLabel(tool).isEmpty());
            assertFalse("menu " + tool, Tools.menuLabel(tool).isEmpty());
            assertFalse("hint " + tool, Tools.hint(tool).isEmpty());
            assertTrue("shortcut " + tool, Tools.shortcut(tool) != 0);
        }
    }

    @Test
    public void shortcutsAreDistinct() {
        Set<Integer> seen = new HashSet<Integer>();
        for (int tool : Tools.ORDER) assertTrue(seen.add(Tools.shortcut(tool)));
    }

    @Test
    public void noToolSelected_stillHasAHint() {
        assertFalse(Tools.hint(Tools.NONE).isEmpty());
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: Tools`.

- [ ] **Step 3: Create `Tools`**

```java
package graphtheory;

import java.awt.event.KeyEvent;

/** The editing tools: the ids Canvas keeps in selectedTool, with their labels and hints. */
public final class Tools {

    public static final int NONE = 0;
    public static final int VERTEX = 1;
    public static final int EDGE = 2;
    public static final int GRAB = 3;
    public static final int REMOVE = 4;
    public static final int ARC = 5;
    public static final int PAIR = 6;
    public static final int WEIGHT = 7;
    public static final int ROOT = 8;
    public static final int WALK = 9;

    /** Order in the palette and the Tools menu. */
    public static final int[] ORDER = { GRAB, VERTEX, EDGE, ARC, WEIGHT, ROOT, REMOVE, PAIR, WALK };

    private Tools() {}

    /** Palette button text. */
    public static String shortLabel(int tool) {
        switch (tool) {
            case GRAB:   return "Grab";
            case VERTEX: return "Vertex";
            case EDGE:   return "Edge";
            case ARC:    return "Arc";
            case WEIGHT: return "Weight";
            case ROOT:   return "Root";
            case REMOVE: return "Remove";
            case PAIR:   return "Pair";
            case WALK:   return "Walk";
            default:     return "";
        }
    }

    /** Tools menu text (the names the menu has always used). */
    public static String menuLabel(int tool) {
        switch (tool) {
            case GRAB:   return "Grab Tool";
            case VERTEX: return "Add Vertex";
            case EDGE:   return "Add Edges";
            case ARC:    return "Add Directed Edge";
            case WEIGHT: return "Set Edge Weight";
            case ROOT:   return "Mark as Root";
            case REMOVE: return "Remove Tool";
            case PAIR:   return "Select Pair";
            case WALK:   return "Build Walk";
            default:     return "";
        }
    }

    /** Key used with Ctrl (the shortcuts the menu has always used). */
    public static int shortcut(int tool) {
        switch (tool) {
            case GRAB:   return KeyEvent.VK_G;
            case VERTEX: return KeyEvent.VK_A;
            case EDGE:   return KeyEvent.VK_E;
            case ARC:    return KeyEvent.VK_D;
            case WEIGHT: return KeyEvent.VK_W;
            case ROOT:   return KeyEvent.VK_T;
            case REMOVE: return KeyEvent.VK_R;
            case PAIR:   return KeyEvent.VK_P;
            case WALK:   return KeyEvent.VK_L;
            default:     return 0;
        }
    }

    /** Status-bar text while the tool is active. */
    public static String hint(int tool) {
        switch (tool) {
            case GRAB:   return "Click a vertex to select it, drag to move it. Double-click a vertex to rename it, or an edge to set its weight.";
            case VERTEX: return "Click empty space to add a vertex.";
            case EDGE:   return "Drag from one vertex to another to add an undirected edge. Release on the same vertex for a self-loop.";
            case ARC:    return "Drag from the source vertex to the destination to add a directed edge.";
            case WEIGHT: return "Click an edge to set its weight.";
            case ROOT:   return "Click a vertex to make it (or stop it being) the root of its component.";
            case REMOVE: return "Click a vertex or edge to remove it.";
            case PAIR:   return "Click two vertices to inspect the ordered pair. Up/Down browse its paths.";
            case WALK:   return "Click a vertex to start, then vertices or edges to extend. Backspace or right-click undoes a step, Esc clears.";
            default:     return "Pick a tool on the left.";
        }
    }
}
```

- [ ] **Step 4: Create `ToolPalette`**

```java
package graphtheory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Left-hand column of tool buttons; the active tool stays pressed. */
public class ToolPalette extends JPanel {

    public interface Listener {
        void toolSelected(int tool);
    }

    private final Map<Integer, JToggleButton> buttons = new HashMap<Integer, JToggleButton>();
    private final ButtonGroup group = new ButtonGroup();

    public ToolPalette(final Listener listener) {
        super(new BorderLayout());
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 4));
        column.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        for (final int tool : Tools.ORDER) {
            JToggleButton b = new JToggleButton(Tools.shortLabel(tool));
            b.setToolTipText(Tools.menuLabel(tool) + " (Ctrl+" + KeyEvent.getKeyText(Tools.shortcut(tool)) + ")");
            // Not focusable, so keyboard shortcuts (Backspace, arrows) keep reaching the canvas.
            b.setFocusable(false);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.toolSelected(tool);
                }
            });
            group.add(b);
            column.add(b);
            buttons.put(tool, b);
        }
        add(column, BorderLayout.NORTH);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
    }

    /** Shows the tool as pressed, without notifying the listener. */
    public void setSelectedTool(int tool) {
        JToggleButton b = buttons.get(tool);
        if (b != null) {
            b.setSelected(true);
        } else {
            group.clearSelection();
        }
    }
}
```

- [ ] **Step 5: Run all tests**

Run with `graphtheory.ToolsTest` added. Expected: `OK (132 tests)`.

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/Tools.java src/graphtheory/ToolPalette.java test/graphtheory/ToolsTest.java
git commit -m "feat: Tools table and ToolPalette

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 8: Window layout (palette, tabs, status bar, menus, About)

This task restructures `Canvas`'s window. File commands still use the old `Open File` / `Save to File` code until Task 9 replaces them.

**Files:**
- Modify: `src/graphtheory/Canvas.java`, `src/graphtheory/Main.java`

- [ ] **Step 1: New imports and fields**

Add to the imports at the top of `Canvas.java`:

```java
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
```

Add these fields after `private FileManager fileManager = new FileManager();`:

```java
    private final String appName;
    private final MenuListener menuListener = new MenuListener();
    private JTabbedPane tabs;
    private ToolPalette palette;
    private JLabel statusHint;
    private JLabel statusCounts;
```

- [ ] **Step 2: Replace the constructor**

Replace the whole constructor `public Canvas(String title, int width, int height, Color bgColour) { … }` (it ends just before `private void buildPropertiesPanel()`) with:

```java
    public Canvas(String appName, int width, int height, Color bgColour) {
        this.appName = appName;
        this.width = width;
        this.height = height;
        backgroundColour = bgColour;
        vertexList = new Vector<Vertex>();
        edgeList = new Vector<Edge>();

        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        canvas = new CanvasPane();
        canvas.setPreferredSize(new Dimension(width, height));
        InputListener inputListener = new InputListener();
        canvas.addMouseListener(inputListener);
        canvas.addMouseMotionListener(inputListener);
        installKeyBindings();

        palette = new ToolPalette(new ToolPalette.Listener() {
            public void toolSelected(int tool) {
                selectTool(tool);
            }
        });
        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.add(palette, BorderLayout.WEST);
        graphPanel.add(canvas, BorderLayout.CENTER);

        buildPropertiesPanel();
        // Without this the tab would take the (huge) preferred size of the properties content.
        propertiesScroll.setPreferredSize(new Dimension(width, height));

        tabs = new JTabbedPane();
        tabs.addTab("Graph", graphPanel);
        tabs.addTab("Properties", propertiesScroll);
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                onTabChanged();
            }
        });

        statusHint = new JLabel(" ");
        statusCounts = new JLabel(" ");
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        status.add(statusHint, BorderLayout.CENTER);
        status.add(statusCounts, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        frame.setContentPane(root);

        buildMenuBar();
        frame.pack();
        frame.setLocationRelativeTo(null);
        setVisible(true);        // creates the canvas image, so it must come before refresh()
        selectTool(Tools.VERTEX);
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        addItem(file, "Open File", KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save to File", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));

        JMenu edit = new JMenu("Edit");
        addItem(edit, "Remove All", null);

        JMenu tools = new JMenu("Tools");
        for (final int tool : Tools.ORDER) {
            JMenuItem item = new JMenuItem(Tools.menuLabel(tool));
            item.setAccelerator(KeyStroke.getKeyStroke(Tools.shortcut(tool), KeyEvent.CTRL_DOWN_MASK));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectTool(tool);
                }
            });
            tools.add(item);
        }

        JMenu extras = new JMenu("Extras");
        addItem(extras, "Auto Arrange Vertices", null);
        addItem(extras, "Show Induced Subgraph", null);
        addItem(extras, "Show Greedy Coloring", KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
        addItem(extras, "Clear Coloring", null);
        extras.addSeparator();
        for (String find : new String[] {"Find Euler Trail", "Find Euler Tour",
                                         "Find Hamiltonian Path", "Find Hamiltonian Cycle"}) {
            addItem(extras, find, null);
        }

        JMenu window = new JMenu("Window");
        addItem(window, "Graph", null);
        addItem(window, "Properties", null);

        JMenu help = new JMenu("Help");
        addItem(help, "About", null);

        bar.add(file);
        bar.add(edit);
        bar.add(tools);
        bar.add(extras);
        bar.add(window);
        bar.add(help);
        frame.setJMenuBar(bar);
    }

    private JMenuItem addItem(JMenu menu, String label, KeyStroke key) {
        JMenuItem item = new JMenuItem(label);
        if (key != null) item.setAccelerator(key);
        item.addActionListener(menuListener);
        menu.add(item);
        return item;
    }

    /** From the palette or the Tools menu. */
    private void selectTool(int tool) {
        clearHover();
        walkMessage = null;
        selectedTool = tool;
        if (tool == Tools.PAIR) {
            pairedVertex1Index = -1;
            pairedVertex2Index = -1;
            currentPairVP = null;
            pairPaths = null;
        } else if (tool == Tools.WALK) {
            clearWalk();
        }
        palette.setSelectedTool(tool);
        tabs.setSelectedIndex(0);
        refresh();
    }

    private void onTabChanged() {
        selectedWindow = tabs.getSelectedIndex();
        clearHover();
        if (selectedWindow == 1) computeProperties();
        refresh();
    }

    /** Recomputes what the Properties tab shows. */
    private void computeProperties() {
        if (vertexList.size() > 0) {
            int[][] matrix = gP.generateAdjacencyMatrix(vertexList, edgeList);

            gP.vertexConnectivity(vertexList);
            gP.edgeConnectivity(vertexList, edgeList);

            for (Vertex v : vertexList) v.wasClicked = false;
            for (Edge ed : edgeList)    ed.wasClicked = false;

            for (Vertex v : gP.witnessVertices) v.wasClicked = true;
            for (Edge ed : gP.witnessEdges)     ed.wasClicked = true;

            reloadVertexConnections(matrix, vertexList);

            gP.generateDistanceMatrix(vertexList);
            gP.displayContainers(vertexList);
        }
        refreshPropertiesScrollSize();
    }

    private void updateStatus() {
        if (statusHint == null) return;
        statusHint.setText(selectedWindow == 0
                ? Tools.hint(selectedTool)
                : "Properties of the current graph. Switch to the Graph tab to edit.");
        statusCounts.setText(vertexList.size() + " vertices \u00b7 " + edgeList.size() + " edges");
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                appName + "\n\n"
                + "Based on Graph Theory SY08-09 Term3 by Team DGLSS (v0.5).\n"
                + "Extended by jbarguilles and rcoporto.",
                "About " + appName, JOptionPane.INFORMATION_MESSAGE);
    }
```

- [ ] **Step 3: Replace `MenuListener`**

Replace the whole `class MenuListener implements ActionListener { … }`. The tool commands now go through `selectTool`, and window switching goes through the tabs:

```java
    class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            clearHover();
            walkMessage = null;

            if (command.startsWith("Find ")) {
                findTraversal(command.substring("Find ".length()));
            } else if (command.equals("Auto Arrange Vertices")) {
                arrangeVertices();
            } else if (command.equals("Show Induced Subgraph")) {
                Vector<Vector> sub = buildInducedSubgraph();
                if (sub == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Select at least one vertex (Grab Tool) before showing the subgraph.",
                            "No selection",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showSubgraphWindow(sub);
                }
            } else if (command.equals("Show Greedy Coloring")) {
                gP.greedyColoring(vertexList);
            } else if (command.equals("Clear Coloring")) {
                gP.clearColoring(vertexList);
            } else if (command.equals("Remove All")) {
                edgeList.removeAllElements();
                vertexList.removeAllElements();
                clickedVertexIndex = 0;
                pairedVertex1Index = -1;
                pairedVertex2Index = -1;
                currentPairVP = null;
                clearWalk();
                markGraphDirty();
            } else if (command.equals("Open File")) {
                int returnValue = fileManager.jF.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    clearWalk();
                    pairedVertex1Index = -1;
                    pairedVertex2Index = -1;
                    currentPairVP = null;
                    pairPaths = null;
                    loadFile(fileManager.loadFile(fileManager.jF.getSelectedFile()));
                    tabs.setSelectedIndex(0);
                }
            } else if (command.equals("Save to File")) {
                int returnValue = fileManager.jF.showSaveDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    fileManager.saveFile(vertexList, edgeList, fileManager.jF.getSelectedFile());
                }
            } else if (command.equals("Graph")) {
                tabs.setSelectedIndex(0);
            } else if (command.equals("Properties")) {
                tabs.setSelectedIndex(1);
            } else if (command.equals("About")) {
                showAbout();
            }

            refresh();
        }
    }
```

- [ ] **Step 4: Keep the palette in sync when Find… switches tool**

In `findTraversal`, replace

```java
        selectedTool = 9;
        selectedWindow = 0;
        clearWalk();
```

with

```java
        selectedTool = Tools.WALK;
        palette.setSelectedTool(Tools.WALK);
        tabs.setSelectedIndex(0);
        clearWalk();
```

- [ ] **Step 5: Status bar refresh, canvas image size, drop the old status line**

At the end of `refresh()`, after the `propertiesContent.repaint()` block, add:

```java
        updateStatus();
```

In `setVisible`, replace

```java
            Dimension size = canvas.getSize();
```

with

```java
            Dimension size = new Dimension(width, height);
```

In `CanvasPane.paint`, `case 0`, delete the `String toolName; switch (selectedTool) { … }` block and the `graphic.drawString("Vertex Count=" …);` call after it. The status bar replaces them. `case 0` should now start with `g.drawImage(canvasImage, 0, 0, null);`.

- [ ] **Step 6: Rename the app**

Replace the body of `Main.java` with:

```java
package graphtheory;

import java.awt.Color;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new Canvas("Graph Theory Visualizer", 800, 600, Color.WHITE);
    }
}
```

- [ ] **Step 7: Compile, run the tests, launch**

Expected: `OK (132 tests)`. Launch the app and check:
1. The palette is on the left, **Vertex** is pressed, and the status bar shows *"Click empty space to add a vertex."* and *"0 vertices · 0 edges"*.
2. The canvas is still 800×600: Auto Arrange on 4 vertices centres them in the white area.
3. Clicking **Edge** in the palette presses it. **Ctrl+G** from the keyboard presses **Grab**. Tools → Mark as Root presses **Root**.
4. The **Properties** tab shows the properties view, and the palette isn't visible. The status bar changes, and **Graph** returns you to the canvas.
5. Extras → Find Euler Tour on a triangle presses **Walk** and shows the walk.
6. In Build Walk, Backspace and Esc still work after you click a palette button, which shows the buttons don't take focus.
7. Help → About shows the credits.

- [ ] **Step 8: Commit**

```bash
git add src/graphtheory/Canvas.java src/graphtheory/Main.java
git commit -m "feat: tool palette, status bar, Graph/Properties tabs, Help > About

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 9: File commands, unsaved changes, close prompt

**Files:**
- Rewrite: `src/graphtheory/FileManager.java`
- Modify: `src/graphtheory/Canvas.java`
- Test: `test/graphtheory/FileManagerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package graphtheory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class FileManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void withExtension_addsGraphWhenMissing() {
        assertEquals("triangle.graph", FileManager.withExtension(new File("triangle")).getName());
    }

    @Test
    public void withExtension_keepsAnExtensionTheUserTyped() {
        assertEquals("triangle.graph", FileManager.withExtension(new File("triangle.graph")).getName());
        assertEquals("notes.txt", FileManager.withExtension(new File("notes.txt")).getName());
    }

    @Test
    public void writeThenRead_returnsTheSameText() throws Exception {
        File f = tmp.newFile("g.graph");
        String text = "graph-theory 1\nvertex a 1 2\n";
        FileManager.write(f, text);
        assertEquals(text, FileManager.read(f));
    }
}
```

- [ ] **Step 2: Run it and check that it fails**

Expected: compilation error, `cannot find symbol: method withExtension`.

- [ ] **Step 3: Rewrite `FileManager`**

```java
package graphtheory;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** File dialogs and disk access for .graph files. The format itself is GraphFile. */
public class FileManager {

    public static final String EXTENSION = "graph";

    private final JFileChooser chooser = new JFileChooser();

    public FileManager() {
        chooser.setFileFilter(new FileNameExtensionFilter("Graph files (*.graph)", EXTENSION));
    }

    /** The file to open, or null if the user cancelled. */
    public File chooseOpen(Component parent) {
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile() : null;
    }

    /** The file to save to (".graph" added if missing, overwrite confirmed), or null if cancelled. */
    public File chooseSave(Component parent, File current) {
        chooser.setSelectedFile(current != null ? current : new File("untitled." + EXTENSION));
        while (true) {
            if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
            File f = withExtension(chooser.getSelectedFile());
            if (!f.exists()) return f;
            int answer = JOptionPane.showConfirmDialog(parent,
                    f.getName() + " already exists. Replace it?", "Confirm Save As",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) return f;
        }
    }

    /** Adds ".graph" unless the name already has an extension. */
    static File withExtension(File f) {
        String name = f.getName();
        return name.contains(".") ? f : new File(f.getParentFile(), name + "." + EXTENSION);
    }

    public static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    public static void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 4: Run the tests**

Run with `graphtheory.FileManagerTest` added. `Canvas` won't compile yet, because it still calls `fileManager.jF` and `loadFile`. Compile just the logic and tests to check this step:

```bash
rm -rf out/test && mkdir -p out/test && javac -encoding UTF-8 -d out/test -cp "$CP" \
  $(ls src/graphtheory/*.java | grep -v -e Canvas.java -e Main.java) test/graphtheory/*.java \
  && MSYS_NO_PATHCONV=1 java -cp "out/test;$CP" org.junit.runner.JUnitCore graphtheory.FileManagerTest
```

Expected: `OK (3 tests)`.

- [ ] **Step 5: Snapshot, unsaved changes and title in `Canvas`**

Add to the imports:

```java
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
```

Add the fields (next to `fileManager`):

```java
    private File currentFile = null;
    private String savedText;
    private String pressBefore = null;
```

Add these methods (e.g. after `computeProperties`):

```java
    /** The graph as .graph text: the unit of undo, saving and "unsaved changes". */
    private String snapshot() {
        return GraphFile.write(vertexList, edgeList);
    }

    /** Call after an edit with the snapshot from before it. */
    private void afterEdit(String before) {
        if (!before.equals(snapshot())) {
            markGraphDirty();
            if (selectedWindow == 1) computeProperties();
        }
        updateTitle();
    }

    private boolean isModified() {
        return !snapshot().equals(savedText);
    }

    private String documentName() {
        return currentFile == null ? "Untitled" : currentFile.getName();
    }

    private void updateTitle() {
        frame.setTitle(documentName() + (isModified() ? "*" : "") + " \u2014 " + appName);
    }

    /** Swaps in a whole new graph (New, Open, Remove All, undo); clears analysis state. */
    private void replaceGraph(Vector<Vertex> vs, Vector<Edge> es) {
        vertexList = vs;
        edgeList = es;
        clearWalk();
        clickedVertexIndex = 0;
        pairedVertex1Index = -1;
        pairedVertex2Index = -1;
        currentPairVP = null;
        pairPaths = null;
        markGraphDirty();
        if (selectedWindow == 1) computeProperties();
    }

    /** Offers to save unsaved changes before `action`; false means the user cancelled. */
    private boolean confirmDiscard(String action) {
        if (!isModified()) return true;
        Object[] options = { "Save", "Don't Save", "Cancel" };
        int choice = JOptionPane.showOptionDialog(frame,
                "Save changes to " + documentName() + " before " + action + "?",
                appName, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (choice == 0) return save();
        return choice == 1;
    }

    private void newGraph() {
        if (!confirmDiscard("starting a new graph")) return;
        replaceGraph(new Vector<Vertex>(), new Vector<Edge>());
        currentFile = null;
        savedText = snapshot();
        tabs.setSelectedIndex(0);
        updateTitle();
    }

    private void openGraph() {
        if (!confirmDiscard("opening another file")) return;
        File f = fileManager.chooseOpen(frame);
        if (f == null) return;
        GraphFile.Data d;
        try {
            d = GraphFile.read(FileManager.read(f));
        } catch (IOException | GraphFile.FormatException ex) {
            showError("Couldn't open " + f.getName(), ex.getMessage());
            return;
        }
        String asWritten = GraphFile.write(d.vertices, d.edges);
        Layout.arrangeOnCircle(d.unplaced, width, height);
        Layout.clampInto(d.vertices, width, height);
        replaceGraph(d.vertices, d.edges);
        currentFile = f;
        // If arranging or clamping moved anything, the graph now differs from the file: unsaved changes.
        savedText = asWritten;
        tabs.setSelectedIndex(0);
        updateTitle();
    }

    /** Saves to the current file, or asks for one; false if cancelled or failed. */
    private boolean save() {
        return currentFile == null ? saveAs() : writeTo(currentFile);
    }

    private boolean saveAs() {
        File f = fileManager.chooseSave(frame, currentFile);
        return f != null && writeTo(f);
    }

    private boolean writeTo(File f) {
        String text = snapshot();
        try {
            FileManager.write(f, text);
        } catch (IOException ex) {
            showError("Couldn't save " + f.getName(), ex.getMessage());
            return false;
        }
        currentFile = f;
        savedText = text;
        updateTitle();
        return true;
    }

    private void exitApp() {
        if (!confirmDiscard("closing")) return;
        frame.dispose();
        System.exit(0);
    }

    private void removeAll() {
        if (vertexList.isEmpty()) return;
        int answer = JOptionPane.showConfirmDialog(frame,
                "Remove all " + vertexList.size() + " vertices and " + edgeList.size() + " edges?",
                "Remove All", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        String before = snapshot();
        replaceGraph(new Vector<Vertex>(), new Vector<Edge>());
        afterEdit(before);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }
```

Delete the old `private void loadFile(Vector<Vector> File) { … }` method.

- [ ] **Step 6: Constructor: close prompt, initial saved text, title**

In the constructor, replace

```java
        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
```

with

```java
        savedText = snapshot();

        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
```

At the end of the constructor, after `selectTool(Tools.VERTEX);`, add:

```java
        updateTitle();
```

- [ ] **Step 7: File menu**

In `buildMenuBar`, replace the two `File` items

```java
        addItem(file, "Open File", KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save to File", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
```

with

```java
        addItem(file, "New", KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Open...", KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save As...", KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        file.addSeparator();
        addItem(file, "Exit", null);
```

In `MenuListener.actionPerformed`, replace the three branches `"Remove All"`, `"Open File"` and `"Save to File"` (from `} else if (command.equals("Remove All")) {` up to the line before `} else if (command.equals("Graph")) {`) with:

```java
            } else if (command.equals("Remove All")) {
                removeAll();
            } else if (command.equals("New")) {
                newGraph();
            } else if (command.equals("Open...")) {
                openGraph();
            } else if (command.equals("Save")) {
                save();
            } else if (command.equals("Save As...")) {
                saveAs();
            } else if (command.equals("Exit")) {
                exitApp();
```

Make Auto Arrange an edit. Replace

```java
            } else if (command.equals("Auto Arrange Vertices")) {
                arrangeVertices();
```

with

```java
            } else if (command.equals("Auto Arrange Vertices")) {
                String before = snapshot();
                arrangeVertices();
                afterEdit(before);
```

- [ ] **Step 8: Wrap mouse edits**

Clicks (add vertex, remove, weight, root) are wrapped as a whole. Press-to-release (edge and arc creation, dragging) is wrapped from press to release, so **a drag is one edit**.

In `InputListener`, rename the existing `public void mouseClicked(MouseEvent e)` to `private void handleClick(MouseEvent e)` and remove its `@Override`. Then add above it:

```java
        @Override
        public void mouseClicked(MouseEvent e) {
            if (selectedWindow != 0) return;
            String before = snapshot();
            handleClick(e);
            afterEdit(before);
        }
```

At the very start of `mousePressed`, before `if (selectedWindow == 0 && vertexList.size() > 0) {`, add:

```java
            if (selectedWindow == 0) pressBefore = snapshot();
```

In `mouseReleased`, just before the final `updateHover(e.getX(), e.getY());`, add:

```java
            if (pressBefore != null) {
                afterEdit(pressBefore);
                pressBefore = null;
            }
```

- [ ] **Step 9: Compile, run all tests, launch**

Run the full build-and-test command. Expected: `OK (135 tests)`. Launch and check:
1. The title is **`Untitled — Graph Theory Visualizer`**. Adding a vertex changes it to `Untitled* — …`.
2. Ctrl+S opens Save As with `untitled.graph` filled in. Saving as `tri` creates `tri.graph`, and the title becomes `tri.graph — …` with no `*`.
3. Open `tri.graph` in a text editor. It's in the full form (header, `vertex … x y`, `edge … weight`).
4. Dragging a vertex sets `*`. Saving clears it. Clicking Select Pair vertices, building a walk, greedy coloring and switching to Properties **do not** set `*`.
5. Build a mixed graph with `{a,b}`, an arc `(b,a)`, an undirected self-loop, a directed self-loop, weights other than 1 and a root. Save, choose New, then Open: everything comes back exactly, and the title has no `*`.
6. Hand-write `hand.graph`:
   ```
   graph-theory 1
   vertex a
   vertex b
   vertex c 5000 -10
   edge a b
   arc b c 4
   ```
   Opening it places `a` and `b` on the circle and clamps `c` to the top-right corner (775, 25). The title shows `hand.graph*`.
7. Opening a file containing `edge a q` shows **"Couldn't open …"** with `Line N: unknown vertex 'q' …`, and the current graph is unchanged.
8. With unsaved changes, New, Open, File → Exit and the window's **X** each ask *Save / Don't Save / Cancel*. Cancel keeps the window open. Save on an untitled graph opens Save As, and cancelling that also cancels the close.
9. Save As onto an existing file asks before replacing it.
10. Edit → Remove All asks first, and nothing happens on an empty graph.

- [ ] **Step 10: Commit**

```bash
git add src/graphtheory/FileManager.java src/graphtheory/Canvas.java test/graphtheory/FileManagerTest.java
git commit -m "feat: New/Open/Save/Save As with .graph files, unsaved-changes prompt

Replaces the adjacency-matrix format, which lost arc direction, parallel
edges, self-loops and roots, and misassigned weights (ADR 0004).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 10: Undo and redo

**Files:**
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: History field and Edit menu items**

Add the fields:

```java
    private final EditHistory history = new EditHistory(50);
    private JMenuItem undoItem;
    private JMenuItem redoItem;
```

In `buildMenuBar`, replace

```java
        JMenu edit = new JMenu("Edit");
        addItem(edit, "Remove All", null);
```

with

```java
        JMenu edit = new JMenu("Edit");
        undoItem = addItem(edit, "Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        redoItem = addItem(edit, "Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        edit.addSeparator();
        addItem(edit, "Remove All", null);
```

In `MenuListener.actionPerformed`, add before `} else if (command.equals("Remove All")) {`:

```java
            } else if (command.equals("Undo")) {
                undo();
            } else if (command.equals("Redo")) {
                redo();
```

- [ ] **Step 2: Record edits and restore snapshots**

In `afterEdit`, add `history.record(before);` as the first line inside the `if`:

```java
    private void afterEdit(String before) {
        if (!before.equals(snapshot())) {
            history.record(before);
            markGraphDirty();
            if (selectedWindow == 1) computeProperties();
        }
        updateTitle();
    }
```

At the end of `updateTitle`, add:

```java
        if (undoItem != null) {
            undoItem.setEnabled(history.canUndo());
            redoItem.setEnabled(history.canRedo());
        }
```

Add the methods:

```java
    private void undo() {
        String previous = history.undo(snapshot());
        if (previous != null) restore(previous);
    }

    private void redo() {
        String next = history.redo(snapshot());
        if (next != null) restore(next);
    }

    /** Rebuilds the graph from a snapshot. The walk and pair are cleared: their vertices are gone. */
    private void restore(String text) {
        try {
            GraphFile.Data d = GraphFile.read(text);
            replaceGraph(d.vertices, d.edges);
        } catch (GraphFile.FormatException ex) {
            throw new IllegalStateException("Undo snapshot did not parse: " + ex.getMessage());
        }
        updateTitle();
    }
```

In `newGraph` and `openGraph`, add `history.clear();` on the line after `replaceGraph(…);`. A different document starts with an empty history.

- [ ] **Step 3: Compile, run all tests, launch**

Expected: `OK (135 tests)`. Launch and check:
1. When the app starts, Undo and Redo are greyed out.
2. Add three vertices, then Ctrl+Z three times: they disappear in reverse order. Ctrl+Y brings them back.
3. Dragging a vertex across the canvas is **one** Ctrl+Z.
4. Remove a vertex with incident edges, then Ctrl+Z: the vertex and all its edges return with their weights.
5. Remove All followed by Ctrl+Z restores the graph.
6. Save, make one edit, then Ctrl+Z: the `*` disappears because the text equals the saved text again.
7. In Build Walk, Backspace still removes walk steps, and Ctrl+Z undoes a graph edit (and clears the walk).
8. Open a file, then Ctrl+Z does nothing (the history was cleared).

- [ ] **Step 4: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "feat: snapshot undo/redo (Ctrl+Z / Ctrl+Y, 50 steps)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 11: Rename vertices; double-click edges for weight

**Files:**
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Hit-test helpers**

Add to `Canvas`:

```java
    private Vertex vertexAt(int x, int y) {
        for (Vertex v : vertexList) {
            if (v.hasIntersection(x, y)) return v;
        }
        return null;
    }

    private Edge edgeAt(int x, int y) {
        for (Edge ed : edgeList) {
            if (ed.hasIntersection(x, y)) return ed;
        }
        return null;
    }
```

- [ ] **Step 2: Move the weight dialog into a method**

In `handleClick`, replace the whole `case 7: { … }` block with:

```java
                    case 7: {
                        Edge target = edgeAt(e.getX(), e.getY());
                        if (target != null) editEdgeWeight(target);
                        break;
                    }
```

and add this method to `Canvas`. It contains the old dialog code, with `break` changed to `return`:

```java
    private void editEdgeWeight(Edge target) {
        String input = JOptionPane.showInputDialog(
                frame,
                "Edge " + target.vertex1.name + " \u2192 " + target.vertex2.name
                     + (target.directed ? " (directed)" : " (undirected)")
                     + "\nEnter new weight (non-negative integer):",
                "" + target.weight);
        if (input == null) return;
        try {
            int w = Integer.parseInt(input.trim());
            if (w < 0) {
                JOptionPane.showMessageDialog(frame,
                        "Dijkstra requires non-negative weights.",
                        "Invalid weight",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            target.setWeight(w);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter a whole number.",
                    "Invalid weight",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
```

The old code called `markGraphDirty(); refresh();` after `setWeight`. The `mouseClicked` wrapper (`afterEdit`) and the final `refresh()` now handle both.

- [ ] **Step 3: Rename dialog**

Add to `Canvas`:

```java
    /** Asks for a new name until it is valid and unused, or the user cancels. */
    private void renameVertex(Vertex v) {
        String prompt = "New name for vertex " + v.name + " (" + VertexNames.RULE + "):";
        String input = (String) JOptionPane.showInputDialog(frame, prompt, "Rename Vertex",
                JOptionPane.PLAIN_MESSAGE, null, null, v.name);
        while (input != null) {
            String name = input.trim();
            String problem = VertexNames.checkRename(v, name, vertexList);
            if (problem == null) {
                v.name = name;
                return;
            }
            input = (String) JOptionPane.showInputDialog(frame, problem + "\n" + prompt, "Rename Vertex",
                    JOptionPane.WARNING_MESSAGE, null, null, input);
        }
    }
```

- [ ] **Step 4: Double-click in the Grab tool**

In `handleClick`, add a new case inside `switch (selectedTool)`. The Grab tool (3) currently has no click case:

```java
                    case 3: {
                        if (e.getClickCount() != 2) break;
                        Vertex hitV = vertexAt(e.getX(), e.getY());
                        if (hitV != null) {
                            renameVertex(hitV);
                            break;
                        }
                        Edge hitE = edgeAt(e.getX(), e.getY());
                        if (hitE != null) editEdgeWeight(hitE);
                        break;
                    }
```

A double-click's two presses toggle the vertex's selection twice, so it ends up selected as it was before. The `mouseClicked` wrapper makes the rename one undoable edit.

- [ ] **Step 5: Compile, run all tests, launch**

Expected: `OK (135 tests)`. Launch and check:
1. Grab tool: double-click vertex `0`, type `s`, and the vertex shows `s`. The Properties tab matrices, the walk text and the pair box all show `s`.
2. Renaming to a name already used shows *"'x' is already used by another vertex."* and asks again. `toolong` or `a b` shows the rule. Cancel leaves the name unchanged.
3. Ctrl+Z undoes a rename.
4. Double-clicking an edge with the Grab tool opens the weight dialog. The Weight tool still works with a single click.
5. Save a graph with renamed vertices, then reopen it: the names are kept.
6. Rename `0` to `x`, then add a vertex: it gets `0` again (lowest unused number).

- [ ] **Step 6: Commit**

```bash
git add src/graphtheory/Canvas.java
git commit -m "feat: rename vertices and set weights by double-clicking with Grab

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Task 12: Docs, final verification, PR

- [ ] **Step 1: Update the glossary's display conventions**

In `CONTEXT.md`, under **Display Conventions**, replace

```
- **Properties window**: shows a table of node properties for all vertices, plus the adjacency matrix and distance matrix.
```

with

```
- **Properties tab**: shows a table of node properties for all vertices, plus the adjacency matrix and distance matrix. It sits beside the **Graph** tab and is recomputed whenever it is opened.
```

- [ ] **Step 2: Full test run and a clean launch**

Run the full build-and-test command. Expected: `OK (135 tests)`. Launch once more and go through checks 1, 2 and 8 from Task 9 to confirm the app works end to end.

- [ ] **Step 3: Commit**

```bash
git add CONTEXT.md
git commit -m "docs: Properties is a tab

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 4: Open the PR (ask the user first)**

Check that `gh auth status` shows **jbarguilles** (PRs must come from that account). Then:

```bash
git push -u origin feature/file-io-ux
gh pr create --base master --title "File I/O and UX: .graph files, undo, rename, tool palette" --body "$(cat <<'EOF'
## Summary
- New `.graph` text format (ADR 0004) replaces the adjacency-matrix format, which lost arc direction, parallel edges, self-loops and roots, and misassigned weights. Hand-writable: comments, optional weights and coordinates.
- File menu: New / Open / Save / Save As, title shows the file name and `*` for unsaved changes, Save / Don't Save / Cancel on New, Open and close.
- Snapshot undo/redo (Ctrl+Z / Ctrl+Y, 50 steps); Remove All asks first.
- Rename vertices by double-clicking with Grab (1-4 letters, digits or _, unique); double-click an edge to set its weight.
- Left tool palette, status bar with per-tool hints, Graph | Properties tabs, Help > About with the original DGLSS credits.
- Edge weights are now keyed by vertex identity, not name.

## Test plan
- [ ] `OK (135 tests)`
- [ ] Mixed graph with parallel edges, both kinds of self-loop, weights and a root survives Save → New → Open
- [ ] Hand-written file with missing coordinates opens arranged and marked `*`
- [ ] Bad file shows a line-numbered error and leaves the current graph alone
- [ ] Unsaved-changes prompt on New, Open, Exit and the window's X
- [ ] Undo/redo of add, remove, drag, rename, weight, Remove All

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```
