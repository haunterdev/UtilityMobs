package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelChestGolem;

public class RenderChestGolem extends MobRenderer<EntityChestGolem, ModelChestGolem>
{
    public RenderChestGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelChestGolem(ctx.bakeLayer(ClientSetup.CHEST_GOLEM_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityChestGolem entity) {
        return entity.getTexture();
    }
}
