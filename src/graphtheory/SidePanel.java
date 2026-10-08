package graphtheory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * The Graph tab's right-hand panel (CONTEXT.md, Display Conventions): Selection, Pair
 * and Walk sections in that order, each shown only when it has something to say.
 * A section is only rebuilt when its text changes, so display() is cheap enough to
 * call on every refresh. Path rows are built only when the list paints them, so the
 * cost of display() does not grow with the number of paths.
 */
public class SidePanel extends JPanel implements Scrollable {

    public static final int WIDTH = 260;

    public interface Listener {
        /** The user clicked row 'index' of the pair's path list. */
        void pathChosen(int index);
    }

    /** What the panel shows. Canvas fills one in on every refresh. */
    public static class Content {
        public List<Vertex> selected = Collections.emptyList();
        /** null = no pair. */
        public PairSummary pair;
        /** Highlighted row of the path list, or -1. */
        public int pathIndex = -1;
        /** null = no walk. */
        public Walk walk;
        /** The Find command's kind (e.g. "Euler Tour") when the walk is a found walk, else null. */
        public String foundKind;
        /** Red line in the Walk section, or null. */
        public String walkMessage;
        public boolean weighted;
        /** The graph's edges, which a tour must all cross. */
        public List<Edge> edges = Collections.emptyList();
    }

    private final Listener listener;
    private boolean updating;
    private String selectionKey, walkKey;
    /** What the Pair section shows now: the summary object, its facts and row names, the highlighted row. */
    private PairSummary shownPair;
    private List<Vertex> rowVertices = Collections.emptyList();
    private String shownPairText;
    private int shownPathIndex = -1;

    // Package-private for SidePanelTest.
    final JTextArea hint = textArea();
    final JPanel selection = section("Selection");
    final JTable selectionTable = new JTable() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JPanel pair = section("Pair");
    final JTextArea pairFacts = textArea();
    PathRows pathRows = new PathRows(null);
    final JList<String> pathList = new JList<String>(pathRows) {
        /** The whole row, so a long walk can be read without scrolling sideways. */
        @Override
        public String getToolTipText(MouseEvent e) {
            int i = locationToIndex(e.getPoint());
            if (i < 0 || !getCellBounds(i, i).contains(e.getPoint())) return null;
            return getModel().getElementAt(i);
        }
    };
    final JPanel walk = section("Walk");
    final JLabel walkHeading = new JLabel();
    final JTextArea walkText = textArea();
    final JTextArea walkFacts = textArea();
    final JTextArea walkMessage = textArea();

