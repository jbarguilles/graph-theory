package graphtheory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class FileManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void withExtension_addsGraphWhenMissing() {
        assertEquals("triangle.graph", FileManager.withExtension(new File("triangle")).getName());
    }

    @Test
    public void withExtension_keepsAnExtensionTheUserTyped() {
        assertEquals("triangle.graph", FileManager.withExtension(new File("triangle.graph")).getName());
        assertEquals("notes.txt", FileManager.withExtension(new File("notes.txt")).getName());
    }

    @Test
    public void writeThenRead_returnsTheSameText() throws Exception {
        File f = tmp.newFile("g.graph");
        String text = "graph-theory 1\nvertex a 1 2\n";
        FileManager.write(f, text);
        assertEquals(text, FileManager.read(f));
    }
}
