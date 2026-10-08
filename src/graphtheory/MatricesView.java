package graphtheory;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

/** The Matrices sub-tab: one matrix at a time, names frozen on both edges, ∞ = unreachable. */
public class MatricesView extends JPanel {

    static final String ADJACENCY = "Adjacency", DISTANCE = "Distance", WEIGHTED = "Weighted distance";

    final JComboBox<String> kind = new JComboBox<String>();
    final JLabel caption = new JLabel(" ");
    final JTable table = new JTable();
    final JTable rowHeader = new JTable();
    private final String[][] rowNames = { new String[0] };
    private PropertiesReport report;
    private boolean updating;

    public MatricesView() {
        super(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(8, 4));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(kind, BorderLayout.WEST);
        top.add(caption, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);
        rowHeader.setFocusable(false);
        rowHeader.setRowSelectionAllowed(false);
        rowHeader.setBackground(UIManager.getColor("TableHeader.background"));
        TableCopy.install(table, rowNames);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setRowHeaderView(rowHeader);
        add(scroll, BorderLayout.CENTER);

        kind.addActionListener(e -> { if (!updating) showSelected(); });
    }

    public void show(PropertiesReport r) {
        report = r;
        updating = true;
        Object was = kind.getSelectedItem();
        kind.removeAllItems();
        kind.addItem(ADJACENCY);
        kind.addItem(DISTANCE);
        if (r.weightedDistances != null) kind.addItem(WEIGHTED);
        kind.setSelectedItem(was != null && (r.weightedDistances != null || !WEIGHTED.equals(was)) ? was : ADJACENCY);
        updating = false;
        showSelected();
    }

    private void showSelected() {
        if (report == null) return;
        Object k = kind.getSelectedItem();
        final int[][] m = DISTANCE.equals(k) ? report.distances
                : WEIGHTED.equals(k) ? report.weightedDistances : report.adjacency;
        caption.setText(DISTANCE.equals(k) ? "Fewest edges on a walk from row to column. ∞ = unreachable."
                : WEIGHTED.equals(k) ? "Smallest weight of a walk from row to column. ∞ = unreachable."
                : "Edges from row to column. An undirected self-loop counts 2.");
        final String[] names = new String[report.vertices.size()];
        for (int i = 0; i < names.length; i++) names[i] = report.vertices.get(i).name;
        rowNames[0] = names;

        table.setModel(new AbstractTableModel() {
            public int getRowCount() { return names.length; }
            public int getColumnCount() { return names.length; }
            public String getColumnName(int c) { return names[c]; }
            public Object getValueAt(int r, int c) {
                return m[r][c] == GraphMatrices.UNREACHABLE ? PanelText.INFINITY : String.valueOf(m[r][c]);
            }
        });
        rowHeader.setModel(new AbstractTableModel() {
            public int getRowCount() { return names.length; }
            public int getColumnCount() { return 1; }
            public Object getValueAt(int r, int c) { return names[r]; }
        });

        FontMetrics fm = table.getFontMetrics(table.getFont());
        int widest = fm.stringWidth(PanelText.INFINITY);
        for (int i = 0; i < names.length; i++) {
            widest = Math.max(widest, fm.stringWidth(names[i]));
            for (int j = 0; j < names.length; j++) widest = Math.max(widest, fm.stringWidth(String.valueOf(m[i][j])));
        }
        int colW = widest + 16;
        for (int c = 0; c < table.getColumnCount(); c++) table.getColumnModel().getColumn(c).setPreferredWidth(colW);
        rowHeader.getColumnModel().getColumn(0).setPreferredWidth(colW);
        rowHeader.setPreferredScrollableViewportSize(new java.awt.Dimension(colW, 0));
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
}
