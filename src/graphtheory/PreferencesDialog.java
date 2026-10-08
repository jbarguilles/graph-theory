package graphtheory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** Edit > Preference Lists...: create, reorder or clear each vertex's preference list. */
public class PreferencesDialog extends JDialog {

    private final PreferenceEditor editor;
    private final JList<Vertex> vertexList;
    private final DefaultListModel<Vertex> ranking = new DefaultListModel<Vertex>();
    private final JList<Vertex> rankingList = new JList<Vertex>(ranking);
    private final JButton create = new JButton("Create list"), clear = new JButton("Clear list");
    private final JButton up = new JButton("Up"), down = new JButton("Down");
    private boolean ok;

    /** Shows the dialog modally; true if the user pressed OK and changed something (the editor is then applied by the caller). */
    public static boolean edit(Window owner, PreferenceEditor editor) {
        PreferencesDialog d = new PreferencesDialog(owner, editor);
        d.setVisible(true);
        return d.ok && editor.changed();
    }

    private PreferencesDialog(Window owner, PreferenceEditor editor) {
        super(owner, "Preference Lists", ModalityType.APPLICATION_MODAL);
        this.editor = editor;
        DefaultListModel<Vertex> vm = new DefaultListModel<Vertex>();
        for (Vertex v : editor.vertices()) vm.addElement(v);
        vertexList = new JList<Vertex>(vm);
        vertexList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vertexList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean sel, boolean f) {
                Vertex v = (Vertex) value;
                String mark = PreferencesDialog.this.editor.listOf(v) == null ? "  (no list)" : "  ✓";
                return super.getListCellRendererComponent(l, v.name + mark, i, sel, f);
            }
        });
        rankingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rankingList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean sel, boolean f) {
                return super.getListCellRendererComponent(l, (i + 1) + ". " + ((Vertex) value).name, i, sel, f);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(create);
        buttons.add(clear);
        buttons.add(up);
        buttons.add(down);
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.add(new JLabel("Most preferred first"), BorderLayout.NORTH);
        right.add(new JScrollPane(rankingList), BorderLayout.CENTER);
        right.add(buttons, BorderLayout.EAST);

        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centre.add(new JScrollPane(vertexList));
        centre.add(right);

        JButton okButton = new JButton("OK"), cancel = new JButton("Cancel");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(okButton);
        bottom.add(cancel);

        getContentPane().add(centre, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        vertexList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showRanking(0); });
        create.addActionListener(e -> { editor.create(selected()); showRanking(0); vertexList.repaint(); });
        clear.addActionListener(e -> { editor.clear(selected()); showRanking(-1); vertexList.repaint(); });
        up.addActionListener(e -> showRanking(editor.moveUp(selected(), rankingList.getSelectedIndex())));
        down.addActionListener(e -> showRanking(editor.moveDown(selected(), rankingList.getSelectedIndex())));
        okButton.addActionListener(e -> { ok = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        if (!editor.vertices().isEmpty()) vertexList.setSelectedIndex(0);
        showRanking(0);
        setSize(480, 360);
        setLocationRelativeTo(owner);
    }

    private Vertex selected() { return vertexList.getSelectedValue(); }

    private void showRanking(int select) {
        ranking.clear();
        Vertex v = selected();
        List<Vertex> l = v == null ? null : editor.listOf(v);
        if (l != null) for (Vertex p : l) ranking.addElement(p);
        if (select >= 0 && select < ranking.size()) rankingList.setSelectedIndex(select);
        create.setEnabled(v != null && l == null);
        clear.setEnabled(l != null);
        up.setEnabled(l != null);
        down.setEnabled(l != null);
    }
}
