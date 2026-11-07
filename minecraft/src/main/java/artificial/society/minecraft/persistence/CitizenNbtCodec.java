package artificial.society.minecraft.persistence;

import artificial.society.simulation.NpcState;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.goal.Goal;
import artificial.society.simulation.goal.GoalType;
import artificial.society.simulation.memory.Memory;
import artificial.society.simulation.memory.MemoryCategory;
import artificial.society.simulation.memory.MemoryImportance;
import artificial.society.simulation.needs.NeedType;
import artificial.society.simulation.social.Relationship;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the Minecraft-free {@link NpcState} record and entity NBT.
 *
 * <p>This is the persistence boundary of the architecture: the simulation
 * never sees {@link NbtCompound} structures and the Minecraft layer never
 * interprets simulation internals beyond these named fields. Every field is
 * validated on read — corrupted or future-format data degrades gracefully
 * (spec §7.2: "all persistent data must be validated and recoverable").</p>
 */
public final class CitizenNbtCodec {

    private static final String KEY = "ArtificialSociety";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SPAWN_TICK = "spawnTick";
    private static final String KEY_OCCUPATION = "occupation";
    private static final String KEY_HOME = "home";
    private static final String KEY_NEEDS = "needs";
    private static final String KEY_PERSONALITY = "personality";
    private static final String KEY_ACTION = "currentAction";
    private static final String KEY_MEMORIES = "memories";
    private static final String KEY_RELATIONSHIPS = "relationships";
    private static final String KEY_GOALS = "goals";

    private CitizenNbtCodec() {
    }

    public static void write(NbtCompound entityNbt, NpcState state) {
        NbtCompound c = new NbtCompound();
        c.putString(KEY_ID, state.id());
        c.putString(KEY_NAME, state.name());
        c.putLong(KEY_SPAWN_TICK, state.spawnTick());
        c.putString(KEY_OCCUPATION, state.occupation());

        NbtCompound home = new NbtCompound();
        home.putDouble("x", state.home().x());
        home.putDouble("y", state.home().y());
        home.putDouble("z", state.home().z());
        c.put(KEY_HOME, home);

        double[] needs = state.needs();
        NbtList needsList = new NbtList();
        for (double v : needs) {
            needsList.add(net.minecraft.nbt.NbtDouble.of(v));
        }
        c.put(KEY_NEEDS, needsList);

        double[] personality = state.personality();
        NbtList traitList = new NbtList();
        for (double v : personality) {
            traitList.add(net.minecraft.nbt.NbtDouble.of(v));
        }
        c.put(KEY_PERSONALITY, traitList);

        if (state.currentActionType() != null) {
            c.putString(KEY_ACTION, state.currentActionType());
        }

        NbtList memories = new NbtList();
        for (Memory m : state.memories()) {
            NbtCompound mc = new NbtCompound();
            mc.putLong("id", m.id());
            mc.putString("category", m.category().name());
            mc.putString("description", m.description());
            mc.putString("participants", String.join(",", m.participantIds()));
            if (m.location() != null) {
                mc.putBoolean("hasLocation", true);
                mc.putDouble("locX", m.location().x());
                mc.putDouble("locY", m.location().y());
                mc.putDouble("locZ", m.location().z());
            }
            mc.putLong("day", m.day());
            mc.putLong("tick", m.tick());
            mc.putDouble("impact", m.emotionalImpact());
            mc.putString("importance", m.importance().name());
            memories.add(mc);
        }
        c.put(KEY_MEMORIES, memories);

        NbtList relationships = new NbtList();
        for (Relationship r : state.relationships()) {
            NbtCompound rc = new NbtCompound();
            rc.putString("targetId", r.targetId());
            rc.putDouble("friendship", r.friendship());
            rc.putDouble("trust", r.trust());
            rc.putDouble("respect", r.respect());
            rc.putDouble("fear", r.fear());
            rc.putDouble("rivalry", r.rivalry());
            rc.putDouble("familiarity", r.familiarity());
            rc.putLong("createdTick", r.createdTick());
            rc.putLong("lastInteractionTick", r.lastInteractionTick());
            relationships.add(rc);
        }
        c.put(KEY_RELATIONSHIPS, relationships);

        NbtList goals = new NbtList();
        for (Goal g : state.goals()) {
            NbtCompound gc = new NbtCompound();
            gc.putString("type", g.type().name());
            gc.putDouble("priority", g.priority());
            gc.putLong("createdTick", g.createdTick());
            if (g.target() != null) {
                gc.putBoolean("hasTarget", true);
                gc.putDouble("targetX", g.target().x());
                gc.putDouble("targetY", g.target().y());
                gc.putDouble("targetZ", g.target().z());
            }
            if (g.subjectId() != null) {
                gc.putString("subjectId", g.subjectId());
            }
            goals.add(gc);
        }
        c.put(KEY_GOALS, goals);

        entityNbt.put(KEY, c);
    }

