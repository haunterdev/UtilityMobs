package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.client.model.ModelColossalGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

@SideOnly(Side.CLIENT)
public class RenderColossalGolem extends RenderLiving<EntityUtilityGolem>
{
    public RenderColossalGolem(RenderManager renderManager) {
        super(renderManager, new ModelColossalGolem(), 1.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUtilityGolem entity) {
        return entity.getTexture();
    }

    @Override
    protected void preRenderCallback(EntityUtilityGolem entity, float partialTick) {
        super.preRenderCallback(entity, partialTick);
        GlStateManager.scale(1.35F, 1.35F, 1.35F);
    }

    @Override
    protected void renderLeash(EntityUtilityGolem entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.renderLeash(entity, x, y - 0.8D, z, entityYaw, partialTicks);
    }

    @Override
    protected void applyRotations(EntityUtilityGolem entity, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        if (entity.limbSwingAmount >= 0.01 && !entity.isBeingRidden()) {
            float f3 = 13.0F;
            float f4 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks) + 6.0F;
            float f5 = (Math.abs(f4 % f3 - f3 * 0.5F) - f3 * 0.25F) / (f3 * 0.25F);
            GlStateManager.rotate(6.5F * f5, 0.0F, 0.0F, 1.0F);
        }
    }
}
