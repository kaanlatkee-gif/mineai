package artificial.society.simulation.perception;

import artificial.society.simulation.core.Vec3;

/**
 * A simplified view of a dropped, edible item in the world.
 *
 * @param itemId     registry id such as {@code "minecraft:bread"}
 * @param position   world position of the item entity
 * @param distance   distance from the observing NPC
 * @param nutrition  food points the item restores
 * @param count      stack size
 */
public record PerceivedItem(
        String itemId,
        Vec3 position,
        double distance,
        int nutrition,
        int count
) {
}
