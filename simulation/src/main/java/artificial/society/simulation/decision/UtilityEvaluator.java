package artificial.society.simulation.decision;

import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.goal.GoalType;
import artificial.society.simulation.memory.MemoryCategory;
import artificial.society.simulation.memory.Memory;
import artificial.society.simulation.memory.MemorySystem;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.perception.AgentKind;
import artificial.society.simulation.perception.PerceivedAgent;
import artificial.society.simulation.perception.PerceivedItem;
import artificial.society.simulation.perception.PerceivedThreat;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.personality.TraitType;
import artificial.society.simulation.social.Relationship;
import artificial.society.simulation.social.RelationshipManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Utility AI scorer: converts needs, personality, memories, relationships,
 * goals and the current world snapshot into one utility number per candidate
 * action.
 *
 * <p>Design rules (spec §3.1):</p>
 * <ul>
 *   <li>Needs drive the magnitude of scores but never force an action — a
 *       brave, curious NPC can still choose to explore while hungry.</li>
 *   <li>Personality biases every relevant score by a bounded amount, so two
 *       NPCs with identical needs can pick different actions.</li>
 *   <li>Memories and relationships add situational offsets (avoid remembered
 *       threat sites, prefer trusted friends).</li>
 *   <li>Scores are kept in a comparable range and each contribution is
 *       written to the breakdown list for the debug command.</li>
 * </ul>
 *
 * <p>All maths lives here as plain Java — this class is the heart of the
 * "brain" and runs unmodified in unit tests.</p>
 */
public final class UtilityEvaluator {

    private static final double THREAT_MEMORY_RADIUS = 12.0;
    private static final long THREAT_MEMORY_WINDOW = 12_000L;

    public List<ActionScore> evaluate(DecisionContext ctx) {
        List<ActionScore> scores = new ArrayList<>();
        scores.add(scoreEat(ctx));
        scores.add(scoreSleep(ctx));
        scores.add(scoreRest(ctx));
        scores.add(scoreWork(ctx));
        scores.add(scoreGather(ctx));
        scores.add(scoreSocialize(ctx));
        scores.add(scoreExplore(ctx));
        scores.add(scoreTrade(ctx));
        scores.add(scoreAssist(ctx));
        scores.add(scoreAvoidDanger(ctx));
        scores.add(scoreReturnHome(ctx));
        scores.add(scoreIdle(ctx));
        return scores;
    }

    // ------------------------------------------------------------------ needs

    private ActionScore scoreEat(DecisionContext ctx) {
        Needs needs = ctx.needs();
        WorldSnapshot snap = ctx.snapshot();
        Personality p = ctx.personality();
        List<String> b = new ArrayList<>();

        double hunger = needs.get(NeedType.HUNGER);
        double urgency = 100.0 - hunger;
        double score = urgency;
        b.add(String.format("Hunger urgency: +%.1f", urgency));

        Optional<PerceivedItem> food = snap.nearestFood();
        if (food.isPresent()) {
            double bonus = 25.0 - Math.min(20.0, food.get().distance());
            score += bonus;
            b.add(String.format("Food in sight (%s @ %.0fm): +%.1f", food.get().itemId(), food.get().distance(), bonus));
        } else {
            b.add("No food in sight: +0");
        }
        if (hunger < 25.0) {
            score += 25.0;
            b.add("Starvation threshold: +25");
        }
        double ambitionBias = p.get(TraitType.AMBITION) * 5.0;
        score += ambitionBias;
        b.add(String.format("Ambition: +%.1f", ambitionBias));

        if (ctx.goals().isCurrent(GoalType.FIND_FOOD)) {
            score += 20.0;
            b.add("Goal FIND_FOOD: +20");
        }
        return new ActionScore(ActionType.EAT, score, food.map(PerceivedItem::position).orElse(null), null, b);
    }

    private ActionScore scoreSleep(DecisionContext ctx) {
        Needs needs = ctx.needs();
        WorldSnapshot snap = ctx.snapshot();
        Personality p = ctx.personality();
        List<String> b = new ArrayList<>();

        double energy = needs.get(NeedType.ENERGY);
        double score = (100.0 - energy) * 0.9;
        b.add(String.format("Energy deficit x0.9: +%.1f", score));

        if (snap.isNight() && energy < 75.0) {
            score += 25.0;
            b.add("Night + tired: +25");
        }
        if (energy < 20.0) {
            score += 25.0;
            b.add("Exhaustion threshold: +25");
        }
        double safetyBoost = (1.0 - p.get(TraitType.BRAVERY)) * 10.0;
        score += safetyBoost;
        b.add(String.format("Caution while weary: +%.1f", safetyBoost));

        if (ctx.goals().isCurrent(GoalType.FIND_BED)) {
            score += 15.0;
            b.add("Goal FIND_BED: +15");
        }
        Vec3 target = snap.homePosition();
        return new ActionScore(ActionType.SLEEP, score, target, null, b);
    }

