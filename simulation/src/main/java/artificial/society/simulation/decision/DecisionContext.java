package artificial.society.simulation.decision;

import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.memory.MemorySystem;
import artificial.society.simulation.needs.Needs;
import artificial.society.simulation.goal.GoalManager;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.personality.Personality;
import artificial.society.simulation.social.RelationshipManager;

import java.util.Random;

/**
 * Everything a decision cycle needs, bundled. Built fresh by
 * {@code NpcBrain#decide} for each evaluation.
 *
 * @param identity      who is deciding
 * @param personality   stable traits
 * @param needs         current need values
 * @param snapshot      current world view (the only environmental input)
 * @param relationships directional relationships
 * @param memories      memory store
 * @param goals         active goals
 * @param currentAction action currently being executed (continuity bonus)
 * @param tick          current simulation tick
 * @param random        seeded source of controlled variation
 */
public record DecisionContext(
        NpcIdentity identity,
        Personality personality,
        Needs needs,
        WorldSnapshot snapshot,
        RelationshipManager relationships,
        MemorySystem memories,
        GoalManager goals,
        NpcAction currentAction,
        long tick,
        Random random
) {
}
