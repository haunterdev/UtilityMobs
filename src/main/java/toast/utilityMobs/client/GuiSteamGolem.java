package toast.utilityMobs.client;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import toast.utilityMobs.GuideBook;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.golem.ContainerSteamGolem;
import toast.utilityMobs.golem.EntitySteamGolem;

public class GuiSteamGolem extends GuiContainer {

    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.MODID, "textures/gui/guisteamgolem.png");
    private EntitySteamGolem steamGolem;

    public GuiSteamGolem(InventoryPlayer player, EntitySteamGolem golem) {
        super(new ContainerSteamGolem(player, golem));
        this.steamGolem = golem;
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
        String s = this.steamGolem.hasCustomName() ? this.steamGolem.getName() : I18n.format(this.steamGolem.getName());
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(GuiSteamGolem.TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        if (this.steamGolem.getBurningState()) {
            int fireSize = this.steamGolem.getBurnTimeRemainingScaled(13);
            this.drawTexturedModalRect(x + 80, y + 27 + 12 - fireSize, 176, 12 - fireSize, 14, fireSize + 1);
        }
    }
}
