package artificial.society.simulation.goal;

import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.needs.Needs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holds the NPC's current short-term goal plus a small set of long-term goals,
 * and re-derives the short-term goal from the most urgent need each decision
 * cycle.
 *
 * <p>Goals are reconsidered when circumstances change (spec §3.3): they are
 * pure functions of needs + situation, never scripted schedules.</p>
 */
public final class GoalManager {

    private Goal current;
    private final List<Goal> longTerm = new ArrayList<>();

    public GoalManager() {
        longTerm.add(Goal.of(GoalType.ACCUMULATE_RESOURCES, 30.0, 0L));
        longTerm.add(Goal.of(GoalType.EARN_RESPECT, 25.0, 0L));
    }

    /**
     * Re-derives the active short-term goal from the current needs.
     * Returns {@code true} when the active goal changed.
     */
    public boolean reconsider(Needs needs, boolean isNight, long tick) {
        NeedType urgent = needs.mostUrgent();
        GoalType wanted;
        if (needs.get(NeedType.HUNGER) < 45.0) {
            wanted = GoalType.FIND_FOOD;
        } else if (isNight && needs.get(NeedType.ENERGY) < 75.0) {
            wanted = GoalType.FIND_BED;
        } else if (needs.get(NeedType.SOCIAL) < 45.0) {
            wanted = GoalType.SOCIALIZE;
        } else if (urgent == NeedType.COMFORT && needs.get(NeedType.COMFORT) < 50.0) {
            wanted = GoalType.RETURN_HOME;
        } else if (needs.get(NeedType.ENERGY) < 30.0) {
            wanted = GoalType.RETURN_HOME;
        } else {
            wanted = null;
        }

        if (wanted == null) {
            boolean changed = current != null;
            current = null;
            return changed;
        }
        if (current != null && current.type() == wanted) {
            return false;
        }
        double priority = 100.0 - Math.min(needs.get(needFor(wanted)), 100.0);
        current = new Goal(wanted, null, null, Math.max(20.0, priority), tick);
        return true;
    }

    private static NeedType needFor(GoalType type) {
        if (type == GoalType.FIND_FOOD) {
            return NeedType.HUNGER;
        }
        if (type == GoalType.SOCIALIZE) {
            return NeedType.SOCIAL;
        }
        if (type == GoalType.RETURN_HOME || type == GoalType.FIND_BED) {
            return NeedType.ENERGY;
        }
        return NeedType.RECREATION;
    }

    public Optional<Goal> current() {
        return Optional.ofNullable(current);
    }

    public boolean isCurrent(GoalType type) {
        return current != null && current.type() == type;
    }

    public void completeCurrent() {
        current = null;
    }

    public List<Goal> longTermGoals() {
        return List.copyOf(longTerm);
    }

    public void replaceCurrent(Goal goal) {
        this.current = goal;
    }

    public void replaceLongTerm(List<Goal> goals) {
        longTerm.clear();
        longTerm.addAll(goals);
    }

    /** All goals (active short-term first, then long-term) for persistence. */
    public List<Goal> allPersisted() {
        List<Goal> out = new ArrayList<>();
        if (current != null) {
            out.add(current);
        }
        out.addAll(longTerm);
        return List.copyOf(out);
    }

    /** Restores persisted goals: first short-term entry becomes the active goal. */
    public void restore(List<Goal> persisted) {
        current = null;
        longTerm.clear();
        for (Goal g : persisted) {
            if (g.type().isLongTerm()) {
                longTerm.add(g);
            } else if (current == null) {
                current = g;
            }
        }
    }
}