    /**
     * Reads persisted state, or returns {@code null} when nothing usable is
     * stored (fresh entity, incompatible format, or corrupt data).
     */
    public static NpcState read(NbtCompound entityNbt) {
        if (!entityNbt.contains(KEY)) {
            return null;
        }
        NbtCompound c = entityNbt.getCompound(KEY);
        if (!c.contains(KEY_ID) || !c.contains(KEY_NAME)) {
            return null;
        }

        String id = c.getString(KEY_ID);
        String name = c.getString(KEY_NAME);
        if (id.isEmpty() || name.isEmpty()) {
            return null;
        }

        long spawnTick = c.contains(KEY_SPAWN_TICK) ? c.getLong(KEY_SPAWN_TICK) : 0L;
        String occupation = c.contains(KEY_OCCUPATION) ? c.getString(KEY_OCCUPATION) : "UNEMPLOYED";

        Vec3 home = Vec3.ZERO;
        if (c.contains(KEY_HOME)) {
            NbtCompound h = c.getCompound(KEY_HOME);
            home = new Vec3(h.getDouble("x"), h.getDouble("y"), h.getDouble("z"));
        }

        double[] needs = readDoubles(c, KEY_NEEDS, NeedType.values().length);
        double[] personality = readDoubles(c, KEY_PERSONALITY,
                artificial.society.simulation.personality.TraitType.values().length);

        String currentAction = c.contains(KEY_ACTION) ? c.getString(KEY_ACTION) : null;
        if (currentAction != null && currentAction.isEmpty()) {
            currentAction = null;
        }

        List<Memory> memories = new ArrayList<>();
        NbtList memoryList = c.getList(KEY_MEMORIES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < memoryList.size(); i++) {
            NbtCompound mc = memoryList.getCompound(i);
            Memory memory = readMemory(mc);
            if (memory != null) {
                memories.add(memory);
            }
        }

        List<Relationship> relationships = new ArrayList<>();
        NbtList relationshipList = c.getList(KEY_RELATIONSHIPS, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < relationshipList.size(); i++) {
            NbtCompound rc = relationshipList.getCompound(i);
            String targetId = rc.getString("targetId");
            if (targetId.isEmpty()) {
                continue;
            }
            relationships.add(new Relationship(
                    targetId,
                    rc.getDouble("friendship"),
                    rc.getDouble("trust"),
                    rc.getDouble("respect"),
                    rc.getDouble("fear"),
                    rc.getDouble("rivalry"),
                    rc.getDouble("familiarity"),
                    rc.getLong("createdTick"),
                    rc.getLong("lastInteractionTick")));
        }

        List<Goal> goals = new ArrayList<>();
        NbtList goalList = c.getList(KEY_GOALS, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < goalList.size(); i++) {
            NbtCompound gc = goalList.getCompound(i);
            Goal goal = readGoal(gc);
            if (goal != null) {
                goals.add(goal);
            }
        }

        try {
            return new NpcState(id, name, spawnTick, occupation, home, needs, personality,
                    currentAction, memories, relationships, goals);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static double[] readDoubles(NbtCompound c, String key, int expectedLength) {
        double[] out = new double[expectedLength];
        NbtList list = c.getList(key, NbtElement.DOUBLE_TYPE);
        for (int i = 0; i < expectedLength && i < list.size(); i++) {
            out[i] = list.getDouble(i);
        }
        return out;
    }

    private static Memory readMemory(NbtCompound mc) {
        try {
            String categoryName = mc.getString("category");
            String importanceName = mc.getString("importance");
            MemoryCategory category = MemoryCategory.valueOf(categoryName);
            MemoryImportance importance = MemoryImportance.valueOf(importanceName);
            String description = mc.getString("description");
            if (description.isEmpty()) {
                return null;
            }
            List<String> participants = new ArrayList<>();
            String joined = mc.getString("participants");
            if (!joined.isEmpty()) {
                for (String p : joined.split(",")) {
                    if (!p.isEmpty()) {
                        participants.add(p);
                    }
                }
            }
            Vec3 location = null;
            if (mc.getBoolean("hasLocation")) {
                location = new Vec3(mc.getDouble("locX"), mc.getDouble("locY"), mc.getDouble("locZ"));
            }
            return new Memory(
                    mc.getLong("id"),
                    category,
                    description,
                    participants,
                    location,
                    mc.getLong("day"),
                    mc.getLong("tick"),
                    mc.getDouble("impact"),
                    importance);
        } catch (RuntimeException ex) {
            return null; // skip corrupt entry instead of failing the load
        }
    }

    private static Goal readGoal(NbtCompound gc) {
        try {
            GoalType type = GoalType.valueOf(gc.getString("type"));
            Vec3 target = null;
            if (gc.getBoolean("hasTarget")) {
                target = new Vec3(gc.getDouble("targetX"), gc.getDouble("targetY"), gc.getDouble("targetZ"));
            }
            String subject = gc.contains("subjectId") ? gc.getString("subjectId") : null;
            if (subject != null && subject.isEmpty()) {
                subject = null;
            }
            return new Goal(type, target, subject, gc.getDouble("priority"), gc.getLong("createdTick"));
        } catch (RuntimeException ex) {
            return null; // skip corrupt entry
        }
    }
}
