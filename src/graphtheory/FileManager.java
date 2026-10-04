package graphtheory;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** File dialogs and disk access for .graph files. The format itself is GraphFile. */
public class FileManager {

    public static final String EXTENSION = "graph";

    private final JFileChooser chooser = new JFileChooser();

    public FileManager() {
        chooser.setFileFilter(new FileNameExtensionFilter("Graph files (*.graph)", EXTENSION));
    }

    /** The file to open, or null if the user cancelled. */
    public File chooseOpen(Component parent) {
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile() : null;
    }

    /** The file to save to (".graph" added if missing, overwrite confirmed), or null if cancelled. */
    public File chooseSave(Component parent, File current) {
        chooser.setSelectedFile(current != null ? current : new File("untitled." + EXTENSION));
        while (true) {
            if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
            File f = withExtension(chooser.getSelectedFile());
            if (!f.exists()) return f;
            int answer = JOptionPane.showConfirmDialog(parent,
                    f.getName() + " already exists. Replace it?", "Confirm Save As",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) return f;
        }
    }

    /** Adds ".graph" unless the name already has an extension. */
    static File withExtension(File f) {
        String name = f.getName();
        return name.contains(".") ? f : new File(f.getParentFile(), name + "." + EXTENSION);
    }

    public static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    public static void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
