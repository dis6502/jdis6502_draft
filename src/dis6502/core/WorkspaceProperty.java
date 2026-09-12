package dis6502.core;

/**
 * Direct Java 6 translation of the WorkspaceProperty enum in WorkspaceTypes.h.
 * Properties in alphabetical order, same as the C++ source.
 */
public enum WorkspaceProperty {
    COMPUTER_SYSTEM_TYPE,
    FONT,
    FILE_PATH,
    PROFILE,
    SEGMENTS,
    SELECTED_SEGMENT,
    SELECTED_MEMORY_RANGE,
    SYSTEM_EQUATES,
    USER_EQUATES;

    /** Direct translation of the free function "constexpr const wchar_t* ToString(WorkspaceProperty value)". */
    public String toDisplayString() {
        switch (this) {
            case COMPUTER_SYSTEM_TYPE:
                return "COMPUTER_SYSTEM_TYPE";
            case FILE_PATH:
                return "FILE_PATH";
            case FONT:
                return "FONT";
            case PROFILE:
                return "PROFILE";
            case SEGMENTS:
                return "SEGMENTS";
            case SELECTED_SEGMENT:
                return "SELECTED_SEGMENT";
            case SELECTED_MEMORY_RANGE:
                return "SELECTED_MEMORY_RANGE";
            case SYSTEM_EQUATES:
                return "SYSTEM_EQUATES";
            case USER_EQUATES:
                return "USER_EQUATES";
            default:
                return "";
        }
    }
}
