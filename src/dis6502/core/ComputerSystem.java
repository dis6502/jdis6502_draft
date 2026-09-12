package dis6502.core;

/**
 * PLACEHOLDER -- this is NOT a full translation of systems/ComputerSystem.h/.cpp. The real
 * class (plus its Atari800/Atari5200/C64/Oric/Unknown subclasses) owns resource-file lookup,
 * disk-image handling, and Windows GDI font sizing (HFONT) for each supported machine -- all
 * of that is out of scope for this pass and belongs to a dedicated "systems" translation pass.
 *
 * This placeholder exposes only what Workspace needs (the type and its ComputerSystemTypeInfo)
 * so Workspace compiles and its own type-switching logic can be exercised/tested now. Font
 * sizing is not meaningfully portable ahead of the Swing UI layer, so getFont() below is a
 * stand-in that documents the deferral rather than reproducing any real per-system font.
 *
 * Replace this class (with real Atari800/Atari5200/C64/Oric/Unknown subclasses) when the
 * "systems" layer is ported.
 */
public class ComputerSystem {

    private final ComputerSystemType.ComputerSystemTypeInfo typeInfo;

    public ComputerSystem(ComputerSystemType.ComputerSystemTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public ComputerSystemType getType() {
        return typeInfo.getType();
    }

    public ComputerSystemType.ComputerSystemTypeInfo getTypeInfo() {
        return typeInfo;
    }

    /**
     * Placeholder for ComputerSystem::GetReturnCharacter(): each real subclass sets this to
     * its machine's line-ending byte (e.g. 0x9B/ATASCII EOL for the Atari systems). Since the
     * real subclasses aren't ported yet, this defaults to 0x9B, the most common case for this
     * tool's primary target (Atari 8-bit).
     */
    public int getReturnCharacter() {
        return 0x9B;
    }

    /**
     * Placeholder for ComputerSystem::GetFont(bool doubleHeight): the real per-system,
     * per-DPI HFONT lookup is deferred until the Swing UI layer exists to make use of it.
     */
    public Object getFont(boolean doubleHeight) {
        throw new UnsupportedOperationException(
                "ComputerSystem::GetFont is deferred to the Swing UI / systems translation pass");
    }
}
