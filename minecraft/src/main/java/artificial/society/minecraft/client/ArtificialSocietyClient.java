package artificial.society.minecraft.client;

import artificial.society.minecraft.entity.CitizenRegistry;
import artificial.society.minecraft.client.render.CitizenEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client-only entrypoint: registers the citizen renderer. Kept strictly apart
 * so dedicated servers never load any rendering class (spec §7.3: server-side
 * execution for authoritative simulation, no client rendering requirement for
 * server AI).
 */
public final class ArtificialSocietyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(CitizenRegistry.CITIZEN, CitizenEntityRenderer::new);
    }
}
