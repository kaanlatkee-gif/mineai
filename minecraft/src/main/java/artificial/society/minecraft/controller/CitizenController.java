package artificial.society.minecraft.controller;

import artificial.society.minecraft.entity.CitizenEntity;
import artificial.society.minecraft.perception.MinecraftWorldAdapter;
import artificial.society.simulation.NpcBrain;
import artificial.society.simulation.NpcIdentity;
import artificial.society.simulation.NpcState;
import artificial.society.simulation.NameGenerator;
import artificial.society.simulation.action.ActionType;
import artificial.society.simulation.action.NpcAction;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.perception.WorldSnapshot;
import artificial.society.simulation.social.SocialInteractionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The controller/adapter layer (requirements #5 and #7):
 *
 * <ol>
 *   <li>reads the Minecraft world through {@link MinecraftWorldAdapter} into a
 *       plain {@link WorldSnapshot};</li>
 *   <li>asks the pure-Java {@link NpcBrain} for an abstract {@link NpcAction}
 *       on a staggered schedule;</li>
 *   <li><b>translates</b> that action into Minecraft effects: navigation
 *       targets, item consumption, hand swings and relationship events.</li>
 * </ol>
 *
 * <p>Scheduling follows the spec's simulation-frequency table (§7.3): needs
 * every 20 ticks, perception every 10 ticks, decisions every 40 ticks (every
 * 10 ticks while a threat is present), movement every tick via Minecraft's
 * navigation. Updates are staggered per entity id so a population never
 * synchronises into a single expensive tick.</p>
 */
public final class CitizenController {

    private static final int NEED_INTERVAL = 20;
    private static final int PERCEPTION_INTERVAL = 10;
    private static final int DECISION_INTERVAL = 40;
    private static final int DECISION_INTERVAL_DANGER = 10;
    private static final int SOCIAL_COOLDOWN = 100;
    private static final double INTERACT_RANGE = 2.5;

    private final NpcBrain brain;
    private WorldSnapshot lastSnapshot;
    private NpcAction activeAction;
    private int lastSocialTick = Integer.MIN_VALUE / 2;

    private CitizenController(NpcBrain brain) {
        this.brain = brain;
    }

    /**
     * Creates the controller for an entity: restores persisted simulation
     * state when present, otherwise derives a fresh identity deterministically
     * from the entity UUID (stable name + personality across restarts).
     */
    public static CitizenController bootstrap(CitizenEntity entity) {
        NpcState state = entity.consumePendingState();
        NpcBrain brain;
        if (state != null) {
            brain = NpcBrain.restore(state);
        } else {
            Random seedSource = new Random(entity.getUuid().getLeastSignificantBits());
            NpcIdentity identity = new NpcIdentity(
                    entity.getUuid().toString(),
                    NameGenerator.generate(seedSource),
                    entity.age);
            Vec3 home = entity.getSimulationHome();
            brain = NpcBrain.create(identity, home, entity.getUuid().hashCode());
            entity.setCustomName(net.minecraft.text.Text.literal(brain.getIdentity().name()));
            entity.setCustomNameVisible(true);
        }
        return new CitizenController(brain);
    }

    public NpcBrain getBrain() {
        return brain;
    }

    /** Called once per server tick from {@link CitizenEntity#tick()}. */
    public void tick(CitizenEntity entity) {
        int age = entity.age;
        int stagger = Math.floorMod(entity.getId(), DECISION_INTERVAL);

        // Perception: short interval so threats are noticed quickly.
        boolean perceptionDue = Math.floorMod(age - stagger, PERCEPTION_INTERVAL) == 0;
        if (perceptionDue || lastSnapshot == null) {
            lastSnapshot = MinecraftWorldAdapter.build(entity, brain.getTick());
        }

        // Needs: every NEED_INTERVAL ticks (spec §7.3), using the latest view.
        if (Math.floorMod(age - stagger, NEED_INTERVAL) == 0 && lastSnapshot != null) {
            brain.advanceTicks(lastSnapshot, NEED_INTERVAL);
        }

        // Decisions: every DECISION_INTERVAL ticks, faster under threat.
        int decisionInterval = (lastSnapshot != null && lastSnapshot.hasThreats())
                ? DECISION_INTERVAL_DANGER : DECISION_INTERVAL;
        boolean decisionDue = Math.floorMod(age - stagger, decisionInterval) == 0;
        if (decisionDue || activeAction == null) {
            if (lastSnapshot == null) {
                lastSnapshot = MinecraftWorldAdapter.build(entity, brain.getTick());
            }
            activeAction = brain.decide(lastSnapshot);
        }

        // Action execution: every tick (Minecraft navigation handles smoothing).
        if (activeAction != null) {
            execute(entity, activeAction);
        }
    }

    // ------------------------------------------------------------ translation

    private void execute(CitizenEntity entity, NpcAction action) {
        switch (action.type()) {
            case EAT -> executeEat(entity, action);
            case AVOID_DANGER -> moveOrFlee(entity, action);
            case SOCIALIZE, TRADE, ASSIST_OTHER -> executeSocial(entity, action);
            case SLEEP, REST -> {
                // Rest in place: energy/comfort regen happens in the brain's
                // need tick while the current action is REST/SLEEP.
                entity.getNavigation().stop();
            }
            case RETURN_HOME -> moveTo(entity, action.target() != null ? action.target() : entity.getSimulationHome(), 1.0);
            case EXPLORE -> wander(entity);
            case WORK -> wander(entity);
            case GATHER_RESOURCES -> {
                if (action.target() != null) {
                    moveTo(entity, action.target(), 1.0);
                } else {
                    wander(entity);
                }
            }
            case IDLE -> {
                if (entity.getRandom().nextInt(200) == 0) {
                    wander(entity);
                }
            }
            default -> entity.getNavigation().stop();
        }
    }

    /** Walk to the food item the brain targeted and consume it on arrival. */
    private void executeEat(CitizenEntity entity, NpcAction action) {
        if (action.target() == null) {
            // No food in sight: head home (future storage) and idle-search.
            moveTo(entity, entity.getSimulationHome(), 0.9);
            return;
        }
        boolean arrived = moveTo(entity, action.target(), 1.1);
        if (!arrived) {
            return;
        }
        ItemEntity food = findFoodEntity(entity, action.target(), 1.75);
        if (food == null) {
            return;
        }
        ItemStack stack = food.getStack();
        Item item = stack.getItem();
        FoodComponent component = item.getFoodComponent();
        int nutrition = component != null ? component.getNutrition() : 4;
        String itemId = String.valueOf(net.minecraft.registry.Registries.ITEM.getId(item));

        stack.decrement(1);
        if (stack.isEmpty()) {
            food.discard();
        }
        entity.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        brain.onAte(itemId, nutrition);
        activeAction = null; // force a fresh decision next tick
    }

    /**
     * Movement part of AVOID_DANGER: the brain supplies a flee point away from
     * the threat; without one we fall back to running home.
     */
    private void moveOrFlee(CitizenEntity entity, NpcAction action) {
        Vec3 target = action.target() != null ? action.target() : entity.getSimulationHome();
        moveTo(entity, target, 1.3);
    }

    /** Walk to the subject and, once close, run a real social interaction. */
    private void executeSocial(CitizenEntity entity, NpcAction action) {
        if (action.subjectId() == null) {
            wander(entity);
            return;
        }
        Entity subject = findAgent(entity, action.subjectId(), 16.0);
        if (subject == null) {
            // Subject gone: the brain will re-decide shortly.
            entity.getNavigation().stop();
            return;
        }

        boolean arrived = moveTo(entity, subject, 1.0);
        int age = entity.age;
        if (arrived && age - lastSocialTick >= SOCIAL_COOLDOWN) {
            lastSocialTick = age;
            entity.swingHand(net.minecraft.util.Hand.MAIN_HAND);

            SocialInteractionType type;
            if (action.type() == ActionType.TRADE) {
                type = SocialInteractionType.TRADING;
            } else if (action.type() == ActionType.ASSIST_OTHER) {
                type = SocialInteractionType.HELPING;
            } else {
                type = age % 2 == 0 ? SocialInteractionType.GREETING : SocialInteractionType.CONVERSATION;
            }
            brain.noteInteraction(
                    action.subjectId(),
                    subject.getName().getString(),
                    type,
                    6.0,
                    true);
        }
    }

    // ------------------------------------------------------------------ utils

    /** Returns true once the entity is within interaction range of the target point. */
    private static boolean moveTo(CitizenEntity entity, Vec3 target, double speed) {
        entity.getNavigation().startMovingTo(target.x(), target.y(), target.z(), speed);
        Vec3d pos = entity.getPos();
        double dx = pos.x - target.x();
        double dy = pos.y - target.y();
        double dz = pos.z - target.z();
        return dx * dx + dy * dy + dz * dz < INTERACT_RANGE * INTERACT_RANGE;
    }

    /** Moves towards another entity; true once within interaction range. */
    private static boolean moveTo(CitizenEntity entity, Entity target, double speed) {
        Vec3d pos = target.getPos();
        entity.getNavigation().startMovingTo(pos.x, pos.y, pos.z, speed);
        Vec3d self = entity.getPos();
        double dx = self.x - pos.x;
        double dy = self.y - pos.y;
        double dz = self.z - pos.z;
        return dx * dx + dy * dy + dz * dz < INTERACT_RANGE * INTERACT_RANGE;
    }

    /** Small random hop so WORK/EXPLORE NPCs keep roaming without a fixed site. */
    private void wander(CitizenEntity entity) {
        if (!entity.getNavigation().isIdle()) {
            return;
        }
        if (entity.getRandom().nextInt(60) != 0) {
            return;
        }
        Random random = entity.getRandom();
        double dx = (random.nextDouble() - 0.5) * 12.0;
        double dz = (random.nextDouble() - 0.5) * 12.0;
        Vec3d pos = entity.getPos();
        entity.getNavigation().startMovingTo(pos.x + dx, pos.y, pos.z + dz, 0.7);
    }

    private static ItemEntity findFoodEntity(CitizenEntity entity, Vec3 around, double radius) {
        double r2 = radius * radius;
        for (Entity other : entity.getWorld().getOtherEntities(entity,
                entity.getBoundingBox().expand(radius))) {
            if (other instanceof ItemEntity itemEntity) {
                Vec3d pos = itemEntity.getPos();
                double dx = pos.x - around.x();
                double dy = pos.y - around.y();
                double dz = pos.z - around.z();
                if (dx * dx + dy * dy + dz * dz <= r2) {
                    FoodComponent food = itemEntity.getStack().getItem().getFoodComponent();
                    if (food != null) {
                        return itemEntity;
                    }
                }
            }
        }
        return null;
    }

    /** Locates a nearby agent by its simulation identity (citizen or player). */
    private static Entity findAgent(CitizenEntity entity, String simulationId, double radius) {
        List<Entity> nearby = entity.getWorld().getOtherEntities(entity,
                entity.getBoundingBox().expand(radius));
        for (Entity other : nearby) {
            if (other instanceof CitizenEntity citizen) {
                if (simulationId.equals(citizen.getSimulationId())) {
                    return citizen;
                }
            } else if (other instanceof PlayerEntity player) {
                if (simulationId.equals(player.getUuid().toString())) {
                    return player;
                }
            }
        }
        return null;
    }

    /** Last world view, exposed for debug command implementations. */
    public Optional<WorldSnapshot> lastSnapshot() {
        return Optional.ofNullable(lastSnapshot);
    }

    /** Last abstract action, exposed for debug command implementations. */
    public Optional<NpcAction> lastAction() {
        return Optional.ofNullable(activeAction);
    }
}
