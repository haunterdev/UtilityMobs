package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelBlockGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class RenderBlockGolem extends MobRenderer<EntityUtilityGolem, ModelBlockGolem>
{
    public RenderBlockGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelBlockGolem(ctx.bakeLayer(ClientSetup.BLOCK_GOLEM_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
