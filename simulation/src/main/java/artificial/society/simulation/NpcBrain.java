package artificial.society.simulation;

import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.decision.DecisionContext;
import artificial.society.simulation.decision.DecisionEngine;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.goal.GoalManager;
import artificial.society.simulation.memory.Memory;
import artificial.society.simulation.memory.MemoryCategory;
import artificial.society.simulation.memory.MemoryImportance;
import artificial.society.simulation.memory.MemorySystem;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.perception.PerceivedThreat;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.PersonalityGenerator;
import artificial.society.simulation.personality.TraitType;
import artificial.society.simulation.social.RelationshipManager;
import artificial.society.simulation.social.SocialInteractionType;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The complete artificial brain of one NPC.
 *
 * <p>This is the top-level facade of the simulation engine: it owns needs,
 * personality, memories, relationships and goals, advances them over time,
 * and answers the single question the Minecraft layer ever asks —
 * <i>"given this {@link WorldSnapshot}, what should I do?"</i> — with an
 * abstract {@link NpcAction}.</p>
 *
 * <p><b>Architectural invariant:</b> this class (and everything it references)
 * must stay free of Minecraft, Fabric, entity, block and world classes. That
 * invariant is what allows the brain to be constructed and tested on a bare
 * JVM — see {@code BrainDecisionTest}.</p>
 */
public final class NpcBrain {

    /** Base per-second decay rates for each need while awake and safe. */
    private static final double HUNGER_DECAY_PER_SECOND = 0.10;
    private static final double ENERGY_DECAY_PER_SECOND = 0.07;
    private static final double SOCIAL_DECAY_PER_SECOND = 0.12;
    private static final double RECREATION_DECAY_PER_SECOND = 0.08;
    private static final double BELONGING_DECAY_PER_SECOND = 0.05;
    private static final double COMFORT_DECAY_PER_SECOND = 0.04;

    private static final long TICKS_PER_SECOND = 20L;

    private final NpcIdentity identity;
    private final Personality personality;
    private final Needs needs;
    private final MemorySystem memories;
    private final RelationshipManager relationships;
    private final GoalManager goals;
    private final DecisionEngine decisions;
    private final Random random;
    private final Vec3 home;

    private String occupation;
    private NpcAction lastAction;
    private long tick;

    private NpcBrain(NpcIdentity identity, Personality personality, Vec3 home, String occupation, long rngSeed) {
        this.identity = identity;
        this.personality = personality;
        this.home = home;
        this.occupation = occupation;
        this.needs = new Needs();
        this.memories = new MemorySystem();
        this.relationships = new RelationshipManager();
        this.goals = new GoalManager();
        this.decisions = new DecisionEngine();
        this.random = new Random(rngSeed);
        this.tick = identity.spawnTick();
    }

    /** Creates a brand-new brain with a personality derived from the seed. */
    public static NpcBrain create(NpcIdentity identity, Vec3 home, long personalitySeed) {
        NpcBrain brain = new NpcBrain(identity, PersonalityGenerator.generate(personalitySeed), home,
                "UNEMPLOYED", identity.id().hashCode());
        brain.memories.record(
                MemoryCategory.IMPORTANT_LOCATION,
                "Registered home at " + home,
                List.of(),
                home,
                0L,
                identity.spawnTick(),
                0.3,
                MemoryImportance.MEDIUM);
        return brain;
    }

    /** Restores a brain from persisted state (server restart path). */
    public static NpcBrain restore(NpcState state) {
        NpcIdentity identity = new NpcIdentity(state.id(), state.name(), state.spawnTick());
        NpcBrain brain = new NpcBrain(identity, Personality.fromArray(state.personality()),
                state.home(), state.occupation(), state.id().hashCode());
        brain.needs.setAll(state.needs());
        brain.memories.replaceAll(state.memories());
        brain.relationships.replaceAll(state.relationships());
        brain.goals.restore(state.goals());
        return brain;
    }

    /** Full persistence snapshot (see {@link NpcState}). */
    public NpcState toState() {
        return NpcState.fromBrain(this);
    }

    // ------------------------------------------------------------------ ticking

    /**
     * Advances needs by {@code elapsedTicks} using the latest world view:
     * base decay (scaled by personality) plus contextual modifiers such as
     * threats lowering safety, night-time lowering safety away from home, and
     * resting restoring energy.
     */
    public void advanceTicks(WorldSnapshot snapshot, long elapsedTicks) {
        tick += elapsedTicks;
        double seconds = elapsedTicks / (double) TICKS_PER_SECOND;
        if (seconds <= 0.0) {
            return;
        }

        double sociability = personality.get(TraitType.SOCIABILITY);
        double ambition = personality.get(TraitType.AMBITION);

        needs.drain(NeedType.HUNGER, HUNGER_DECAY_PER_SECOND * (1.0 + 0.4 * ambition) * seconds);
        needs.drain(NeedType.ENERGY, ENERGY_DECAY_PER_SECOND * seconds);
        needs.drain(NeedType.SOCIAL, SOCIAL_DECAY_PER_SECOND * (0.8 + 0.6 * sociability) * seconds);
        needs.drain(NeedType.RECREATION, RECREATION_DECAY_PER_SECOND * seconds);
        needs.drain(NeedType.BELONGING, BELONGING_DECAY_PER_SECOND * seconds);
        needs.drain(NeedType.COMFORT, COMFORT_DECAY_PER_SECOND * seconds);

        // Contextual safety: perceived threats frighten everyone, brave NPCs less.
        if (snapshot != null) {
            Optional<PerceivedThreat> threat = snapshot.nearestThreat();
            if (threat.isPresent()) {
                double bravery = personality.get(TraitType.BRAVERY);
                double drain = threat.get().severity() * 6.0 * (1.5 - bravery) * seconds;
                needs.drain(NeedType.SAFETY, drain);
            } else {
                needs.satisfy(NeedType.SAFETY, 1.5 * seconds);
            }

            // Night away from home is unsettling for the timid.
            if (snapshot.isNight() && snapshot.distanceFromHome() > 8.0) {
                double bravery = personality.get(TraitType.BRAVERY);
                needs.drain(NeedType.SAFETY, 0.4 * (1.0 - 0.5 * bravery) * seconds);
            }

            // Being at home steadily restores comfort.
            if (snapshot.distanceFromHome() <= 6.0) {
                needs.satisfy(NeedType.COMFORT, 2.5 * seconds);
            }

            // Working is exhausting.
            if (lastAction != null && (lastAction.type() == artificial.society.simulation.action.ActionType.WORK
                    || lastAction.type() == artificial.society.simulation.action.ActionType.GATHER_RESOURCES)) {
                needs.drain(NeedType.ENERGY, 0.15 * seconds);
            }

            // Resting / sleeping restores energy.
            if (lastAction != null && (lastAction.type() == artificial.society.simulation.action.ActionType.REST
                    || lastAction.type() == artificial.society.simulation.action.ActionType.SLEEP)) {
                needs.satisfy(NeedType.ENERGY, 6.0 * seconds);
                needs.satisfy(NeedType.COMFORT, 1.0 * seconds);
            }
        }

        relationships.decay(tick);
    }

