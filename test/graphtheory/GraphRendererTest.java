package graphtheory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphRendererTest {

    private static BufferedImage blank() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        return img;
    }

    private static Color pixel(BufferedImage img, int x, int y) {
        return new Color(img.getRGB(x, y));
    }

    private static void paint(BufferedImage img, List<Vertex> vs, List<Edge> es, GraphRenderer.Options o) {
        Graphics2D g = img.createGraphics();
        GraphRenderer.paint(g, vs, es, o);
        g.dispose();
    }

    @Test
    public void rootCutpoint_showsBothRings() {
        Vertex v = new Vertex("v", 100, 100);
        v.isRoot = true;
        v.isCutpoint = true;
        BufferedImage img = blank();
        paint(img, Arrays.asList(v), Collections.<Edge>emptyList(), new GraphRenderer.Options());
        // cutpoint ring is centred on radius + 4, root ring on radius + 8, both 2px wide
        Color inner = pixel(img, 100 + Vertex.RADIUS + 3, 100);
        Color outer = pixel(img, 100 + Vertex.RADIUS + 7, 100);
        assertTrue("inner ring orange, was " + inner, inner.getRed() > 200 && inner.getBlue() < 100);
        assertTrue("outer ring green, was " + outer,
                outer.getGreen() > outer.getRed() + 50 && outer.getGreen() > outer.getBlue() + 50);
    }

    @Test
    public void removeHover_replacesRings_onlyWithInteraction() {
        Vertex v = new Vertex("v", 100, 100);
        v.isRoot = true;
        v.removeHover = true;
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        BufferedImage img = blank();
        paint(img, Arrays.asList(v), Collections.<Edge>emptyList(), o);
        Color root = pixel(img, 100 + Vertex.RADIUS + 7, 100);
        assertFalse("no green root ring while remove-hovered",
                root.getGreen() > root.getRed() + 50);
    }

    @Test
    public void edgeColor_highlightBeatsBridge() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        e.isBridge = true;
        e.highlight = Color.CYAN;
        assertEquals(Color.CYAN, GraphRenderer.edgeColor(e, new GraphRenderer.Options()));
    }

    @Test
    public void edgeColor_removeHoverIgnoredWithoutInteraction() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        e.removeHover = true;
        assertEquals(GraphRenderer.EDGE, GraphRenderer.edgeColor(e, new GraphRenderer.Options()));
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        assertEquals(GraphRenderer.REMOVE, GraphRenderer.edgeColor(e, o));
    }

    @Test
    public void edgeColor_cutEdge() {
        Edge e = new Edge(new Vertex("a", 0, 0), new Vertex("b", 1, 0), false);
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.cutEdges = new HashSet<Edge>(Arrays.asList(e));
        assertEquals(GraphRenderer.CUT, GraphRenderer.edgeColor(e, o));
    }

    @Test
    public void nameFont_shortName_isFullSize() {
        Graphics2D g = blank().createGraphics();
        assertEquals(GraphRenderer.NAME_MAX, GraphRenderer.nameFont(g, "1").getSize());
    }

    @Test
    public void nameFont_fourChars_fitsInsideVertex() {
        Graphics2D g = blank().createGraphics();
        Font f = GraphRenderer.nameFont(g, "WWWW");
        assertTrue(g.getFontMetrics(f).stringWidth("WWWW") <= 2 * Vertex.RADIUS - 8
                || f.getSize() == GraphRenderer.NAME_MIN);
    }

    @Test
    public void fit_singleVertex_centred() {
        AffineTransform t = GraphRenderer.fit(Arrays.asList(new Vertex("a", 10, 10)), 200, 100, 40);
        Point2D p = t.transform(new Point2D.Double(10, 10), null);
        assertEquals(100, p.getX(), 1e-9);
        assertEquals(50, p.getY(), 1e-9);
    }

    @Test
    public void fit_largeGraph_scaledIntoMargins() {
        List<Vertex> vs = Arrays.asList(new Vertex("a", 0, 0), new Vertex("b", 1000, 0));
        AffineTransform t = GraphRenderer.fit(vs, 200, 200, 50);
        assertEquals(50, t.transform(new Point2D.Double(0, 0), null).getX(), 1e-9);
        assertEquals(150, t.transform(new Point2D.Double(1000, 0), null).getX(), 1e-9);
    }

    @Test
    public void fit_smallGraph_neverScaledUp() {
        List<Vertex> vs = Arrays.asList(new Vertex("a", 0, 0), new Vertex("b", 10, 0));
        assertEquals(1.0, GraphRenderer.fit(vs, 200, 200, 50).getScaleX(), 1e-9);
    }

    @Test
    public void paint_everyKindOfEdge_doesNotThrow() {
        Vertex a = new Vertex("a", 50, 100);
        Vertex b = new Vertex("bcde", 150, 100);
        Edge e1 = new Edge(a, b, false);
        Edge e2 = new Edge(b, a, true);
        e2.setWeight(3);
        e2.stepLabel = "#1,2";
        e2.highlight = Color.ORANGE;
        Edge loop1 = new Edge(a, a, true);
        Edge loop2 = new Edge(a, a, false);
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.interaction = true;
        o.cutVertices = new HashSet<Vertex>(Arrays.asList(b));
        o.cutEdges = new HashSet<Edge>(Arrays.asList(e1));
        paint(blank(), Arrays.asList(a, b), Arrays.asList(e1, e2, loop1, loop2), o);
    }

    @Test
    public void paint_leavesCallersGraphicsStateAlone() {
        Graphics2D g = blank().createGraphics();
        java.awt.Stroke stroke = g.getStroke();
        Font font = g.getFont();
        Object aa = g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
        g.setColor(Color.MAGENTA);
        Vertex a = new Vertex("a", 50, 50);
        Vertex b = new Vertex("b", 150, 50);
        GraphRenderer.paint(g, Arrays.asList(a, b), Arrays.asList(new Edge(a, b, true)), new GraphRenderer.Options());
        assertEquals(Color.MAGENTA, g.getColor());
        assertSame(stroke, g.getStroke());
        assertEquals(font, g.getFont());
        assertEquals(aa, g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING));
    }

    @Test
    public void fit_areaSmallerThanMargins_stillPositiveScale() {
        List<Vertex> vs = Arrays.asList(new Vertex("a", 0, 0), new Vertex("b", 100, 0));
        assertTrue(GraphRenderer.fit(vs, 60, 60, 50).getScaleX() > 0);
    }

    @Test
    public void arrowhead_drawnOverRootRing() {
        Vertex a = new Vertex("a", 20, 100);
        Vertex b = new Vertex("b", 150, 100);
        b.isRoot = true;
        BufferedImage img = blank();
        paint(img, Arrays.asList(a, b), Arrays.asList(new Edge(a, b, true)), new GraphRenderer.Options());
        // the root ring crosses the arrowhead's axis at x = 150 - (RADIUS + 8)
        Color c = pixel(img, 150 - Vertex.RADIUS - 8, 100);
        assertFalse("arrowhead should cover the green ring, was " + c, c.getGreen() > c.getRed() + 50);
    }

    @Test
    public void paintGrid_dotsEveryGridStep() {
        BufferedImage img = blank();
        Graphics2D g = img.createGraphics();
        GraphRenderer.paintGrid(g, 200, 200);
        g.dispose();
        assertEquals(GraphRenderer.GRID, pixel(img, GraphRenderer.GRID_STEP, GraphRenderer.GRID_STEP));
        assertEquals(Color.WHITE, pixel(img, GraphRenderer.GRID_STEP + 10, GraphRenderer.GRID_STEP + 10));
    }

    @Test
    public void analysisOff_hidesRingsBridgesAndHighlights() {
        Vertex v = new Vertex("v", 100, 100);
        v.isRoot = true;
        Edge e = new Edge(v, new Vertex("w", 150, 100), false);
        e.isBridge = true;
        e.highlight = Color.CYAN;
        GraphRenderer.Options o = new GraphRenderer.Options();
        o.analysis = false;
        assertEquals(GraphRenderer.EDGE, GraphRenderer.edgeColor(e, o));
        BufferedImage img = blank();
        paint(img, Arrays.asList(v), Collections.<Edge>emptyList(), o);
        assertEquals(Color.WHITE, pixel(img, 100 + Vertex.RADIUS + 7, 100));
    }
}
