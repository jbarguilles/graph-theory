package graphtheory;

import java.util.Arrays;
import java.util.Vector;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidePanelTest {

    private int chosen = -2;

    private SidePanel panel() {
        return new SidePanel(new SidePanel.Listener() {
            public void pathChosen(int index) {
                chosen = index;
            }
        });
    }

    private final Vertex u = new Vertex("u", 0, 0);
    private final Vertex v = new Vertex("v", 0, 0);

    private PairSummary pairWithTwoPaths() {
        return new PairSummary(u, v, new Vector<Edge>(Arrays.asList(new Edge(u, v, false), new Edge(u, v, true))));
    }

    @Test
    public void nothingToShow_onlyTheHint() {
        SidePanel p = panel();
        p.display(new SidePanel.Content());
        assertTrue(p.hint.isVisible());
        assertFalse(p.selection.isVisible());
        assertFalse(p.pair.isVisible());
        assertFalse(p.walk.isVisible());
    }

    @Test
    public void oneVertex_propertyRows() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.selected = Arrays.asList(u);
        p.display(c);
        assertTrue(p.selection.isVisible());
        assertFalse(p.hint.isVisible());
        assertEquals(8, p.selectionTable.getRowCount());
        assertEquals(2, p.selectionTable.getColumnCount());
    }

    @Test
    public void severalVertices_oneRowEach() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.selected = Arrays.asList(u, v);
        p.display(c);
        assertEquals(2, p.selectionTable.getRowCount());
        assertEquals(PanelText.VERTEX_COLUMNS.length, p.selectionTable.getColumnCount());
    }

    @Test
    public void pair_listsEveryPath_andSelectsTheGivenRowWithoutCallingBack() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        c.pathIndex = 1;
        p.display(c);
        assertTrue(p.pair.isVisible());
        assertEquals(2, p.pathList.getModel().getSize());
        assertEquals(1, p.pathList.getSelectedIndex());
        assertEquals(-2, chosen);
    }

    @Test
    public void clickingAPathRow_tellsTheListener() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        p.display(c);
        p.pathList.setSelectedIndex(1);
        assertEquals(1, chosen);
    }

    @Test
    public void walk_headingTextAndMessage() {
        SidePanel p = panel();
        Edge uv = new Edge(u, v, false);
        Walk w = new Walk(u);
        w.extend(uv);
        SidePanel.Content c = new SidePanel.Content();
        c.walk = w;
        c.foundKind = "Euler Trail";
        c.walkMessage = "No edge from v to u";
        p.display(c);
        assertTrue(p.walk.isVisible());
        assertEquals("Euler trail (found)", p.walkHeading.getText());
        assertEquals(w.toString(), p.walkText.getText());
        assertTrue(p.walkMessage.isVisible());
    }

    @Test
    public void messageWithoutWalk_stillShowsWalkSection() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.walkMessage = "No Euler Tour exists";
        p.display(c);
        assertTrue(p.walk.isVisible());
        assertFalse(p.walkText.isVisible());
    }

    @Test
    public void walkMessage_isWrappingText() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.walkMessage = "No edge from v to u <here> & there";
        p.display(c);
        assertTrue(p.walkMessage.getLineWrap());
        assertEquals("No edge from v to u <here> & there", p.walkMessage.getText());
        assertTrue(p.hint.getLineWrap());
    }

    /** Counts list events so a test can tell whether display() touched the rows. */
    private static class Events implements ListDataListener {
        int count;
        public void intervalAdded(ListDataEvent e) { count++; }
        public void intervalRemoved(ListDataEvent e) { count++; }
        public void contentsChanged(ListDataEvent e) { count++; }
    }

    @Test
    public void unchangedContent_keepsTheRowsAndTheUsersSelection() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        c.pathIndex = 0;
        p.display(c);
        ListModel<String> model = p.pathList.getModel();
        Events events = new Events();
        model.addListDataListener(events);
        p.pathList.setSelectedIndex(1);
        p.display(c);
        assertSame(model, p.pathList.getModel());
        assertEquals(0, events.count);
        assertEquals(1, p.pathList.getSelectedIndex());
    }

    @Test
    public void newSummary_replacesTheRows() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        p.display(c);
        SidePanel.Content d = new SidePanel.Content();
        d.pair = new PairSummary(u, v, new Vector<Edge>(Arrays.asList(new Edge(u, v, false))));
        p.display(d);
        assertEquals(1, p.pathList.getModel().getSize());
        assertEquals(PanelText.pathRow(0, d.pair.paths.get(0), d.pair), p.pathList.getModel().getElementAt(0));
    }

    @Test
    public void renameUnderTheSameSummary_rowsReRender() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        p.display(c);
        Events events = new Events();
        p.pathList.getModel().addListDataListener(events);
        u.name = "w";
        p.display(c);
        assertTrue(events.count > 0);
        assertTrue(p.pathList.getModel().getElementAt(0).contains("w -{"));
    }

    @Test
    public void pathRowTooltip_isTheWholeRow() {
        SidePanel p = panel();
        SidePanel.Content c = new SidePanel.Content();
        c.pair = pairWithTwoPaths();
        p.display(c);
        p.pathList.setSize(200, 200);
        java.awt.Rectangle r = p.pathList.getCellBounds(1, 1);
        java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(p.pathList, java.awt.event.MouseEvent.MOUSE_MOVED,
                0, 0, r.x + 1, r.y + 1, 0, false);
        assertEquals(p.pathList.getModel().getElementAt(1), p.pathList.getToolTipText(e));
    }

    @Test
    public void readOnlyText_neverTakesKeyboardFocusFromTheCanvas() {
        SidePanel p = panel();
        assertFalse(p.hint.isFocusable());
        assertFalse(p.pairFacts.isFocusable());
        assertFalse(p.walkFacts.isFocusable());
        assertFalse(p.walkMessage.isFocusable());
        // The walk is copyable, but only once the user clicks into it.
        assertFalse(p.walkText.isFocusable());
        p.walkText.dispatchEvent(new java.awt.event.MouseEvent(p.walkText, java.awt.event.MouseEvent.MOUSE_PRESSED,
                0, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false, java.awt.event.MouseEvent.BUTTON1));
        assertTrue(p.walkText.isFocusable());
    }

    @Test
    public void pathList_letsCtrlLettersThroughToTheMenu() {
        SidePanel p = panel();
        javax.swing.KeyStroke ctrlA = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A,
                java.awt.event.InputEvent.CTRL_DOWN_MASK);
        assertEquals("none", p.pathList.getInputMap().get(ctrlA));
    }
}
