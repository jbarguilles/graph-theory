# Node Properties Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all six node properties (Degree/In/Out-Degree, Isolated, Cutpoint, Self-loop, Root) on the existing Java Swing graph editor, including a fixed info box in the Graph window and a node properties table in the Properties window.

**Architecture:** `Vertex` gains three separate neighbor lists (`undirectedNeighbors`, `inNeighbors`, `outNeighbors`) replacing the current single `connectedVertices` list; `Edge` gains a `directed` flag so the graph is a mixed graph. Pure-logic methods (`degree()`, `isIsolated()`, Tarjan's cutpoint) live on `Vertex`/`GraphProperties`; all visual rendering stays in `Vertex.draw()` and `Canvas`.

**Tech Stack:** Java 8+, Java Swing (AWT Graphics), JUnit 4 (new — add via NetBeans: right-click project > Properties > Libraries > Add Library > JUnit 4)

---

## File Map

| File | Change |
|---|---|
| `src/graphtheory/Vertex.java` | Rename field; add `inNeighbors`, `outNeighbors`; add `degree()`, `inDegree()`, `outDegree()`, `isIsolated()`; add `isCutpoint`, `isRoot` flags; update `draw()` for color rings |
| `src/graphtheory/Edge.java` | Add `directed` boolean, update constructors, add `isSelfLoop()`, update `draw()` for arrowheads and self-loop visual |
| `src/graphtheory/GraphProperties.java` | Add Tarjan's cutpoint algorithm; add `drawNodePropertiesTable()`; fix `generateAdjacencyMatrix` for directed edges |
| `src/graphtheory/Canvas.java` | Rename references; add directed-edge tool (tool 5); remove self-loop guard; add "Mark as Root" and "Add Directed Edge" menu items; add `drawInfoBox()`; call `computeCutpoints` and `drawNodePropertiesTable` in Properties window |
| `src/graphtheory/VertexPair.java` | Rename `connectedVertices` → `undirectedNeighbors` references |
| `src/graphtheory/FileManager.java` | Rename `connectedVertices`/`addVertex` references; note: directed edges not yet persisted to file (known limitation) |
| `test/graphtheory/VertexTest.java` | **New** — unit tests for degree methods and `isIsolated()` |
| `test/graphtheory/GraphPropertiesTest.java` | **New** — unit tests for Tarjan's cutpoint algorithm |

---

## Task 1: Rename `connectedVertices` → `undirectedNeighbors`

**Files:**
- Modify: `src/graphtheory/Vertex.java`
- Modify: `src/graphtheory/Canvas.java`
- Modify: `src/graphtheory/GraphProperties.java`
- Modify: `src/graphtheory/VertexPair.java`
- Modify: `src/graphtheory/FileManager.java`

This is a pure mechanical rename — no logic changes. Do it first so all later tasks work against the final name.

- [ ] **Step 1: Rename the field and method in `Vertex.java`**

Replace the entire class with these changes (field rename + method rename):

```java
public Vector<Vertex> undirectedNeighbors;   // was: connectedVertices

public Vertex(String name, int x, int y) {
    this.name = name;
    location = new Point(x, y);
    undirectedNeighbors = new Vector<Vertex>();   // was: connectedVertices
}

public void addUndirectedNeighbor(Vertex v) {   // was: addVertex
    undirectedNeighbors.add(v);
}

public boolean connectedToVertex(Vertex v) {
    return undirectedNeighbors.contains(v);   // same logic, new field name
}

public int getDegree() {
    return undirectedNeighbors.size();   // temporary; replaced properly in Task 3
}
```

- [ ] **Step 2: Update `Canvas.java`**

Find every occurrence of `connectedVertices` and `addVertex(` in Canvas.java. There is one occurrence of `addVertex` in `mouseReleased` (case 2):

```java
// BEFORE:
v.addVertex(parentV);
parentV.addVertex(v);

// AFTER:
v.addUndirectedNeighbor(parentV);
parentV.addUndirectedNeighbor(v);
```

- [ ] **Step 3: Update `VertexPair.java`**

`connectedVertices` appears in `getShortestDistance()` and `recursePaths()`. Replace every `.connectedVertices` with `.undirectedNeighbors`:

```java
// getShortestDistance — line 39:
for (Vertex x : visitedNodes.get(i).undirectedNeighbors) {

// recursePaths — line 124:
for (Vertex x : v.undirectedNeighbors) {
```

- [ ] **Step 4: Update `GraphProperties.java`**

Two methods use `connectedVertices`:

```java
// vertexConnectivity() — line 171:
for (Vertex v : origList) {
    if (v.getDegree() == maxPossibleRemove) {
        for (Vertex z : v.undirectedNeighbors) {   // was connectedVertices

// vertexConnectivity() — line 185:
for (Vertex x : origList) {
    x.undirectedNeighbors.remove(victim);   // was connectedVertices

// recurseGraphConnectivity() — line 210:
recurseGraphConnectivity(vList.firstElement().undirectedNeighbors, visitedList);

// recurseGraphConnectivity() — line 213:
recurseGraphConnectivity(v.undirectedNeighbors, visitedList);
```

- [ ] **Step 5: Update `FileManager.java`**

Two call sites:

```java
// saveFile — line 44:
if (vList.get(i).connectedToVertex(vList.get(j))) {   // no change needed, method name kept

// loadFile — line 83:
vertexList.get(j).addUndirectedNeighbor(vertexList.get(k));   // was addVertex

// loadFile — line 90:
Edge e = new Edge(vertexList.get(j), vertexList.get(l));   // no change yet
```

- [ ] **Step 6: Build the project in NetBeans**

Open the project in NetBeans. Press **F11** (Clean and Build). Expected: BUILD SUCCESSFUL with zero errors.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: rename connectedVertices to undirectedNeighbors"
```

---

## Task 2: Add `directed` flag and `isSelfLoop()` to `Edge`; update drawing

**Files:**
- Modify: `src/graphtheory/Edge.java`

- [ ] **Step 1: Update `Edge.java` — add fields and constructors**

Replace the entire `Edge.java` with:

```java
package graphtheory;

import java.awt.Color;
import java.awt.Graphics;

public class Edge {

    public Vertex vertex1;
    public Vertex vertex2;
    public boolean directed;
    public boolean wasFocused;
    public boolean wasClicked;

    public Edge(Vertex v1, Vertex v2, boolean directed) {
        vertex1 = v1;
        vertex2 = v2;
        this.directed = directed;
    }

    public boolean isSelfLoop() {
        return vertex1 == vertex2;
    }

    public void draw(Graphics g) {
        if (wasClicked) {
            g.setColor(Color.red);
        } else if (wasFocused) {
            g.setColor(Color.blue);
        } else {
            g.setColor(Color.black);
        }

        if (isSelfLoop()) {
            // draw a small circle above the vertex
            int lx = vertex1.location.x;
            int ly = vertex1.location.y;
            g.drawOval(lx - 10, ly - 40, 24, 24);
        } else {
            g.drawLine(vertex1.location.x, vertex1.location.y,
                       vertex2.location.x, vertex2.location.y);
            if (directed) {
                drawArrowhead(g);
            }
        }
    }

    private void drawArrowhead(Graphics g) {
        int x1 = vertex1.location.x, y1 = vertex1.location.y;
        int x2 = vertex2.location.x, y2 = vertex2.location.y;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 12;
        g.drawLine(x2, y2,
                (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6)),
                (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6)));
        g.drawLine(x2, y2,
                (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6)),
                (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6)));
    }

    public boolean hasIntersection(int x, int y) {
        if (isSelfLoop()) return false;   // self-loops not clickable yet
        int x1, x2, y1, y2;
        x1 = vertex1.location.x;
        x2 = vertex2.location.x;
        y1 = vertex1.location.y;
        y2 = vertex2.location.y;
        float slope = 0;
        if (x2 != x1) {
            slope = (y2 - y1) / (x2 - x1);
        }
        float b = Math.abs(x1 * slope - y1);
        if (y + b <= Math.round(slope * x) + 10 && y + b >= Math.round(slope * x) - 10) {
            if (x1 > x2 && y1 > y2) {
                if (x <= x1 && x >= x2 && y <= y1 && y >= y2) return true;
            } else if (x1 < x2 && y1 > y2) {
                if (x <= x2 && x >= x1 && y <= y1 && y >= y2) return true;
            } else if (x1 < x2 && y1 < y2) {
                if (x <= x2 && x >= x1 && y <= y2 && y >= y1) return true;
            } else if (x <= x1 && x >= x2 && y <= y2 && y >= y1) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Fix the two call sites that use the old no-arg `Edge` constructor**

`Canvas.java` line ~215 (mouseReleased case 2):
```java
// BEFORE:
Edge edge = new Edge(v, parentV);

// AFTER:
Edge edge = new Edge(v, parentV, false);
```

`FileManager.java` line ~90 (loadFile):
```java
// BEFORE:
Edge e = new Edge(vertexList.get(j), vertexList.get(l));

// AFTER:
Edge e = new Edge(vertexList.get(j), vertexList.get(l), false);
```

- [ ] **Step 3: Build the project (F11 in NetBeans)**

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add directed flag and isSelfLoop to Edge; draw arrowheads and self-loop visuals"
```

---

## Task 3: Set up JUnit test infrastructure

**Files:**
- Create: `test/graphtheory/VertexTest.java`
- Create: `test/graphtheory/GraphPropertiesTest.java`

NetBeans manages the `test/` source root automatically. Right-click project > New > JUnit Test to confirm JUnit 4 is available.

- [ ] **Step 1: Add JUnit 4 to the project**

In NetBeans: right-click project > Properties > Libraries > Compile Tests tab > Add Library > select **JUnit 4** > OK.

- [ ] **Step 2: Create `test/graphtheory/VertexTest.java` (placeholder for now)**

```java
package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class VertexTest {

    @Test
    public void placeholder() {
        assertTrue(true);
    }
}
```

- [ ] **Step 3: Create `test/graphtheory/GraphPropertiesTest.java` (placeholder)**

```java
package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class GraphPropertiesTest {

    @Test
    public void placeholder() {
        assertTrue(true);
    }
}
```

- [ ] **Step 4: Run the tests**

In NetBeans: right-click `VertexTest.java` > Test File.
Expected: 1 test passed, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: set up JUnit 4 test infrastructure"
```

---

## Task 4: Add `inNeighbors`/`outNeighbors` to `Vertex`; implement degree methods and `isIsolated()`

**Files:**
- Modify: `src/graphtheory/Vertex.java`
- Modify: `test/graphtheory/VertexTest.java`

- [ ] **Step 1: Write the failing tests first**

Replace `test/graphtheory/VertexTest.java` entirely:

```java
package graphtheory;

import org.junit.Test;
import static org.junit.Assert.*;

public class VertexTest {

    // --- degree() ---

    @Test
    public void degree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.degree());
    }

    @Test
    public void degree_twoUndirectedNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        Vertex a = new Vertex("a", 0, 0);
        Vertex b = new Vertex("b", 0, 0);
        v.undirectedNeighbors.add(a);
        v.undirectedNeighbors.add(b);
        assertEquals(2, v.degree());
    }

    @Test
    public void degree_selfLoop_countsAsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.undirectedNeighbors.add(v);   // self-loop
        assertEquals(2, v.degree());
    }

    @Test
    public void degree_ignoresDirectedNeighbors() {
        Vertex v = new Vertex("v", 0, 0);
        Vertex a = new Vertex("a", 0, 0);
        v.inNeighbors.add(a);
        v.outNeighbors.add(a);
        assertEquals(0, v.degree());   // directed edges don't count toward undirected degree
    }

    // --- inDegree() ---

    @Test
    public void inDegree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.inDegree());
    }

    @Test
    public void inDegree_twoInNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(new Vertex("a", 0, 0));
        v.inNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(2, v.inDegree());
    }

    @Test
    public void inDegree_directedSelfLoop_countsAsOne() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(v);
        assertEquals(1, v.inDegree());
    }

    // --- outDegree() ---

    @Test
    public void outDegree_noEdges_returnsZero() {
        Vertex v = new Vertex("v", 0, 0);
        assertEquals(0, v.outDegree());
    }

    @Test
    public void outDegree_twoOutNeighbors_returnsTwo() {
        Vertex v = new Vertex("v", 0, 0);
        v.outNeighbors.add(new Vertex("a", 0, 0));
        v.outNeighbors.add(new Vertex("b", 0, 0));
        assertEquals(2, v.outDegree());
    }

    // --- isIsolated() ---

    @Test
    public void isIsolated_noNeighbors_returnsTrue() {
        Vertex v = new Vertex("v", 0, 0);
        assertTrue(v.isIsolated());
    }

    @Test
    public void isIsolated_hasUndirectedNeighbor_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.undirectedNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }

    @Test
    public void isIsolated_hasInNeighborOnly_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.inNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }

    @Test
    public void isIsolated_hasOutNeighborOnly_returnsFalse() {
        Vertex v = new Vertex("v", 0, 0);
        v.outNeighbors.add(new Vertex("a", 0, 0));
        assertFalse(v.isIsolated());
    }
}
```

- [ ] **Step 2: Run the tests — verify they FAIL**

Right-click `VertexTest.java` > Test File.
Expected: multiple failures — `inNeighbors`, `outNeighbors`, `degree()`, `inDegree()`, `outDegree()`, `isIsolated()` do not exist yet.

- [ ] **Step 3: Add `inNeighbors`, `outNeighbors`, and the new methods to `Vertex.java`**

Add these fields and methods to `Vertex.java`. Place the fields right after `undirectedNeighbors`:

```java
public Vector<Vertex> inNeighbors;
public Vector<Vertex> outNeighbors;
public boolean isCutpoint;
public boolean isRoot;
```

Update the constructor to initialise them:

```java
public Vertex(String name, int x, int y) {
    this.name = name;
    location = new Point(x, y);
    undirectedNeighbors = new Vector<Vertex>();
    inNeighbors = new Vector<Vertex>();
    outNeighbors = new Vector<Vertex>();
}
```

Replace `getDegree()` with three separate methods (keep `getDegree()` as a delegate for now — `GraphProperties` comparators use it):

```java
public int degree() {
    int d = undirectedNeighbors.size();
    for (Vertex v : undirectedNeighbors) {
        if (v == this) d++;   // undirected self-loop counts as 2
    }
    return d;
}

public int inDegree() {
    return inNeighbors.size();   // directed self-loop already counts as 1
}

public int outDegree() {
    return outNeighbors.size();   // directed self-loop already counts as 1
}

public int getDegree() {
    return degree() + inDegree() + outDegree();
}

public boolean isIsolated() {
    return undirectedNeighbors.isEmpty() && inNeighbors.isEmpty() && outNeighbors.isEmpty();
}
```

Also update `connectedToVertex` to check all three lists:

```java
public boolean connectedToVertex(Vertex v) {
    return undirectedNeighbors.contains(v)
        || inNeighbors.contains(v)
        || outNeighbors.contains(v);
}
```

- [ ] **Step 4: Run the tests — verify they PASS**

Right-click `VertexTest.java` > Test File.
Expected: all 14 tests pass.

- [ ] **Step 5: Build the full project (F11)**

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add inNeighbors/outNeighbors/degree methods/isIsolated to Vertex"
```

---

## Task 5: Allow self-loops; add directed-edge tool to Canvas

**Files:**
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Add "Add Directed Edge" menu item**

In `Canvas` constructor, after the `"Add Edges"` menu item block, add:

```java
item = new JMenuItem("Add Directed Edge");
item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
item.addActionListener(new MenuListener());
menuOptions.add(item);
```

- [ ] **Step 2: Handle the new menu action in `MenuListener.actionPerformed`**

Add this branch (alongside the existing tool selections):

```java
} else if (command.equals("Add Directed Edge")) {
    selectedTool = 5;
}
```

- [ ] **Step 3: Update `mouseReleased` — remove self-loop guard and add directed edge case**

The existing case 2 block currently has `v != parentV` in the condition. Remove it, and update the undirected edge creation to use the new constructor:

```java
case 2: {   // undirected edge (allows self-loops)
    Vertex parentV = vertexList.get(clickedVertexIndex);
    for (Vertex v : vertexList) {
        if (v.hasIntersection(e.getX(), e.getY()) && !v.connectedToVertex(parentV)) {
            Edge edge = new Edge(v, parentV, false);
            v.addUndirectedNeighbor(parentV);
            parentV.addUndirectedNeighbor(v);
            v.wasClicked = false;
            parentV.wasClicked = false;
            edgeList.add(edge);
        } else {
            v.wasClicked = false;
        }
    }
    break;
}
```

Add a new case 5 directly after case 2, inside the same `switch`:

```java
case 5: {   // directed edge (source = parentV, destination = v)
    Vertex parentV = vertexList.get(clickedVertexIndex);
    for (Vertex v : vertexList) {
        if (v.hasIntersection(e.getX(), e.getY()) && !v.connectedToVertex(parentV)) {
            Edge edge = new Edge(parentV, v, true);
            parentV.outNeighbors.add(v);
            v.inNeighbors.add(parentV);
            parentV.wasClicked = false;
            v.wasClicked = false;
            edgeList.add(edge);
        } else {
            v.wasClicked = false;
        }
    }
    break;
}
```

- [ ] **Step 4: Manual test — undirected edge**

Run the project. Use Add Vertex (Ctrl+A) to place two vertices. Use Add Edges (Ctrl+E), drag from one vertex to another. Verify a plain line appears.

- [ ] **Step 5: Manual test — directed edge**

Use Add Directed Edge (Ctrl+D), drag from vertex A to vertex B. Verify an arrowhead appears at vertex B.

- [ ] **Step 6: Manual test — self-loop**

Use Add Edges (Ctrl+E), click-drag a vertex and release on that same vertex. Verify a small circle appears on the vertex.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: allow self-loops and add directed edge tool to Canvas"
```

---

## Task 6: Add `isRoot` to `Vertex`; add "Mark as Root" menu action

**Files:**
- Modify: `src/graphtheory/Canvas.java`

(`isRoot` boolean was already added to `Vertex` in Task 4.)

- [ ] **Step 1: Add "Mark as Root" menu item**

In the `Canvas` constructor, add to `menuOptions2`:

```java
item = new JMenuItem("Mark as Root");
item.addActionListener(new MenuListener());
menuOptions2.add(item);
```

- [ ] **Step 2: Handle "Mark as Root" in `MenuListener.actionPerformed`**

```java
} else if (command.equals("Mark as Root")) {
    for (Vertex v : vertexList) {
        if (v.wasClicked) {
            v.isRoot = !v.isRoot;   // toggle
        }
    }
    erase();
}
```

- [ ] **Step 3: Manual test**

Run the project. Place a vertex. Click it (wasClicked = true, vertex turns red). Open Extras > Mark as Root. The vertex's `isRoot` is now true. (Visual ring added in Task 10.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add isRoot flag to Vertex and Mark as Root menu action"
```

---

## Task 7: Implement Tarjan's cutpoint algorithm in `GraphProperties`

**Files:**
- Modify: `src/graphtheory/GraphProperties.java`
- Modify: `test/graphtheory/GraphPropertiesTest.java`

- [ ] **Step 1: Write the failing tests**

Replace `test/graphtheory/GraphPropertiesTest.java`:

```java
package graphtheory;

import org.junit.Test;
import java.util.Vector;
import static org.junit.Assert.*;

public class GraphPropertiesTest {

    // Helper: build undirected edge and wire up both neighbor lists
    private void addUndirectedEdge(Vertex a, Vertex b) {
        a.undirectedNeighbors.add(b);
        b.undirectedNeighbors.add(a);
        // edgeList not needed for cutpoint computation
    }

    // Graph: A - B - C  (path of 3)
    // Only B is a cutpoint
    @Test
    public void cutpoints_pathOfThree_onlyMiddleIsCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        GraphProperties gp = new GraphProperties();
        gp.computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertTrue(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    // Graph: A - B - C - A  (triangle)
    // No cutpoints
    @Test
    public void cutpoints_triangle_noCutpoints() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);
        addUndirectedEdge(c, a);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        GraphProperties gp = new GraphProperties();
        gp.computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertFalse(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    // Graph: A - B - C, A - D - C  (two paths sharing endpoints)
    // Neither A nor C is a cutpoint (two independent paths)
    @Test
    public void cutpoints_twoPathsSharedEndpoints_noCutpoints() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        Vertex d = new Vertex("D", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);
        addUndirectedEdge(a, d);
        addUndirectedEdge(d, c);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c); vList.add(d);

        GraphProperties gp = new GraphProperties();
        gp.computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertFalse(c.isCutpoint);
    }

    // Self-loops should not affect cutpoint detection
    @Test
    public void cutpoints_selfLoopOnPathMiddle_middleStillCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        addUndirectedEdge(a, b);
        addUndirectedEdge(b, c);
        b.undirectedNeighbors.add(b);   // self-loop on b

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        GraphProperties gp = new GraphProperties();
        gp.computeCutpoints(vList);

        assertTrue(b.isCutpoint);
    }

    // Directed edges treated as undirected for cutpoint purposes
    @Test
    public void cutpoints_directedPathOfThree_middleIsCutpoint() {
        Vertex a = new Vertex("A", 0, 0);
        Vertex b = new Vertex("B", 0, 0);
        Vertex c = new Vertex("C", 0, 0);
        // directed: a -> b -> c (stored in inNeighbors/outNeighbors)
        a.outNeighbors.add(b); b.inNeighbors.add(a);
        b.outNeighbors.add(c); c.inNeighbors.add(b);

        Vector<Vertex> vList = new Vector<>();
        vList.add(a); vList.add(b); vList.add(c);

        GraphProperties gp = new GraphProperties();
        gp.computeCutpoints(vList);

        assertFalse(a.isCutpoint);
        assertTrue(b.isCutpoint);
        assertFalse(c.isCutpoint);
    }
}
```

- [ ] **Step 2: Run the tests — verify they FAIL**

Right-click `GraphPropertiesTest.java` > Test File.
Expected: failures — `computeCutpoints` does not exist yet.

- [ ] **Step 3: Add `computeCutpoints` to `GraphProperties.java`**

Add these imports at the top of `GraphProperties.java`:

```java
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
```

Add these methods to the `GraphProperties` class:

```java
public void computeCutpoints(Vector<Vertex> vList) {
    int n = vList.size();
    int[] disc = new int[n];
    int[] low = new int[n];
    int[] parent = new int[n];
    boolean[] visited = new boolean[n];
    boolean[] ap = new boolean[n];
    int[] timer = {0};
    Arrays.fill(parent, -1);

    for (int i = 0; i < n; i++) {
        if (!visited[i]) {
            dfsAP(i, vList, disc, low, parent, visited, ap, timer);
        }
    }
    for (int i = 0; i < n; i++) {
        vList.get(i).isCutpoint = ap[i];
    }
}

