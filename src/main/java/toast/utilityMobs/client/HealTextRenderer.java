package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import toast.utilityMobs.Properties;
import toast.utilityMobs._UtilityMobs;

/**
 * The floating green "+N" that pops over a golem when something heals it.
 *
 * <p>1.12.2 drew this with raw GlStateManager calls in RenderWorldLastEvent. 1.20.1 has no immediate
 * mode, so the text goes through Font.drawInBatch on a billboard matrix built from the camera, during
 * RenderLevelStageEvent. The list, the 32 tick lifetime, the fade and the rise are unchanged.
 */
@Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HealTextRenderer {
    private HealTextRenderer() {}

    private static final List<HealText> TEXTS = new ArrayList<HealText>();

    public static void add(Entity entity, float amount) {
        if (!Properties.getBoolean(Properties.GENERAL, "heal_numbers")) {
            return;
        }
        HealTextRenderer.TEXTS.add(new HealText(entity, amount));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<HealText> iter = HealTextRenderer.TEXTS.iterator();
        while (iter.hasNext()) {
            HealText text = iter.next();
            text.age++;
            if (text.age > HealText.MAX_AGE || !text.entity.isAlive()) {
                iter.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || HealTextRenderer.TEXTS.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick();
        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (HealText text : HealTextRenderer.TEXTS) {
            Entity e = text.entity;
            float life = (text.age + partial) / HealText.MAX_AGE;
            double x = Mth.lerp(partial, e.xo, e.getX()) - camX;
            double y = Mth.lerp(partial, e.yo, e.getY()) - camY + e.getBbHeight() + 0.35 + life * 0.5;
            double z = Mth.lerp(partial, e.zo, e.getZ()) - camZ;
            int alpha = Mth.clamp((int)((1.0F - life) * 255.0F), 0, 255);
            HealTextRenderer.drawText(mc, poseStack, buffer, camera, text.label, x, y, z, alpha);
        }
        buffer.endBatch();
    }

    private static void drawText(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, Camera camera, String text, double x, double y, double z, int alpha) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // Face the camera, then flip: the font draws with +y down and the world has +y up.
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Font font = mc.font;
        float width = -font.width(text) / 2.0F;
        int color = (alpha << 24) | 0x55FF55;
        font.drawInBatch(text, width, 0.0F, color, false, poseStack.last().pose(), buffer,
            Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
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
                this.label = "+" + String.format(Locale.ROOT, "%.1f", Float.valueOf(amount));
            }
        }
    }
}
