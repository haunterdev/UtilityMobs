package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelAnvilGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class RenderAnvilGolem extends MobRenderer<EntityUtilityGolem, ModelAnvilGolem>
{
    public RenderAnvilGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelAnvilGolem(ctx.bakeLayer(ClientSetup.ANVIL_GOLEM_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
