package graphtheory;

import java.util.Arrays;
import java.util.Vector;
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
}
