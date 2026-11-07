package artificial.society.simulation.goal;

/**
 * Short- and long-term objectives an NPC can hold.
 *
 * <p>Short-term goals steer the current decision cycle (find food, return
 * home, …); long-term goals are reconsidered when circumstances change and
 * provide persistent bonuses to related actions.</p>
 */
public enum GoalType {
    /** Short term: hunger is rising. */
    FIND_FOOD(false),
    /** Short term: get back to the registered home position. */
    RETURN_HOME(false),
    /** Short term: find a place to sleep. */
    FIND_BED(false),
    /** Short term: talk to someone. */
    SOCIALIZE(false),
    /** Short term: bring wood home. */
    GATHER_WOOD(false),
    /** Short term: bring stone home. */
    GATHER_STONE(false),
    /** Long term: obtain a better home. */
    IMPROVE_HOME(true),
    /** Long term: become a skilled profession holder. */
    BECOME_SKILLED(true),
    /** Long term: establish a family. */
    START_FAMILY(true),
    /** Long term: accumulate resources. */
    ACCUMULATE_RESOURCES(true),
    /** Long term: become a respected community member. */
    EARN_RESPECT(true);

    private final boolean longTerm;

    GoalType(boolean longTerm) {
        this.longTerm = longTerm;
    }

    public boolean isLongTerm() {
        return longTerm;
    }
}