private void dfsAP(int u, Vector<Vertex> vList, int[] disc, int[] low,
                   int[] parent, boolean[] visited, boolean[] ap, int[] timer) {
    visited[u] = true;
    disc[u] = low[u] = timer[0]++;
    int children = 0;

    for (int v : getAllNeighborIndices(u, vList)) {
        if (v == u) continue;   // skip self-loops
        if (!visited[v]) {
            children++;
            parent[v] = u;
            dfsAP(v, vList, disc, low, parent, visited, ap, timer);
            low[u] = Math.min(low[u], low[v]);
            if (parent[u] == -1 && children > 1) ap[u] = true;
            if (parent[u] != -1 && low[v] >= disc[u]) ap[u] = true;
        } else if (v != parent[u]) {
            low[u] = Math.min(low[u], disc[v]);
        }
    }
}

private Set<Integer> getAllNeighborIndices(int u, Vector<Vertex> vList) {
    Vertex vu = vList.get(u);
    Set<Integer> neighbors = new HashSet<>();
    for (Vertex n : vu.undirectedNeighbors) {
        int idx = vList.indexOf(n);
        if (idx >= 0) neighbors.add(idx);
    }
    for (Vertex n : vu.inNeighbors) {
        int idx = vList.indexOf(n);
        if (idx >= 0) neighbors.add(idx);
    }
    for (Vertex n : vu.outNeighbors) {
        int idx = vList.indexOf(n);
        if (idx >= 0) neighbors.add(idx);
    }
    return neighbors;
}
```

- [ ] **Step 4: Run the tests — verify they PASS**

Right-click `GraphPropertiesTest.java` > Test File.
Expected: all 5 tests pass.

- [ ] **Step 5: Build the full project (F11)**

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: implement Tarjan's cutpoint detection in GraphProperties"
```

