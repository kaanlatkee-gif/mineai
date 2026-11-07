package artificial.society.simulation.memory;

import artificial.society.simulation.core.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * One structured, immutable memory record.
 *
 * <p>Example: "Received food from NPC_104 in the village on day 12,
 * positive emotional impact, medium importance."</p>
 *
 * @param id              monotonic id inside one brain's memory store
 * @param category        one of the eight memory categories
 * @param description     short human-readable text (also used by /society debug)
 * @param participantIds  simulation identities of other involved NPCs (may be empty)
 * @param location        where it happened, or {@code null} if not location-bound
 * @param day             in-game day the event occurred on
 * @param tick            simulation tick the event occurred at (for decay maths)
 * @param emotionalImpact -1 (very negative) .. +1 (very positive)
 * @param importance      retention tier
 */
public record Memory(
        long id,
        MemoryCategory category,
        String description,
        List<String> participantIds,
        Vec3 location,
        long day,
        long tick,
        double emotionalImpact,
        MemoryImportance importance
) {

    public Memory {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        participantIds = participantIds == null ? List.of() : List.copyOf(participantIds);
        if (Double.isNaN(emotionalImpact)) {
            emotionalImpact = 0.0;
        }
        emotionalImpact = Math.max(-1.0, Math.min(1.0, emotionalImpact));
        importance = importance == null ? MemoryImportance.MEDIUM : importance;
    }

    public boolean involves(String npcId) {
        return participantIds.contains(npcId);
    }

    public boolean within(double radius, Vec3 of) {
        return location != null && location.distanceTo(of) <= radius;
    }
}
