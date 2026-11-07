package artificial.society.simulation.tests;

import artificial.society.simulation.NpcBrain;
import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.NpcState;
import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.decision.DecisionContext;
import artificial.society.simulation.decision.DecisionEngine;
import artificial.society.simulation.goal.GoalManager;
import artificial.society.simulation.memory.MemorySystem;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.perception.AgentKind;
import artificial.society.simulation.perception.PerceivedAgent;
import artificial.society.simulation.perception.PerceivedItem;
import artificial.society.simulation.perception.PerceivedThreat;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.TraitType;
import artificial.society.simulation.social.RelationshipManager;

import java.util.Random;

/**
 * THE separation demonstration (project requirement #9).
 *
 * <p>Creates an NPC brain, supplies it with simulated environmental
 * information ({@link WorldSnapshot}), evaluates a decision and receives an
 * abstract {@link NpcAction} - <b>without loading Minecraft</b>. The test
 * even asserts that no Minecraft classes are present on the classpath.</p>
 */
public final class BrainDecisionTest implements SimTest {

    @Override
    public String name() {
        return "BrainDecisionTest - decide() from simulated data, no Minecraft";
    }

    @Override
    public void run() throws Exception {
        assertNoMinecraftOnClasspath();

        // 1. Create a brain (identity + personality seed + home).
        NpcIdentity identity = new NpcIdentity("npc-0001", "Thomas", 0L);
        Vec3 home = new Vec3(0.0, 64.0, 0.0);
        NpcBrain brain = NpcBrain.create(identity, home, 42L);

        // 2. Make the situation urgent: hungry, bread six blocks away.
        brain.getNeeds().set(NeedType.HUNGER, 15.0);

        WorldSnapshot snapshot = WorldSnapshot.builder()
                .tick(2_000L)
                .timeOfDayTicks(2_000L)          // daytime
                .lightLevel(15)
                .selfPosition(home)
                .homePosition(home)
                .item(new PerceivedItem("minecraft:bread", new Vec3(6.0, 64.0, 0.0), 6.0, 5, 1))
                .agent(new PerceivedAgent("npc-0002", "Greta", AgentKind.FELLOW_CITIZEN,
                        new Vec3(4.0, 64.0, 3.0), 5.0, 0.0))
                .build();

        // 3. Decide - the brain only ever sees the snapshot above.
        NpcAction action = brain.decide(snapshot);

        // 4. The result is a fully abstract action value.
        Assert.notNull(action, "decide() must return an action");
        Assert.equals(ActionType.EAT, action.type(),
                "Hungry NPC with food in sight must choose EAT");
        Assert.notNull(action.target(), "EAT should carry the food position as target");
        Assert.equals(6.0, action.target().x(), 1.0e-9, "target x = bread x");
        Assert.equals(64.0, action.target().y(), 1.0e-9, "target y = bread y");
        Assert.isTrue(action.score() > 60.0, "EAT should score high when starving, got " + action.score());
        Assert.isTrue(action.explanation().contains("Hunger urgency"),
                "explanation must expose the score breakdown");

        // 5. The same brain flees when a threat appears instead of food.
        brain.getNeeds().set(NeedType.HUNGER, 80.0);
        WorldSnapshot danger = WorldSnapshot.builder()
                .tick(2_100L)
                .timeOfDayTicks(2_100L)
                .selfPosition(home)
                .homePosition(home)
                .threat(new PerceivedThreat("zombie", new Vec3(5.0, 64.0, 0.0), 5.0, 0.9))
                .build();
        NpcAction flee = brain.decide(danger);
        Assert.equals(ActionType.AVOID_DANGER, flee.type(),
                "Adjacent severe threat must dominate the decision");
        Assert.notNull(flee.target(), "AVOID_DANGER should carry a flee destination");
        Assert.isTrue(flee.target().x() < 0.0,
                "flee destination must point away from the zombie at +x, got " + flee.target());

        // 6. Personality changes the numbers behind identical decisions: the
        //    same needs and same world, two extreme sociability traits.
        Personality reserved = Personality.of(java.util.Map.of(TraitType.SOCIABILITY, 0.0));
        Personality sociable = Personality.of(java.util.Map.of(TraitType.SOCIABILITY, 1.0));
        NpcAction actionA = new DecisionEngine().decide(contextWith(reserved, new Random(77L)));
        NpcAction actionB = new DecisionEngine().decide(contextWith(sociable, new Random(77L)));
        Assert.isTrue(Math.abs(actionA.score() - actionB.score()) > 5.0,
                "personalities must shift utility scores (A=" + actionA.score()
                        + ", B=" + actionB.score() + ")");
        Assert.isTrue(actionA.type() != actionB.type()
                        || Math.abs(actionA.score() - actionB.score()) > 5.0,
                "identical needs must still yield different evaluations per personality");

        // 7. Debug output is available for the /society tooling.
        String explanation = brain.explainLastDecision();
        Assert.isTrue(explanation.contains("Decision evaluation"),
                "explainLastDecision() must produce the debug report");

        System.out.println("--- demonstration: abstract decision without Minecraft ---");
        System.out.println(explanation);
        System.out.println("Chosen: " + flee.summary());
        System.out.println("---------------------------------------------------------");
    }

    private static void assertNoMinecraftOnClasspath() {
        boolean minecraftPresent = true;
        try {
            Class.forName("net.minecraft.entity.Entity");
        } catch (ClassNotFoundException expected) {
            minecraftPresent = false;
        }
        Assert.isFalse(minecraftPresent,
                "Minecraft classes must not be loadable while testing simulation code");
    }

    private static DecisionContext contextWith(Personality personality, Random random) {
        NpcIdentity id = new NpcIdentity("npc-0007", "Probe", 0L);
        WorldSnapshot snapshot = WorldSnapshot.builder()
                .tick(1L)
                .timeOfDayTicks(1_000L)
                .selfPosition(new Vec3(0.0, 64.0, 0.0))
                .homePosition(new Vec3(0.0, 64.0, 0.0))
                .build();
        Needs needs = new Needs();
        needs.set(NeedType.SOCIAL, 40.0);
        needs.set(NeedType.HUNGER, 90.0);
        needs.set(NeedType.ENERGY, 90.0);
        return new DecisionContext(
                id,
                personality,
                needs,
                snapshot,
                new RelationshipManager(),
                new MemorySystem(),
                new GoalManager(),
                null,
                1L,
                random);
    }
}