---

## Task 8: Update adjacency matrix for directed edges; wire cutpoints into Properties window

**Files:**
- Modify: `src/graphtheory/GraphProperties.java`
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Update `generateAdjacencyMatrix` in `GraphProperties.java`**

Replace the existing `generateAdjacencyMatrix` method:

```java
public int[][] generateAdjacencyMatrix(Vector<Vertex> vList, Vector<Edge> eList) {
    adjacencyMatrix = new int[vList.size()][vList.size()];

    for (Edge e : eList) {
        int i = vList.indexOf(e.vertex1);
        int j = vList.indexOf(e.vertex2);
        if (i < 0 || j < 0) continue;
        adjacencyMatrix[i][j] = 1;
        if (!e.directed) {
            adjacencyMatrix[j][i] = 1;   // undirected: both directions
        }
    }
    return adjacencyMatrix;
}
```

- [ ] **Step 2: Call `computeCutpoints` in `Canvas.MenuListener` when Properties window opens**

In `Canvas.java`, find the `command.equals("Properties")` block. Add `gP.computeCutpoints(vertexList);` right after `gP.generateDistanceMatrix(vertexList);`:

```java
} else if (command.equals("Properties")) {
    selectedWindow = 1;
    if (vertexList.size() > 0) {
        int[][] matrix = gP.generateAdjacencyMatrix(vertexList, edgeList);

        Vector<Vertex> tempList = gP.vertexConnectivity(vertexList);
        for (Vertex v : tempList) {
            vertexList.get(vertexList.indexOf(v)).wasClicked = true;
        }
        reloadVertexConnections(matrix, vertexList);

        gP.generateDistanceMatrix(vertexList);
        gP.computeCutpoints(vertexList);   // <-- add this line
        gP.displayContainers(vertexList);
    }
    erase();
}
```

