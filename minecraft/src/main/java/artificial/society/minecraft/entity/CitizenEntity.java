package artificial.society.minecraft.entity;

import artificial.society.minecraft.controller.CitizenController;
import artificial.society.minecraft.persistence.CitizenNbtCodec;
import artificial.society.simulation.NpcBrain;
import artificial.society.simulation.NpcState;
import artificial.society.simulation.core.Vec3;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * The Minecraft-side citizen mob.
 *
 * <p><b>Design rule:</b> this entity holds <i>no intelligence of its own</i>.
 * On the first server tick it bootstraps a {@link CitizenController}, which in
 * turn owns a pure-Java {@link NpcBrain}. Every tick the controller converts
 * the real world into a {@code WorldSnapshot}, asks the brain for an abstract
 * action and translates that action back into navigation/inventory calls.
 * All persistent simulation data is written through
 * {@link CitizenNbtCodec} as a plain {@link NpcState} — never as entity
 * references.</p>
 */
public class CitizenEntity extends PathAwareEntity {

    /** Simulation state restored from NBT, consumed by the controller on first tick. */
    private NpcState pendingState;
    private CitizenController controller;

    public CitizenEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    /**
     * Attribute set required by the controller's navigation calls.
     * Pattern verified against Fabric 1.21.1 usage
     * ({@code LivingEntity.createLivingAttributes() + GENERIC_*}).
     */
    public static DefaultAttributeContainer.Builder createCitizenAttributes() {
        return net.minecraft.entity.LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if (controller == null) {
                controller = CitizenController.bootstrap(this);
            }
            controller.tick(this);
        }
    }

    // ------------------------------------------------------- controller bridge

    /** Restores persisted simulation state (null for a freshly spawned citizen). */
    public NpcState consumePendingState() {
        NpcState state = pendingState;
        pendingState = null;
        return state;
    }

    /** The live brain, or {@code null} before the first server tick. */
    public NpcBrain getBrain() {
        return controller == null ? null : controller.getBrain();
    }

    /**
     * Simulation identity used by memories/relationships. Falls back to the
     * entity UUID so perception adapters never see {@code null}.
     */
    public String getSimulationId() {
        NpcBrain brain = getBrain();
        if (brain != null) {
            return brain.getIdentity().id();
        }
        return getUuid().toString();
    }

    /** Registered home position (spawn point until Phase 6 settlement logic). */
    public Vec3 getSimulationHome() {
        NpcBrain brain = getBrain();
        if (brain != null) {
            return brain.getHome();
        }
        return toSimulationVec(getPos());
    }

    static Vec3 toSimulationVec(net.minecraft.util.math.Vec3d pos) {
        return new Vec3(pos.x, pos.y, pos.z);
    }

    // -------------------------------------------------------------- persistence

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NpcState state = null;
        if (controller != null && controller.getBrain() != null) {
            state = controller.getBrain().toState();
        } else if (pendingState != null) {
            state = pendingState;
        }
        if (state != null) {
            CitizenNbtCodec.write(nbt, state);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        pendingState = CitizenNbtCodec.read(nbt);
        if (pendingState != null) {
            setCustomName(Text.literal(pendingState.name()));
        }
    }
}
