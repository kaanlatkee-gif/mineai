package artificial.society.simulation;

import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.goal.Goal;
import artificial.society.simulation.memory.Memory;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.TraitType;
import artificial.society.simulation.social.Relationship;

import java.util.List;
import java.util.Objects;

/**
 * Complete, Minecraft-free snapshot of one NPC's persistent data (spec §7.2).
 *
 * <p>This record is the interchange format between a live {@link NpcBrain}
 * and any storage backend. The Minecraft module serialises it to entity NBT;
 * tests round-trip it in memory. Nothing in this type references entity
 * classes, so NPC data outlives any particular Minecraft entity instance.</p>
 *
 * @param id               simulation identity id
 * @param name             display name
 * @param spawnTick        creation tick
 * @param occupation       occupation label (Phase 4+), may be {@code "UNEMPLOYED"}
 * @param home             registered home position
 * @param needs            values in {@link NeedType#values()} order
 * @param personality      values in {@link TraitType#values()} order
 * @param currentActionType name of the last chosen action, or {@code null}
 * @param memories         memory records (already importance-pruned)
 * @param relationships    directional relationships
 * @param goals            persisted goals (short-term active + long-term)
 */
public record NpcState(
        String id,
        String name,
        long spawnTick,
        String occupation,
        Vec3 home,
        double[] needs,
        double[] personality,
        String currentActionType,
        List<Memory> memories,
        List<Relationship> relationships,
        List<Goal> goals
) {

    public NpcState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        occupation = occupation == null ? "UNEMPLOYED" : occupation;
        needs = needs == null ? new Needs().toArray() : needs.clone();
        personality = personality == null ? defaultPersonality() : personality.clone();
        memories = memories == null ? List.of() : List.copyOf(memories);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        goals = goals == null ? List.of() : List.copyOf(goals);
    }

    private static double[] defaultPersonality() {
        double[] out = new double[TraitType.values().length];
        java.util.Arrays.fill(out, 0.5);
        return out;
    }

    /** Captures the full state of a live brain. */
    public static NpcState fromBrain(NpcBrain brain) {
        NpcIdentity identity = brain.getIdentity();
        return new NpcState(
                identity.id(),
                identity.name(),
                identity.spawnTick(),
                brain.getOccupation(),
                brain.getHome(),
                brain.getNeeds().toArray(),
                brain.getPersonality().toArray(),
                brain.getLastAction() == null ? null : brain.getLastAction().type().name(),
                brain.getMemories().all(),
                brain.getRelationships().all(),
                brain.getGoals().allPersisted()
        );
    }
}
