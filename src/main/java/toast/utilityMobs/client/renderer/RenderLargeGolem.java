package toast.utilityMobs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelLargeGolem;
import toast.utilityMobs.golem.EntityLargeGolem;

public class RenderLargeGolem extends MobRenderer<EntityLargeGolem, ModelLargeGolem>
{
    public RenderLargeGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelLargeGolem(ctx.bakeLayer(ClientSetup.LARGE_GOLEM_LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityLargeGolem entity) {
        return entity.getTexture();
    }

    /// The lumbering side-to-side sway while walking, as in 1.12.2.
    @Override
    protected void setupRotations(EntityLargeGolem entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        if (entity.walkAnimation.speed() >= 0.01F) {
            float period = 13.0F;
            float pos = entity.walkAnimation.position() - entity.walkAnimation.speed() * (1.0F - partialTicks) + 6.0F;
            float sway = (Math.abs(pos % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }
}
