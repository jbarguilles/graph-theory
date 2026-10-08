package graphtheory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** The Overview sub-tab: the graph with its minimum cuts, the summary, and the stable-matching controls. */
public class OverviewView extends JPanel {

    static final int THUMB_W = 400, THUMB_H = 300;

    final JEditorPane summary = new JEditorPane("text/html", "");
    final JComboBox<String> side = new JComboBox<String>();
    final JButton editPreferences = new JButton("Edit preferences\u2026");
    private final Thumbnail thumbnail = new Thumbnail();
    private List<List<Vertex>> sides;
    private boolean updating;

    public OverviewView(final PropertiesPanel.Listener listener) {
        super(new BorderLayout(8, 8));
        summary.setEditable(false);

        // A fixed width whatever the sides are; the full side is in the tooltips.
        side.setPrototypeDisplayValue("{a, b, c, d, e}");
        side.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focused) {
                Component c = super.getListCellRendererComponent(list, value, index, selected, focused);
                setToolTipText(value == null ? null : value.toString());
                return c;
            }
        });

        // The label on its own line, so the row fits beside the thumbnail at the default size.
        JPanel label = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        label.add(new JLabel("Stable matching \u2014 proposing side:"));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(side);
        row.add(editPreferences);
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        label.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(label);
        controls.add(row);

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(summary), BorderLayout.CENTER);
        right.add(controls, BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout());
        left.add(thumbnail, BorderLayout.NORTH);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        side.addActionListener(e -> {
            int i = side.getSelectedIndex();
            side.setToolTipText(i < 0 ? null : side.getItemAt(i));
            if (!updating && sides != null && i >= 0 && !sides.get(i).isEmpty()) listener.proposerChosen(sides.get(i).get(0));
        });
        editPreferences.addActionListener(e -> listener.editPreferences());
    }

    public void showReport(PropertiesReport r) {
        thumbnail.setReport(r);
        summary.setText(SummaryText.html(SummaryText.sections(r)));
        summary.setCaretPosition(0);

        updating = true;
        side.removeAllItems();
        sides = r.sides;
        boolean usable = sides != null && !sides.get(0).isEmpty() && !sides.get(1).isEmpty();
        if (sides != null) {
            for (List<Vertex> s : sides) side.addItem(SummaryText.vertexSet(s));
            side.setSelectedIndex(sides.get(1).contains(r.proposer) ? 1 : 0);
        }
        side.setToolTipText(side.getSelectedIndex() < 0 ? null : side.getItemAt(side.getSelectedIndex()));
        side.setEnabled(usable);
        editPreferences.setEnabled(!r.vertices.isEmpty());
        updating = false;
    }

    /** The graph fitted into a box, with the minimum vertex and edge cuts dashed in magenta. */
    private static final class Thumbnail extends JComponent {
        private PropertiesReport report;
        private Set<Vertex> cutVertices = Collections.emptySet();
        private Set<Edge> cutEdges = Collections.emptySet();

        Thumbnail() { setPreferredSize(new Dimension(THUMB_W, THUMB_H)); }

        void setReport(PropertiesReport r) {
            report = r;
            cutVertices = new HashSet<Vertex>(r.vertexCut.members);
            cutEdges = new HashSet<Edge>(r.edgeCut.members);
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (report != null) {
                    g.transform(GraphRenderer.fit(report.vertices, getWidth(), getHeight(), 40));
                    GraphRenderer.Options o = new GraphRenderer.Options();
                    o.cutVertices = cutVertices;
                    o.cutEdges = cutEdges;
                    GraphRenderer.paint(g, report.vertices, report.edges, o);
                }
            } finally {
                g.dispose();
            }
            g0.setColor(Color.BLACK);
            g0.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
