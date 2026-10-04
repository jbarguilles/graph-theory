package graphtheory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** The rules for vertex names (CONTEXT.md, Vertex → Name). */
public final class VertexNames {

    /** Human-readable rule, used in dialogs and file errors. */
    public static final String RULE = "1-4 letters, digits or _";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{1,4}");

    private VertexNames() {}

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    /** Why v can't be renamed to newName, or null if it can. */
    public static String checkRename(Vertex v, String newName, List<Vertex> all) {
        if (!isValid(newName)) return "Names are " + RULE + ".";
        for (Vertex other : all) {
            if (other != v && other.name.equals(newName)) {
                return "'" + newName + "' is already used by another vertex.";
            }
        }
        return null;
    }

    /** The lowest unused number, as a name: "0", "1", … */
    public static String nextFree(List<Vertex> all) {
        Set<String> used = new HashSet<String>();
        for (Vertex v : all) used.add(v.name);
        int i = 0;
        while (used.contains("" + i)) i++;
        return "" + i;
    }
}
