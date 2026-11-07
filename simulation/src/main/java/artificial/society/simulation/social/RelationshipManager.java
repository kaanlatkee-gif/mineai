package artificial.society.simulation.social;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All relationships one NPC holds towards others.
 *
 * <p>Relationships are directional: NPC A's store about B is completely
 * independent of B's store about A. Every update is driven by real
 * interactions ({@link #applyInteraction}) or by the slow passage of time
 * ({@link #decay}), never by random assignment.</p>
 */
public final class RelationshipManager {

    /** Familiarity lost per in-game day without contact. */
    private static final double FAMILIARITY_DECAY_PER_DAY = 1.5;
    private static final long TICKS_PER_DAY = 24_000L;

    private final Map<String, Relationship> byTarget = new LinkedHashMap<>();

    public Relationship getOrCreate(String targetId, long tick) {
        return byTarget.computeIfAbsent(targetId, id -> Relationship.initial(id, tick));
    }

    public Optional<Relationship> find(String targetId) {
        return Optional.ofNullable(byTarget.get(targetId));
    }

    public Relationship get(String targetId) {
        Relationship r = byTarget.get(targetId);
        return r != null ? r : Relationship.initial(targetId, 0L);
    }

    public boolean knows(String targetId) {
        return byTarget.containsKey(targetId);
    }

    /**
     * Applies one social interaction to the relationship towards {@code targetId}.
     *
     * <p>The magnitude scales the effect; {@code initiatedByMe} makes generous
     * acts count slightly stronger (you remember what you chose to do), and the
     * actor's own personality colours the result — a suspicious NPC gains less
     * trust from the same help, an aggressive one stokes rivalry faster.</p>
     */
    public Relationship applyInteraction(
            String targetId,
            SocialInteractionType type,
            double magnitude,
            boolean initiatedByMe,
            double actorTrustfulness,
            double actorKindness,
            double actorAggression,
            long tick
    ) {
        Relationship current = getOrCreate(targetId, tick);
        double m = Math.max(0.0, magnitude) * (initiatedByMe ? 1.1 : 1.0);

        double trustGain = 0.0;
        double friendshipGain = 0.0;
        double respectGain = 0.0;
        double fearGain = 0.0;
        double rivalryGain = 0.0;

        switch (type) {
            case GREETING -> {
                friendshipGain = 2.0 * m;
                respectGain = 0.5 * m;
            }
            case CONVERSATION -> {
                friendshipGain = 3.0 * m;
                respectGain = 1.0 * m;
            }
            case SHARING -> {
                friendshipGain = 6.0 * m;
                trustGain = 5.0 * m * (0.5 + actorTrustfulness);
            }
            case HELPING -> {
                friendshipGain = 7.0 * m;
                trustGain = 8.0 * m * (0.5 + actorTrustfulness);
                respectGain = 4.0 * m;
            }
            case TRADING -> {
                friendshipGain = 2.0 * m;
                trustGain = 3.0 * m;
                respectGain = 2.0 * m;
            }
            case COOPERATION -> {
                friendshipGain = 5.0 * m;
                trustGain = 5.0 * m;
                respectGain = 5.0 * m;
            }
            case DISAGREEMENT -> {
                friendshipGain = -5.0 * m;
                trustGain = -6.0 * m * (1.4 - actorTrustfulness);
                rivalryGain = 4.0 * m * (0.5 + actorAggression);
            }
            case COMPETITION -> {
                rivalryGain = 6.0 * m * (0.5 + actorAggression);
                respectGain = 2.0 * m;
                friendshipGain = -1.0 * m;
            }
            case CELEBRATION -> {
                friendshipGain = 6.0 * m;
                respectGain = 2.0 * m;
            }
            default -> {
                // Unlisted types are no-ops rather than errors.
            }
        }

        // A kind actor forgives faster; a generous one shares more warmly.
        friendshipGain *= 0.8 + 0.4 * actorKindness;

        Relationship updated = current
                .withAttribute(Relationship.Attr.FRIENDSHIP, current.friendship() + friendshipGain)
                .withAttribute(Relationship.Attr.TRUST, current.trust() + trustGain)
                .withAttribute(Relationship.Attr.RESPECT, current.respect() + respectGain)
                .withAttribute(Relationship.Attr.FEAR, current.fear() + fearGain)
                .withAttribute(Relationship.Attr.RIVALRY, current.rivalry() + rivalryGain)
                .withAttribute(Relationship.Attr.FAMILIARITY, Math.min(100.0, current.familiarity() + 8.0 * m))
                .withLastInteraction(tick);
        byTarget.put(targetId, updated);
        return updated;
    }

    /** Slow drift: familiarity fades without contact, fear subsides, rivalry cools. */
    public void decay(long currentTick) {
        List<String> keys = new ArrayList<>(byTarget.keySet());
        for (String key : keys) {
            Relationship r = byTarget.get(key);
            long idleTicks = Math.max(0, currentTick - r.lastInteractionTick());
            double idleDays = idleTicks / (double) TICKS_PER_DAY;
            if (idleDays <= 0.0) {
                continue;
            }
            double familiarityLoss = FAMILIARITY_DECAY_PER_DAY * idleDays;
            double fearLoss = 1.5 * idleDays;
            double rivalryLoss = 1.0 * idleDays;
            Relationship updated = r
                    .withAttribute(Relationship.Attr.FAMILIARITY, r.familiarity() - familiarityLoss)
                    .withAttribute(Relationship.Attr.FEAR, r.fear() - fearLoss)
                    .withAttribute(Relationship.Attr.RIVALRY, r.rivalry() - rivalryLoss);
            // Long isolation also erodes friendship slightly (spec §4.3).
            updated = updated.withAttribute(
                    Relationship.Attr.FRIENDSHIP, r.friendship() - 0.8 * idleDays);
            byTarget.put(key, updated);
        }
    }

    public List<Relationship> all() {
        return List.copyOf(byTarget.values());
    }

    public void replaceAll(List<Relationship> persisted) {
        byTarget.clear();
        for (Relationship r : persisted) {
            byTarget.put(r.targetId(), r);
        }
    }

    public int size() {
        return byTarget.size();
    }
}
