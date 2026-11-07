package artificial.society.simulation;

/**
 * Stable identity of one simulated citizen.
 *
 * <p>The {@code id} is the simulation-level primary key: it survives server
 * restarts, is independent of any Minecraft entity UUID handling, and is the
 * key used for memories and relationships. The {@code name} is the display
 * name shown to players.</p>
 *
 * @param id        stable identifier, e.g. {@code "npc-0042"}
 * @param name      human readable name, e.g. {@code "Thomas"}
 * @param spawnTick simulation tick the citizen was created at
 */
public record NpcIdentity(String id, String name, long spawnTick) {
}
