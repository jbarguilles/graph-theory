package graphtheory;

import javax.swing.JTable;
import org.junit.Test;
import static org.junit.Assert.*;

public class TableCopyTest {

    @Test
    public void tsv_withColumnAndRowHeaders() {
        assertEquals("\tb\tc\na\t1\t\u221E\n",
                TableCopy.tsv(new String[] {"b", "c"}, new String[] {"a"}, new String[][] {{"1", "\u221E"}}));
    }

    @Test
    public void tsv_withoutRowHeaders() {
        assertEquals("Name\tDegree\na\t2\n",
                TableCopy.tsv(new String[] {"Name", "Degree"}, null, new String[][] {{"a", "2"}}));
    }

    @Test
    public void selection_copiesOnlySelectedCells_inViewOrder() {
        JTable t = new JTable(new Object[][] {{"a", 1, "x"}, {"b", 2, "y"}}, new Object[] {"N", "D", "Z"});
        t.setCellSelectionEnabled(true);
        t.changeSelection(1, 0, false, false);
        t.changeSelection(1, 1, false, true);
        assertEquals("N\tD\nb\t2\n", TableCopy.selection(t, null));
    }
}