    // --------------------------------------------------------------- deciding

    /**
     * The single entry point used by controllers: re-derives goals, scores all
     * candidate actions and returns the chosen abstract action.
     *
     * <p>The returned action contains only simulation data (enum, coordinates,
     * ids, scores). How it maps onto movement, block interactions or chat is
     * entirely the Minecraft adapter's business.</p>
     */
    public NpcAction decide(WorldSnapshot snapshot) {
        goals.reconsider(needs, snapshot != null && snapshot.isNight(), tick);

        DecisionContext ctx = new DecisionContext(
                identity,
                personality,
                needs,
                snapshot,
                relationships,
                memories,
                goals,
                lastAction,
                tick,
                random);

        NpcAction chosen = decisions.decide(ctx);
        lastAction = chosen;
        return chosen;
    }

    // ------------------------------------------------------------- experiences

    /** Records that this NPC ate something. */
    public void onAte(String itemId, int nutrition) {
        needs.satisfy(NeedType.HUNGER, nutrition * 4.0);
        needs.satisfy(NeedType.COMFORT, 2.0);
        memories.record(
                MemoryCategory.POSITIVE_EXPERIENCE,
                "Ate " + itemId,
                List.of(),
                null,
                day(),
                tick,
                0.4,
                MemoryImportance.LOW);
    }

    /**
     * Records a social interaction: updates the directional relationship and
     * writes a memory. Dialogue lines are chosen by the adapter but their
     * consequences happen here, so interactions are never cosmetic.
     */
    public void noteInteraction(
            String otherId,
            String otherName,
            SocialInteractionType type,
            double magnitude,
            boolean initiatedByMe
    ) {
        relationships.applyInteraction(
                otherId,
                type,
                magnitude,
                initiatedByMe,
                personality.get(TraitType.TRUSTFULNESS),
                personality.get(TraitType.KINDNESS),
                personality.get(TraitType.AGGRESSION),
                tick);

        double impact = switchSign(type) * Math.min(1.0, magnitude / 10.0);
        memories.record(
                MemoryCategory.SOCIAL_INTERACTION,
                type.name().toLowerCase() + " with " + otherName,
                List.of(otherId),
                null,
                day(),
                tick,
                impact,
                magnitude >= 8.0 ? MemoryImportance.MEDIUM : MemoryImportance.LOW);
    }

    private static double switchSign(SocialInteractionType type) {
        if (type == SocialInteractionType.DISAGREEMENT || type == SocialInteractionType.COMPETITION) {
            return -0.6;
        }
        if (type == SocialInteractionType.CELEBRATION || type == SocialInteractionType.HELPING
                || type == SocialInteractionType.SHARING) {
            return 0.8;
        }
        return 0.3;
    }

    /** Records an arbitrary significant event as a memory. */
    public void noteEvent(
            MemoryCategory category,
            String description,
            List<String> participants,
            Vec3 location,
            double emotionalImpact,
            MemoryImportance importance
    ) {
        memories.record(category, description, participants, location, day(), tick, emotionalImpact, importance);
    }

    // ------------------------------------------------------------------ access

    public NpcIdentity getIdentity() {
        return identity;
    }

    public Personality getPersonality() {
        return personality;
    }

    public Needs getNeeds() {
        return needs;
    }

    public MemorySystem getMemories() {
        return memories;
    }

    public RelationshipManager getRelationships() {
        return relationships;
    }

    public GoalManager getGoals() {
        return goals;
    }

    public Vec3 getHome() {
        return home;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation == null ? "UNEMPLOYED" : occupation;
    }

    public NpcAction getLastAction() {
        return lastAction;
    }

    public long getTick() {
        return tick;
    }

    /** In-game day derived from the simulation tick (24000 ticks/day). */
    public long day() {
        return Math.max(0, tick / 24_000L);
    }

    /** Multi-line explanation of the last decision (spec §9 debug output). */
    public String explainLastDecision() {
        if (lastAction == null) {
            return "No decision has been made yet.";
        }
        return lastAction.explanation();
    }

    @Override
    public String toString() {
        return "NpcBrain{" + identity.name() + " [" + identity.id() + "], needs=" + needs
                + ", lastAction=" + (lastAction == null ? "none" : lastAction.type()) + "}";
    }
}
