# ADR 0005 — Preference Lists Are Saved in Graph Files

**Status:** Accepted (amends ADR 0004)

Stable matching needs each vertex to rank its neighbours (CONTEXT.md, *Preference list*). We treat those rankings as part of the graph, like weights: they are saved in the graph file, editing them is an unsaved change and an undoable edit, and an exercise file can ship with them. The previous summary invented preferences (neighbours in name order), which made its "stable matching" meaningless.

The file gains one line kind, `prefer a x z y` (vertex `a` ranks `x`, then `z`, then `y`), and a version 2 header. A file is written as `graph-theory 2` only when some vertex has a preference list; otherwise it is still written as `graph-theory 1`, so graphs without preferences stay readable by older builds. The reader accepts both versions; `prefer` lines are only valid in version 2, must come after every `vertex` and `edge`/`arc` line, and must name each of the vertex's neighbours (ignoring direction) exactly once, or the whole file is rejected as usual.

## Considered Options

- **Analysis-only preferences** (entered in a dialog, never saved, not an edit) — rejected: every exercise would need the lists typed in again each session, which defeats the point for a teaching tool.
- **Always writing version 2** — rejected: it would make every newly saved file unreadable by older builds for no benefit when there are no preferences.
- **Free-form lists that may name non-neighbours** (an edge plus mutual listing meaning "acceptable") — rejected: lists and edges could disagree in confusing ways. Lists instead follow edge edits automatically.

## Consequences

- The **graph file** definition in CONTEXT.md now includes each vertex's preference list.
- Undo snapshots, unsaved-change detection and the Properties tab's change key are all the graph-file text, so they pick up preference edits with no extra wiring.
- The proposing side for a stable matching is analysis, not part of the graph, and is not saved.
