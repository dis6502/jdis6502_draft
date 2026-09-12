package dis6502.core;

/**
 * PLACEHOLDER for the ComputerSystem-instantiation part of systems/ComputerSystemFactory.h/.cpp
 * -- the type lookup (GetComputerSystemType / GetComputerSystemTypeInfo) below is a real,
 * complete translation (it's pure data, no OS dependency), but GetComputerSystem() hands back
 * a generic ComputerSystem placeholder instead of the real Atari800/Atari5200/C64/Oric/Unknown
 * subclasses, since those belong to the "systems" translation pass.
 */
public final class ComputerSystemFactory {

    private final ComputerSystem atari800;
    private final ComputerSystem atari5200;
    private final ComputerSystem c64;
    private final ComputerSystem oric;
    private final ComputerSystem unknown;

    public ComputerSystemFactory() {
        atari5200 = new ComputerSystem(getComputerSystemTypeInfo(ComputerSystemType.ATARI5200));
        atari800 = new ComputerSystem(getComputerSystemTypeInfo(ComputerSystemType.ATARI800));
        c64 = new ComputerSystem(getComputerSystemTypeInfo(ComputerSystemType.C64));
        oric = new ComputerSystem(getComputerSystemTypeInfo(ComputerSystemType.ORIC));
        unknown = new ComputerSystem(getComputerSystemTypeInfo(ComputerSystemType.UNKNOWN));
    }

    public ComputerSystemType getComputerSystemType(String id) {
        if ("ATARI5200".equals(id)) {
            return ComputerSystemType.ATARI5200;
        } else if ("ATARI800".equals(id)) {
            return ComputerSystemType.ATARI800;
        } else if ("C64".equals(id)) {
            return ComputerSystemType.C64;
        } else if ("ORIC".equals(id)) {
            return ComputerSystemType.ORIC;
        }
        return ComputerSystemType.UNKNOWN;
    }

    public ComputerSystemType.ComputerSystemTypeInfo getComputerSystemTypeInfo(ComputerSystemType type) {
        switch (type) {
            case UNKNOWN:
                return new ComputerSystemType.ComputerSystemTypeInfo(ComputerSystemType.UNKNOWN, "UNKNOWN", "Unknown", "Unknown");
            case ATARI800:
                return new ComputerSystemType.ComputerSystemTypeInfo(ComputerSystemType.ATARI800, "ATARI800", "Atari 800", "Atari800");
            case ATARI5200:
                return new ComputerSystemType.ComputerSystemTypeInfo(ComputerSystemType.ATARI5200, "ATARI5200", "Atari 5200", "Atari5200");
            case C64:
                return new ComputerSystemType.ComputerSystemTypeInfo(ComputerSystemType.C64, "C64", "C64", "C64");
            case ORIC:
                return new ComputerSystemType.ComputerSystemTypeInfo(ComputerSystemType.ORIC, "ORIC", "Oric", "Oric");
            default:
                throw new RuntimeException("Unknown computer system type");
        }
    }

    public ComputerSystem getComputerSystem(ComputerSystemType type) {
        switch (type) {
            case UNKNOWN:
                return unknown;
            case ATARI800:
                return atari800;
            case ATARI5200:
                return atari5200;
            case C64:
                return c64;
            case ORIC:
                return oric;
            default:
                throw new RuntimeException("Invalid computer system type");
        }
    }
}
