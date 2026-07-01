package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class HealTextRenderer {

    private static final List<HealText> TEXTS = new ArrayList<HealText>();

    public static void add(Entity entity, float amount) {
        if (!toast.utilityMobs.Properties.getBoolean(toast.utilityMobs.Properties.GENERAL, "heal_numbers")) {
            return;
        }
        TEXTS.add(new HealText(entity, amount));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<HealText> iter = TEXTS.iterator();
        while (iter.hasNext()) {
            HealText text = iter.next();
            text.age++;
            if (text.age > HealText.MAX_AGE || text.entity.isDead) {
                iter.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || TEXTS.isEmpty()) {
            return;
        }
        float partial = event.getPartialTicks();
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        for (HealText text : TEXTS) {
            Entity e = text.entity;
            float life = ((float)text.age + partial) / (float)HealText.MAX_AGE;
            double x = e.prevPosX + (e.posX - e.prevPosX) * partial - camX;
            double y = e.prevPosY + (e.posY - e.prevPosY) * partial - camY + e.height + 0.35D + life * 0.5D;
            double z = e.prevPosZ + (e.posZ - e.prevPosZ) * partial - camZ;
            int alpha = MathHelper.clamp((int)((1.0F - life) * 255.0F), 0, 255);
            drawText(mc, text.label, x, y, z, alpha);
        }
    }

    private static void drawText(Minecraft mc, String text, double x, double y, double z, int alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        int width = mc.fontRenderer.getStringWidth(text) / 2;
        int color = (alpha << 24) | 0x55FF55;
        mc.fontRenderer.drawString(text, -width, 0, color);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static class HealText {
        private static final int MAX_AGE = 32;
        private final Entity entity;
        private final String label;
        private int age;

        private HealText(Entity entity, float amount) {
            this.entity = entity;
            if (amount == (int)amount) {
                this.label = "+" + (int)amount;
            }
            else {
                this.label = "+" + String.format(java.util.Locale.ROOT, "%.1f", Float.valueOf(amount));
            }
        }
    }
}
