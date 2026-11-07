package artificial.society.minecraft.client.render;

import artificial.society.minecraft.ArtificialSocietyMod;
import artificial.society.minecraft.entity.CitizenEntity;
import net.minecraft.client.model.BipedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Minimal humanoid renderer for citizens.
 *
 * <p>Uses the standard player biped model with the mod's own 64×64 texture —
 * no custom model code, which keeps this class small and robust. Rendering is
 * client-side only (registered from {@code ArtificialSocietyClient}).</p>
 */
public class CitizenEntityRenderer
        extends LivingEntityRenderer<CitizenEntity, BipedEntityModel<CitizenEntity>> {

    private static final Identifier TEXTURE =
            new Identifier(ArtificialSocietyMod.MOD_ID, "textures/entity/citizen.png");

    public CitizenEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5F);
    }

    @Override
    protected Identifier getTexture(CitizenEntity entity) {
        return TEXTURE;
    }
}
