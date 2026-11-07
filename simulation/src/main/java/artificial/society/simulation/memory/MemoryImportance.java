package artificial.society.simulation.memory;

/**
 * Importance tier of a memory. Higher importance means longer retention: the
 * memory manager prunes low-importance entries first (see
 * {@link MemorySystem}).
 */
public enum MemoryImportance {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int level;

    MemoryImportance(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
