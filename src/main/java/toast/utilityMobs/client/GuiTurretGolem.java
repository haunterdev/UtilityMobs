package toast.utilityMobs.client;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.GuideBook;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.MessageTurretToggle;
import toast.utilityMobs.turret.ContainerTurretGolem;
import toast.utilityMobs.turret.EntityTurretGolem;
import toast.utilityMobs.turret.TurretStats;

@SideOnly(Side.CLIENT)
public class GuiTurretGolem extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/gui/turret_upgrade.png");
    private final EntityTurretGolem turret;
    private GuiButton outlineButton;
    private GuiButton hostileButton;
    private GuiButton passiveButton;
    private GuiButton neutralButton;
    private GuiButton targetModeButton;
    /// Whether the "?" help button is shown (general.show_help_button); also gates its tooltip.
    private boolean helpButton;

    public GuiTurretGolem(InventoryPlayer playerInv, EntityTurretGolem turret) {
        super(new ContainerTurretGolem(playerInv, turret));
        this.turret = turret;
        this.xSize = 200;
        this.ySize = 196;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        // 18px pitch (2px gaps) from y=42; last button ends at y=110, clear of the player inventory (y=114).
        // BorderedButton (not vanilla GuiButton): a sub-20px-tall vanilla button samples only the TOP
        // of the 20px widget graphic and drops the bottom border row, leaving every button visibly
        // cut off at the bottom. BorderedButton re-draws the bottom border slice so short buttons frame
        // cleanly.
        this.outlineButton    = new BorderedButton(0, this.guiLeft + 8, this.guiTop + 41, 80, 14, this.outlineLabel());
        this.hostileButton    = new BorderedButton(1, this.guiLeft + 8, this.guiTop + 55, 80, 14, this.hostileLabel());
        this.passiveButton    = new BorderedButton(2, this.guiLeft + 8, this.guiTop + 69, 80, 14, this.passiveLabel());
        this.neutralButton    = new BorderedButton(5, this.guiLeft + 8, this.guiTop + 83, 80, 14, this.neutralLabel());
        this.targetModeButton = new BorderedButton(3, this.guiLeft + 8, this.guiTop + 97, 80, 14, this.targetModeLabel());
        this.buttonList.add(this.outlineButton);
        this.buttonList.add(this.hostileButton);
        this.buttonList.add(this.passiveButton);
        this.buttonList.add(this.neutralButton);
        this.buttonList.add(this.targetModeButton);
        // Help button - compact square tucked in the top-right corner; opens the guide's Turret Upgrades page.
        // Gated by general.show_help_button, matching the block-golem/steam-golem GUIs.
        this.helpButton = toast.utilityMobs.Properties.getBoolean(toast.utilityMobs.Properties.GENERAL, "show_help_button");
        if (this.helpButton) {
            this.buttonList.add(new BorderedButton(4, this.guiLeft + this.xSize - 22, this.guiTop + 6, 14, 14, "?"));
        }
    }

    private String outlineLabel() {
        boolean on = TurretOutlineRenderer.isOutlined(this.turret.getEntityId());
        return I18n.format(on ? "utilitymobs.gui.range_on" : "utilitymobs.gui.range_off");
    }

    private String hostileLabel() {
        return I18n.format(this.turret.attacksHostile() ? "utilitymobs.gui.hostile_on" : "utilitymobs.gui.hostile_off");
    }

    private String passiveLabel() {
        return I18n.format(this.turret.attacksPassive() ? "utilitymobs.gui.passive_on" : "utilitymobs.gui.passive_off");
    }

    private String neutralLabel() {
        return I18n.format(this.turret.attacksNeutral() ? "utilitymobs.gui.neutral_on" : "utilitymobs.gui.neutral_off");
    }

    private String targetModeLabel() {
        switch (this.turret.getTargetMode()) {
            case 1:  return I18n.format("utilitymobs.gui.target_far");
            case 2:  return I18n.format("utilitymobs.gui.target_strong");
            case 3:  return I18n.format("utilitymobs.gui.target_weak");
            default: return I18n.format("utilitymobs.gui.target_close");
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Keep the toggle labels in sync with the (server-synced) turret flags.
        this.outlineButton.displayString = this.outlineLabel();
        this.hostileButton.displayString = this.hostileLabel();
        this.passiveButton.displayString = this.passiveLabel();
        this.neutralButton.displayString = this.neutralLabel();
        this.targetModeButton.displayString = this.targetModeLabel();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            TurretOutlineRenderer.toggle(this.turret.getEntityId());
            this.outlineButton.displayString = this.outlineLabel();
        }
        else if (button.id == 1) {
            _UtilityMobs.CHANNEL.sendToServer(new MessageTurretToggle(this.turret.getEntityId(), 0));
        }
        else if (button.id == 2) {
            _UtilityMobs.CHANNEL.sendToServer(new MessageTurretToggle(this.turret.getEntityId(), 1));
        }
        else if (button.id == 3) {
            _UtilityMobs.CHANNEL.sendToServer(new MessageTurretToggle(this.turret.getEntityId(), 2));
        }
        else if (button.id == 5) {
            _UtilityMobs.CHANNEL.sendToServer(new MessageTurretToggle(this.turret.getEntityId(), 3));
        }
        else if (button.id == 4) {
            this.openUpgradesGuide();
        }
    }

    /** Opens the guide book directly to the "Turret Upgrades" category page. TurretGuide pushes the
        landing page underneath first, so the category's back arrow still returns to the book root -
        avoiding the old "stuck with no way back" problem that an unparented category GUI caused. */
    private void openUpgradesGuide() {
        TurretGuide.openUpgrades();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        if (this.turret.requiresAmmo()) {
            int px = this.guiLeft - 68;
            int py = this.guiTop + 10;
            int right = this.guiLeft;
            int bottom = this.guiTop + 80;
            // face
            this.drawRect(px, py, right, bottom, 0xFFC6C6C6);
            // raised bevel: light top + left, dark bottom + right
            this.drawRect(px, py, right, py + 1, 0xFFFFFFFF);
            this.drawRect(px, py, px + 1, bottom, 0xFFFFFFFF);
            this.drawRect(px, bottom - 1, right, bottom, 0xFF555555);
            this.drawRect(right - 1, py, right, bottom, 0xFF555555);
            // recessed title strip behind the "Ammo" label
            this.drawRect(px + 3, py + 2, right - 3, py + 12, 0xFF8B8B8B);
            this.drawRect(px + 3, py + 2, right - 3, py + 3, 0xFF555555);
            // slot wells
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    int sx = this.guiLeft - 64 + col * 18;
                    int sy = this.guiTop + 24 + row * 18;
                    this.drawRect(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                    this.drawRect(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                }
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String tname = this.turret.getName();
        int budget = 84;
        int w = this.fontRenderer.getStringWidth(tname);
        if (w > budget) {
            float s = (float) budget / (float) w;
            GlStateManager.pushMatrix();
            GlStateManager.translate(8.0F, 6.0F, 0.0F);
            GlStateManager.scale(s, s, 1.0F);
            this.fontRenderer.drawString(tname, 0, 0, 0x404040);
            GlStateManager.popMatrix();
        } else {
            this.fontRenderer.drawString(tname, 8, 6, 0x404040);
        }
        if (this.turret.requiresAmmo()) {
            this.fontRenderer.drawString(I18n.format("utilitymobs.gui.ammo"), -63, 13, 0x404040);
        }
        TurretStats base = TurretStats.base(this.turret);
        ItemStack slot = this.turret.getEquipmentInSlot(0);
        EnumUpgrade up = EnumUpgrade.getUpgrade(this.turret.upgrades, slot);
        TurretStats cur = TurretStats.withUpgrade(this.turret, up);
        int x = 96;
        int y = 18;
        y = this.drawStatRow("utilitymobs.stat.health", fmt(this.turret.getMaxHealth()), null, 0, x, y);
        y = this.drawStatRow("utilitymobs.stat.armor", String.valueOf(this.turret.getTotalArmorValue()), null, 0, x, y);
        // Damage (with projectiles)
        String baseDmg = dmgStr(base);
        String newDmg = dmgStr(cur);
        boolean dmgChanged = cur.damageDiffersFrom(base);
        y = this.drawStatRow("utilitymobs.stat.damage", baseDmg, dmgChanged ? newDmg : null, up.gradientColor, x, y);
        // Fire rate
        y = this.drawStatRow("utilitymobs.stat.firerate", fmt(base.shotsPerSecond()) + "/s", null, 0, x, y);
        // Range
        boolean rangeChanged = cur.rangeDiffersFrom(base);
        y = this.drawStatRow("utilitymobs.stat.range", fmt(base.range), rangeChanged ? fmt(cur.range) : null, up.gradientColor, x, y);
        // Accuracy - drawn plain (no upgrade gradient): "near→far%" conveys range falloff, a single
        // value means flat accuracy at all ranges.
        int accNear = cur.accuracyNear();
        int accFar = cur.accuracyFar();
        String accLabel = I18n.format("utilitymobs.stat.accuracy") + ": ";
        this.fontRenderer.drawString(accLabel, x, y, 0x404040);
        int accLw = this.fontRenderer.getStringWidth(accLabel);
        String accVal = accNear == accFar ? accNear + "%" : accNear + "→" + accFar + "%";
        this.fontRenderer.drawString(accVal, x + accLw, y, 0x202020);
        y += 10;
        // Effect rows
        List<String> effects = cur.effects;
        for (int i = 0; i < effects.size(); i++) {
            int color = cur.effectColors.get(i);
            this.fontRenderer.drawString("+ " + I18n.format(effects.get(i)), x, y, color & 0xFFFFFF);
            y += 10;
        }
        // Upgrade-slot indicator: label beside the slot (slot is at 24,24), brighter when empty so it
        // reads as "put something here". Split across two lines so "Upgrade Slot" sits entirely between
        // the slot (ends x=40) and the stat column (starts x=96) without overrunning into it.
        boolean slotEmpty = this.turret.getEquipmentInSlot(0).isEmpty();
        int slotLabelColor = slotEmpty ? 0x707070 : 0x404040;
        this.fontRenderer.drawString("Upgrade", 44, 24, slotLabelColor);
        this.fontRenderer.drawString("Slot", 44, 33, slotLabelColor);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        // The stat panel is drawn in the foreground layer; re-assert the hovered-item tooltip AFTER
        // super so item names still show on top of it, then draw our own zone tooltips above that.
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderHoveredToolTip(mouseX, mouseY);
        int sx = this.guiLeft + 24, sy = this.guiTop + 24;
        if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
            this.drawHoveringText(java.util.Arrays.asList(
                    "Upgrade Slot",
                    "§7Insert an upgrade item to boost",
                    "§7this turret's stats."), mouseX, mouseY);
        }
        // NOTE: the "?" help tooltip is intentionally NOT drawn here. It sits in the top-right corner
        // next to JEI's ingredient column, and JEI paints its item list in DrawScreenEvent.Post - after
        // this GUI's drawScreen - so a tooltip drawn here is overdrawn by JEI items. HelpTooltipHandler
        // re-draws it in DrawScreenEvent.Post at LOWEST priority (i.e. after JEI) so it lands on top.
    }

    /** Draws the "?" help-button tooltip when hovered. Called from {@link HelpTooltipHandler} in
        DrawScreenEvent.Post so it renders above JEI's overlay (see drawScreen). */
    void drawHelpTooltip(int mouseX, int mouseY) {
        if (!this.helpButton) return;
        int qx = this.guiLeft + this.xSize - 22, qy = this.guiTop + 6;
        if (mouseX >= qx && mouseX < qx + 14 && mouseY >= qy && mouseY < qy + 14) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawHoveringText(java.util.Arrays.asList(
                    "§eTurret Upgrades",
                    "§7Open the guide to the upgrades page."), mouseX, mouseY);
        }
    }

    /** Draws "Label: old" or "Label: old -> new" with the new value gradient-tinted. Returns next y. */
    private int drawStatRow(String labelKey, String oldVal, String newVal, int accent, int x, int y) {
        String label = I18n.format(labelKey) + ": ";
        this.fontRenderer.drawString(label, x, y, 0x404040);
        int lw = this.fontRenderer.getStringWidth(label);
        if (newVal == null) {
            this.fontRenderer.drawString(oldVal, x + lw, y, 0x202020);
        } else {
            this.fontRenderer.drawString(oldVal, x + lw, y, 0x808080);
            int ow = this.fontRenderer.getStringWidth(oldVal);
            this.fontRenderer.drawString(" → ", x + lw + ow, y, 0x404040);
            int aw = this.fontRenderer.getStringWidth(" → ");
            this.drawGradientCell(x + lw + ow + aw, y, this.fontRenderer.getStringWidth(newVal) + 2, 9, accent);
            this.fontRenderer.drawString(newVal, x + lw + ow + aw + 1, y, 0xFFFFFF);
        }
        return y + 10;
    }

    /** Dark->bright vertical gradient of the accent color behind the new value. */
    private void drawGradientCell(int x, int y, int w, int h, int accent) {
        int r = (accent >> 16) & 0xFF, g = (accent >> 8) & 0xFF, b = accent & 0xFF;
        int dark = 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
        int bright = 0xFF000000 | (r << 16) | (g << 8) | b;
        this.drawGradientRect(x, y - 1, x + w, y + h, dark, bright);
    }

    private static String fmt(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.05) return String.valueOf((int)Math.rint(d));
        return String.format("%.1f", d);
    }

    private static String dmgStr(TurretStats s) {
        String d = fmt(s.displayedDamage());
        return s.projectiles > 1 ? d + " ×" + s.projectiles : d;
    }

    /**
        A vanilla {@link GuiButton} shorter than 20px tall samples only the top {@code height} pixels of
        the 20px-tall widget graphic, which clips off the button's bottom border row - every short button
        looks cut off at the bottom. This variant draws the body minus a 3px bottom strip, then re-draws
        that strip from the texture's own bottom 3 pixels, so the frame closes cleanly at any height.
     */
    private static final class BorderedButton extends GuiButton {
        private static final int BORDER = 3;

        BorderedButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;
            FontRenderer fr = mc.fontRenderer;
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

    /**
        Re-draws the "?" help tooltip in DrawScreenEvent.Post at LOWEST priority so it paints AFTER JEI's
        ingredient overlay (also drawn in Post) and therefore lands on top of JEI items. Registered once
        in ClientProxy. See {@link #drawHelpTooltip}.
     */
    public static final class HelpTooltipHandler {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
            if (event.getGui() instanceof GuiTurretGolem) {
                ((GuiTurretGolem) event.getGui()).drawHelpTooltip(event.getMouseX(), event.getMouseY());
            }
        }
    }
}
