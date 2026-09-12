package jDIS6502.views;

import java.io.File;

/**
 * Locates files/folders that ship alongside the running app (e.g. "systems/" for computer-system
 * equates, "profiles/" for *.prf profile files) regardless of how the app was launched.
 *
 * Not a translation of any C++ class -- there is no equivalent in the provided C++ sources
 * (this project's real launcher/installer would have set the working directory or an absolute
 * install path itself). Three candidate locations are checked, covering the ways this project
 * is actually run in practice:
 *   1. next to the running jar/classes (covers running a built jDIS6502.jar directly)
 *   2. the current working directory (covers NetBeans' "Run Project", which starts the app with
 *      the project directory as its working directory)
 *   3. "dist" under the current working directory (covers running the built jar via
 *      "java -jar dist/jDIS6502.jar" from the project directory)
 */
final class AppPaths {

    private AppPaths() {
    }

    /** The directory containing the running jar (or the exploded classes directory, e.g. under an IDE run). */
    static File getApplicationDirectory() {
        try {
            File codeSource = new File(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return codeSource.isFile() ? codeSource.getParentFile() : codeSource;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    private static File[] candidatePaths(String relativePath) {
        return new File[] {
                new File(getApplicationDirectory(), relativePath),
                new File(new File(System.getProperty("user.dir")), relativePath),
                new File(new File(System.getProperty("user.dir"), "dist"), relativePath)
        };
    }

    /** Returns the first candidate location for relativePath that exists as a file, or null. */
    static File findExistingFile(String relativePath) {
        File[] candidates = candidatePaths(relativePath);
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].isFile()) {
                return candidates[i];
            }
        }
        return null;
    }

    /** Returns the first candidate location for relativePath that exists as a directory, or null. */
    static File findExistingDirectory(String relativePath) {
        File[] candidates = candidatePaths(relativePath);
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].isDirectory()) {
                return candidates[i];
            }
        }
        return null;
    }

    /** All candidate locations for relativePath, one per line, for use in "not found" messages. */
    static String candidatePathsText(String relativePath) {
        File[] candidates = candidatePaths(relativePath);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < candidates.length; i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append(candidates[i].getAbsolutePath());
        }
        return text.toString();
    }
}
