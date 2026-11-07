package artificial.society.simulation.needs;

/**
 * The continuously changing drives of an NPC. Every need is a value in
 * {@code [0, 100]} where 100 = fully satisfied and 0 = urgent.
 *
 * <p>Needs never force an action directly; they only shift the utility scores
 * evaluated by the decision engine (see {@code UtilityEvaluator}).</p>
 */
public enum NeedType {
    /** Desire to acquire and consume food. */
    HUNGER,
    /** Desire to rest and sleep. */
    ENERGY,
    /** Desire to avoid dangerous situations. */
    SAFETY,
    /** Desire for social interaction. */
    SOCIAL,
    /** Desire for entertainment and leisure. */
    RECREATION,
    /** Desire to maintain social connections. */
    BELONGING,
    /** Desire for suitable living conditions. */
    COMFORT;

    /** Initial satisfaction given to a freshly spawned NPC. */
    public double initialSatisfaction() {
        if (this == HUNGER) {
            return 78.0;
        }
        if (this == ENERGY) {
            return 85.0;
        }
        return 90.0;
    }
}
