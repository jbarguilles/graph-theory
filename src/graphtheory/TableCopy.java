package graphtheory;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

/** Ctrl+C on a Properties table copies the selected cells as tab-separated values, with headers. */
public final class TableCopy {

    private TableCopy() {}

    /** Header row (blank corner cell when rowNames is given), then one line per row. */
    public static String tsv(String[] columnNames, String[] rowNames, String[][] cells) {
        StringBuilder sb = new StringBuilder();
        if (rowNames != null) sb.append('\t');
        sb.append(String.join("\t", columnNames)).append('\n');
        for (int r = 0; r < cells.length; r++) {
            if (rowNames != null) sb.append(rowNames[r]).append('\t');
            sb.append(String.join("\t", cells[r])).append('\n');
        }
        return sb.toString();
    }

    /** The table's selected cells; rowNames[viewRow] labels each row, or null for none. */
    public static String selection(JTable t, String[] rowNames) {
        int[] rows = t.getSelectedRows(), cols = t.getSelectedColumns();
        String[] colNames = new String[cols.length];
        for (int c = 0; c < cols.length; c++) colNames[c] = t.getColumnName(cols[c]);
        String[] names = rowNames == null ? null : new String[rows.length];
        String[][] cells = new String[rows.length][cols.length];
        for (int r = 0; r < rows.length; r++) {
            if (names != null) names[r] = rowNames[rows[r]];
            for (int c = 0; c < cols.length; c++) cells[r][c] = String.valueOf(t.getValueAt(rows[r], cols[c]));
        }
        return tsv(colNames, names, cells);
    }

    /** Makes Ctrl+C on t copy selection(t, rowNames). rowNames may be replaced later via the array. */
    public static void install(final JTable t, final String[][] rowNamesHolder) {
        t.setTransferHandler(new TransferHandler() {
            @Override public int getSourceActions(JComponent c) { return COPY; }

            @Override protected Transferable createTransferable(JComponent c) {
                return new StringSelection(selection(t, rowNamesHolder == null ? null : rowNamesHolder[0]));
            }
        });
    }
}
