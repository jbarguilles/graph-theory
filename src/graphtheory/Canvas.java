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
import java.util.HashSet;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Canvas {

    public JFrame frame;
    private CanvasPane canvas;
    private JScrollPane propertiesScroll;
    private JPanel propertiesContent;
    private Color backgroundColour;
    private int selectedTool;
    private int selectedWindow;
    public int width,  height;
    private int clickedVertexIndex = -1;
    private final EditHistory history = new EditHistory(50);
    private JMenuItem undoItem;
    private JMenuItem redoItem;
    private int clickedEdgeIndex;
    private int pairedVertex1Index = -1;
    private int pairedVertex2Index = -1;
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

    // Paths for the selected pair (Tools.PAIR), browsed one at a time
    private Vector<Walk> pairPaths = null;
    private int selectedPathIndex = 0;
    private static final Color PATH_COLOR = new Color(220, 160, 0);
    private static final int PATH_ROWS = 6;

    // Size of the graph picture at the top left of the Properties tab.
    private static final int THUMB_W = 400;
    private static final int THUMB_H = 300;

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
        tabs.setFocusable(false);
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

    /** The graph as .graph text: the unit of undo, saving and "unsaved changes". */
    private String snapshot() {
        return GraphFile.write(vertexList, edgeList);
    }

    /** Call after an edit with the snapshot from before it. */
    private void afterEdit(String before) {
        if (!before.equals(snapshot())) {
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
        clearWalk();
        clickedVertexIndex = -1;
        pairedVertex1Index = -1;
        pairedVertex2Index = -1;
        currentPairVP = null;
        pairPaths = null;
        pressBefore = null;
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

    /** Recomputes what the Properties tab shows. */
    private void computeProperties() {
        if (vertexList.size() > 0) {
            int[][] matrix = gP.generateAdjacencyMatrix(vertexList, edgeList);

            gP.vertexConnectivity(vertexList);
            gP.edgeConnectivity(vertexList, edgeList);

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

    private void buildPropertiesPanel() {
        propertiesContent = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                int w = getWidth();
                int h = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, w, h);

                drawThumbnail(g2, 10, 10, THUMB_W, THUMB_H);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(getFont());

                int rightX = THUMB_W + 60;
                int adjY = 50;
                gP.drawAdjacencyMatrix(g2, vertexList, rightX, adjY);
                int adjHeight = (vertexList.size() + 1) * 20 + 30;

                int distY = adjY + adjHeight + 20;
                gP.drawDistanceMatrix(g2, vertexList, rightX, distY);
                int distHeight = (vertexList.size() + 1) * 20 + 30;

                int summaryY = distY + distHeight + 20;
                int summaryHeight = gP.drawGraphSummary(g2, vertexList, edgeList, rightX, summaryY);

                int nodeY = THUMB_H + 90;
                gP.drawNodePropertiesTable(g2, vertexList, 10, nodeY);
                int nodeTableHeight = (vertexList.size() + 2) * 18 + 10;

                int listY = nodeY + nodeTableHeight + 20;
                gP.drawAdjacencyList(g2, vertexList, 10, listY);

                int captionY = Math.max(
                        nodeY + (vertexList.size() + 2) * 18 + 40,
                        summaryY + summaryHeight + 40);
                captionY = Math.max(captionY, h - 40);

                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(20f));
            }
        };
        propertiesContent.setBackground(Color.WHITE);

        propertiesScroll = new JScrollPane(propertiesContent);
        propertiesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        propertiesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        propertiesScroll.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void refreshPropertiesScrollSize() {
        if (propertiesContent == null) return;

        int matrixRows = vertexList.size() + 1;
        int matrixHeight = matrixRows * 20 + 30;

        int rightHeight = 50
                + matrixHeight + 20
                + matrixHeight + 20
                + 34 * 16 + 20;
        int leftHeight = 10
                       + THUMB_H + 20
                       + (vertexList.size() + 2) * 18 + 30
                       + (vertexList.size() + 1) * 18 + 20
                       + 80;

        int neededHeight = Math.max(rightHeight, leftHeight) + 60;
        neededHeight = Math.max(neededHeight, height);
        int neededWidth = Math.max(width + 40, THUMB_W + 60 + 700);

        propertiesContent.setPreferredSize(new Dimension(neededWidth, neededHeight));
        propertiesContent.revalidate();
        propertiesContent.repaint();
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
        gP.invalidateTraversalSummary();
        refreshPairPaths();
    }

    private void recomputeGraphProperties() {
        if (!graphDirty) return;
        if (vertexList.size() > 0) {
            gP.computeCutpoints(vertexList);
            gP.computeBridges(vertexList, edgeList);
            gP.computeBlocks(vertexList, edgeList);
        } else {
            for (Vertex v : vertexList) v.isCutpoint = false;
            for (Edge e : edgeList)   e.isBridge   = false;
        }
        graphDirty = false;
    }

    private void clearWalk() {
        currentWalk = null;
        walkMessage = null;
    }

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

    private void installKeyBindings() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "walkUndo");
        am.put("walkUndo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 9 && selectedWindow == 0) { undoWalkStep(); refresh(); }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "walkClear");
        am.put("walkClear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 9 && selectedWindow == 0) { clearWalk(); refresh(); }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "pathPrev");
        am.put("pathPrev", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 6 && selectedWindow == 0
                        && pairPaths != null && selectedPathIndex > 0) {
                    selectedPathIndex--;
                    refresh();
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "pathNext");
        am.put("pathNext", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 6 && selectedWindow == 0 && pairPaths != null
                        && selectedPathIndex < pairPaths.size() - 1) {
                    selectedPathIndex++;
                    refresh();
                }
            }
        });
    }

    /** Removes the walk's last step; undoing a trivial walk clears it. */
    private void undoWalkStep() {
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
     * Extras > Find …: switches to the Build Walk tool on the Graph window and
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
        if (currentWalk == null) walkMessage = "No " + kind + " exists";
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
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
                GraphRenderer.paint(g2, sV, sE, new GraphRenderer.Options());
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
                        refresh();
                        break;
                    }
                    case 9: {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            undoWalkStep();
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            handleWalkClick(e.getX(), e.getY());
                        }
                        refresh();
                        break;
                    }
                    case 6: {
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                int idx = vertexList.indexOf(v);
                                if (pairedVertex1Index == -1) {
                                    pairedVertex1Index = idx;
                                    v.wasClicked = true;
                                } else if (pairedVertex2Index == -1 && idx != pairedVertex1Index) {
                                    pairedVertex2Index = idx;
                                    v.wasClicked = true;
                                    currentPairVP = new VertexPair(vertexList.get(pairedVertex1Index), v);
                                    refreshPairPaths();
                                } else {
                                    if (pairedVertex1Index >= 0) vertexList.get(pairedVertex1Index).wasClicked = false;
                                    if (pairedVertex2Index >= 0) vertexList.get(pairedVertex2Index).wasClicked = false;
                                    pairedVertex1Index = idx;
                                    pairedVertex2Index = -1;
                                    currentPairVP = null;
                                    pairPaths = null;
                                    v.wasClicked = true;
                                }
                            }
                        }
                        refresh();
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

                            if (currentWalk != null && currentWalk.visits(victim)) clearWalk();
                            vertexList.remove(victim);
                            markGraphDirty();

                            updateHover(e.getX(), e.getY());
                            refresh();
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
                                b.undirectedNeighbors.remove(a);
                            }
                            if (currentWalk != null && currentWalk.uses(edgeVictim)) clearWalk();
                            edgeList.remove(edgeVictim);
                            markGraphDirty();

                            updateHover(e.getX(), e.getY());
                            refresh();
                        }
                        break;
                    }
                    case 7: {
                        Edge target = edgeAt(e.getX(), e.getY());
                        if (target != null) {
                            editEdgeWeight(target);
                            refresh();
                        }
                        break;
                    }
                    case 3: {
                        if (e.getClickCount() != 2) break;
                        Vertex hitV = vertexAt(e.getX(), e.getY());
                        if (hitV != null) {
                            renameVertex(hitV);
                            refresh();
                            break;
                        }
                        Edge hitE = edgeAt(e.getX(), e.getY());
                        if (hitE != null) {
                            editEdgeWeight(hitE);
                            refresh();
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
                        refresh();
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

    private void reloadVertexConnections(int[][] aMatrix, Vector<Vertex> vList) {
        for (Vertex v : vList) {
            v.undirectedNeighbors.clear();
        }

        for (int i = 0; i < aMatrix.length; i++) {
            for (int j = 0; j < aMatrix.length; j++) {
                if (aMatrix[i][j] == 1) {
                    vList.get(i).addUndirectedNeighbor(vList.get(j));
                }
            }
        }
    }

    public void refresh() {
        recomputeGraphProperties();
        EdgeRegistry.rebuild(edgeList);
        applyHighlights();
        canvas.repaint();
        if (propertiesContent != null) {
            propertiesContent.repaint();
        }
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
        if (selectedTool == 6 && pairPaths != null && !pairPaths.isEmpty()) {
            for (Edge ed : pairPaths.get(selectedPathIndex).edges()) {
                ed.highlight = PATH_COLOR;
                ed.stepLabel = null;
            }
        }
    }

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

    private void drawInfoBox(Graphics g) {
        Vertex clicked = null;
        int selCount = 0;
        for (Vertex v : vertexList) {
            if (v.wasClicked) {
                selCount++;
                if (clicked == null) clicked = v;
            }
        }
        if (clicked == null) return;

        int x = 10, y = 10, w = 190, h = 158;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int ty = y + 16;
        g.drawString("Selected: " + selCount + " vertex" + (selCount == 1 ? "" : "es"),
                     x + 6, ty); ty += 16;
        g.drawString("First: " + clicked.name,              x + 6, ty); ty += 16;
        g.drawString("Degree: " + clicked.degree(),         x + 6, ty); ty += 16;
        g.drawString("In-Degree: " + clicked.inDegree(),    x + 6, ty); ty += 16;
        g.drawString("Out-Degree: " + clicked.outDegree(),  x + 6, ty); ty += 16;
        g.drawString("Isolated: " + clicked.isIsolated(),   x + 6, ty); ty += 16;
        g.drawString("Self-loop: " + clicked.hasSelfLoop(), x + 6, ty); ty += 16;
        g.drawString("Cutpoint: " + clicked.isCutpoint,     x + 6, ty); ty += 16;
        g.drawString("Root: " + clicked.isRoot,             x + 6, ty);
    }

    private void drawPairInfoBox(Graphics g) {
        if (currentPairVP == null) return;

        Vertex v1 = currentPairVP.vertex1;
        Vertex v2 = currentPairVP.vertex2;

        boolean adjacent = false;
        for (Edge e : edgeList) {
            if ((e.vertex1 == v1 && e.vertex2 == v2) ||
                (!e.directed && e.vertex1 == v2 && e.vertex2 == v1)) {
                adjacent = true; break;
            }
        }

        int dist = currentPairVP.getShortestDistance();
        boolean reachable = dist != -1;
        Vector<Vertex> geodesic = currentPairVP.getShortestPath();

        String geodesicStr = "";
        if (geodesic != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < geodesic.size(); i++) {
                if (i > 0) sb.append("\u2192");
                sb.append(geodesic.get(i).name);
            }
            geodesicStr = sb.length() > 28 ? sb.substring(0, 25) + "..." : sb.toString();
        }

        int maxWidth = 0;
        if (currentPairVP.VertexDisjointContainer != null) {
            for (Vector<Vector<Vertex>> c : currentPairVP.VertexDisjointContainer) {
                if (c.size() > maxWidth) maxWidth = c.size();
            }
        }

        String pairLabel = v1.name + " \u2192 " + v2.name;
        int pathCount = pairPaths != null ? pairPaths.size() : 0;
        int rows = Math.min(PATH_ROWS, pathCount);

        int x = 210, y = 10, w = 430;
        int h = 15 * (8 + Math.max(rows, 1)) + 8;
        g.setColor(new Color(240, 248, 255));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int ty = y + 15;
        int lx = x + 6;
        g.drawString("Ordered pair: (" + pairLabel + ")",          lx, ty); ty += 15;
        g.drawString("Adjacent: " + adjacent,                      lx, ty); ty += 15;
        g.drawString("Reachable: " + reachable,                    lx, ty); ty += 15;
        g.drawString("Geodesic dist (weighted): " + (reachable ? dist : "\u221E"), lx, ty); ty += 15;
        if (!geodesicStr.isEmpty()) {
            g.drawString("Geodesic path: " + geodesicStr,          lx, ty); ty += 15;
        } else {
            g.drawString("Geodesic path: N/A",                     lx, ty); ty += 15;
        }
        int simplePathCount = currentPairVP.pathList != null ? currentPairVP.pathList.size() : 0;
        g.drawString("Simple paths (Walk\u2229no-repeat): " + simplePathCount, lx, ty); ty += 15;
        g.drawString("Max vertex-disjoint width: " + maxWidth,     lx, ty); ty += 15;
        g.drawString("Paths " + pairLabel + ": " + pathCount
                + (pathCount > 1 ? "   (\u2191/\u2193 to browse)" : ""),     lx, ty); ty += 15;

        if (pathCount == 0) {
            g.drawString("  No path from " + v1.name + " to " + v2.name, lx, ty);
            return;
        }

        int minLen = pairPaths.get(0).length();
        int first = (selectedPathIndex / PATH_ROWS) * PATH_ROWS;
        for (int i = first; i < Math.min(first + PATH_ROWS, pathCount); i++) {
            Walk p = pairPaths.get(i);
            if (i == selectedPathIndex && selectedTool == 6) {
                g.setColor(new Color(255, 230, 160));
                g.fillRect(x + 2, ty - 12, w - 4, 15);
                g.setColor(Color.BLACK);
            }
            String row = (i + 1) + ". " + truncate(p.toString(), 40)
                    + "  len " + p.length()
                    + (p.length() == minLen ? "  \u2190 geodesic" : "");
            g.drawString(row, lx, ty);
            ty += 15;
        }
    }

    private void drawWalkInfoBox(Graphics g) {
        if (currentWalk == null && walkMessage == null) return;

        int x = 10, y = canvasHeight() - 180, w = 360, h = 95;
        g.setColor(new Color(235, 250, 250));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int ty = y + 16;
        int lx = x + 6;
        if (currentWalk == null) {
            g.drawString("Walk: (none)", lx, ty); ty += 16;
        } else {
            Walk w0 = currentWalk;
            g.drawString("Walk: " + truncate(w0.toString(), 54), lx, ty); ty += 16;
            g.drawString("Length: " + w0.length(), lx, ty); ty += 16;
            g.drawString("Trail: " + yesNo(w0.isTrail())
                    + "   Path: " + yesNo(w0.isPath()), lx, ty); ty += 16;
            g.drawString("Closed: " + yesNo(w0.isClosed())
                    + "   Circuit: " + yesNo(w0.isCircuit())
                    + "   Cycle: " + yesNo(w0.isCycle()), lx, ty); ty += 16;
        }
        if (walkMessage != null) {
            g.setColor(new Color(200, 0, 0));
            g.drawString(walkMessage, lx, ty);
            g.setColor(Color.BLACK);
        }
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

            g2.setStroke(new BasicStroke(1f));
            g2.setFont(getFont());
            drawInfoBox(g2);
            drawPairInfoBox(g2);
            drawWalkInfoBox(g2);
        }
    }
}