package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import toast.utilityMobs.EntityGolemFishHook;

@SideOnly(Side.CLIENT)
public class RenderGolemFishHook extends Render<EntityGolemFishHook>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/particle/particles.png");

    public RenderGolemFishHook(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGolemFishHook entity) {
        return RenderGolemFishHook.TEXTURE;
    }

    @Override
    public void doRender(EntityGolemFishHook entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        this.bindEntityTexture(entity);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        float minU = (1 * 8 + 0) / 128.0F;
        float maxU = (1 * 8 + 8) / 128.0F;
        float minV = (2 * 8 + 0) / 128.0F;
        float maxV = (2 * 8 + 8) / 128.0F;
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        buffer.pos(-0.5, -0.5, 0.0).tex(minU, maxV).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(0.5, -0.5, 0.0).tex(maxU, maxV).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(0.5, 0.5, 0.0).tex(maxU, minV).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(-0.5, 0.5, 0.0).tex(minU, minV).normal(0.0F, 1.0F, 0.0F).endVertex();
        tessellator.draw();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        if (entity.angler != null) {
            float swing = MathHelper.sin(MathHelper.sqrt(entity.angler.getSwingProgress(partialTicks)) * (float)Math.PI);
            Vec3d vec = new Vec3d(-0.5, 0.03, 0.8);
            vec = vec.rotatePitch(-(entity.angler.prevRotationPitch + (entity.angler.rotationPitch - entity.angler.prevRotationPitch) * partialTicks) * (float)Math.PI / 180.0F);
            vec = vec.rotateYaw(-(entity.angler.prevRotationYaw + (entity.angler.rotationYaw - entity.angler.prevRotationYaw) * partialTicks) * (float)Math.PI / 180.0F);
            vec = vec.rotateYaw(swing * 0.5F);
            vec = vec.rotatePitch(-swing * 0.7F);

            double d5 = entity.angler.prevPosX + (entity.angler.posX - entity.angler.prevPosX) * partialTicks + vec.x;
            double d6 = entity.angler.prevPosY + (entity.angler.posY - entity.angler.prevPosY) * partialTicks + vec.y;
            double d8 = entity.angler.prevPosZ + (entity.angler.posZ - entity.angler.prevPosZ) * partialTicks + vec.z;
            double d9 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double d10 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + 0.25;
            double d11 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            double d12 = (float)(d5 - d9);
            double d13 = (float)(d6 - d10) + 1.7F;
            double d14 = (float)(d8 - d11);

            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            byte b = 16;
            for (int i = 0; i <= b; i++) {
                float f12 = (float)i / (float)b;
                buffer.pos(x + d12 * f12, y + d13 * (f12 * f12 + f12) * 0.5 + 0.25, z + d14 * f12).color(0, 0, 0, 255).endVertex();
            }
            tessellator.draw();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }
    }
}