- [ ] **Step 3: Build and manual test**

Press F11. Run the project. Build a path A-B-C. Open Properties window. In the console, confirm no errors. (Visual table added next task.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: update adjacency matrix for directed edges; compute cutpoints on Properties open"
```

---

## Task 9: Draw node properties table in Properties window

**Files:**
- Modify: `src/graphtheory/GraphProperties.java`
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Add `drawNodePropertiesTable` to `GraphProperties.java`**

The Properties window layout puts the minimap in the top-left quadrant (0,0 to width/2, height/2). The table must go below the minimap — pass `x=10, y=height/2+70` from the call site.

```java
public void drawNodePropertiesTable(Graphics g, Vector<Vertex> vList, int x, int y) {
    int rowH = 18;
    int colW = 65;
    String[] headers = {"Name", "Deg", "In", "Out", "Isolated", "Cut", "Root"};

    g.setColor(new Color(230, 230, 230));
    g.fillRect(x, y - 20, headers.length * colW, (vList.size() + 2) * rowH + 10);
    g.setColor(Color.BLACK);
    g.drawString("Node Properties", x, y - 5);

    for (int i = 0; i < headers.length; i++) {
        g.setColor(new Color(80, 80, 80));
        g.drawString(headers[i], x + i * colW + 3, y + rowH);
    }

    for (int r = 0; r < vList.size(); r++) {
        Vertex v = vList.get(r);
        int ry = y + (r + 2) * rowH;
        g.setColor(Color.BLACK);
        g.drawString(v.name,                  x + 0 * colW + 3, ry);
        g.drawString("" + v.degree(),         x + 1 * colW + 3, ry);
        g.drawString("" + v.inDegree(),       x + 2 * colW + 3, ry);
        g.drawString("" + v.outDegree(),      x + 3 * colW + 3, ry);
        g.drawString("" + v.isIsolated(),     x + 4 * colW + 3, ry);
        g.drawString("" + v.isCutpoint,       x + 5 * colW + 3, ry);
        g.drawString("" + v.isRoot,           x + 6 * colW + 3, ry);
    }
}
```

- [ ] **Step 2: Call `drawNodePropertiesTable` in `Canvas.CanvasPane.paint()`, Properties window case**

In the `case 1:` branch of `CanvasPane.paint()`, add the call after the `drawDistanceMatrix` call:

```java
case 1: {
    canvasImage2.getGraphics().clearRect(0, 0, width, height);
    gP.drawAdjacencyMatrix(canvasImage2.getGraphics(), vertexList, width / 2 + 50, 50);
    gP.drawDistanceMatrix(canvasImage2.getGraphics(), vertexList, width / 2 + 50, height / 2 + 50);
    gP.drawNodePropertiesTable(canvasImage2.getGraphics(), vertexList, 10, height / 2 + 70);   // <-- add this; below the minimap
    g.drawImage(canvasImage2, 0, 0, null);
    drawString("Graph disconnects when nodes in color red are removed.", 100, height - 30, 20);
    g.drawString("See output console for Diameter of Graph", 100, height / 2 + 50);
    g.drawImage(canvasImage.getScaledInstance(width / 2, height / 2, Image.SCALE_SMOOTH), 0, 0, null);
    g.draw3DRect(0, 0, width / 2, height / 2, true);
    g.setColor(Color.black);
    break;
}
```

- [ ] **Step 3: Manual test**

Run the project. Place three vertices: A-B-C in a path. Open Properties window. Verify a table appears with columns Name | Deg | In | Out | Isolated | Cut | Root. B should have Cut=true, others false. All Isolated=false.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: draw node properties table in Properties window"
```

---

## Task 10: Add fixed info box to Graph window

**Files:**
- Modify: `src/graphtheory/Canvas.java`

- [ ] **Step 1: Add `drawInfoBox` method to `Canvas`**

Add this private method to `Canvas` (place it near the other draw helpers):

```java
private void drawInfoBox(Graphics g) {
    Vertex clicked = null;
    for (Vertex v : vertexList) {
        if (v.wasClicked) { clicked = v; break; }
    }
    if (clicked == null) return;

    int x = 10, y = 10, w = 170, h = 126;
    g.setColor(new Color(245, 245, 245));
    g.fillRect(x, y, w, h);
    g.setColor(Color.BLACK);
    g.drawRect(x, y, w, h);

    int ty = y + 16;
    g.drawString("Vertex: " + clicked.name,               x + 6, ty); ty += 16;
    g.drawString("Degree: " + clicked.degree(),           x + 6, ty); ty += 16;
    g.drawString("In-Degree: " + clicked.inDegree(),      x + 6, ty); ty += 16;
    g.drawString("Out-Degree: " + clicked.outDegree(),    x + 6, ty); ty += 16;
    g.drawString("Isolated: " + clicked.isIsolated(),     x + 6, ty); ty += 16;
    g.drawString("Cutpoint: " + clicked.isCutpoint,       x + 6, ty); ty += 16;
    g.drawString("Root: " + clicked.isRoot,               x + 6, ty);
}
```

- [ ] **Step 2: Call `drawInfoBox` in `CanvasPane.paint()`, Graph window case**

In the `case 0:` branch of `CanvasPane.paint()`, add the call after `g.drawImage(canvasImage, 0, 0, null)`:

```java
case 0: {
    graphic.drawString("Vertex Count=" + vertexList.size() +
            "  Edge Count=" + edgeList.size() +
            "  Selected Tool=" + selectedTool, 50, height / 2 + (height * 2) / 5);
    g.drawImage(canvasImage, 0, 0, null);
    drawInfoBox(g);   // <-- add this
    g.setColor(Color.black);
    break;
}
```

- [ ] **Step 3: Manual test**

Run the project. Place vertices and edges. Select the Grab Tool (Ctrl+G) and click a vertex. Verify a white info box appears in the top-left showing all six properties for that vertex. Click elsewhere — box should disappear (no vertex wasClicked).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add fixed info box showing node properties on vertex click"
```

---

## Task 11: Add color rings to `Vertex.draw()` for root / cutpoint / isolated

**Files:**
- Modify: `src/graphtheory/Vertex.java`

- [ ] **Step 1: Update `draw()` in `Vertex.java`**

Replace the existing `draw(Graphics g)` method:

```java
public void draw(Graphics g) {
    // Draw property ring behind the vertex circle
    if (isRoot) {
        g.setColor(new Color(0, 180, 0));   // green
        g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
    } else if (isCutpoint) {
        g.setColor(new Color(255, 140, 0));   // orange
        g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
    } else if (isIsolated()) {
        g.setColor(Color.GRAY);
        g.fillOval(location.x - size2 / 2 - 4, location.y - size2 / 2 - 4, size2 + 8, size2 + 8);
    }

    // Existing vertex drawing
    if (wasClicked) {
        g.setColor(Color.red);
    } else if (wasFocused) {
        g.setColor(Color.blue);
    } else {
        g.setColor(Color.black);
    }

    g.fillOval(location.x - size2 / 2, location.y - size2 / 2, size2, size2);
    g.setColor(Color.WHITE);
    g.fillOval(location.x - size1 / 2, location.y - size1 / 2, size1, size1);
    g.setColor(Color.BLACK);
    g.drawString(name, location.x, location.y);
}
```

- [ ] **Step 2: Manual test — isolated vertex**

Run the project. Place a single vertex with no edges. Verify it has a **grey** ring around it.

- [ ] **Step 3: Manual test — root vertex**

Place two connected vertices. Select one, Extras > Mark as Root. Verify it shows a **green** ring.

- [ ] **Step 4: Manual test — cutpoint vertex**

Place A-B-C path. Open Properties window (to trigger `computeCutpoints`). Switch back to Graph window. Vertex B should show an **orange** ring.

- [ ] **Step 5: Run all tests (F6 in NetBeans — Run All Tests)**

Expected: all tests still pass (19 total).

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "feat: draw color rings on vertices for root (green), cutpoint (orange), isolated (grey)"
```

---

## Known Limitation

File save/load (`FileManager`) does not yet persist directed edges or `isRoot`. Loaded graphs will have all edges treated as undirected. This is intentional scope — file format extension is a separate plan.
