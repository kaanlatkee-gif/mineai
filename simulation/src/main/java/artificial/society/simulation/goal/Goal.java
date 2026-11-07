package artificial.society.simulation.goal;

import artificial.society.simulation.core.Vec3;

/**
 * One concrete goal instance (immutable).
 *
 * @param type       goal kind
 * @param target     optional destination
 * @param subjectId  optional NPC the goal concerns
 * @param priority   0..100 urgency of this goal right now
 * @param createdTick when the goal was adopted
 */
public record Goal(
        GoalType type,
        Vec3 target,
        String subjectId,
        double priority,
        long createdTick
) {
    public Goal {
        if (priority < 0.0) {
            priority = 0.0;
        }
        if (priority > 100.0) {
            priority = 100.0;
        }
    }

    public static Goal of(GoalType type, double priority, long createdTick) {
        return new Goal(type, null, null, priority, createdTick);
    }
}