    private ActionScore scoreRest(DecisionContext ctx) {
        Needs needs = ctx.needs();
        WorldSnapshot snap = ctx.snapshot();
        List<String> b = new ArrayList<>();

        double energy = needs.get(NeedType.ENERGY);
        double score = (100.0 - energy) * 0.45;
        b.add(String.format("Energy deficit x0.45: +%.1f", score));
        if (!snap.isNight() && energy < 45.0) {
            score += 15.0;
            b.add("Daytime tiredness: +15");
        }
        if (snap.hasThreats()) {
            score -= 20.0;
            b.add("Danger nearby: -20");
        }
        return new ActionScore(ActionType.REST, score, null, null, b);
    }

    // ------------------------------------------------------------ work & resources

    private ActionScore scoreWork(DecisionContext ctx) {
        Needs needs = ctx.needs();
        Personality p = ctx.personality();
        List<String> b = new ArrayList<>();

        double score = 10.0;
        b.add("Base work drive: +10");

        double ambition = p.get(TraitType.AMBITION) * 30.0;
        score += ambition;
        b.add(String.format("Ambition x30: +%.1f", ambition));

        double patience = p.get(TraitType.PATIENCE) * 8.0;
        score += patience;
        b.add(String.format("Patience x8: +%.1f", patience));

        if (needs.get(NeedType.ENERGY) > 40.0) {
            score += 10.0;
            b.add("Fresh enough to work: +10");
        } else {
            score -= 15.0;
            b.add("Too tired to work: -15");
        }
        if (needs.get(NeedType.HUNGER) < 30.0) {
            score -= 20.0;
            b.add("Too hungry to work: -20");
        }
        return new ActionScore(ActionType.WORK, score, null, null, b);
    }

    private ActionScore scoreGather(DecisionContext ctx) {
        Needs needs = ctx.needs();
        Personality p = ctx.personality();
        WorldSnapshot snap = ctx.snapshot();
        List<String> b = new ArrayList<>();

        double score = 5.0;
        b.add("Base gathering drive: +5");

        double ambition = p.get(TraitType.AMBITION) * 15.0;
        double curiosity = p.get(TraitType.CURIOSITY) * 10.0;
        score += ambition + curiosity;
        b.add(String.format("Ambition x15: +%.1f, Curiosity x10: +%.1f", ambition, curiosity));

        if (ctx.goals().isCurrent(GoalType.GATHER_WOOD) || ctx.goals().isCurrent(GoalType.GATHER_STONE)) {
            score += 35.0;
            b.add("Active gathering goal: +35");
        }
        if (needs.get(NeedType.ENERGY) > 35.0) {
            score += 5.0;
            b.add("Energy available: +5");
        }
        if (snap.hasThreats()) {
            score -= 25.0;
            b.add("Danger nearby: -25");
        }
        return new ActionScore(ActionType.GATHER_RESOURCES, score, null, null, b);
    }

    // ------------------------------------------------------------------ social

