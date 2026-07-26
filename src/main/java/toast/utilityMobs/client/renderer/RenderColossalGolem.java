package toast.utilityMobs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import toast.utilityMobs.client.ClientSetup;
import toast.utilityMobs.client.model.ModelColossalGolem;
import toast.utilityMobs.colossal.EntityColossalGolem;

public class RenderColossalGolem extends MobRenderer<EntityColossalGolem, ModelColossalGolem>
{
    public RenderColossalGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelColossalGolem(ctx.bakeLayer(ClientSetup.COLOSSAL_GOLEM_LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityColossalGolem entity) {
        return entity.getTexture();
    }

    /// 1.12.2 preRenderCallback: the colossus is rendered at 1.35x its model size.
    @Override
    protected void scale(EntityColossalGolem entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.35F, 1.35F, 1.35F);
    }

    /// The lumbering side-to-side sway while walking, suppressed while it is being ridden (as in 1.12.2).
    @Override
    protected void setupRotations(EntityColossalGolem entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        if (entity.walkAnimation.speed() >= 0.01F && !entity.isVehicle()) {
            float period = 13.0F;
            float pos = entity.walkAnimation.position() - entity.walkAnimation.speed() * (1.0F - partialTicks) + 6.0F;
            float sway = (Math.abs(pos % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
        // 1.12.2 also lowered the leash anchor 0.8 blocks (renderLeash override). Cosmetic; the vanilla
        // leash anchor is used here.
    }
}
