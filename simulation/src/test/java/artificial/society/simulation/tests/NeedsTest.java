package artificial.society.simulation.tests;

import artificial.society.simulation.NpcBrain;
import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.perception.PerceivedThreat;
import artificial.society.simulation.perception.WorldSnapshot;

/** Needs change over time, clamp to [0,100], and react to the environment. */
public final class NeedsTest implements SimTest {

    @Override
    public String name() {
        return "NeedsTest - decay, clamping, environmental effects";
    }

    @Override
    public void run() {
        // Clamping.
        Needs needs = new Needs();
        needs.set(NeedType.HUNGER, 150.0);
        Assert.equals(100.0, needs.get(NeedType.HUNGER), 1.0e-9, "satisfaction clamps at 100");
        needs.set(NeedType.HUNGER, -5.0);
        Assert.equals(0.0, needs.get(NeedType.HUNGER), 1.0e-9, "satisfaction clamps at 0");
        needs.drain(NeedType.HUNGER, 50.0);
        Assert.equals(0.0, needs.get(NeedType.HUNGER), 1.0e-9, "drain cannot go below 0");

        // Hunger decays over time in a peaceful snapshot.
        NpcBrain brain = NpcBrain.create(new NpcIdentity("npc-0010", "Ada", 0L),
                new Vec3(0, 64, 0), 7L);
        brain.getNeeds().set(NeedType.HUNGER, 80.0);
        WorldSnapshot calm = WorldSnapshot.builder()
                .tick(0L)
                .timeOfDayTicks(2_000L)
                .selfPosition(new Vec3(0, 64, 0))
                .homePosition(new Vec3(0, 64, 0))
                .build();
        brain.advanceTicks(calm, 100L); // 5 seconds
        double after = brain.getNeeds().get(NeedType.HUNGER);
        Assert.isTrue(after < 80.0 && after > 70.0,
                "hunger should decay moderately over 5s, got " + after);

        // Threats drain safety; the effect is real, not random.
        brain.getNeeds().set(NeedType.SAFETY, 90.0);
        WorldSnapshot threatened = WorldSnapshot.builder()
                .tick(100L)
                .timeOfDayTicks(2_000L)
                .selfPosition(new Vec3(0, 64, 0))
                .homePosition(new Vec3(0, 64, 0))
                .threat(new PerceivedThreat("zombie", new Vec3(3, 64, 0), 3.0, 0.8))
                .build();
        brain.advanceTicks(threatened, 100L);
        Assert.isTrue(brain.getNeeds().get(NeedType.SAFETY) < 90.0,
                "safety must drop while a threat is perceived");

        // Safe surroundings restore safety, being home restores comfort.
        brain.getNeeds().set(NeedType.SAFETY, 50.0);
        brain.getNeeds().set(NeedType.COMFORT, 50.0);
        brain.advanceTicks(calm, 100L);
        Assert.isTrue(brain.getNeeds().get(NeedType.SAFETY) > 50.0,
                "safety must recover when no threat is perceived");
        Assert.isTrue(brain.getNeeds().get(NeedType.COMFORT) > 50.0,
                "comfort must recover at home");

        // mostUrgent() reports the lowest need.
        Needs mixed = new Needs();
        mixed.set(NeedType.ENERGY, 12.0);
        Assert.equals(NeedType.ENERGY, mixed.mostUrgent(), "mostUrgent() finds the lowest need");
    }
}
