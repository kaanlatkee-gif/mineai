package artificial.society.simulation.decision;

import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.personality.TraitType;

import java.util.List;

/**
 * Selects the action with the highest utility from the candidates produced by
 * {@link UtilityEvaluator}, with <b>controlled variation</b> (spec §3.1):
 *
 * <ul>
 *   <li>every score receives a small seeded random jitter whose magnitude
 *       depends on the NPC's patience (impatient NPCs flip more often);</li>
 *   <li>the currently running action gets a small continuity bonus so NPCs
 *       do not thrash between equivalent options every cycle;</li>
 *   <li>ties are broken deterministically by the seeded RNG, so a given NPC
 *       with a given state behaves reproducibly.</li>
 * </ul>
 */
public final class DecisionEngine {

    /** Maximum absolute jitter added to a score, before personality scaling. */
    static final double BASE_VARIATION = 8.0;

    /** Bonus for continuing the action that is already running. */
    static final double CONTINUITY_BONUS = 3.0;

    private final UtilityEvaluator evaluator = new UtilityEvaluator();

    public NpcAction decide(DecisionContext ctx) {
        List<ActionScore> candidates = evaluator.evaluate(ctx);
        if (candidates.isEmpty()) {
            return NpcAction.of(ActionType.IDLE, 0.0, "no candidates");
        }

        double variation = BASE_VARIATION * (1.25 - 0.5 * ctx.personality().get(TraitType.PATIENCE));

        ActionScore best = null;
        double bestAdjusted = Double.NEGATIVE_INFINITY;
        for (ActionScore candidate : candidates) {
            double jitter = (ctx.random().nextDouble() * 2.0 - 1.0) * variation;
            double adjusted = candidate.score() + jitter;
            if (ctx.currentAction() != null && ctx.currentAction().type() == candidate.type()) {
                adjusted += CONTINUITY_BONUS;
            }
            if (adjusted > bestAdjusted) {
                bestAdjusted = adjusted;
                best = candidate;
            }
        }

        String explanation = buildExplanation(ctx, best, bestAdjusted, variation);
        Vec3 target = best.target();
        String subject = best.subjectId();
        return new NpcAction(best.type(), target, subject, bestAdjusted, explanation);
    }

    private String buildExplanation(DecisionContext ctx, ActionScore best, double adjusted, double variation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Decision evaluation for ").append(ctx.identity().name()).append('\n');
        for (String line : best.breakdown()) {
            sb.append(String.format("  %-46s%n", line));
        }
        sb.append(String.format("  %-46s%n", String.format("Raw utility: %.1f", best.score())));
        sb.append(String.format("  %-46s%n", String.format(
                "Variation (seeded, +-%.1f): included", variation)));
        sb.append(String.format("  %-46s%n", String.format(
                "Continuity bonus: %s",
                ctx.currentAction() != null && ctx.currentAction().type() == best.type()
                        ? String.format("+%.1f", CONTINUITY_BONUS) : "+0")));
        sb.append(String.format("  %-46s%n", String.format("Final score: %.1f", adjusted)));
        sb.append("Selected action: ").append(best.type().name());
        return sb.toString();
    }

    /** Identity helper kept package-visible for tests. */
    static String identityLabel(NpcIdentity identity) {
        return identity.name() + " [" + identity.id() + "]";
    }
}
