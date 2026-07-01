package toast.utilityMobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/**
    A vanilla {@link GuiButton} shorter than 20px tall samples only the top {@code height} pixels of
    the 20px-tall widget graphic, which clips off the button's bottom border row - every short button
    looks cut off at the bottom. This variant draws the body minus a 3px bottom strip, then re-draws
    that strip from the texture's own bottom 3 pixels, so the frame closes cleanly at any height.

    Shared by the turret, steam golem, and generic inventory GUIs for their short help / toggle buttons.
*/
public class GuiBorderedButton extends GuiButton {
    private static final int BORDER = 3;

    public GuiBorderedButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        net.minecraft.client.gui.FontRenderer fr = mc.fontRenderer;
        mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int state = this.getHoverState(this.hovered);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.blendFunc(770, 771);
        int half = this.width / 2;
        int v = 46 + state * 20;
        int bodyH = Math.max(0, this.height - BORDER);
        // body (top portion, left + right halves to cover odd widths)
        this.drawTexturedModalRect(this.x, this.y, 0, v, half, bodyH);
        this.drawTexturedModalRect(this.x + this.width - half, this.y, 200 - half, v, half, bodyH);
        // bottom border strip, sampled from the bottom BORDER pixels of the 20px widget row
        this.drawTexturedModalRect(this.x, this.y + bodyH, 0, v + 20 - BORDER, half, BORDER);
        this.drawTexturedModalRect(this.x + this.width - half, this.y + bodyH, 200 - half, v + 20 - BORDER, half, BORDER);
        this.mouseDragged(mc, mouseX, mouseY);
        int color = 0xE0E0E0;
        if (!this.enabled) color = 0xA0A0A0;
        else if (this.hovered) color = 0xFFFFA0;
        this.drawCenteredString(fr, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
    }
}
