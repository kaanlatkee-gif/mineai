package artificial.society.simulation.perception;

import artificial.society.simulation.core.Vec3;

/**
 * A danger perceived in the current snapshot.
 *
 * @param kind      free-form threat class, e.g. {@code "zombie"}
 * @param position  world position of the threat
 * @param distance  distance from the observing NPC
 * @param severity  0..1 (1 = adjacent and lethal)
 */
public record PerceivedThreat(
        String kind,
        Vec3 position,
        double distance,
        double severity
) {
}
