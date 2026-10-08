package graphtheory;

import java.util.List;

/** Places vertices on the canvas. */
public final class Layout {

    /** Distance kept from the canvas edge: vertex radius plus its outer (root) ring. */
    public static final int MARGIN = Vertex.RADIUS + 12;

    private Layout() {}

    /** Evenly around a circle of radius height/5 at the canvas centre (Extras > Auto Arrange). */
    public static void arrangeOnCircle(List<Vertex> vs, int width, int height) {
        if (vs.isEmpty()) return;
        double radius = height / 5.0;
        double centreX = width / 2.0;
        double centreY = height / 2.0;
        for (int i = 0; i < vs.size(); i++) {
            double angle = 2 * Math.PI * i / vs.size();
            vs.get(i).location.x = (int) Math.round(centreX + Math.cos(angle) * radius);
            vs.get(i).location.y = (int) Math.round(centreY + Math.sin(angle) * radius);
        }
    }

    /** Moves any vertex outside the canvas (less MARGIN) to the nearest point inside it. */
    public static void clampInto(List<Vertex> vs, int width, int height) {
        for (Vertex v : vs) {
            v.location.x = Math.max(MARGIN, Math.min(width - MARGIN, v.location.x));
            v.location.y = Math.max(MARGIN, Math.min(height - MARGIN, v.location.y));
        }
    }
}
