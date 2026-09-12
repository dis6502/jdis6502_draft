package dis6502.core;

/**
 * Direct Java 6 translation of EquateListChangedListener.h.
 * Implementations are expected to be provided as anonymous inner classes (e.g. by the Swing
 * UI layer), in keeping with the Java 6 event-listener idiom used throughout this port.
 */
public interface EquateListChangedListener {
    void handleEquateListChanged(EquateList equateList, WorkspaceProperty workspaceProperty);
}
