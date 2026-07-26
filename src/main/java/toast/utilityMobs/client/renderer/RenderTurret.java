package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelTurret;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class RenderTurret extends MobRenderer<EntityUtilityGolem, ModelTurret>
{
    public RenderTurret(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelTurret(ctx.bakeLayer(ClientSetup.TURRET_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
