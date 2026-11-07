package artificial.society.simulation.perception;

/**
 * What kind of thing an agent is, as far as a brain needs to know.
 *
 * <p>The Minecraft adapter classifies raw entities into these values; brains
 * never see Minecraft classes.</p>
 */
public enum AgentKind {
    PLAYER,
    FELLOW_CITIZEN,
    HOSTILE,
    PASSIVE,
    OTHER
}
