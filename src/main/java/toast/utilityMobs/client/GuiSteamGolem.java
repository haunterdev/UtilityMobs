package toast.utilityMobs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.golem.ContainerSteamGolem;

/**
 * The steam golem's fuel screen, with the furnace flame drawn from its synced burn timer.
 *
 */
public class GuiSteamGolem extends AbstractContainerScreen<ContainerSteamGolem> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.MODID, "textures/gui/guisteamgolem.png");

    public GuiSteamGolem(ContainerSteamGolem menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Help button (top-right) - opens the Patchouli guide book, same gate as the other golem GUIs.
        if (toast.utilityMobs.GuideBook.showHelpButton()) {
            this.addRenderableWidget(HelpButton.build(this.leftPos + this.imageWidth - 20, this.topPos + 4, 16,
                b -> toast.utilityMobs.GuideBook.openClient(),
                Component.translatable("utilitymobs.gui.help"),
                Component.translatable("utilitymobs.gui.help.desc")));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GuiSteamGolem.TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int burnTime = this.menu.getBurnTime();
        if (burnTime > 0) {
            int maxBurnTime = this.menu.getMaxBurnTime();
            int fireSize = burnTime * 13 / (maxBurnTime == 0 ? 200 : maxBurnTime);
            graphics.blit(GuiSteamGolem.TEXTURE, x + 80, y + 27 + 12 - fireSize, 176, 12 - fireSize, 14, fireSize + 1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
