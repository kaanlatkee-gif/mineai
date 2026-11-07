package artificial.society.simulation.tests;

import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.PersonalityGenerator;
import artificial.society.simulation.personality.TraitType;

import java.util.Arrays;

/** Personalities are unique, seed-stable and bounded to [0, 1]. */
public final class PersonalityTest implements SimTest {

    @Override
    public String name() {
        return "PersonalityTest - uniqueness, seed stability, bounds";
    }

    @Override
    public void run() {
        Personality a = PersonalityGenerator.generate(12345L);
        Personality b = PersonalityGenerator.generate(12345L);
        Personality c = PersonalityGenerator.generate(99999L);

        // Seed stability: the same seed always regenerates the same personality,
        // which is what keeps personalities identical across server restarts.
        Assert.isTrue(Arrays.equals(a.toArray(), b.toArray()),
                "same seed must generate identical personality");

        // Uniqueness: different seeds generate different personalities.
        Assert.isFalse(Arrays.equals(a.toArray(), c.toArray()),
                "different seeds should generate different personalities");

        // Bounds + coverage of all ten traits.
        for (TraitType trait : TraitType.values()) {
            double v = a.get(trait);
            Assert.isTrue(v >= 0.0 && v <= 1.0, trait + " out of range: " + v);
        }
        Assert.equals(TraitType.values().length, a.toArray().length,
                "one value per trait");

        // Persisted round-trip through raw arrays.
        Personality restored = Personality.fromArray(a.toArray());
        Assert.isTrue(Arrays.equals(a.toArray(), restored.toArray()),
                "fromArray(toArray()) must be lossless");
    }
}
