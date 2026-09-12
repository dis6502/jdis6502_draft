package dis6502.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Java 6 translation of Debug.h / Debug.cpp.
 * The original writes to the Windows debugger output (OutputDebugString); this translation
 * writes to standard error instead, since there is no Windows debug-output equivalent in a
 * portable Java 6 Swing application. Behavior (a running counter prefix, label/value join) is
 * otherwise preserved.
 *
 * Addition (not present in the C++ source): a small Listener mechanism so a UI layer (e.g. the
 * "Log" panel of a Swing front-end) can display these messages live, in addition to (not
 * instead of) the original System.err output. This is purely additive -- existing behavior is
 * unchanged if no listener is registered.
 */
public final class Debug {

    private Debug() {
    }

    private static long count = 0;

    /** Not part of the C++ source; lets a UI subscribe to log messages as they're produced. */
    public interface Listener {
        void handleLog(String message);
    }

    private static final List<Listener> listeners = new ArrayList<Listener>();

    public static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void log(String text) {
        String message = count + ": " + text;
        System.err.println(message);
        count++;

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).handleLog(message);
        }
    }

    public static void logValue(String label, String value) {
        log(label + ": " + value);
    }
}
