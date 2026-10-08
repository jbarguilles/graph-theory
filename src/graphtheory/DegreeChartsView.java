package graphtheory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.JComponent;

/** The Distributions sub-tab: stacked degree charts, with exact counts on hover. */
public class DegreeChartsView extends JComponent {

    static final int PLOT_W = 360, X = 20, Y = 20;

    private Vector<Vertex> vs = new Vector<Vertex>();
    private Vector<Edge> es = new Vector<Edge>();

    public DegreeChartsView() {
        setToolTipText("");   // registers with the ToolTipManager; getToolTipText(MouseEvent) supplies the text
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    public void showReport(PropertiesReport r) {
        vs = new Vector<Vertex>(r.vertices);
        es = new Vector<Edge>(r.edges);
        setPreferredSize(new Dimension(X + 60 + PLOT_W + 40, Y + GraphProperties.degreeDistributionsHeight(es)));
        revalidate();
        repaint();
    }

    @Override public String getToolTipText(MouseEvent e) {
        return GraphProperties.distributionTooltip(vs, es, X, Y, PLOT_W, e.getX(), e.getY());
    }

    @Override protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        GraphProperties.drawDegreeDistributions(g, vs, es, X, Y, PLOT_W);
    }
}
