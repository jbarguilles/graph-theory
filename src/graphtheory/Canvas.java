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
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.Vector;

public class Canvas {

    public JFrame frame;
    private JMenuBar menuBar;
    private CanvasPane canvas;
    private Graphics2D graphic;
    private Color backgroundColour;
    private Image canvasImage,  canvasImage2;
    private int selectedTool;
    private int selectedWindow;
    private Dimension screenSize;
    public int width,  height;
    private int clickedVertexIndex;
    private int clickedEdgeIndex;
    private int pairedVertex1Index = -1;
    private int pairedVertex2Index = -1;
    private VertexPair currentPairVP = null;
    private FileManager fileManager = new FileManager();

    /////////////
    private Vector<Vertex> vertexList;
    private Vector<Edge> edgeList;
    private GraphProperties gP = new GraphProperties();
    /////////////

    private boolean graphDirty = true;

    // Build Walk tool (tool 7)
    private Walk currentWalk = null;
    private String walkMessage = null;
    private static final Color WALK_COLOR = new Color(0, 150, 150);

    // Paths for the selected pair (tool 6), browsed one at a time
    private Vector<Walk> pairPaths = null;
    private int selectedPathIndex = 0;
    private static final Color PATH_COLOR = new Color(220, 160, 0);
    private static final int PATH_ROWS = 6;

