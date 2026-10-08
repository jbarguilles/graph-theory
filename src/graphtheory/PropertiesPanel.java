package graphtheory;

import java.awt.Dimension;
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * The Properties tab (CONTEXT.md, Display Conventions): Overview, Vertices, Matrices and
 * Distributions. display() does nothing when given the report it already shows.
 */
public class PropertiesPanel extends JTabbedPane {

    public interface Listener {
        /** The user picked the side containing p to propose. */
        void proposerChosen(Vertex p);

        void editPreferences();
    }

    final OverviewView overview;
    final JTable verticesTable = new JTable() {
        // Fill the width when the columns fit; otherwise keep their widths and scroll sideways.
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() == null || getPreferredSize().width <= getParent().getWidth();
        }
    };
    final MatricesView matrices = new MatricesView();
    final DegreeChartsView charts = new DegreeChartsView();
    private PropertiesReport shown;

    public PropertiesPanel(Listener listener) {
        overview = new OverviewView(listener);
        verticesTable.setAutoCreateRowSorter(true);
        verticesTable.setCellSelectionEnabled(true);
        TableCopy.install(verticesTable, null);
        addTab("Overview", overview);
        addTab("Vertices", new JScrollPane(verticesTable));
        addTab("Matrices", matrices);
        addTab("Distributions", new JScrollPane(charts));
        setPreferredSize(new Dimension(800, 600));
    }

    public void display(PropertiesReport r) {
        if (r == shown) return;
        shown = r;
        overview.showReport(r);
        Object[][] rows = new Object[r.vertices.size()][];
        for (int i = 0; i < rows.length; i++) rows[i] = PanelText.verticesRow(r.vertices.get(i), r.vertices, r.edges);
        // A new model gets a new sorter; keep the user's sort (the columns never change).
        List<? extends RowSorter.SortKey> sortKeys = verticesTable.getRowSorter() == null ? null
                : verticesTable.getRowSorter().getSortKeys();
        verticesTable.setModel(new DefaultTableModel(rows, PanelText.VERTICES_COLUMNS) {
            @Override public Class<?> getColumnClass(int c) { return c >= 1 && c <= 3 ? Integer.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        if (sortKeys != null && verticesTable.getRowSorter() != null) verticesTable.getRowSorter().setSortKeys(sortKeys);
        fitColumns(verticesTable);
        matrices.showReport(r);
        charts.showReport(r);
    }

    /** Each column's preferred width = its header or its widest cell, so nothing is cut off with "...". */
    private static void fitColumns(JTable t) {
        for (int c = 0; c < t.getColumnCount(); c++) {
            TableColumn col = t.getColumnModel().getColumn(c);
            TableCellRenderer hr = t.getTableHeader().getDefaultRenderer();
            int w = hr.getTableCellRendererComponent(t, col.getHeaderValue(), false, false, -1, c)
                    .getPreferredSize().width;
            for (int row = 0; row < t.getRowCount(); row++) {
                w = Math.max(w, t.prepareRenderer(t.getCellRenderer(row, c), row, c).getPreferredSize().width);
            }
            col.setPreferredWidth(w + 12);
        }
    }
}
