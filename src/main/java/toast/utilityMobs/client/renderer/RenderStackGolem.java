package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelStackGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class RenderStackGolem extends MobRenderer<EntityUtilityGolem, ModelStackGolem>
{
    public RenderStackGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelStackGolem(ctx.bakeLayer(ClientSetup.STACK_GOLEM_LAYER)), 0.5F);
        // The block in the HEAD equipment slot is drawn on the head bone. 1.12.2 used the generic
        // LayerCustomHead rather than vanilla's pumpkin-only LayerSnowmanHead, so any head block shows;
        // CustomHeadLayer is that same generic layer in 1.20.1.
        this.addLayer(new CustomHeadLayer<>(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
