package toast.utilityMobs.client;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import toast.utilityMobs.GuideBook;

public class GuiGenericInventory extends GuiContainer {

    public static final ResourceLocation TEXTURE_DISPENSER = new ResourceLocation("textures/gui/container/dispenser.png");

    private ResourceLocation texture;
    private IInventory inventory;

    public GuiGenericInventory(Container container, IInventory inventory, ResourceLocation texture) {
        super(container);
        this.texture = texture;
        this.inventory = inventory;
    }

    @Override
    public void initGui() {
        super.initGui();
        // Help button (top-right) - opens the Patchouli guide book. Hidden when general.show_help_button is false.
        if (toast.utilityMobs.Properties.getBoolean(toast.utilityMobs.Properties.GENERAL, "show_help_button")) {
            this.buttonList.add(new GuiBorderedButton(90, this.guiLeft + this.xSize - 20, this.guiTop + 4, 16, 16, "?"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 90) {
            vazkii.patchouli.api.PatchouliAPI.instance.openBookGUI(GuideBook.BOOK_RL);
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = this.inventory.hasCustomName() ? this.inventory.getName() : I18n.format(this.inventory.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.texture);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }
}
