package graphtheory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * The Graph tab's right-hand panel (CONTEXT.md, Display Conventions): Selection, Pair
 * and Walk sections in that order, each shown only when it has something to say.
 * A section is only rebuilt when its text changes, so display() is cheap enough to
 * call on every refresh.
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
    }

    private final Listener listener;
    private boolean updating;
    private String selectionKey, pairKey, walkKey;

    // Package-private for SidePanelTest.
    final JLabel hint = new JLabel("<html>Select a vertex, pick a pair, or build a walk.</html>");
    final JPanel selection = section("Selection");
    final JTable selectionTable = new JTable() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JPanel pair = section("Pair");
    final JTextArea pairFacts = textArea();
    final DefaultListModel<String> pathModel = new DefaultListModel<String>();
    final JList<String> pathList = new JList<String>(pathModel);
    final JPanel walk = section("Walk");
    final JLabel walkHeading = new JLabel();
    final JTextArea walkText = textArea();
    final JTextArea walkFacts = textArea();
    final JLabel walkMessage = new JLabel();

    public SidePanel(Listener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

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
        pathList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!updating && !e.getValueIsAdjusting() && pathList.getSelectedIndex() >= 0) {
                    SidePanel.this.listener.pathChosen(pathList.getSelectedIndex());
                }
            }
        });
        pair.add(left(new JScrollPane(pathList)));
        add(pair);

        walkHeading.setFont(walkHeading.getFont().deriveFont(Font.BOLD));
        walkMessage.setForeground(new Color(200, 0, 0));
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
        String facts = c.pair == null ? "" : PanelText.join(java.util.Arrays.asList(PanelText.pairFacts(c.pair)), "\n");
        StringBuilder rows = new StringBuilder();
        if (c.pair != null) {
            for (int i = 0; i < c.pair.paths.size(); i++) {
                rows.append(PanelText.pathRow(i, c.pair.paths.get(i), c.pair)).append('\n');
            }
        }
        String key = facts + '\u0000' + rows + '\u0000' + c.pathIndex;
        if (key.equals(pairKey)) return false;
        String oldRows = pairKey == null ? null : pairKey.substring(pairKey.indexOf('\u0000') + 1, pairKey.lastIndexOf('\u0000'));
        pairKey = key;

        pair.setVisible(c.pair != null);
        pairFacts.setText(facts);
        if (!rows.toString().equals(oldRows)) {
            pathModel.clear();
            if (c.pair != null) {
                for (int i = 0; i < c.pair.paths.size(); i++) {
                    pathModel.addElement(PanelText.pathRow(i, c.pair.paths.get(i), c.pair));
                }
            }
        }
        if (c.pathIndex >= 0 && c.pathIndex < pathModel.getSize()) {
            pathList.setSelectedIndex(c.pathIndex);
            pathList.ensureIndexIsVisible(c.pathIndex);
        } else {
            pathList.clearSelection();
        }
        return true;
    }

    private boolean showWalk(Content c) {
        String key = (c.walk == null ? "" : c.walk.toString() + '\u0000'
                + PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted)), "\n"))
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
            walkFacts.setText(PanelText.join(java.util.Arrays.asList(PanelText.walkFacts(c.walk, c.weighted)), "\n"));
        }
        walkMessage.setVisible(c.walkMessage != null);
        walkMessage.setText(c.walkMessage == null ? "" : "<html>" + escape(c.walkMessage) + "</html>");
        return true;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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

    private static JTextArea textArea() {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        t.setFont(new JLabel().getFont());
        return t;
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