    public Canvas(String title, int width, int height, Color bgColour) {
        frame = new JFrame();
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        canvas = new CanvasPane();
        InputListener inputListener = new InputListener();
        canvas.addMouseListener(inputListener);
        canvas.addMouseMotionListener(inputListener);
        installKeyBindings();
        frame.setContentPane(canvas);

        this.width = width;
        this.height = height;
        canvas.setPreferredSize(new Dimension(width, height));

        //events
        menuBar = new JMenuBar();
        JMenu menuOptions = new JMenu("Tools");
        JMenu menuOptions1 = new JMenu("File");
        JMenu menuOptions2 = new JMenu("Extras");
        JMenu menuOptions3 = new JMenu("Window");

        JMenuItem item = new JMenuItem("Add Vertex");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Open File");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions1.add(item);
        item = new JMenuItem("Save to File");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions1.add(item);
        item = new JMenuItem("Add Edges");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Add Directed Edge");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Grab Tool");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Select Pair");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Remove Tool");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Build Walk");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(new MenuListener());
        menuOptions.add(item);
        item = new JMenuItem("Auto Arrange Vertices");
        item.addActionListener(new MenuListener());

        menuOptions2.add(item);
        item = new JMenuItem("Remove All");
        item.addActionListener(new MenuListener());
        menuOptions2.add(item);
        item = new JMenuItem("Mark as Root");
        item.addActionListener(new MenuListener());
        menuOptions2.add(item);

        item = new JMenuItem("Graph");
        item.addActionListener(new MenuListener());
        menuOptions3.add(item);
        item = new JMenuItem("Properties");
        item.addActionListener(new MenuListener());
        menuOptions3.add(item);

        menuBar.add(menuOptions1);
        menuBar.add(menuOptions);
        menuBar.add(menuOptions2);
        menuBar.add(menuOptions3);

        frame.setJMenuBar(menuBar);

        backgroundColour = bgColour;

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2, width, height);
        frame.pack();
        setVisible(true);

        vertexList = new Vector<Vertex>();
        edgeList = new Vector<Edge>();
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

        for (Edge d : edgeList) {
            boolean hit = (hoveredVertex == null) && d.hasIntersection(mx, my);
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
            gP.computeBridges(vertexList, edgeList);
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
                if (selectedTool == 7 && selectedWindow == 0) { undoWalkStep(); refresh(); }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "walkClear");
        am.put("walkClear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedTool == 7 && selectedWindow == 0) { clearWalk(); refresh(); }
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

    /** Tool 7 left-click: start the walk, or extend it by the clicked vertex or edge. */
    private void handleWalkClick(int x, int y) {
        Vertex hitV = null;
        for (Vertex v : vertexList) {
            if (v.hasIntersection(x, y)) { hitV = v; break; }
        }
        Edge hitE = null;
        if (hitV == null) {
            for (Edge ed : edgeList) {
                if (ed.hasIntersection(x, y)) { hitE = ed; break; }
            }
        }
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

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    class InputListener implements MouseListener, MouseMotionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (selectedWindow == 0) {
                switch (selectedTool) {
                    case 1: {
                        Vertex v = new Vertex("" + vertexList.size(), e.getX(), e.getY());
                        vertexList.add(v);
                        v.draw(graphic);
                        markGraphDirty();
                        updateHover(e.getX(), e.getY());
                        refresh();
                        break;
                    }
                    case 7: {
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
                                v.undirectedNeighbors.remove(victim);
                                v.inNeighbors.remove(victim);
                                v.outNeighbors.remove(victim);
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

                        Edge edgeVictim = null;
                        for (Edge ed : edgeList) {
                            if (ed.hasIntersection(e.getX(), e.getY())) {
                                edgeVictim = ed;
                                break;
                            }
                        }

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

        @Override
        public void mousePressed(MouseEvent e) {
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2:
                    case 5: {
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
                        for (Vertex v : vertexList) {
                            v.wasClicked = false;
                        }

                        for (Edge d : edgeList) {
                            if (d.hasIntersection(e.getX(), e.getY())) {
                                d.wasClicked = true;
                                clickedEdgeIndex = edgeList.indexOf(d);
                            } else {
                                d.wasClicked = false;
                            }
                        }
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY())) {
                                v.wasClicked = true;
                                clickedVertexIndex = vertexList.indexOf(v);
                            }
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
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2: {
                        Vertex parentV = vertexList.get(clickedVertexIndex);
                        boolean addedAny = false;
                        for (Vertex v : vertexList) {
                            if (v.hasIntersection(e.getX(), e.getY()) && !v.connectedToVertex(parentV)) {
                                Edge edge = new Edge(v, parentV, false);
                                v.addUndirectedNeighbor(parentV);
                                if (v != parentV) {
                                    parentV.addUndirectedNeighbor(v);
                                }
                                v.wasClicked = false;
                                parentV.wasClicked = false;
                                edgeList.add(edge);
                                addedAny = true;
                            } else {
                                v.wasClicked = false;
                            }
                        }
                        if (addedAny) markGraphDirty();
                        break;
                    }
                    case 5: {
                        Vertex parentV = vertexList.get(clickedVertexIndex);
                        boolean addedAny = false;
                        for (Vertex v : vertexList) {
                            boolean alreadyThere = parentV.outNeighbors.contains(v);
                            if (v.hasIntersection(e.getX(), e.getY()) && !alreadyThere) {
                                Edge edge = new Edge(parentV, v, true);
                                parentV.outNeighbors.add(v);
                                v.inNeighbors.add(parentV);
                                parentV.wasClicked = false;
                                v.wasClicked = false;
                                edgeList.add(edge);
                                addedAny = true;
                            } else {
                                v.wasClicked = false;
                            }
                        }
                        if (addedAny) markGraphDirty();
                        break;
                    }
                    case 3: {
                        break;
                    }
                }
            }
            updateHover(e.getX(), e.getY());
            refresh();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedWindow == 0 && vertexList.size() > 0) {
                switch (selectedTool) {
                    case 2:
                    case 5: {
                        refresh();
                        graphic.setColor(Color.RED);
                        drawLine(vertexList.get(clickedVertexIndex).location.x,
                                 vertexList.get(clickedVertexIndex).location.y,
                                 e.getX(), e.getY());
                        canvas.repaint();
                        return;
                    }
                    case 3: {
                        if (vertexList.get(clickedVertexIndex).wasClicked) {
                            vertexList.get(clickedVertexIndex).location.x = e.getX();
                            vertexList.get(clickedVertexIndex).location.y = e.getY();
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

            if (command.equals("Add Vertex")) {
                selectedTool = 1;
            } else if (command.equals("Add Edges")) {
                selectedTool = 2;
            } else if (command.equals("Grab Tool")) {
                selectedTool = 3;
            } else if (command.equals("Remove Tool")) {
                selectedTool = 4;
            } else if (command.equals("Add Directed Edge")) {
                selectedTool = 5;
            } else if (command.equals("Select Pair")) {
                selectedTool = 6;
                pairedVertex1Index = -1;
                pairedVertex2Index = -1;
                currentPairVP = null;
                pairPaths = null;
            } else if (command.equals("Build Walk")) {
                selectedTool = 7;
                clearWalk();
            } else if (command.equals("Mark as Root")) {
                for (Vertex v : vertexList) {
                    if (v.wasClicked) {
                        v.isRoot = !v.isRoot;
                    }
                }
            } else if (command.equals("Auto Arrange Vertices")) {
                arrangeVertices();
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
                    System.out.println(fileManager.jF.getSelectedFile());
                    selectedWindow = 0;
                }
            } else if (command.equals("Save to File")) {
                int returnValue = fileManager.jF.showSaveDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    fileManager.saveFile(vertexList, fileManager.jF.getSelectedFile());
                    System.out.println(fileManager.jF.getSelectedFile());
                }
            } else if (command.equals("Graph")) {
                selectedWindow = 0;
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
                    gP.displayContainers(vertexList);
                }
            }

            refresh();
        }
    }

    private void arrangeVertices() {
        if (vertexList.isEmpty()) return;
        double deg2rad = Math.PI / 180;
        double radius = height / 5;
        double centerX = width / 2;
        double centerY = height / 2;
        int interval = 360 / vertexList.size();

        for (int i = 0; i < vertexList.size(); i++) {
            double degInRad = i * deg2rad * interval;
            double x = centerX + (Math.cos(degInRad) * radius);
            double y = centerY + (Math.sin(degInRad) * radius);
            int X = (int) x;
            int Y = (int) y;
            vertexList.get(i).location.x = X;
            vertexList.get(i).location.y = Y;
        }
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

    private void loadFile(Vector<Vector> File) {
        vertexList = File.firstElement();
        edgeList = File.lastElement();
        markGraphDirty();
        refresh();
    }

    public void refresh() {
        recomputeGraphProperties();
        applyHighlights();
        erase();
        for (Edge e : edgeList) {
            e.draw(graphic);
        }
        for (Vertex v : vertexList) {
            v.draw(graphic);
        }
        drawWalkMarkers(graphic);
        canvas.repaint();
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
        if (graphic == null) {
            Dimension size = canvas.getSize();
            canvasImage = canvas.createImage(size.width, size.height);
            canvasImage2 = canvas.createImage(size.width, size.height);
            graphic = (Graphics2D) canvasImage.getGraphics();
            graphic.setColor(backgroundColour);
            graphic.fillRect(0, 0, size.width, size.height);
            graphic.setColor(Color.black);
        }
        frame.setVisible(visible);
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    public void erase() {
        graphic.setColor(backgroundColour);
        graphic.fillRect(0, 0, width, height);
        graphic.setColor(Color.black);
    }

    public void erase(int x, int y, int x1, int y2) {
        graphic.clearRect(x, y, x1, y2);
    }

    public void drawString(String text, int x, int y, float size) {
        Font orig = graphic.getFont();
        graphic.setFont(graphic.getFont().deriveFont(1, size));
        graphic.drawString(text, x, y);
        graphic.setFont(orig);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        graphic.drawLine(x1, y1, x2, y2);
    }

    private void drawInfoBox(Graphics g) {
        Vertex clicked = null;
        for (Vertex v : vertexList) {
            if (v.wasClicked) { clicked = v; break; }
        }
        if (clicked == null) return;

        int x = 10, y = 10, w = 170, h = 142;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int ty = y + 16;
        g.drawString("Vertex: " + clicked.name,             x + 6, ty); ty += 16;
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
                if (i > 0) sb.append("→");
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

        String pairLabel = v1.name + " → " + v2.name;
        int pathCount = pairPaths != null ? pairPaths.size() : 0;
        int rows = Math.min(PATH_ROWS, pathCount);

        int x = 190, y = 10, w = 430;
        int h = 15 * (7 + Math.max(rows, 1)) + 8;
        g.setColor(new Color(240, 248, 255));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int ty = y + 15;
        int lx = x + 6;
        g.drawString("Ordered pair: (" + pairLabel + ")",          lx, ty); ty += 15;
        g.drawString("Adjacent: " + adjacent,                      lx, ty); ty += 15;
        g.drawString("Reachable: " + reachable,                    lx, ty); ty += 15;
        g.drawString("Geodesic dist (Length): " + (reachable ? dist : "∞"), lx, ty); ty += 15;
        if (!geodesicStr.isEmpty()) {
            g.drawString("Geodesic path: " + geodesicStr,          lx, ty); ty += 15;
        } else {
            g.drawString("Geodesic path: N/A",                     lx, ty); ty += 15;
        }
        g.drawString("Max vertex-disjoint width: " + maxWidth,     lx, ty); ty += 15;
        g.drawString("Paths " + pairLabel + ": " + pathCount
                + (pathCount > 1 ? "   (↑/↓ to browse)" : ""),     lx, ty); ty += 15;

        if (pathCount == 0) {
            g.drawString("  No path from " + v1.name + " to " + v2.name, lx, ty);
            return;
        }

        int minLen = pairPaths.get(0).length();
        int first = (selectedPathIndex / PATH_ROWS) * PATH_ROWS;
        for (int i = first; i < Math.min(first + PATH_ROWS, pathCount); i++) {
            Walk p = pairPaths.get(i);
            if (i == selectedPathIndex) {
                g.setColor(new Color(255, 230, 160));
                g.fillRect(x + 2, ty - 12, w - 4, 15);
                g.setColor(Color.BLACK);
            }
            String row = (i + 1) + ". " + truncate(p.toString(), 34)
                    + "   length " + p.length()
                    + (p.length() == minLen ? "  ← geodesic" : "");
            g.drawString(row, lx, ty);
            ty += 15;
        }
    }

    private void drawWalkInfoBox(Graphics g) {
        if (currentWalk == null && walkMessage == null) return;

        int x = 10, y = 420, w = 360, h = 95;
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

        public void paint(Graphics g) {
            switch (selectedWindow) {
                case 0: {
                    graphic.drawString("Vertex Count=" + vertexList.size() +
                            "  Edge Count=" + edgeList.size() +
                            "  Selected Tool=" + selectedTool, 50, height / 2 + (height * 2) / 5);
                    g.drawImage(canvasImage, 0, 0, null);
                    drawInfoBox(g);
                    drawPairInfoBox(g);
                    drawWalkInfoBox(g);
                    g.setColor(Color.black);
                    break;
                }
                case 1: {
    Graphics g2 = canvasImage2.getGraphics();
    g2.clearRect(0, 0, width, height);

    // ---- Right column: two matrices + summary ----
    int rightX = width / 2 + 60;
    int adjY = 50;
    gP.drawAdjacencyMatrix(g2, vertexList, rightX, adjY);
    int adjHeight = (vertexList.size() + 1) * 20 + 30;

    int distY = adjY + adjHeight + 20;
    gP.drawDistanceMatrix(g2, vertexList, rightX, distY);
    int distHeight = (vertexList.size() + 1) * 20 + 30;

    int summaryY = distY + distHeight + 20;
    gP.drawGraphSummary(g2, vertexList, edgeList, rightX, summaryY);

    // ---- Left column: node properties ----
    gP.drawNodePropertiesTable(g2, vertexList, 10, height / 2 + 90);

    // ---- Composite ----
    g.drawImage(canvasImage2, 0, 0, null);
    g.drawImage(canvasImage.getScaledInstance(width / 2, height / 2, Image.SCALE_SMOOTH),
                0, 0, null);
    g.draw3DRect(0, 0, width / 2, height / 2, true);

    drawString("Graph disconnects when nodes in color red are removed.", 100, height - 30, 20);
    g.setColor(Color.black);
    break;
}
            }
        }
    }
}