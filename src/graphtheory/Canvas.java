package graphtheory;

/**
 *
 * @author mk
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Canvas {

    public JFrame frame;
    private CanvasPane canvas;
    private PropertiesPanel propertiesPanel;
    // The report the Properties tab shows, and the graph text + proposer it was built from.
    private PropertiesReport report;
    private String reportKey;
    // A vertex on the side that proposes for stable matching (analysis; not saved).
    private Vertex proposer;
    private Color backgroundColour;
    private int selectedTool;
    private int selectedWindow;
    public int width,  height;
    private int clickedVertexIndex = -1;
    private final EditHistory history = new EditHistory(50);
    private JMenuItem undoItem;
    private JMenuItem redoItem;
    private int clickedEdgeIndex;
    // The pair being picked with the Pair tool: first vertex, then second (null = not picked yet)
    private Vertex pairFirst = null;
    private Vertex pairSecond = null;
    private VertexPair currentPairVP = null;
    private FileManager fileManager = new FileManager();
    private File currentFile = null;
    private String savedText;
    private String pressBefore = null;
    // Where the mouse is while dragging out a new edge (Add/Directed Edge tools); null otherwise.
    private Point dragPoint = null;
    private final String appName;
    private final MenuListener menuListener = new MenuListener();
    private JTabbedPane tabs;
    private ToolPalette palette;
    private JLabel statusHint;
    private JLabel statusCounts;

    /////////////
    private Vector<Vertex> vertexList;
    private Vector<Edge> edgeList;
    private GraphProperties gP = new GraphProperties();
    /////////////

    private boolean graphDirty = true;

    // Build Walk tool (Tools.WALK)
    private Walk currentWalk = null;
    private String walkMessage = null;
    private static final Color WALK_COLOR = new Color(0, 150, 150);

    // The selected pair (Tools.PAIR): its paths and distances, and the path shown in amber
    private PairSummary pairSummary = null;
    private int selectedPathIndex = 0;
    private static final Color PATH_COLOR = new Color(220, 160, 0);

    // The Find command (e.g. "Euler Tour") that produced currentWalk, or null for a built walk
    private String foundKind = null;
    // The graph a found walk was found in; when it changes the found walk is dropped
    private GraphShape foundShape = null;

    private SidePanel sidePanel;

    public Canvas(String appName, int width, int height, Color bgColour) {
        this.appName = appName;
        this.width = width;
        this.height = height;
        backgroundColour = bgColour;
        vertexList = new Vector<Vertex>();
        edgeList = new Vector<Edge>();

        savedText = snapshot();

        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
        frame.setResizable(true);

        canvas = new CanvasPane();
        canvas.setPreferredSize(new Dimension(width, height));
        // The canvas holds keyboard focus so its key bindings and the menu accelerators reach it
        // (a focused text component in the side panel would consume arrows, Backspace, Ctrl+A/C).
        canvas.setFocusable(true);
        InputListener inputListener = new InputListener();
        canvas.addMouseListener(inputListener);
        canvas.addMouseMotionListener(inputListener);
        installKeyBindings();

        palette = new ToolPalette(new ToolPalette.Listener() {
            public void toolSelected(int tool) {
                selectTool(tool);
            }
        });
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

        tabs = new JTabbedPane();
        tabs.setFocusable(false);
        tabs.addTab("Graph", graphPanel);
        tabs.addTab("Properties", propertiesPanel);
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
        setVisible(true);
        canvas.requestFocusInWindow();
        selectTool(Tools.VERTEX);
        updateTitle();
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        addItem(file, "New", KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Open...", KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        addItem(file, "Save As...", KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        file.addSeparator();
        addItem(file, "Exit", null);

        JMenu edit = new JMenu("Edit");
        undoItem = addItem(edit, "Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        redoItem = addItem(edit, "Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        edit.addSeparator();
        addItem(edit, "Remove All", null);
        edit.addSeparator();
        addItem(edit, "Preference Lists...", null);

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
        addItem(extras, "Show Greedy Coloring", KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
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
        dragPoint = null;
        selectedTool = tool;
        palette.setSelectedTool(tool);
        tabs.setSelectedIndex(0);
        refresh();
    }

    private void onTabChanged() {
        selectedWindow = tabs.getSelectedIndex();
        clearHover();
        if (selectedWindow == 1) computeProperties();
        refresh();
        if (selectedWindow == 0) canvas.requestFocusInWindow();
    }

    /** The graph as .graph text: the unit of undo, saving and "unsaved changes". Brings preference lists in step first. */
    private String snapshot() {
        PreferenceLists.sync(vertexList, edgeList);
        return GraphFile.write(vertexList, edgeList);
    }

    /** Call after an edit with the snapshot from before it. */
    private void afterEdit(String before) {
        if (!before.equals(snapshot())) {
            if (foundShape != null && !foundShape.matches(vertexList, edgeList)) clearWalk();
            history.record(before);
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
        if (undoItem != null) {
            undoItem.setEnabled(history.canUndo());
            redoItem.setEnabled(history.canRedo());
        }
    }

    private Vertex vertexAt(int x, int y) {
        for (Vertex v : vertexList) {
            if (v.hasIntersection(x, y)) return v;
        }
        return null;
    }

    private Edge edgeAt(int x, int y) {
        return EdgeShapes.of(edgeList).nearest(x, y);
    }

    /** The Graph canvas's current size; before it is laid out, the size it was created with. */
    private int canvasWidth() {
        return canvas.getWidth() > 0 ? canvas.getWidth() : width;
    }

    private int canvasHeight() {
        return canvas.getHeight() > 0 ? canvas.getHeight() : height;
    }

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

    /** Swaps in a whole new graph (New, Open, Remove All, undo); clears analysis state. */
    private void replaceGraph(Vector<Vertex> vs, Vector<Edge> es) {
        vertexList = vs;
        edgeList = es;
        reportKey = null;
        clearWalk();
        clickedVertexIndex = -1;
        pairFirst = null;
        pairSecond = null;
        currentPairVP = null;
        pairSummary = null;
        pressBefore = null;
        dragPoint = null;
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
            showError("Couldn't undo", ex.getMessage());
            history.clear();
            updateTitle();
            return;
        }
        updateTitle();
    }

    private void newGraph() {
        if (!confirmDiscard("starting a new graph")) return;
        replaceGraph(new Vector<Vertex>(), new Vector<Edge>());
        history.clear();
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
        Layout.arrangeOnCircle(d.unplaced, canvasWidth(), canvasHeight());
        Layout.clampInto(d.vertices, canvasWidth(), canvasHeight());
        replaceGraph(d.vertices, d.edges);
        history.clear();
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

    /** Rebuilds the Properties report if the graph or the proposing side changed since the last one. */
    private void computeProperties() {
        recomputeGraphProperties();   // cutpoints, read by the Vertices table
        // Undo, redo and open rebuild the vertices: keep the side by finding the vertex of the same name.
        if (proposer != null && !vertexList.contains(proposer)) {
            String name = proposer.name;
            proposer = null;
            for (Vertex v : vertexList) if (v.name.equals(name)) proposer = v;
        }
        String key = snapshot() + "\u0000" + (proposer == null ? "" : proposer.name);
        if (!key.equals(reportKey)) {
            report = new PropertiesReport(vertexList, edgeList, proposer);
            reportKey = key;
        }
        propertiesPanel.display(report);
    }

    /** Edit > Preference Lists... and the Overview's button: OK is one undoable edit; Cancel records nothing. */
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

    private void updateStatus() {
        if (statusHint == null) return;
        statusHint.setText(selectedWindow == 0
                ? Tools.hint(selectedTool)
                : "Ctrl+C copies the selected cells \u00b7 Ctrl+Shift+C colours the graph greedily \u00b7 switch to the Graph tab to edit.");
        statusCounts.setText((selectedWindow == 0 ? "Ctrl+Shift+C: greedy colouring \u00b7 " : "")
                + vertexList.size() + " vertices \u00b7 " + edgeList.size() + " edges");
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                appName + "\n\n"
                + "Based on Graph Theory SY08-09 Term3 by Team DGLSS (v0.5).\n"
                + "Extended by jbarguilles and rcoporto.",
                "About " + appName, JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateHover(int mx, int my) {
        boolean removeMode = (selectedTool == 4);

        Vertex hoveredVertex = null;
        for (Vertex v : vertexList) {
            if (v.hasIntersection(mx, my)) { hoveredVertex = v; break; }
        }

        for (Vertex v : vertexList) {
            boolean hit = (v == hoveredVertex);
            v.wasFocused  = hit;
            v.removeHover = removeMode && hit;
        }

        Edge hoveredEdge = hoveredVertex == null ? edgeAt(mx, my) : null;
        for (Edge d : edgeList) {
            boolean hit = (d == hoveredEdge);
            d.wasFocused  = hit;
            d.removeHover = removeMode && hit;
        }
    }

    private void clearHover() {
        for (Vertex v : vertexList) {
            v.wasFocused  = false;
            v.removeHover = false;
        }
        for (Edge ed : edgeList) {
            ed.wasFocused  = false;
            ed.removeHover = false;
        }
    }

    private void markGraphDirty() {
        graphDirty = true;
        refreshPairPaths();
    }

    private void recomputeGraphProperties() {
        if (!graphDirty) return;
        if (vertexList.size() > 0) {
            gP.computeCutpoints(vertexList);
            Set<Edge> bridges = Blocks.bridges(vertexList, edgeList);
            for (Edge e : edgeList) e.isBridge = bridges.contains(e);
        } else {
            for (Vertex v : vertexList) v.isCutpoint = false;
            for (Edge e : edgeList)   e.isBridge   = false;
        }
        graphDirty = false;
    }

    private void clearWalk() {
        currentWalk = null;
        walkMessage = null;
        foundKind = null;
        foundShape = null;
    }

    /**
     * Forgets the pair (Esc with the Pair tool, picking a new first vertex, or the Remove tool
     * removing one of its vertices) and unselects its vertices.
     */
    private void clearPair() {
        if (pairFirst != null) pairFirst.wasClicked = false;
        if (pairSecond != null) pairSecond.wasClicked = false;
        pairFirst = null;
        pairSecond = null;
        currentPairVP = null;
        pairSummary = null;
        selectedPathIndex = 0;
    }

    /** Recomputes the selected pair's summary (paths, distances) after the pair or the graph changes. */
    private void refreshPairPaths() {
        selectedPathIndex = 0;
        pairSummary = currentPairVP == null ? null
                : new PairSummary(currentPairVP.vertex1, currentPairVP.vertex2, edgeList);
    }

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

    private void installKeyBindings() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "walkUndo");
        am.put("walkUndo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 9 && selectedWindow == 0) { undoWalkStep(); refresh(); }
            }
        });

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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "pathPrev");
        am.put("pathPrev", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 6 && selectedWindow == 0
                        && pairSummary != null && selectedPathIndex > 0) {
                    selectedPathIndex--;
                    refresh();
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "pathNext");
        am.put("pathNext", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 6 && selectedWindow == 0 && pairSummary != null
                        && selectedPathIndex < pairSummary.paths.size() - 1) {
                    selectedPathIndex++;
                    refresh();
                }
            }
        });

        // Also bound while the canvas has focus, ahead of the split pane's ancestor bindings
        // (its arrow keys move the divider and would swallow Up/Down before the window bindings).
        InputMap focused = canvas.getInputMap(JComponent.WHEN_FOCUSED);
        for (KeyStroke k : im.keys()) focused.put(k, im.get(k));
    }

    /** Removes the walk's last step; undoing a trivial walk clears it. */
    private void undoWalkStep() {
        if (foundKind != null) return;   // a found walk is read-only
        walkMessage = null;
        if (currentWalk != null && !currentWalk.undo()) currentWalk = null;
    }

    /** Tool 9 left-click: start the walk, or extend it by the clicked vertex or edge. */
    private void handleWalkClick(int x, int y) {
        Vertex hitV = null;
        for (Vertex v : vertexList) {
            if (v.hasIntersection(x, y)) { hitV = v; break; }
        }
        Edge hitE = hitV == null ? edgeAt(x, y) : null;
        if (hitV == null && hitE == null) return;
        if (foundKind != null) {
            // A found walk is read-only: clicking a vertex starts a new built walk in its place.
            if (hitV == null) {
                walkMessage = "Click a vertex to start a new walk";
                return;
            }
            clearWalk();
        }

        if (currentWalk == null) {
            if (hitV == null) {
                walkMessage = "Click a vertex to start the walk";
            } else {
                currentWalk = new Walk(hitV);
                walkMessage = null;
            }
            return;
        }

        Vertex end = currentWalk.end();
        if (hitE != null) {
            if (currentWalk.extend(hitE)) {
                walkMessage = null;
            } else {
                walkMessage = "Edge " + Walk.edgeLabel(hitE, hitE.vertex1)
                        + " can't be traversed from " + end.name;
            }
            return;
        }

        Vector<Edge> options = Walk.edgesBetween(end, hitV, edgeList);
        if (options.isEmpty()) {
            walkMessage = "No edge from " + end.name + " to " + hitV.name;
        } else if (options.size() > 1) {
            walkMessage = "Several edges from " + end.name + " to " + hitV.name
                    + ": click the edge to use";
        } else {
            currentWalk.extend(options.get(0));
            walkMessage = null;
        }
    }

    /**
     * Extras > Find ...: switches to the Build Walk tool on the Graph window and
     * loads the walk found, or says none exists. kind is e.g. "Euler Tour".
     */
    private void findTraversal(String kind) {
        selectedTool = Tools.WALK;
        palette.setSelectedTool(Tools.WALK);
        tabs.setSelectedIndex(0);
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
        } else if (kind.equals("Hamiltonian Cycle")) {
            currentWalk = Traversals.hamiltonianCycle(vertexList, edgeList);
        }
        if (currentWalk == null) {
            walkMessage = "No " + kind + " exists";
        } else {
            foundKind = kind;
            foundShape = GraphShape.of(vertexList, edgeList);
        }
    }

    private Set<Vertex> connectedComponentOf(Vertex start) {
        return Components.of(start, edgeList);
    }

    /** The root of start's connected component, or null. */
    private Vertex rootOf(Vertex start) {
        for (Vertex v : connectedComponentOf(start)) {
            if (v.isRoot) return v;
        }
        return null;
    }

    private Vector<Vector> buildInducedSubgraph() {
        Vector<Vertex> selV = new Vector<Vertex>();
        for (Vertex v : vertexList) {
            if (v.wasClicked) selV.add(v);
        }
        if (selV.isEmpty()) return null;

        Vector<Edge> selE = new Vector<Edge>();
        for (Edge e : edgeList) {
            if (selV.contains(e.vertex1) && selV.contains(e.vertex2)) {
                selE.add(e);
            }
        }

        Vector<Vector> result = new Vector<Vector>();
        result.add(selV);
        result.add(selE);
        return result;
    }

    private void showSubgraphWindow(Vector<Vector> sub) {
        Vector<Vertex> sV = sub.firstElement();
        Vector<Edge> sE = sub.lastElement();

        JFrame w = new JFrame("Induced Subgraph (" + sV.size() + " vertices, "
                              + sE.size() + " edges)");
        w.setSize(500, 500);

        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (sV.isEmpty()) return;
                g2.transform(GraphRenderer.fit(sV, getWidth(), getHeight(), 50));
                GraphRenderer.Options o = new GraphRenderer.Options();
                o.analysis = false;
                GraphRenderer.paint(g2, sV, sE, o);
            }
        };

        w.setContentPane(p);
        w.setVisible(true);
    }

    class InputListener implements MouseListener, MouseMotionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (selectedWindow != 0) return;
            String before = snapshot();
            handleClick(e);
            afterEdit(before);
            // afterEdit may drop a found walk the edit no longer fits; show that now, not on the next mouse move.
            refresh();
        }

        private void handleClick(MouseEvent e) {
            if (selectedWindow == 0) {
                switch (selectedTool) {
                    case 1: {
                        String name = VertexNames.nextFree(vertexList);
                        Vertex v = new Vertex(name, e.getX(), e.getY());
                        vertexList.add(v);
                        markGraphDirty();
                        updateHover(e.getX(), e.getY());
                        break;
                    }
                    case 9: {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            undoWalkStep();
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            handleWalkClick(e.getX(), e.getY());
                        }
                        break;
                    }
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
                        break;
                    }
                    case 4: {
                        Vertex victim = null;
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                victim = v;
                                break;
                            }
                        }

                        if (victim != null) {
                            Vector<Edge> toRemove = new Vector<Edge>();
                            for (Edge ed : edgeList) {
                                if (ed.vertex1 == victim || ed.vertex2 == victim) {
                                    toRemove.add(ed);
                                }
                            }
                            edgeList.removeAll(toRemove);

                            for (Vertex v : vertexList) {
                                v.undirectedNeighbors.removeAll(java.util.Collections.singleton(victim));
                                v.inNeighbors.removeAll(java.util.Collections.singleton(victim));
                                v.outNeighbors.removeAll(java.util.Collections.singleton(victim));
                            }

                            if (victim == pairFirst || victim == pairSecond) clearPair();

                            if (currentWalk != null && currentWalk.visits(victim)) clearWalk();
                            vertexList.remove(victim);
                            markGraphDirty();

                            updateHover(e.getX(), e.getY());
                            break;
                        }

                        Edge edgeVictim = edgeAt(e.getX(), e.getY());

                        if (edgeVictim != null) {
                            Vertex a = edgeVictim.vertex1;
                            Vertex b = edgeVictim.vertex2;

                            if (edgeVictim.directed) {
                                a.outNeighbors.remove(b);
                                b.inNeighbors.remove(a);
                            } else {
                                a.undirectedNeighbors.remove(b);
                                if (a != b) b.undirectedNeighbors.remove(a);
                            }
                            if (currentWalk != null && currentWalk.uses(edgeVictim)) clearWalk();
                            edgeList.remove(edgeVictim);
                            markGraphDirty();

                            updateHover(e.getX(), e.getY());
                        }
                        break;
                    }
                    case 7: {
                        // Vertices take priority over edges, as everywhere else.
                        if (vertexAt(e.getX(), e.getY()) != null) break;
                        Edge target = edgeAt(e.getX(), e.getY());
                        if (target != null) {
                            editEdgeWeight(target);
                        }
                        break;
                    }
                    case 3: {
                        if (e.getClickCount() != 2) break;
                        Vertex hitV = vertexAt(e.getX(), e.getY());
                        if (hitV != null) {
                            renameVertex(hitV);
                            break;
                        }
                        Edge hitE = edgeAt(e.getX(), e.getY());
                        if (hitE != null) {
                            editEdgeWeight(hitE);
                        }
                        break;
                    }
                    case 8: {
                        Vertex target = null;
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                target = v;
                                break;
                            }
                        }
                        if (target == null) break;

                        if (target.isRoot) {
                            target.isRoot = false;
                        } else {
                            Set<Vertex> component = connectedComponentOf(target);
                            for (Vertex v : component) {
                                v.isRoot = false;
                            }
                            target.isRoot = true;
                        }

                        updateHover(e.getX(), e.getY());
                        break;
                    }
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (selectedWindow == 0) {
                updateHover(e.getX(), e.getY());
                refresh();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            clearHover();
            refresh();
        }

        /** The vertex the current press started on, or null (pressed on empty space, or it was removed). */
        private Vertex pressedVertex() {
            return clickedVertexIndex >= 0 && clickedVertexIndex < vertexList.size()
                    ? vertexList.get(clickedVertexIndex) : null;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            canvas.requestFocusInWindow();
            if (selectedWindow == 0) pressBefore = snapshot();
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2:
                    case 5: {
                        clickedVertexIndex = -1;
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                v.wasClicked = true;
                                clickedVertexIndex = vertexList.indexOf(v);
                            } else {
                                v.wasClicked = false;
                            }
                        }
                        break;
                    }
                    case 3: {
                        boolean hitAny = false;
                        clickedVertexIndex = -1;
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                v.wasClicked = !v.wasClicked;
                                clickedVertexIndex = vertexList.indexOf(v);
                                hitAny = true;
                            }
                        }
                        if (!hitAny) {
                            for (Vertex v : vertexList) v.wasClicked = false;
                        }

                        updateHover(e.getX(), e.getY());
                        refresh();
                        break;
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragPoint = null;
            String before = pressBefore;
            pressBefore = null;
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2: {
                        Vertex parentV = pressedVertex();
                        if (parentV == null) break;
                        boolean addedAny = false;
                        for (Vertex v : vertexList) {
                            if (!v.hasIntersection(e.getX(), e.getY())) {
                                v.wasClicked = false;
                                continue;
                            }

                            boolean sameVertex = (v == parentV);

                            boolean alreadyThere = sameVertex
                                    ? parentV.undirectedNeighbors.contains(parentV)
                                    : v.connectedToVertex(parentV);

                            if (alreadyThere) {
                                v.wasClicked = false;
                                continue;
                            }

                            Vertex keptRoot = rootOf(parentV);
                            Vertex otherRoot = rootOf(v);
                            Edge edge = new Edge(v, parentV, false);
                            v.addUndirectedNeighbor(parentV);
                            if (v != parentV) {
                                parentV.addUndirectedNeighbor(v);
                            }
                            v.wasClicked = false;
                            parentV.wasClicked = false;
                            edgeList.add(edge);
                            if (keptRoot != null && otherRoot != null && keptRoot != otherRoot) otherRoot.isRoot = false;
                            addedAny = true;
                        }
                        if (addedAny) markGraphDirty();
                        break;
                    }
                    case 5: {
                        Vertex parentV = pressedVertex();
                        if (parentV == null) break;
                        boolean addedAny = false;
                        for (Vertex v : vertexList) {
                            if (!v.hasIntersection(e.getX(), e.getY())) {
                                v.wasClicked = false;
                                continue;
                            }

                            boolean alreadyThere = parentV.outNeighbors.contains(v);

                            if (alreadyThere) {
                                v.wasClicked = false;
                                continue;
                            }

                            Vertex keptRoot = rootOf(parentV);
                            Vertex otherRoot = rootOf(v);
                            Edge edge = new Edge(parentV, v, true);
                            parentV.outNeighbors.add(v);
                            v.inNeighbors.add(parentV);
                            parentV.wasClicked = false;
                            v.wasClicked = false;
                            edgeList.add(edge);
                            if (keptRoot != null && otherRoot != null && keptRoot != otherRoot) otherRoot.isRoot = false;
                            addedAny = true;
                        }
                        if (addedAny) markGraphDirty();
                        break;
                    }
                    case 3: {
                        break;
                    }
                }
            }
            if (before != null) afterEdit(before);
            updateHover(e.getX(), e.getY());
            refresh();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2:
                    case 5: {
                        dragPoint = pressedVertex() != null ? e.getPoint() : null;
                        refresh();
                        return;
                    }
                    case 3: {
                        Vertex grabbed = pressedVertex();
                        if (grabbed != null && grabbed.wasClicked) {
                            grabbed.location.x = e.getX();
                            grabbed.location.y = e.getY();
                        }
                        break;
                    }
                }
                updateHover(e.getX(), e.getY());
                refresh();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (selectedWindow == 0) {
                updateHover(e.getX(), e.getY());
                refresh();
            }
        }
    }

    class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            clearHover();
            walkMessage = null;

            if (command.startsWith("Find ")) {
                findTraversal(command.substring("Find ".length()));
            } else if (command.equals("Auto Arrange Vertices")) {
                String before = snapshot();
                arrangeVertices();
                afterEdit(before);
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
            } else if (command.equals("Undo")) {
                undo();
            } else if (command.equals("Redo")) {
                redo();
            } else if (command.equals("Remove All")) {
                removeAll();
            } else if (command.equals("Preference Lists...")) {
                editPreferenceLists();
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
            } else if (command.equals("Graph")) {
                tabs.setSelectedIndex(0);
            } else if (command.equals("Properties")) {
                tabs.setSelectedIndex(1);
            } else if (command.equals("About")) {
                showAbout();
            }

            if (selectedWindow == 1) computeProperties();
            refresh();
        }
    }

    private void arrangeVertices() {
        Layout.arrangeOnCircle(vertexList, canvasWidth(), canvasHeight());
    }

    public void refresh() {
        recomputeGraphProperties();
        EdgeRegistry.rebuild(edgeList);
        applyHighlights();
        updateSidePanel();
        canvas.repaint();
        // Colouring changes no graph text, so the report is reused; repaint to show the colours.
        if (selectedWindow == 1) propertiesPanel.repaint();
        updateStatus();
    }

    /** Pushes the walk highlight and step labels onto the edges before drawing. */
    private void applyHighlights() {
        for (Edge ed : edgeList) {
            ed.highlight = null;
            ed.stepLabel = null;
        }
        if (currentWalk != null) {
            for (Edge ed : currentWalk.edges()) {
                ed.highlight = WALK_COLOR;
                StringBuilder sb = new StringBuilder("#");
                for (int step : currentWalk.stepsUsing(ed)) {
                    if (sb.length() > 1) sb.append(",");
                    sb.append(step);
                }
                ed.stepLabel = sb.toString();
            }
        }
        if (selectedTool == 6 && pairSummary != null && !pairSummary.paths.isEmpty()) {
            for (Edge ed : pairSummary.paths.get(selectedPathIndex).edges()) {
                ed.highlight = PATH_COLOR;
                ed.stepLabel = null;
            }
        }
    }

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
        c.edges = edgeList;
        sidePanel.display(c);
    }

    /** Grey dashed line from the pressed vertex to the mouse while dragging out an edge. */
    private void drawDragPreview(Graphics2D g) {
        if (selectedTool != 2 && selectedTool != 5) return;
        if (dragPoint == null || clickedVertexIndex < 0 || clickedVertexIndex >= vertexList.size()) return;
        Vertex from = vertexList.get(clickedVertexIndex);
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10f, new float[] { 6f, 4f }, 0f));
        g.drawLine(from.location.x, from.location.y, dragPoint.x, dragPoint.y);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawWalkMarkers(Graphics g) {
        if (currentWalk == null) return;
        Vertex s = currentWalk.start();
        Vertex t = currentWalk.end();
        g.setColor(WALK_COLOR);
        if (s == t) {
            g.drawString("start/end", s.location.x - 24, s.location.y + 34);
        } else {
            g.drawString("start", s.location.x - 12, s.location.y + 34);
            g.drawString("end", t.location.x - 9, t.location.y + 34);
        }
        g.setColor(Color.black);
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    private class CanvasPane extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(backgroundColour);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (selectedWindow != 0) return;
            GraphRenderer.paintGrid(g2, getWidth(), getHeight());

            GraphRenderer.Options o = new GraphRenderer.Options();
            o.interaction = true;
            GraphRenderer.paint(g2, vertexList, edgeList, o);
            drawDragPreview(g2);
            drawWalkMarkers(g2);
        }
    }
}