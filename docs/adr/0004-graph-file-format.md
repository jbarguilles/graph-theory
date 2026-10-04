# ADR 0004 — Line-Based, Hand-Writable Graph File Format

**Status:** Accepted

Graphs are saved as `.graph` text files with one declaration per line (`vertex a 120 200 root`, `edge a b 5`, `arc b c`), after a `graph-theory 1` version header. It replaces the original adjacency-matrix format, which could not represent mixed graphs (ADR 0001) or parallel edges (ADR 0002), dropped self-loops and roots, and misassigned weights. Old-format files are deliberately **not** readable — none needed preserving.

## Considered Options

- **JSON** — rejected: needs a library or a hand-written parser in an Ant/NetBeans project with no dependency management, and is tedious to type by hand.
- **Java serialization** — rejected: binary, unreadable, and breaks whenever a class changes.
- **Extending the adjacency matrix** — rejected: a matrix cell cannot hold several parallel edges with different directions and weights.

## Consequences

- Designed for hand-writing (exercise graphs, test fixtures): `#` comments and blank lines are ignored, weights are optional (default 1), and coordinates are optional (vertices without them are auto-arranged on load). Saving always writes the full form.
- Vertices are referenced by **name**, which is why names are restricted to 1–4 characters of letters, digits and `_` and must be unique (CONTEXT.md).
- Line order is `vertexList`/`edgeList` order, so a future version can refer to edges by index (e.g. to save walks, see TODO.md). The header version lets that future format still read version 1.
- The file stores only the graph. Derived properties (cutpoints, bridges, distances), coloring, walks and selections are not saved.
- Invalid files are rejected as a whole with a line-numbered error; a partial graph is never loaded.
