package graphtheory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Left-hand column of tool buttons; the active tool stays pressed. */
public class ToolPalette extends JPanel {

    public interface Listener {
        void toolSelected(int tool);
    }

    private final Map<Integer, JToggleButton> buttons = new HashMap<Integer, JToggleButton>();
    private final ButtonGroup group = new ButtonGroup();

    public ToolPalette(final Listener listener) {
        super(new BorderLayout());
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 4));
        column.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        for (final int tool : Tools.ORDER) {
            JToggleButton b = new JToggleButton(Tools.shortLabel(tool));
            b.setToolTipText(Tools.menuLabel(tool) + " (Ctrl+" + KeyEvent.getKeyText(Tools.shortcut(tool)) + ")");
            // Not focusable, so keyboard shortcuts (Backspace, arrows) keep reaching the canvas.
            b.setFocusable(false);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.toolSelected(tool);
                }
            });
            group.add(b);
            column.add(b);
            buttons.put(tool, b);
        }
        add(column, BorderLayout.NORTH);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
    }

    /** Shows the tool as pressed, without notifying the listener. */
    public void setSelectedTool(int tool) {
        JToggleButton b = buttons.get(tool);
        if (b != null) {
            b.setSelected(true);
        } else {
            group.clearSelection();
        }
    }
}
