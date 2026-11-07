package artificial.society.simulation.action;

import artificial.society.simulation.core.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * An abstract decision result: <i>what</i> the NPC wants to do, optionally
 * <i>where</i> and <i>with whom</i>, plus the utility score and a human
 * readable explanation of how the score was built (used by the debug tools).
 *
 * <p>This object is deliberately dumb. It contains no world references, no
 * entities and no Minecraft types — the controller translates it into real
 * game actions.</p>
 *
 * @param type        chosen action
 * @param target      optional destination / target position
 * @param subjectId   optional simulation identity of another NPC involved
 * @param score       final (post-variation) utility score
 * @param explanation multi-line score breakdown for debugging
 */
public record NpcAction(
        ActionType type,
        Vec3 target,
        String subjectId,
        double score,
        String explanation
) {

    public NpcAction {
        Objects.requireNonNull(type, "type");
        explanation = explanation == null ? "" : explanation;
    }

    public static NpcAction of(ActionType type, double score, String explanation) {
        return new NpcAction(type, null, null, score, explanation);
    }

    public NpcAction withTarget(Vec3 target) {
        return new NpcAction(type, target, subjectId, score, explanation);
    }

    public NpcAction withSubject(String subjectId) {
        return new NpcAction(type, target, subjectId, score, explanation);
    }

    /** Single-line summary suitable for logs. */
    public String summary() {
        StringBuilder sb = new StringBuilder(type.name());
        sb.append(" (score ").append(String.format("%.1f", score)).append(')');
        if (target != null) {
            sb.append(" -> ").append(target.toString());
        }
        if (subjectId != null) {
            sb.append(" subject=").append(subjectId);
        }
        return sb.toString();
    }

    /** All explanation lines (first line is the action summary). */
    public List<String> explanationLines() {
        if (explanation.isBlank()) {
            return List.of(summary());
        }
        return List.of(explanation.split("\n"));
    }
}
