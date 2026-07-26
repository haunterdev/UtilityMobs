package toast.utilityMobs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import toast.utilityMobs.EntityGolemFishHook;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * Renders the golem's fishing bobber: a camera-facing quad plus a line back to the golem's rod tip.
 *
 * <p>1.12.2 drew the bobber by cutting the 8x8 bobber sprite out of the shared particle sheet
 * (particles.png, cell 1,2 of a 128px sheet). That sheet no longer carries the bobber in 1.20.1 -
 * it lives in its own texture - so the whole quad is that texture instead, which is the same art.
 *
 * <p>The line-strip geometry is unchanged apart from 1.20.1's line render type wanting a per-segment
 * normal, so the vertex helper is taken from vanilla's FishingHookRenderer rather than emitting bare
 * POSITION_COLOR vertices.
 *
 * <p>{@link EntityGolemFishHook#angler} is a plain server-side field, so it reads null on the client
 * exactly as it did in 1.12.2. The bobber still draws; only the line is skipped.
 */
public class RenderGolemFishHook extends EntityRenderer<EntityGolemFishHook>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(RenderGolemFishHook.TEXTURE);

    public RenderGolemFishHook(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGolemFishHook entity) {
        return RenderGolemFishHook.TEXTURE;
    }

    @Override
    public void render(EntityGolemFishHook hook, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose quadPose = poseStack.last();
        Matrix4f pose = quadPose.pose();
        Matrix3f normal = quadPose.normal();
        VertexConsumer quad = buffer.getBuffer(RenderGolemFishHook.RENDER_TYPE);
        RenderGolemFishHook.vertex(quad, pose, normal, packedLight, 0.0F, 0, 0, 1);
        RenderGolemFishHook.vertex(quad, pose, normal, packedLight, 1.0F, 0, 1, 1);
        RenderGolemFishHook.vertex(quad, pose, normal, packedLight, 1.0F, 1, 1, 0);
        RenderGolemFishHook.vertex(quad, pose, normal, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        EntityUtilityGolem angler = hook.angler;
        if (angler != null) {
            // The rod tip, as an offset from the golem's feet: pitched and yawed with the golem, then
            // swung along with its arm swing.
            float swing = Mth.sin(Mth.sqrt(angler.getAttackAnim(partialTicks)) * (float)Math.PI);
            Vec3 tip = new Vec3(-0.5, 0.03, 0.8);
            tip = tip.xRot(-Mth.lerp(partialTicks, angler.xRotO, angler.getXRot()) * (float)Math.PI / 180.0F);
            tip = tip.yRot(-Mth.lerp(partialTicks, angler.yRotO, angler.getYRot()) * (float)Math.PI / 180.0F);
            tip = tip.yRot(swing * 0.5F);
            tip = tip.xRot(-swing * 0.7F);

            double anglerX = Mth.lerp(partialTicks, angler.xo, angler.getX()) + tip.x;
            double anglerY = Mth.lerp(partialTicks, angler.yo, angler.getY()) + tip.y;
            double anglerZ = Mth.lerp(partialTicks, angler.zo, angler.getZ()) + tip.z;
            double hookX = Mth.lerp(partialTicks, hook.xo, hook.getX());
            double hookY = Mth.lerp(partialTicks, hook.yo, hook.getY()) + 0.25;
            double hookZ = Mth.lerp(partialTicks, hook.zo, hook.getZ());
            float dX = (float)(anglerX - hookX);
            float dY = (float)(anglerY - hookY) + 1.7F;
            float dZ = (float)(anglerZ - hookZ);

            VertexConsumer line = buffer.getBuffer(RenderType.lineStrip());
            PoseStack.Pose linePose = poseStack.last();
            for (int i = 0; i <= 16; i++) {
                RenderGolemFishHook.stringVertex(dX, dY, dZ, line, linePose,
                    RenderGolemFishHook.fraction(i, 16), RenderGolemFishHook.fraction(i + 1, 16));
            }
        }

        poseStack.popPose();
        super.render(hook, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float fraction(int numerator, int denominator) {
        return (float)numerator / (float)denominator;
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int packedLight, float x, int y, int u, int v) {
        consumer.vertex(pose, x - 0.5F, (float)y - 0.5F, 0.0F)
            .color(255, 255, 255, 255)
            .uv((float)u, (float)v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(packedLight)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private static void stringVertex(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose, float from, float to) {
        float fx = x * from;
        float fy = y * (from * from + from) * 0.5F + 0.25F;
        float fz = z * from;
        float nx = x * to - fx;
        float ny = y * (to * to + to) * 0.5F + 0.25F - fy;
        float nz = z * to - fz;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        consumer.vertex(pose.pose(), fx, fy, fz)
            .color(0, 0, 0, 255)
            .normal(pose.normal(), nx / len, ny / len, nz / len)
            .endVertex();
    }
}
