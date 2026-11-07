package artificial.society.simulation.tests;

import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.memory.Memory;
import artificial.society.simulation.memory.MemoryCategory;
import artificial.society.simulation.memory.MemoryImportance;
import artificial.society.simulation.memory.MemorySystem;

import java.util.List;

/** Memories are recorded, bounded, pruned by importance, and searchable. */
public final class MemoryTest implements SimTest {

    @Override
    public String name() {
        return "MemoryTest - recording, capacity pruning, importance, threat recall";
    }

    @Override
    public void run() {
        MemorySystem memories = new MemorySystem(16);

        Vec3 village = new Vec3(10, 64, 10);
        Memory first = memories.record(
                MemoryCategory.THREAT,
                "Attacked by a zombie",
                List.of("npc-0002"),
                village,
                12L,
                1_000L,
                -0.8,
                MemoryImportance.HIGH);
        memories.record(
                MemoryCategory.RESOURCE_DISCOVERY,
                "Found oak trees nearby",
                List.of(),
                new Vec3(30, 64, 12),
                12L,
                1_100L,
                0.5,
                MemoryImportance.LOW);
        memories.record(
                MemoryCategory.SOCIAL_INTERACTION,
                "Conversation with Greta",
                List.of("npc-0002"),
                null,
                12L,
                1_200L,
                0.3,
                MemoryImportance.MEDIUM);

        Assert.equals(3, memories.size(), "all three memories recorded");
        Assert.isTrue(memories.recentThreatNear(village, 5.0, 1_500L, 12_000L),
                "recent threat at village must be recalled");
        Assert.isFalse(memories.recentThreatNear(new Vec3(500, 64, 500), 5.0, 1_500L, 12_000L),
                "no false positives far from the threat");
        Assert.isFalse(memories.recentThreatNear(village, 5.0, 99_000L, 12_000L),
                "old threats age out of the recall window");
        Assert.isTrue(memories.involving("npc-0002").size() == 2,
                "participant lookup finds both memories involving npc-0002");
        Assert.isTrue(memories.strongestPositiveWith("npc-0002").isPresent(),
                "positive memory of npc-0002 is retrievable");
        Assert.equals(first.id(), memories.byCategory(MemoryCategory.THREAT).get(0).id(),
                "category filter works");

        // Capacity: the store never exceeds its budget, and high-importance
        // memories survive longer than low-importance ones.
        MemorySystem bounded = new MemorySystem(8);
        for (int i = 0; i < 40; i++) {
            MemoryImportance importance = (i % 4 == 0) ? MemoryImportance.HIGH : MemoryImportance.LOW;
            bounded.record(MemoryCategory.PERSONAL_ACHIEVEMENT, "event " + i, List.of(), null,
                    0L, 10_000L + i, 0.1, importance);
        }
        Assert.isTrue(bounded.size() <= 8, "store must respect capacity, size=" + bounded.size());
        boolean highSurvived = false;
        for (Memory m : bounded.all()) {
            if (m.importance() == MemoryImportance.HIGH && m.description().startsWith("event 36")) {
                highSurvived = true;
            }
        }
        Assert.isTrue(highSurvived, "the most recent HIGH importance memory must survive pruning");
    }
}