    public SidePanel(Listener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        hint.setText("Select a vertex, pick a pair, or build a walk.");
        hint.setForeground(Color.GRAY);
        add(left(hint));

        selectionTable.setFocusable(false);
        selectionTable.setRowSelectionAllowed(false);
        selection.add(left(selectionTable.getTableHeader()));
        selection.add(left(selectionTable));
        add(selection);

        pair.add(left(pairFacts));
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pathList.setVisibleRowCount(8);
        // Fixed cell sizes: JList would otherwise measure every row to lay out the list.
        pathList.setPrototypeCellValue("1. len 1");
        pathList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!updating && !e.getValueIsAdjusting() && pathList.getSelectedIndex() >= 0) {
                    SidePanel.this.listener.pathChosen(pathList.getSelectedIndex());
                }
            }
        });
        // Once clicked the list keeps focus for its arrow keys, but Ctrl+A/V/X fall through
        // to the menu accelerators (Add Vertex, ...) instead of list actions; Ctrl+C does nothing here.
        for (int key : new int[] {KeyEvent.VK_A, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_X}) {
            pathList.getInputMap().put(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK), "none");
        }
        pair.add(left(new JScrollPane(pathList)));
        add(pair);

        walkHeading.setFont(walkHeading.getFont().deriveFont(Font.BOLD));
        walkMessage.setForeground(new Color(200, 0, 0));
        focusOnlyWhenClicked(walkText);
        walk.add(left(walkHeading));
        walk.add(left(walkText));
        walk.add(left(walkFacts));
        walk.add(left(walkMessage));
        add(walk);

        add(Box.createVerticalGlue());
        display(new Content());
    }

    public void display(Content c) {
        updating = true;
        try {
            boolean changed = showSelection(c) | showPair(c) | showWalk(c);
            hint.setVisible(!selection.isVisible() && !pair.isVisible() && !walk.isVisible());
            if (changed) {
                revalidate();
                repaint();
            }
        } finally {
            updating = false;
        }
    }

    private boolean showSelection(Content c) {
        StringBuilder key = new StringBuilder();
        for (Vertex v : c.selected) {
            for (Object o : PanelText.vertexRow(v)) key.append(o).append('\u0000');
            key.append('\n');
        }
        if (key.toString().equals(selectionKey)) return false;
        selectionKey = key.toString();

        selection.setVisible(!c.selected.isEmpty());
        if (c.selected.size() == 1) {
            selectionTable.setModel(new DefaultTableModel(
                    PanelText.vertexProperties(c.selected.get(0)), new String[] { "Property", "Value" }));
        } else if (c.selected.size() > 1) {
            Object[][] rows = new Object[c.selected.size()][];
            for (int i = 0; i < rows.length; i++) rows[i] = PanelText.vertexRow(c.selected.get(i));
            selectionTable.setModel(new DefaultTableModel(rows, PanelText.VERTEX_COLUMNS));
        }
        return true;
    }

    private boolean showPair(Content c) {
        boolean newPair = c.pair != shownPair;
        if (newPair) rowVertices = verticesOnPaths(c.pair);
        String facts = c.pair == null ? "" : PanelText.join(java.util.Arrays.asList(PanelText.pairFacts(c.pair)), "\n");
        // Rows name the vertices on the paths, so a rename must re-render them.
        StringBuilder text = new StringBuilder(facts);
        for (Vertex v : rowVertices) text.append('\n').append(v.name);
        boolean textChanged = !text.toString().equals(shownPairText);
        if (!newPair && !textChanged && c.pathIndex == shownPathIndex) return false;
        shownPair = c.pair;
        shownPairText = text.toString();
        shownPathIndex = c.pathIndex;

        pair.setVisible(c.pair != null);
        pairFacts.setText(facts);
        if (newPair) {
            pathRows = new PathRows(c.pair);
            pathList.setPrototypeCellValue(widestRowGuess(c.pair));
            pathList.setModel(pathRows);
        } else if (textChanged) {
            pathRows.rowsChanged();
        }
        if (c.pathIndex >= 0 && c.pathIndex < pathRows.getSize()) {
            pathList.setSelectedIndex(c.pathIndex);
            pathList.ensureIndexIsVisible(c.pathIndex);
        } else {
            pathList.clearSelection();
        }
        return true;
    }

    /** Each vertex on some path of s, once. */
    private static List<Vertex> verticesOnPaths(PairSummary s) {
        if (s == null) return Collections.emptyList();
        IdentityHashMap<Vertex, Boolean> seen = new IdentityHashMap<Vertex, Boolean>();
        List<Vertex> vs = new java.util.ArrayList<Vertex>();
        for (Vertex v : java.util.Arrays.asList(s.from, s.to)) {
            if (seen.put(v, Boolean.TRUE) == null) vs.add(v);
        }
        for (Walk p : s.paths) {
            for (Vertex v : p.vertices()) {
                if (seen.put(v, Boolean.TRUE) == null) vs.add(v);
            }
        }
        return vs;
    }

    /** The row of the path with the most edges, sizing every cell so JList need not measure each row. */
    private static String widestRowGuess(PairSummary s) {
        if (s == null || s.paths.isEmpty()) return "1. len 1";
        int widest = 0;
        for (int i = 1; i < s.paths.size(); i++) {
            if (s.paths.get(i).length() > s.paths.get(widest).length()) widest = i;
        }
        // The last row number is the longest one.
        return PanelText.pathRow(s.paths.size() - 1, s.paths.get(widest), s);
    }

    /** The path list's rows, each built from the summary when the list asks for it. */
    static final class PathRows extends AbstractListModel<String> {
        private final PairSummary summary;

        PathRows(PairSummary summary) {
            this.summary = summary;
        }

        public int getSize() {
            return summary == null ? 0 : summary.paths.size();
        }

        public String getElementAt(int i) {
            return PanelText.pathRow(i, summary.paths.get(i), summary);
        }

        /** Same paths, new wording (e.g. a vertex was renamed): every row re-renders. */
        void rowsChanged() {
            if (getSize() > 0) fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    private boolean showWalk(Content c) {
        String key = (c.walk == null ? "" : c.walk.toString() + '\u0000'
                + PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted, c.edges)), "\n"))
                + '\u0000' + c.foundKind + '\u0000' + c.walkMessage;
        if (key.equals(walkKey)) return false;
        walkKey = key;

        walk.setVisible(c.walk != null || c.walkMessage != null);
        walkHeading.setVisible(c.walk != null);
        walkText.setVisible(c.walk != null);
        walkFacts.setVisible(c.walk != null);
        if (c.walk != null) {
            walkHeading.setText(PanelText.walkHeading(c.foundKind));
            walkText.setText(c.walk.toString());
            walkFacts.setText(PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted, c.edges)), "\n"));
        }
        walkMessage.setVisible(c.walkMessage != null);
        walkMessage.setText(c.walkMessage == null ? "" : c.walkMessage);
        return true;
    }

    private static JPanel section(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /**
     * Read-only text. Not focusable: a focused text area would consume the canvas's keys
     * (arrows, Backspace, Ctrl+A/C) and Swing would hand it focus at startup or when a section hides.
     */
    private static JTextArea textArea() {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setFocusable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        t.setFont(new JLabel().getFont());
        return t;
    }

    /**
     * Lets the user select and copy 't' after clicking into it, without it ever taking
     * focus on its own: it is focusable only from a click until it loses focus for good.
     */
    private static void focusOnlyWhenClicked(final JTextArea t) {
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                t.setFocusable(true);
                t.requestFocusInWindow();
            }
        });
        t.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) t.setFocusable(false);
            }
        });
    }

    private static <T extends javax.swing.JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // Scrollable: track the viewport's width so text wraps instead of scrolling sideways.

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(WIDTH, getPreferredSize().height);
    }

    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
        return 16;
    }

    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
        return Math.max(16, r.height - 16);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
