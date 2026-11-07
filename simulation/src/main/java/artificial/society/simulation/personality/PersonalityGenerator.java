package artificial.society.simulation.personality;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates a unique, seed-stable personality for a new NPC.
 *
 * <p>Values are drawn from a bell-shaped distribution centred on 0.5 so that
 * most NPCs are moderate while still producing clear characters over a
 * population. Seeding from the NPC's identity guarantees the same NPC always
 * regenerates the same personality after a server restart.</p>
 */
public final class PersonalityGenerator {

    private PersonalityGenerator() {
    }

    public static Personality generate(long seed) {
        Random random = new Random(seed);
        Map<TraitType, Double> traits = new EnumMap<>(TraitType.class);
        for (TraitType type : TraitType.values()) {
            double sample = 0.5 + (random.nextGaussian() * 0.18);
            traits.put(type, Math.max(0.0, Math.min(1.0, sample)));
        }
        return Personality.of(traits);
    }
}
