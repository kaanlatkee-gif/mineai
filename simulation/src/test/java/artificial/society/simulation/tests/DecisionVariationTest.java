package artificial.society.simulation.tests;

import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.decision.DecisionContext;
import artificial.society.simulation.decision.DecisionEngine;
import artificial.society.simulation.goal.GoalManager;
import artificial.society.simulation.memory.MemorySystem;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.perception.PerceivedItem;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.PersonalityGenerator;
import artificial.society.simulation.social.RelationshipManager;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Controlled variation (spec §3.1): scores jitter a little between cycles —
 * so NPCs are not robotic - but strong situations still produce the same
 * action every time, and every NPC's stream is seeded and reproducible.
 */
public final class DecisionVariationTest implements SimTest {

    @Override
    public String name() {
        return "DecisionVariationTest - jitter exists but never overturns a clear need";
    }

    @Override
    public void run() {
        // A starving NPC with food in sight must pick EAT across many cycles
        // even though each cycle adds fresh jitter.
        Set<ActionType> choices = new HashSet<>();
        Set<Double> scores = new HashSet<>();
        Personality personality = PersonalityGenerator.generate(7L);
        for (int i = 0; i < 50; i++) {
            NpcAction action = decide(personality, new Random(1_000L + i), hungryWithFood());
            choices.add(action.type());
            scores.add(Math.rint(action.score() * 10.0));
        }
        Assert.equals(Set.of(ActionType.EAT), choices,
                "extreme hunger + visible food must stably select EAT, got " + choices);
        Assert.isTrue(scores.size() > 1,
                "scores should vary between cycles (variation is expected), distinct=" + scores.size());

        // Seeded reproducibility: the same seed replays the same decision.
        NpcAction first = decide(personality, new Random(555L), hungryWithFood());
        NpcAction second = decide(personality, new Random(555L), hungryWithFood());
        Assert.equals(first.type(), second.type(), "same seed -> same action");
        Assert.equals(first.score(), second.score(), 1.0e-9, "same seed -> same score");
    }

    private static WorldSnapshot hungryWithFood() {
        Vec3 pos = new Vec3(0, 64, 0);
        return WorldSnapshot.builder()
                .tick(500L)
                .timeOfDayTicks(500L)
                .selfPosition(pos)
                .homePosition(pos)
                .item(new PerceivedItem("minecraft:bread", new Vec3(4, 64, 2), 4.5, 5, 1))
                .build();
    }

    private static NpcAction decide(Personality personality, Random random, WorldSnapshot snapshot) {
        Needs needs = new Needs();
        needs.set(NeedType.HUNGER, 8.0);
        DecisionContext ctx = new DecisionContext(
                new NpcIdentity("npc-0099", "Variation", 0L),
                personality,
                needs,
                snapshot,
                new RelationshipManager(),
                new MemorySystem(),
                new GoalManager(),
                null,
                500L,
                random);
        return new DecisionEngine().decide(ctx);
    }
}
