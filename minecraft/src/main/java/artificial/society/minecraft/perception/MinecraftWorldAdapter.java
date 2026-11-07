package artificial.society.minecraft.perception;

import artificial.society.minecraft.entity.CitizenEntity;
import artificial.society.simulation.core.Vec3;
import artificial.society.simulation.perception.AgentKind;
import artificial.society.simulation.perception.PerceivedAgent;
import artificial.society.simulation.perception.PerceivedItem;
import artificial.society.simulation.perception.PerceivedThreat;
import artificial.society.simulation.perception.WorldSnapshot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.MonsterEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Requirement #6: <b>Minecraft world information is converted into simple
 * simulation data before being passed to the brain.</b>
 *
 * <p>This adapter scans a bounded box around the observer and produces an
 * immutable {@link WorldSnapshot} containing only plain records — positions,
 * distances, food values, hostility estimates. It is the sole bridge between
 * {@code net.minecraft.*} world state and the simulation's perception types,
 * and it is re-created on a short interval (never every entity-tick of every
 * NPC — see the controller's scheduling).</p>
 */
public final class MinecraftWorldAdapter {

    /** Horizontal perception radius in blocks. */
    public static final double PERCEPTION_RADIUS = 16.0;
    /** Vertical perception band in blocks (±). */
    public static final double PERCEPTION_HEIGHT = 6.0;

    private MinecraftWorldAdapter() {
    }

    public static WorldSnapshot build(CitizenEntity observer, long tick) {
        World world = observer.getWorld();
        Vec3d pos = observer.getPos();

        WorldSnapshot.Builder builder = WorldSnapshot.builder()
                .tick(tick)
                .timeOfDayTicks(world.getTimeOfDay())
                .selfPosition(new Vec3(pos.x, pos.y, pos.z))
                .homePosition(observer.getSimulationHome());

        Box box = new Box(
                pos.x - PERCEPTION_RADIUS, pos.y - PERCEPTION_HEIGHT, pos.z - PERCEPTION_RADIUS,
                pos.x + PERCEPTION_RADIUS, pos.y + PERCEPTION_HEIGHT, pos.z + PERCEPTION_RADIUS);

        List<Entity> nearby = world.getOtherEntities(observer, box);
        for (Entity other : nearby) {
            Vec3d otherPos = other.getPos();
            double dx = otherPos.x - pos.x;
            double dy = otherPos.y - pos.y;
            double dz = otherPos.z - pos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (other instanceof CitizenEntity citizen) {
                builder.agent(new PerceivedAgent(
                        citizen.getSimulationId(),
                        citizen.getName().getString(),
                        AgentKind.FELLOW_CITIZEN,
                        new Vec3(otherPos.x, otherPos.y, otherPos.z),
                        distance,
                        0.0));
            } else if (other instanceof PlayerEntity player) {
                builder.agent(new PerceivedAgent(
                        player.getUuid().toString(),
                        player.getName().getString(),
                        AgentKind.PLAYER,
                        new Vec3(otherPos.x, otherPos.y, otherPos.z),
                        distance,
                        0.0));
            } else if (other instanceof MonsterEntity monster) {
                String kind = String.valueOf(Registries.ENTITY_TYPE.getId(monster.getType()));
                double severity = Math.max(0.0, Math.min(1.0, 1.0 - distance / PERCEPTION_RADIUS));
                builder.agent(new PerceivedAgent(
                        "monster:" + monster.getId(),
                        monster.getName().getString(),
                        AgentKind.HOSTILE,
                        new Vec3(otherPos.x, otherPos.y, otherPos.z),
                        distance,
                        severity));
                builder.threat(new PerceivedThreat(kind, new Vec3(otherPos.x, otherPos.y, otherPos.z),
                        distance, severity));
            } else if (other instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                Item item = stack.getItem();
                FoodComponent food = item.getFoodComponent();
                if (food != null) {
                    String itemId = String.valueOf(Registries.ITEM.getId(item));
                    builder.item(new PerceivedItem(
                            itemId,
                            new Vec3(otherPos.x, otherPos.y, otherPos.z),
                            distance,
                            food.getNutrition(),
                            stack.getCount()));
                }
            }
        }

        return builder.build();
    }
}
