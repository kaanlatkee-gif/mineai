package artificial.society.simulation.perception;

import artificial.society.simulation.core.Vec3;

/**
 * A simplified view of one nearby agent, produced by the perception adapter.
 *
 * @param id         simulation identity (citizens) or stable identifier (players)
 * @param name       display name for dialogue/debug output
 * @param kind       classification used by scoring
 * @param position   world position at snapshot time
 * @param distance   distance from the observing NPC (convenience)
 * @param hostile    0..1 hostility estimate (e.g. attacker charging)
 */
public record PerceivedAgent(
        String id,
        String name,
        AgentKind kind,
        Vec3 position,
        double distance,
        double hostile
) {
}
