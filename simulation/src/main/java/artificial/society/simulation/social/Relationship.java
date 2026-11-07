package artificial.society.simulation.social;

/**
 * One directional relationship record: how <b>this</b> NPC currently feels
 * about another NPC.
 *
 * <p>Relationships are never randomly assigned; they start at neutral and
 * develop only through actual interactions and remembered experiences. The
 * record is immutable — {@link RelationshipManager} replaces it after every
 * update, which also makes persistence trivial.</p>
 *
 * <p>All attributes use the {@code [0, 100]} scale from the specification.</p>
 */
public record Relationship(
        String targetId,
        double friendship,
        double trust,
        double respect,
        double fear,
        double rivalry,
        double familiarity,
        long createdTick,
        long lastInteractionTick
) {

    public static Relationship initial(String targetId, long tick) {
        return new Relationship(targetId, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0, tick, tick);
    }

    public Relationship withAttribute(Attr attr, double value) {
        double v = clamp(value);
        if (attr == Attr.FRIENDSHIP) {
            return new Relationship(targetId, v, trust, respect, fear, rivalry, familiarity, createdTick, lastInteractionTick);
        }
        if (attr == Attr.TRUST) {
            return new Relationship(targetId, friendship, v, respect, fear, rivalry, familiarity, createdTick, lastInteractionTick);
        }
        if (attr == Attr.RESPECT) {
            return new Relationship(targetId, friendship, trust, v, fear, rivalry, familiarity, createdTick, lastInteractionTick);
        }
        if (attr == Attr.FEAR) {
            return new Relationship(targetId, friendship, trust, respect, v, rivalry, familiarity, createdTick, lastInteractionTick);
        }
        if (attr == Attr.RIVALRY) {
            return new Relationship(targetId, friendship, trust, respect, fear, v, familiarity, createdTick, lastInteractionTick);
        }
        return new Relationship(targetId, friendship, trust, respect, fear, rivalry, v, createdTick, lastInteractionTick);
    }

    public Relationship withLastInteraction(long tick) {
        return new Relationship(targetId, friendship, trust, respect, fear, rivalry, familiarity, createdTick, tick);
    }

    /** Overall social attachment used by social scoring. */
    public double warmth() {
        return friendship * 0.5 + trust * 0.3 + respect * 0.2;
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, v));
    }

    /** Which of the six relationship attributes is being adjusted. */
    public enum Attr {
        FRIENDSHIP,
        TRUST,
        RESPECT,
        FEAR,
        RIVALRY,
        FAMILIARITY
    }
}