    private ActionScore scoreSocialize(DecisionContext ctx) {
        Needs needs = ctx.needs();
        Personality p = ctx.personality();
        WorldSnapshot snap = ctx.snapshot();
        RelationshipManager rels = ctx.relationships();
        List<String> b = new ArrayList<>();

        double social = needs.get(NeedType.SOCIAL);
        double sociability = p.get(TraitType.SOCIABILITY);
        double score = (100.0 - social) * 0.8 * (0.5 + sociability);
        b.add(String.format("Social deficit x0.8 x(0.5+sociability %.2f): +%.1f", sociability, score));

        Optional<PerceivedAgent> nearest = snap.nearestAgent();
        String subject = null;
        if (nearest.isPresent()) {
            PerceivedAgent agent = nearest.get();
            if (agent.kind() == AgentKind.HOSTILE) {
                score -= 30.0;
                b.add("Only hostile nearby: -30");
            } else if (agent.kind() == AgentKind.PLAYER || agent.kind() == AgentKind.FELLOW_CITIZEN) {
                Relationship r = rels.get(agent.id());
                double warmth = r.warmth();
                double bonus = warmth * 0.2;
                score += bonus;
                b.add(String.format("Warmth of %s (%.0f): +%.1f", agent.name(), warmth, bonus));
                if (warmth > 40.0) {
                    subject = agent.id();
                }
                Optional<Memory> positive = ctx.memories().strongestPositiveWith(agent.id());
                if (positive.isPresent()) {
                    double memBonus = positive.get().emotionalImpact() * 8.0;
                    score += memBonus;
                    b.add(String.format("Positive memory of %s: +%.1f", agent.name(), memBonus));
                }
                if (r.fear() > 60.0) {
                    score -= 25.0;
                    b.add(String.format("Fear of %s: -25", agent.name()));
                }
            }
        } else {
            b.add("Nobody nearby: +0");
        }

        double kindness = p.get(TraitType.KINDNESS) * 5.0;
        score += kindness;
        b.add(String.format("Kindness x5: +%.1f", kindness));

        if (ctx.goals().isCurrent(GoalType.SOCIALIZE)) {
            score += 15.0;
            b.add("Goal SOCIALIZE: +15");
        }

        ActionScore result = new ActionScore(ActionType.SOCIALIZE, score, null, subject, b);
        if (subject == null && nearest.isPresent() && nearest.get().kind() != AgentKind.HOSTILE) {
            result = result.withSubject(nearest.get().id());
        }
        return result;
    }

    private ActionScore scoreExplore(DecisionContext ctx) {
        Personality p = ctx.personality();
        WorldSnapshot snap = ctx.snapshot();
        Needs needs = ctx.needs();
        List<String> b = new ArrayList<>();

        double curiosity = p.get(TraitType.CURIOSITY);
        double score = curiosity * 40.0 * (0.4 + (100.0 - needs.get(NeedType.RECREATION)) / 160.0);
        b.add(String.format("Curiosity x boredom: +%.1f", score));

        double bravery = p.get(TraitType.BRAVERY) * 10.0;
        score += bravery;
        b.add(String.format("Bravery x10: +%.1f", bravery));

        if (snap.hasThreats()) {
            score -= 25.0;
            b.add("Danger nearby: -25");
        } else {
            score += 8.0;
            b.add("Environment safe: +8");
        }
        return new ActionScore(ActionType.EXPLORE, score, null, null, b);
    }

    private ActionScore scoreTrade(DecisionContext ctx) {
        Personality p = ctx.personality();
        WorldSnapshot snap = ctx.snapshot();
        List<String> b = new ArrayList<>();

        double score = 5.0;
        b.add("Base trade interest: +5");

        double ambition = p.get(TraitType.AMBITION) * 15.0;
        score += ambition;
        b.add(String.format("Ambition x15: +%.1f", ambition));

        Optional<PerceivedAgent> nearest = snap.nearestAgent();
        String subject = null;
        if (nearest.isPresent() && (nearest.get().kind() == AgentKind.FELLOW_CITIZEN
                || nearest.get().kind() == AgentKind.PLAYER)) {
            score += 12.0;
            b.add("Potential partner nearby: +12");
            subject = nearest.get().id();
        } else {
            b.add("No trade partner: +0");
        }
        return new ActionScore(ActionType.TRADE, score, null, subject, b);
    }

    private ActionScore scoreAssist(DecisionContext ctx) {
        Personality p = ctx.personality();
        WorldSnapshot snap = ctx.snapshot();
        RelationshipManager rels = ctx.relationships();
        List<String> b = new ArrayList<>();

        double kindness = p.get(TraitType.KINDNESS) * 35.0;
        double score = kindness;
        b.add(String.format("Kindness x35: +%.1f", kindness));

        String subject = null;
        Optional<PerceivedAgent> nearest = snap.nearestAgent();
        Optional<PerceivedThreat> threat = snap.nearestThreat();
        if (nearest.isPresent() && threat.isPresent()
                && nearest.get().kind() == AgentKind.FELLOW_CITIZEN
                && nearest.get().distance() < 12.0) {
            score += 30.0;
            b.add("Fellow citizen in danger: +30");
            subject = nearest.get().id();
        } else {
            b.add("Nobody needs help: +0");
        }

        if (subject != null) {
            double warmth = rels.get(subject).warmth();
            double bonus = warmth * 0.1;
            score += bonus;
            b.add(String.format("Friendship bonus: +%.1f", bonus));
        }
        if (snap.hasThreats() && p.get(TraitType.BRAVERY) < 0.5) {
            score -= 20.0;
            b.add("Not brave enough to help in danger: -20");
        }
        return new ActionScore(ActionType.ASSIST_OTHER, score, null, subject, b);
    }

