package toast.utilityMobs.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.turret.EntityTurretGolem;

@SideOnly(Side.CLIENT)
public class TurretOutlineRenderer {
    private static final Set<Integer> OUTLINED = new HashSet<Integer>();

    public static boolean isOutlined(int entityId) { return OUTLINED.contains(entityId); }
    public static void toggle(int entityId) {
        if (!OUTLINED.remove(entityId)) OUTLINED.add(entityId);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (OUTLINED.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;
        float pt = event.getPartialTicks();
        double vx = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * pt;
        double vy = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * pt;
        double vz = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * pt;

        Iterator<Integer> it = OUTLINED.iterator();
        while (it.hasNext()) {
            int id = it.next();
            Entity e = mc.world.getEntityByID(id);
            if (!(e instanceof EntityTurretGolem) || e.isDead) { it.remove(); continue; }
            EntityTurretGolem turret = (EntityTurretGolem) e;
            double cx = turret.lastTickPosX + (turret.posX - turret.lastTickPosX) * pt;
            double cy = turret.lastTickPosY + (turret.posY - turret.lastTickPosY) * pt + turret.height / 2.0;
            double cz = turret.lastTickPosZ + (turret.posZ - turret.lastTickPosZ) * pt;
            double radius = turret.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            drawSphere(cx - vx, cy - vy, cz - vz, radius);
        }
    }

    private static void drawSphere(double cx, double cy, double cz, double r) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, cz);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();   // draw both hemispheres
        GlStateManager.depthMask(false); // translucent: don't write depth

        // Deep red, 50% opacity, filled surface.
        final float cr = 0.55F, cg = 0.0F, cb = 0.0F, ca = 0.5F;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        int rings = 16;  // latitude bands
        int seg = 32;    // longitude segments
        for (int i = 0; i < rings; i++) {
            double phi0 = Math.PI * i / rings;
            double phi1 = Math.PI * (i + 1) / rings;
            double y0 = r * Math.cos(phi0), rr0 = r * Math.sin(phi0);
            double y1 = r * Math.cos(phi1), rr1 = r * Math.sin(phi1);
            bb.begin(5, DefaultVertexFormats.POSITION_COLOR); // GL_TRIANGLE_STRIP
            for (int j = 0; j <= seg; j++) {
                double th = 2 * Math.PI * j / seg;
                double ct = Math.cos(th), st = Math.sin(th);
                bb.pos(rr0 * ct, y0, rr0 * st).color(cr, cg, cb, ca).endVertex();
                bb.pos(rr1 * ct, y1, rr1 * st).color(cr, cg, cb, ca).endVertex();
            }
            tess.draw();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
