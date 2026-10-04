package graphtheory;

import java.awt.Color;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new Canvas("Graph Theory Visualizer", 800, 600, Color.WHITE);
    }
}
