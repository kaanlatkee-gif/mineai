package artificial.society.simulation.tests;

import artificial.society.simulation.NpcBrain;
import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.NpcState;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.memory.MemoryCategory;
import artificial.society.simulation.memory.MemoryImportance;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.social.SocialInteractionType;

import java.util.List;

/**
 * Persistent NPC data round-trips through {@link NpcState} - a plain record
 * with no Minecraft classes - which is what lets NPC data be stored by any
 * backend (NBT today, SQLite later) without touching entity classes.
 */
public final class PersistenceRoundTripTest implements SimTest {

    @Override
    public String name() {
        return "PersistenceRoundTripTest - NpcState survives a simulated restart";
    }

    @Override
    public void run() {
        Vec3 home = new Vec3(12.0, 64.0, -8.0);
        NpcBrain original = NpcBrain.create(new NpcIdentity("npc-0042", "Mira", 600L), home, 4242L);
        original.setOccupation("FARMER");
        original.getNeeds().set(NeedType.HUNGER, 33.0);
        original.getNeeds().set(NeedType.SOCIAL, 44.0);
        original.getRelationships().applyInteraction("npc-0043", SocialInteractionType.SHARING,
                9.0, true, 0.7, 0.6, 0.1, 700L);
        original.noteEvent(MemoryCategory.THREAT, "Skeleton at the ravine", List.of(),
                new Vec3(20, 64, -4), -0.7, MemoryImportance.HIGH);
        original.noteEvent(MemoryCategory.SOCIAL_INTERACTION, "Shared bread with Greta",
                List.of("npc-0043"), home, 0.6, MemoryImportance.MEDIUM);

        // Serialise → "server restart" → deserialise.
        NpcState state = original.toState();
        Assert.equals("npc-0042", state.id(), "id persists");
        Assert.equals("Mira", state.name(), "name persists");
        Assert.equals("FARMER", state.occupation(), "occupation persists");
        Assert.equals(home, state.home(), "home persists");

        NpcBrain restored = NpcBrain.restore(state);

        Assert.arrayEquals(original.getNeeds().toArray(), restored.getNeeds().toArray(), 1.0e-9,
                "all seven needs must survive");
        Assert.arrayEquals(original.getPersonality().toArray(), restored.getPersonality().toArray(), 1.0e-9,
                "personality must survive");
        Assert.equals(original.getIdentity().id(), restored.getIdentity().id(), "identity id survives");
        Assert.equals(original.getIdentity().name(), restored.getIdentity().name(), "name survives");
        Assert.equals(original.getOccupation(), restored.getOccupation(), "occupation survives");
        Assert.equals(original.getHome(), restored.getHome(), "home survives");

        // 3 = home registration (from create) + threat + social memories.
        Assert.equals(3, restored.getMemories().size(), "all memories survive");
        Assert.equals(1, restored.getRelationships().size(), "relationship survives");
        Assert.equals("npc-0043", restored.getRelationships().all().get(0).targetId(),
                "relationship target survives");
        double trust = restored.getRelationships().get("npc-0043").trust();
        Assert.isTrue(trust > original.getRelationships().get("npc-0043").trust() - 1.0e-9
                        && trust <= 100.0,
                "trust value survives within bounds, got " + trust);

        // The restored brain keeps deciding correctly (simulation continuity).
        restored.getNeeds().set(NeedType.HUNGER, 10.0);
        var snapshot = artificial.society.simulation.perception.WorldSnapshot.builder()
                .tick(800L)
                .timeOfDayTicks(800L)
                .selfPosition(home)
                .homePosition(home)
                .item(new artificial.society.simulation.perception.PerceivedItem(
                        "minecraft:apple", new Vec3(3, 64, 0), 3.0, 4, 1))
                .build();
        Assert.equals(artificial.society.simulation.action.ActionType.EAT,
                restored.decide(snapshot).type(),
                "restored brain still reacts to hunger and food");
    }
}
