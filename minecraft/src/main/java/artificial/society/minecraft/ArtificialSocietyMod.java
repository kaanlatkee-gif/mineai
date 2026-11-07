package artificial.society.minecraft;

import artificial.society.minecraft.entity.CitizenRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint of the Minecraft integration layer.
 *
 * <p>This class (and everything in the {@code artificial.society.minecraft}
 * package) is the <b>only</b> place where Fabric/Minecraft types appear. It
 * registers entities, spawn eggs and spawn rules; the actual NPC intelligence
 * lives in {@code artificial.society.simulation} and is driven through
 * {@link artificial.society.minecraft.controller.CitizenController}.</p>
 */
public final class ArtificialSocietyMod implements ModInitializer {

    public static final String MOD_ID = "artificial_society";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CitizenRegistry.register();
        LOGGER.info("Artificial Society: simulation engine attached, citizen entity registered");
    }
}
