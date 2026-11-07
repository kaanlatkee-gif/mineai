package artificial.society.simulation.decision;

import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.core.Vec3;

import java.util.List;

/**
 * One candidate action with its utility score and debug breakdown.
 *
 * @param type       action being scored
 * @param score      raw utility in roughly {@code [0, 130]}
 * @param target     suggested destination, may be {@code null}
 * @param subjectId  suggested other NPC, may be {@code null}
 * @param breakdown  one line per contributing factor, for /society debug
 */
public record ActionScore(
        ActionType type,
        double score,
        Vec3 target,
        String subjectId,
        List<String> breakdown
) {
    public ActionScore {
        breakdown = breakdown == null ? List.of() : List.copyOf(breakdown);
    }

    public ActionScore withTarget(Vec3 newTarget) {
        return new ActionScore(type, score, newTarget, subjectId, breakdown);
    }

    public ActionScore withSubject(String newSubject) {
        return new ActionScore(type, score, target, newSubject, breakdown);
    }
}
