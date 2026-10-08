package graphtheory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

public class PropertiesPanelTest {

    private static Vertex v(String name) { return new Vertex(name, 0, 0); }

    private static Edge und(Vertex a, Vertex b) { return new Edge(a, b, false); }

    private static Edge arc(Vertex a, Vertex b) { return new Edge(a, b, true); }

    private static Edge w(Edge e, int weight) { e.setWeight(weight); return e; }

    private static List<Vertex> vs(Vertex... xs) { return new Vector<Vertex>(Arrays.asList(xs)); }

    private static List<Edge> es(Edge... xs) { return new Vector<Edge>(Arrays.asList(xs)); }

    private final Vertex a = v("a"), b = v("b"), c = v("c");

    private PropertiesPanel panel() {
        return new PropertiesPanel(new PropertiesPanel.Listener() {
            public void proposerChosen(Vertex p) {}
            public void editPreferences() {}
        });
    }

    @Test
    public void fourSubTabs() {
        PropertiesPanel p = panel();
        assertEquals(4, p.getTabCount());
        assertEquals(Arrays.asList("Overview", "Vertices", "Matrices", "Distributions"),
                Arrays.asList(p.getTitleAt(0), p.getTitleAt(1), p.getTitleAt(2), p.getTitleAt(3)));
    }

    @Test
    public void verticesTable_oneRowPerVertex_sortsNumbersAsNumbers() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(a, c)), null));
        assertEquals(3, p.verticesTable.getRowCount());
        assertEquals(Integer.class, p.verticesTable.getModel().getColumnClass(1));
    }

    @Test
    public void verticesTable_columnsAreWideEnoughForTheirText() {
        PropertiesPanel p = panel();
        Vertex longer = v("averyveryverylongname"), other = v("anotherlongvertexname");
        p.display(new PropertiesReport(vs(a, longer, other), es(und(a, longer), arc(a, other), arc(other, a)), null));
        javax.swing.JTable t = p.verticesTable;
        String text = (String) t.getValueAt(0, 8);   // a's Neighbours
        int needed = t.getFontMetrics(t.getFont()).stringWidth(text);
        assertTrue(t.getColumnModel().getColumn(8).getPreferredWidth() > needed);
    }

    @Test
    public void matrices_weightedOptionOnlyOnWeightedGraph() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        assertEquals(2, p.matrices.kind.getItemCount());
        p.display(new PropertiesReport(vs(a, b), es(w(und(a, b), 3)), null));
        assertEquals(3, p.matrices.kind.getItemCount());
    }

    @Test
    public void matrices_unreachableIsInfinity_rowHeadersAreNames() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(arc(a, b)), null));
        p.matrices.kind.setSelectedIndex(1);   // Distance
        assertEquals(PanelText.INFINITY, p.matrices.table.getValueAt(1, 0));
        assertEquals("b", p.matrices.rowHeader.getValueAt(1, 0));
    }

    @Test
    public void sameReport_isNotRedisplayed() {
        PropertiesPanel p = panel();
        PropertiesReport r = new PropertiesReport(vs(a), es(), null);
        p.display(r);
        javax.swing.table.TableModel before = p.verticesTable.getModel();
        p.display(r);
        assertSame(before, p.verticesTable.getModel());
    }

    @Test
    public void sideSelector_quietWhileShowingReport_firesOnUserChoice() {
        final List<Vertex> chosen = new Vector<Vertex>();
        PropertiesPanel p = new PropertiesPanel(new PropertiesPanel.Listener() {
            public void proposerChosen(Vertex v) { chosen.add(v); }
            public void editPreferences() {}
        });
        PropertiesReport r = new PropertiesReport(vs(a, b, c), es(und(a, b), und(a, c)), null);
        p.display(r);
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(a, c)), null));
        assertTrue(chosen.isEmpty());
        int other = 1 - p.overview.side.getSelectedIndex();
        p.overview.side.setSelectedIndex(other);
        assertEquals(1, chosen.size());
        assertTrue(r.sides.get(other).contains(chosen.get(0)));
    }

    @Test
    public void matrices_weightedThenUnweighted_fallsBackToAdjacency() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(w(und(a, b), 3)), null));
        p.matrices.kind.setSelectedItem(MatricesView.WEIGHTED);
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        assertEquals(MatricesView.ADJACENCY, p.matrices.kind.getSelectedItem());
    }

    @Test
    public void matrices_copyUsesTheCurrentRowNames() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        p.matrices.kind.setSelectedItem(MatricesView.DISTANCE);
        Vertex x = v("x"), y = v("y");
        p.display(new PropertiesReport(vs(x, y), es(arc(x, y)), null));
        p.matrices.kind.setSelectedItem(MatricesView.ADJACENCY);
        p.matrices.table.selectAll();
        assertEquals(TableCopy.tsv(new String[] { "x", "y" }, new String[] { "x", "y" },
                        new String[][] { { "0", "1" }, { "0", "0" } }),
                TableCopy.selection(p.matrices.table, p.matrices.rowNames[0]));
    }

    @Test
    public void verticesTable_sortKeptAcrossDisplay() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b)), null));
        java.util.List<javax.swing.RowSorter.SortKey> keys = Arrays.asList(
                new javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.DESCENDING));
        p.verticesTable.getRowSorter().setSortKeys(keys);
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(b, c)), null));
        assertEquals(keys, p.verticesTable.getRowSorter().getSortKeys());
    }

    @Test
    public void emptyGraph_displaysAndPaints() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(), es(), null));
        p.setSize(800, 600);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 600,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int t = 0; t < p.getTabCount(); t++) {
            p.setSelectedIndex(t);
            p.doLayout();
            java.awt.Graphics2D g = img.createGraphics();
            p.printAll(g);
            g.dispose();
        }
        assertEquals(0, p.verticesTable.getRowCount());
    }

    @Test
    public void sideSelector_listsBothSides_onlyWhenBipartite() {
        PropertiesPanel p = panel();
        p.display(new PropertiesReport(vs(a, b), es(und(a, b)), null));
        assertEquals(2, p.overview.side.getItemCount());
        assertTrue(p.overview.side.isEnabled());
        p.display(new PropertiesReport(vs(a, b, c), es(und(a, b), und(b, c), und(c, a)), null));
        assertFalse(p.overview.side.isEnabled());
    }
}
