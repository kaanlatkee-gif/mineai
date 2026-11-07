package artificial.society.minecraft.entity;

import artificial.society.minecraft.ArtificialSocietyMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers the citizen entity type, its attributes, its spawn egg and its
 * natural spawning rules (spec §8 Phase 1: "register custom NPC entity,
 * implement spawning").
 *
 * <p>Registration happens once from {@code ArtificialSocietyMod#onInitialize}
 * via {@link #register()}.</p>
 */
public final class CitizenRegistry {

    /** The citizen mob: peaceful, path-walking, driven by a simulation brain. */
    public static final EntityType<CitizenEntity> CITIZEN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ArtificialSocietyMod.MOD_ID, "citizen"),
            EntityType.Builder.create(CitizenEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.95F)
                    .build());

    /** Spawn egg so players can place citizens manually (creative + /give). */
    public static final Item CITIZEN_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            new Identifier(ArtificialSocietyMod.MOD_ID, "citizen_spawn_egg"),
            new SpawnEggItem(CITIZEN, 0x9A7B4F, 0x4F3B2A, new Item.Settings()));

    private CitizenRegistry() {
    }

    /** Called once during mod initialization (after class-load registration). */
    public static void register() {
        FabricDefaultAttributeRegistry.register(CITIZEN, CitizenEntity.createCitizenAttributes());

        // Natural spawning across the overworld at modest density.
        // (Biome narrowing / village-only spawning is a Phase 6 refinement.)
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE,
                CITIZEN,
                8,
                1,
                3);

        ArtificialSocietyMod.LOGGER.debug("Citizen entity, egg and spawn rules registered");
    }
}
