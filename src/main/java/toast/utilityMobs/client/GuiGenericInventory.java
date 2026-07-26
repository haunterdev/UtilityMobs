package toast.utilityMobs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A plain container screen over any vanilla inventory background. Used for golems whose menu is just
 * slots, so it needs no widgets of its own.
 *
 * <p>1.12.2's GuiBorderedButton is deliberately not ported: it existed only because a vanilla button
 * shorter than 20px sampled the top of the widget graphic and clipped its own bottom border. 1.20.1
 * draws buttons nine-sliced, so a short button already closes its frame and the workaround would only
 * reimplement vanilla rendering worse.
 */
public class GuiGenericInventory<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public static final ResourceLocation TEXTURE_DISPENSER = new ResourceLocation("textures/gui/container/dispenser.png");

    private final ResourceLocation texture;

    public GuiGenericInventory(T menu, Inventory inventory, Component title, ResourceLocation texture) {
        super(menu, inventory, title);
        this.texture = texture;
    }

    @Override
    protected void init() {
        super.init();
        // Help button (top-right) - opens the Patchouli guide book. Hidden when general.show_help_button
        // is false, or when Patchouli is absent and there is no book to open.
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
        graphics.blit(this.texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