    // --------------------------------------------------------------- survival

    private ActionScore scoreAvoidDanger(DecisionContext ctx) {
        WorldSnapshot snap = ctx.snapshot();
        Personality p = ctx.personality();
        Needs needs = ctx.needs();
        List<String> b = new ArrayList<>();

        double score = 0.0;
        Optional<PerceivedThreat> threat = snap.nearestThreat();
        Vec3 fleeTarget = null;

        if (threat.isPresent()) {
            PerceivedThreat t = threat.get();
            double severity = t.severity();
            double bravery = p.get(TraitType.BRAVERY);
            score = (40.0 + severity * 45.0) * (1.6 - bravery);
            b.add(String.format("Threat %s (severity %.2f) x (1.6-bravery %.2f): +%.1f",
                    t.kind(), severity, bravery, score));
            // Flee: straight away from the threat, 8 blocks out.
            double dx = snap.selfPosition().x() - t.position().x();
            double dz = snap.selfPosition().z() - t.position().z();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0e-3) {
                fleeTarget = new Vec3(
                        snap.selfPosition().x() + dx / len * 8.0,
                        snap.selfPosition().y(),
                        snap.selfPosition().z() + dz / len * 8.0);
            } else {
                fleeTarget = snap.homePosition();
            }
        } else {
            double safety = needs.get(NeedType.SAFETY);
            if (safety < 30.0) {
                score = (30.0 - safety) * 1.5;
                b.add(String.format("Safety below 30 (%.0f): +%.1f", safety, score));
            } else {
                b.add("No perceived danger: +0");
            }
        }

        MemorySystem memories = ctx.memories();
        if (memories.recentThreatNear(snap.selfPosition(), THREAT_MEMORY_RADIUS, ctx.tick(), THREAT_MEMORY_WINDOW)) {
            score += 20.0;
            b.add("Remembered threat at current location: +20");
        } else {
            b.add("No threatening memory here: +0");
        }

        if (ctx.goals().isCurrent(GoalType.RETURN_HOME) && score > 0.0) {
            score += 5.0;
            b.add("Goal RETURN_HOME: +5");
        }
        return new ActionScore(ActionType.AVOID_DANGER, score, fleeTarget, null, b);
    }

    private ActionScore scoreReturnHome(DecisionContext ctx) {
        WorldSnapshot snap = ctx.snapshot();
        Personality p = ctx.personality();
        Needs needs = ctx.needs();
        List<String> b = new ArrayList<>();

        double score = 0.0;
        if (snap.isNight()) {
            double nightBraving = (1.0 - p.get(TraitType.BRAVERY)) * 45.0;
            score += nightBraving;
            b.add(String.format("Night, courage deficit x45: +%.1f", nightBraving));
        } else {
            b.add("Daytime: +0");
        }

        double comfort = needs.get(NeedType.COMFORT);
        if (comfort < 40.0) {
            double bonus = (40.0 - comfort) * 0.8;
            score += bonus;
            b.add(String.format("Comfort deficit: +%.1f", bonus));
        }

        double dist = snap.distanceFromHome();
        if (dist > 24.0) {
            double bonus = Math.min(20.0, (dist - 24.0) * 0.5);
            score += bonus;
            b.add(String.format("Far from home (%.0fm): +%.1f", dist, bonus));
        }

        MemorySystem memories = ctx.memories();
        if (memories.recentThreatNear(snap.selfPosition(), THREAT_MEMORY_RADIUS, ctx.tick(), THREAT_MEMORY_WINDOW)) {
            score += 10.0;
            b.add("Threat memory nearby: +10");
        }
        // Unsettled comfort pushes NPCs toward home even by day.
        if (needs.get(NeedType.COMFORT) < 25.0) {
            score += 15.0;
            b.add("Strongly uncomfortable: +15");
        }
        return new ActionScore(ActionType.RETURN_HOME, score, snap.homePosition(), null, b);
    }

    private ActionScore scoreIdle(DecisionContext ctx) {
        Personality p = ctx.personality();
        List<String> b = new ArrayList<>();
        double score = 8.0 + p.get(TraitType.PATIENCE) * 6.0;
        b.add(String.format("Base patience: +%.1f", score));
        return new ActionScore(ActionType.IDLE, score, null, null, b);
    }
}
