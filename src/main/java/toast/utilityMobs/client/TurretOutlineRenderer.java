package toast.utilityMobs.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.turret.EntityTurretGolem;

/**
 * Draws a translucent red sphere at a turret's follow range, toggled from the turret screen.
 *
 * <p>1.12.2 drew it with immediate-mode triangle strips in RenderWorldLastEvent. 1.20.1 has no
 * immediate mode, so the sphere is emitted as quads into vanilla's debug-quads render type, which is
 * the POSITION_COLOR, no-cull, translucent type this needs. Quads rather than strips because a shared
 * buffer would otherwise join consecutive strips into one and stretch triangles between the rings.
 */
@Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TurretOutlineRenderer {
    private TurretOutlineRenderer() {}

    private static final Set<Integer> OUTLINED = new HashSet<Integer>();

    /// Deep red, 50% opacity, filled surface.
    private static final float CR = 0.55F, CG = 0.0F, CB = 0.0F, CA = 0.5F;
    private static final int RINGS = 16;  // latitude bands
    private static final int SEGMENTS = 32; // longitude segments

    public static boolean isOutlined(int entityId) { return OUTLINED.contains(entityId); }
    public static void toggle(int entityId) {
        if (!OUTLINED.remove(entityId)) OUTLINED.add(entityId);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || OUTLINED.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float pt = event.getPartialTick();
        double vx = event.getCamera().getPosition().x;
        double vy = event.getCamera().getPosition().y;
        double vz = event.getCamera().getPosition().z;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());

        Iterator<Integer> it = OUTLINED.iterator();
        while (it.hasNext()) {
            int id = it.next();
            Entity e = mc.level.getEntity(id);
            if (!(e instanceof EntityTurretGolem turret) || !turret.isAlive()) { it.remove(); continue; }
            double cx = Mth.lerp(pt, turret.xo, turret.getX());
            double cy = Mth.lerp(pt, turret.yo, turret.getY()) + turret.getBbHeight() / 2.0;
            double cz = Mth.lerp(pt, turret.zo, turret.getZ());
            // Not getAttributeValue(FOLLOW_RANGE): that attribute is not syncable, so on the client it is
            // always the base value and the sphere never grew with the sight upgrade.
            double radius = turret.getEffectiveRange();
            TurretOutlineRenderer.drawSphere(poseStack, consumer, cx - vx, cy - vy, cz - vz, radius);
        }
        buffer.endBatch(RenderType.debugQuads());
    }

    private static void drawSphere(PoseStack poseStack, VertexConsumer consumer, double cx, double cy, double cz, double r) {
        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        Matrix4f pose = poseStack.last().pose();
        for (int i = 0; i < TurretOutlineRenderer.RINGS; i++) {
            double phi0 = Math.PI * i / TurretOutlineRenderer.RINGS;
            double phi1 = Math.PI * (i + 1) / TurretOutlineRenderer.RINGS;
            double y0 = r * Math.cos(phi0), rr0 = r * Math.sin(phi0);
            double y1 = r * Math.cos(phi1), rr1 = r * Math.sin(phi1);
            for (int j = 0; j < TurretOutlineRenderer.SEGMENTS; j++) {
                double th0 = 2 * Math.PI * j / TurretOutlineRenderer.SEGMENTS;
                double th1 = 2 * Math.PI * (j + 1) / TurretOutlineRenderer.SEGMENTS;
                double c0 = Math.cos(th0), s0 = Math.sin(th0);
                double c1 = Math.cos(th1), s1 = Math.sin(th1);
                TurretOutlineRenderer.vertex(consumer, pose, rr0 * c0, y0, rr0 * s0);
                TurretOutlineRenderer.vertex(consumer, pose, rr1 * c0, y1, rr1 * s0);
                TurretOutlineRenderer.vertex(consumer, pose, rr1 * c1, y1, rr1 * s1);
                TurretOutlineRenderer.vertex(consumer, pose, rr0 * c1, y0, rr0 * s1);
            }
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, double x, double y, double z) {
        consumer.vertex(pose, (float)x, (float)y, (float)z)
            .color(TurretOutlineRenderer.CR, TurretOutlineRenderer.CG, TurretOutlineRenderer.CB, TurretOutlineRenderer.CA)
            .endVertex();
    }
}
