package toast.utilityMobs.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * The default utility golem renderer: a humanoid body wearing whatever armor the golem is equipped
 * with. 1.12.2 had to attach the armor layer by hand because RenderBiped only added the held-item
 * layer; the same is true of {@link HumanoidMobRenderer} in 1.20.1, so the layer is added here.
 */
public class RenderGolem extends HumanoidMobRenderer<EntityUtilityGolem, HumanoidModel<EntityUtilityGolem>>
{
    public RenderGolem(EntityRendererProvider.Context ctx, HumanoidModel<EntityUtilityGolem> model) {
        super(ctx, model, 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
            ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
