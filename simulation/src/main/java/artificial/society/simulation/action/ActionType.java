package artificial.society.simulation.action;

/**
 * The abstract action vocabulary a brain can choose.
 *
 * <p>These values carry no Minecraft meaning whatsoever: a brain returns an
 * {@link NpcAction} and the controller/adapter layer in the Minecraft module
 * decides how (or whether) to carry it out against real entities and blocks.</p>
 */
public enum ActionType {
    /** Acquire and consume food; {@code target} may point at a perceived item. */
    EAT,
    /** Go to sleep (ideally at a bed / home). */
    SLEEP,
    /** Perform occupation work. */
    WORK,
    /** Collect wood, stone or other raw materials; {@code target} may point at a site. */
    GATHER_RESOURCES,
    /** Talk to another NPC; {@code subjectId} identifies who. */
    SOCIALIZE,
    /** Visit unfamiliar places. */
    EXPLORE,
    /** Exchange resources with another NPC; {@code subjectId} identifies who. */
    TRADE,
    /** Take a break without sleeping. */
    REST,
    /** Help another NPC; {@code subjectId} identifies who. */
    ASSIST_OTHER,
    /** Flee from danger; {@code target} points away from the threat. */
    AVOID_DANGER,
    /** Head back to the registered home position. */
    RETURN_HOME,
    /** Do nothing for now (low-utility fallback). */
    IDLE
}
