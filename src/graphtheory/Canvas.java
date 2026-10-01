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
    private JScrollPane propertiesScroll;
    private JPanel propertiesContent;
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

    public Canvas(String title, int width, int height, Color bgColour) {
        frame = new JFrame();
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        canvas = new CanvasPane();
        InputListener inputListener = new InputListener();
        canvas.addMouseListener(inputListener);
        canvas.addMouseMotionListener(inputListener);
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
        item = new JMenuItem("Set Edge Weight");
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

        // Build the scrollable Properties content panel.
        buildPropertiesPanel();
    }

    /**
     * Build the JScrollPane + content panel used by the Properties window.
     * The content panel paints the whole Properties view (matrices, node
     * table, graph summary, captions) and reports its preferred size so the
     * scroll pane can show a scrollbar when the content is taller than the
     * window.
     */
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

            // Left column: Graph preview
            g2.drawImage(canvasImage.getScaledInstance(width / 2, height / 2, Image.SCALE_SMOOTH),
                         10, 10, null);
            g2.setColor(Color.BLACK);
            g2.draw3DRect(10, 10, width / 2, height / 2, true);

            // Right column: Adjacency Matrix + Distance Matrix + Graph Summary
            int rightX = width / 2 + 60;
            int adjY = 50;
            gP.drawAdjacencyMatrix(g2, vertexList, rightX, adjY);
            int adjHeight = (vertexList.size() + 1) * 20 + 30;

            int distY = adjY + adjHeight + 20;
            gP.drawDistanceMatrix(g2, vertexList, rightX, distY);
            int distHeight = (vertexList.size() + 1) * 20 + 30;

            int summaryY = distY + distHeight + 20;
            int summaryHeight = gP.drawGraphSummary(g2, vertexList, edgeList, rightX, summaryY);

            // Left column below the preview: Node Properties
            int nodeY = height / 2 + 90;
            gP.drawNodePropertiesTable(g2, vertexList, 10, nodeY);

            // Bottom captions: κ(G), λ(G), legend. Pinned at the greater of:
            //   - below the tallest column (so it doesn't overlap content)
            //   - 40 px above the panel's bottom edge (so it isn't clipped
            //     when the panel is taller than the columns)
            int captionY = Math.max(
                    nodeY + (vertexList.size() + 2) * 18 + 40,
                    summaryY + summaryHeight + 40);
            captionY = Math.max(captionY, h - 40);

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(20f));
            // g2.drawString("\u03BA(G) = " + gP.vertexConnectivityValue
            //               + "   \u03BB(G) = " + gP.edgeConnectivityValue,
            //               100, captionY);
            // g2.setFont(g2.getFont().deriveFont(12f));
            // g2.drawString("red vertices = vertex cut, red edges = edge cut",
            //               100, captionY + 20);
        }
    };
    propertiesContent.setBackground(Color.WHITE);

    propertiesScroll = new JScrollPane(propertiesContent);
    propertiesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    propertiesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    propertiesScroll.getVerticalScrollBar().setUnitIncrement(16);
}

    /**
     * Compute the content panel's preferred size so the scroll pane knows how
     * tall the content is. Called whenever the graph changes and whenever the
     * Properties window is opened.
     */
    private void refreshPropertiesScrollSize() {
        if (propertiesContent == null) return;

        int matrixRows = vertexList.size() + 1;
        int matrixHeight = matrixRows * 20 + 30;    // each matrix

        // Right column total height: adj + gap + dist + gap + summary
        int rightHeight = 50
                        + matrixHeight + 20
                        + matrixHeight + 20
                        + 12 * 16 + 20;             // summary box approx
        // Left column total height: preview + gap + node table + captions
        int leftHeight = 10
                       + height / 2 + 20
                       + (vertexList.size() + 2) * 18 + 30
                       + 80;

        int neededHeight = Math.max(rightHeight, leftHeight) + 60;
        neededHeight = Math.max(neededHeight, height);
        int neededWidth = Math.max(width + 40, width / 2 + 60 + 400);

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
                                    currentPairVP.generateVertexDisjointPaths();
                                } else {
                                    if (pairedVertex1Index >= 0) vertexList.get(pairedVertex1Index).wasClicked = false;
                                    if (pairedVertex2Index >= 0) vertexList.get(pairedVertex2Index).wasClicked = false;
                                    pairedVertex1Index = idx;
                                    pairedVertex2Index = -1;
                                    currentPairVP = null;
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
                            edgeList.remove(edgeVictim);
                            markGraphDirty();

                            updateHover(e.getX(), e.getY());
                            refresh();
                        }
                        break;
                    }
                    case 7: {
                        Edge target = null;
                        for (Edge ed : edgeList) {
                            if (ed.hasIntersection(e.getX(), e.getY())) {
                                target = ed;
                                break;
                            }
                        }
                        if (target == null) break;

                        String input = JOptionPane.showInputDialog(
                                frame,
                                "Edge " + target.vertex1.name + " \u2192 " + target.vertex2.name
                                     + (target.directed ? " (directed)" : " (undirected)")
                                     + "\nEnter new weight (non-negative integer):",
                                "" + target.weight);

                        if (input != null) {
                            try {
                                int w = Integer.parseInt(input.trim());
                                if (w < 0) {
                                    JOptionPane.showMessageDialog(frame,
                                            "Dijkstra requires non-negative weights.",
                                            "Invalid weight",
                                            JOptionPane.WARNING_MESSAGE);
                                    break;
                                }
                                target.setWeight(w);
                                markGraphDirty();
                                refresh();
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(frame,
                                        "Please enter a whole number.",
                                        "Invalid weight",
                                        JOptionPane.WARNING_MESSAGE);
                            }
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
            } else if (command.equals("Set Edge Weight")) {
                selectedTool = 7;
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
                markGraphDirty();
            } else if (command.equals("Open File")) {
                int returnValue = fileManager.jF.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    loadFile(fileManager.loadFile(fileManager.jF.getSelectedFile()));
                    System.out.println(fileManager.jF.getSelectedFile());
                    selectedWindow = 0;
                    frame.setContentPane(canvas);
                    frame.revalidate();
                    frame.repaint();
                }
            } else if (command.equals("Save to File")) {
                int returnValue = fileManager.jF.showSaveDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    fileManager.saveFile(vertexList, edgeList, fileManager.jF.getSelectedFile());
                    System.out.println(fileManager.jF.getSelectedFile());
                }
            } else if (command.equals("Graph")) {
                selectedWindow = 0;
                frame.setContentPane(canvas);
                frame.revalidate();
                frame.repaint();
            } else if (command.equals("Properties")) {
                selectedWindow = 1;
                if (vertexList.size() > 0) {
                    int[][] matrix = gP.generateAdjacencyMatrix(vertexList, edgeList);

                    // Compute κ(G) and λ(G); highlight witness sets.
                    gP.vertexConnectivity(vertexList);
                    gP.edgeConnectivity(vertexList, edgeList);

                    // Clear previous red marks, then mark only the witness
                    // vertices and witness edges.
                    for (Vertex v : vertexList) v.wasClicked = false;
                    for (Edge ed : edgeList)    ed.wasClicked = false;

                    for (Vertex v : gP.witnessVertices) v.wasClicked = true;
                    for (Edge ed : gP.witnessEdges)     ed.wasClicked = true;

                    reloadVertexConnections(matrix, vertexList);

                    gP.generateDistanceMatrix(vertexList);
                    gP.displayContainers(vertexList);
                }

                refreshPropertiesScrollSize();
                frame.setContentPane(propertiesScroll);
                frame.revalidate();
                frame.repaint();
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
        EdgeRegistry.rebuild(edgeList);
        erase();
        for (Edge e : edgeList) {
            e.draw(graphic);
        }
        for (Vertex v : vertexList) {
            v.draw(graphic);
        }
        canvas.repaint();
        if (propertiesContent != null) {
            propertiesContent.repaint();
        }
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
                if (i > 0) sb.append("\u2192");
                sb.append(geodesic.get(i).name);
            }
            geodesicStr = sb.length() > 28 ? sb.substring(0, 25) + "..." : sb.toString();
        }

        int pathCount = currentPairVP.pathList != null ? currentPairVP.pathList.size() : 0;

        int maxWidth = 0;
        if (currentPairVP.VertexDisjointContainer != null) {
            for (Vector<Vector<Vertex>> c : currentPairVP.VertexDisjointContainer) {
                if (c.size() > maxWidth) maxWidth = c.size();
            }
        }

        String pairLabel = v1.name + " \u2192 " + v2.name;

        int x = 190, y = 10, w = 280, h = 178;
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
        g.drawString("Simple paths (Walk\u2229no-repeat): " + pathCount, lx, ty); ty += 15;
        g.drawString("Max vertex-disjoint width: " + maxWidth,     lx, ty); ty += 15;
        boolean closed = (v1 == v2);
        g.drawString("Closed walk possible: " + closed,            lx, ty); ty += 15;
        g.drawString("Trail/Path exists: " + reachable,            lx, ty); ty += 15;
        g.drawString("Tour (closed trail): " + closed,             lx, ty);
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
                    g.setColor(Color.black);
                    break;
                }
                // case 1 (Properties) is no longer drawn here — it lives
                // inside the propertiesContent JPanel hosted by propertiesScroll.
                case 1: {
                    // No-op. The frame's content pane is the scroll pane.
                    break;
                }
            }
        }
    }
}