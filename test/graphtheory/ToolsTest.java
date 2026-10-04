package graphtheory;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class ToolsTest {

    @Test
    public void everyToolHasLabelsAHintAndAShortcut() {
        for (int tool : Tools.ORDER) {
            assertFalse("short " + tool, Tools.shortLabel(tool).isEmpty());
            assertFalse("menu " + tool, Tools.menuLabel(tool).isEmpty());
            assertFalse("hint " + tool, Tools.hint(tool).isEmpty());
            assertTrue("shortcut " + tool, Tools.shortcut(tool) != 0);
        }
    }

    @Test
    public void shortcutsAreDistinct() {
        Set<Integer> seen = new HashSet<Integer>();
        for (int tool : Tools.ORDER) assertTrue(seen.add(Tools.shortcut(tool)));
    }

    @Test
    public void noToolSelected_stillHasAHint() {
        assertFalse(Tools.hint(Tools.NONE).isEmpty());
    }
}
