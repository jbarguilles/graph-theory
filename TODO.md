# TODO

- [ ] **Resizable window / canvas (UI phase 2).** Window is fixed at 800×600 (`setResizable(false)`); `width`/`height` are baked into Auto Arrange, load-time clamping and the Properties layout.
- [ ] **Properties view as real Swing tables (UI phase 2).** Currently painted pixel-by-pixel; can't select, copy or sort, and matrix cells are 20px wide.
- [ ] **Save walks in graph files.** Graph files currently store only the graph (vertices, edges, weights, roots). Consider optionally saving the built walk (e.g. a found Euler tour) so it reopens with the file. Walk steps must reference specific edges, not just vertex pairs, since parallel edges exist (ADR 0002).
