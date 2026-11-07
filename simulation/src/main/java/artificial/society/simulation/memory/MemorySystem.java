package artificial.society.simulation.memory;

import artificial.society.simulation.core.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Bounded store of an NPC's memories.
 *
 * <p>Memories are pruned by importance first and by age second, so a store
 * never grows without limit while important events (threats, friendships,
 * achievements) outlive ordinary ones. Pruning keeps at most
 * {@link #capacity()} entries.</p>
 */
public final class MemorySystem {

    private static final long TICKS_PER_DAY = 24_000L;

    private final int capacity;
    private final List<Memory> memories = new ArrayList<>();
    private long nextId = 1L;

    public MemorySystem() {
        this(64);
    }

    public MemorySystem(int capacity) {
        this.capacity = Math.max(8, capacity);
    }

    public synchronized Memory record(
            MemoryCategory category,
            String description,
            List<String> participantIds,
            Vec3 location,
            long day,
            long tick,
            double emotionalImpact,
            MemoryImportance importance
    ) {
        Memory memory = new Memory(
                nextId++, category, description, participantIds, location, day, tick, emotionalImpact, importance);
        memories.add(memory);
        prune(tick);
        return memory;
    }

    public synchronized void add(Memory memory) {
        memories.add(memory);
        if (memory.id() >= nextId) {
            nextId = memory.id() + 1;
        }
        prune(memory.tick());
    }

    /** Replaces the whole store with persisted records (restore path). */
    public synchronized void replaceAll(List<Memory> persisted) {
        memories.clear();
        memories.addAll(persisted);
        for (Memory m : persisted) {
            if (m.id() >= nextId) {
                nextId = m.id() + 1;
            }
        }
    }

    /** Keeps the most valuable memories within the capacity budget. */
    private void prune(long currentTick) {
        if (memories.size() <= capacity) {
            return;
        }
        // Order: most valuable first (highest importance, then newest), so the
        // tail that gets trimmed on overflow is the least valuable memory.
        memories.sort(Comparator
                .comparingInt((Memory m) -> m.importance().level()).reversed()
                .thenComparingLong(m -> currentTick - m.tick()));
        while (memories.size() > capacity) {
            memories.remove(memories.size() - 1);
        }
    }

    public synchronized List<Memory> all() {
        return List.copyOf(memories);
    }

    public synchronized List<Memory> byCategory(MemoryCategory category) {
        List<Memory> out = new ArrayList<>();
        for (Memory m : memories) {
            if (m.category() == category) {
                out.add(m);
            }
        }
        return List.copyOf(out);
    }

    public synchronized List<Memory> involving(String npcId) {
        List<Memory> out = new ArrayList<>();
        for (Memory m : memories) {
            if (m.involves(npcId)) {
                out.add(m);
            }
        }
        return List.copyOf(out);
    }

    /**
     * True when a threatening memory exists near {@code position} that is not
     * older than {@code maxAgeTicks}. Used by the decision engine to avoid
     * places where the NPC previously had a dangerous encounter.
     */
    public synchronized boolean recentThreatNear(Vec3 position, double radius, long currentTick, long maxAgeTicks) {
        for (Memory m : memories) {
            if (m.category() != MemoryCategory.THREAT) {
                continue;
            }
            if (currentTick - m.tick() > maxAgeTicks) {
                continue;
            }
            if (m.within(radius, position)) {
                return true;
            }
        }
        return false;
    }

    /** Strongest positive memory involving another NPC, if any. */
    public synchronized Optional<Memory> strongestPositiveWith(String npcId) {
        return memories.stream()
                .filter(m -> m.involves(npcId) && m.emotionalImpact() > 0.0)
                .max(Comparator.comparingDouble(Memory::emotionalImpact));
    }

    public synchronized int size() {
        return memories.size();
    }

    public synchronized void clear() {
        memories.clear();
    }

    /** Age of a memory in in-game days, for debug output. */
    public static long ageInDays(Memory memory, long currentTick) {
        return Math.max(0, (currentTick - memory.tick()) / TICKS_PER_DAY);
    }
}
