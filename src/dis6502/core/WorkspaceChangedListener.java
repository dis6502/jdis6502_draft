package dis6502.core;

import java.util.List;

/**
 * Direct Java 6 translation of WorkspaceChangedListener.h.
 * Implementations are expected to be provided as anonymous inner classes (e.g. by the Swing
 * UI layer), in keeping with the Java 6 event-listener idiom used throughout this port.
 */
public interface WorkspaceChangedListener {
    void handleWorkspaceChanged(Workspace workspace, List<WorkspaceProperty> propertyChangeEvents);
}
