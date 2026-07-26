package toast.utilityMobs.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.network.MessageTurretToggle;
import toast.utilityMobs.network.UMChannel;
import toast.utilityMobs.turret.ContainerTurretGolem;
import toast.utilityMobs.turret.EntityTurretGolem;
import toast.utilityMobs.turret.TurretStats;

/**
 * The turret configuration screen: upgrade slot, optional ammo grid, targeting toggles, and a live
 * stat panel showing how the inserted upgrade changes the numbers.
 *
 * <p>1.12.2's private BorderedButton is gone. It only existed because a vanilla button shorter than
 * 20px clipped its own bottom border; 1.20.1 nine-slices buttons, so plain Buttons frame correctly at
 * 14px. The "?" help button and its JEI-ordering workaround also wait for the guide book in Task 14.
 */
public class GuiTurretGolem extends AbstractContainerScreen<ContainerTurretGolem> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/gui/turret_upgrade.png");

    private final EntityTurretGolem turret;
    private Button outlineButton;
    private Button hostileButton;
    private Button passiveButton;
    private Button neutralButton;
    private Button targetModeButton;

    public GuiTurretGolem(ContainerTurretGolem menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.turret = menu.getTurret();
        this.imageWidth = 200;
        this.imageHeight = 196;
    }

    @Override
    protected void init() {
        super.init();
        // 18px pitch (2px gaps) from y=41; the last button ends at y=111, clear of the player inventory (y=114).
        this.outlineButton = this.addRenderableWidget(Button.builder(this.outlineLabel(), b -> {
            TurretOutlineRenderer.toggle(this.turret.getId());
            this.outlineButton.setMessage(this.outlineLabel());
        }).bounds(this.leftPos + 8, this.topPos + 41, 80, 14).build());
        this.hostileButton = this.addRenderableWidget(Button.builder(this.hostileLabel(),
            b -> this.toggle(0)).bounds(this.leftPos + 8, this.topPos + 55, 80, 14).build());
        this.passiveButton = this.addRenderableWidget(Button.builder(this.passiveLabel(),
            b -> this.toggle(1)).bounds(this.leftPos + 8, this.topPos + 69, 80, 14).build());
        this.neutralButton = this.addRenderableWidget(Button.builder(this.neutralLabel(),
            b -> this.toggle(3)).bounds(this.leftPos + 8, this.topPos + 83, 80, 14).build());
        this.targetModeButton = this.addRenderableWidget(Button.builder(this.targetModeLabel(),
            b -> this.toggle(2)).bounds(this.leftPos + 8, this.topPos + 97, 80, 14).build());
        // Help button - compact square tucked in the top-right corner; opens the guide's Turret Upgrades
        // page rather than the book root. Gated by general.show_help_button like the other golem GUIs.
        if (toast.utilityMobs.GuideBook.showHelpButton()) {
            this.addRenderableWidget(HelpButton.build(this.leftPos + this.imageWidth - 22, this.topPos + 6, 14,
                b -> UMPatchouli.openTurretUpgrades(),
                Component.translatable("utilitymobs.gui.help.upgrades"),
                Component.translatable("utilitymobs.gui.help.upgrades.desc")));
        }
    }

    private void toggle(int which) {
        UMChannel.sendToServer(new MessageTurretToggle(this.turret.getId(), which));
    }

    private Component outlineLabel() {
        boolean on = TurretOutlineRenderer.isOutlined(this.turret.getId());
        return Component.translatable(on ? "utilitymobs.gui.range_on" : "utilitymobs.gui.range_off");
    }

    private Component hostileLabel() {
        return Component.translatable(this.turret.attacksHostile() ? "utilitymobs.gui.hostile_on" : "utilitymobs.gui.hostile_off");
    }

    private Component passiveLabel() {
        return Component.translatable(this.turret.attacksPassive() ? "utilitymobs.gui.passive_on" : "utilitymobs.gui.passive_off");
    }

    private Component neutralLabel() {
        return Component.translatable(this.turret.attacksNeutral() ? "utilitymobs.gui.neutral_on" : "utilitymobs.gui.neutral_off");
    }

    private Component targetModeLabel() {
        switch (this.turret.getTargetMode()) {
            case 1:  return Component.translatable("utilitymobs.gui.target_far");
            case 2:  return Component.translatable("utilitymobs.gui.target_strong");
            case 3:  return Component.translatable("utilitymobs.gui.target_weak");
            default: return Component.translatable("utilitymobs.gui.target_close");
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // Keep the toggle labels in sync with the (server-synced) turret flags.
        this.outlineButton.setMessage(this.outlineLabel());
        this.hostileButton.setMessage(this.hostileLabel());
        this.passiveButton.setMessage(this.passiveLabel());
        this.neutralButton.setMessage(this.neutralLabel());
        this.targetModeButton.setMessage(this.targetModeLabel());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(GuiTurretGolem.TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.hasAmmoGrid()) {
            int px = this.leftPos - 68;
            int py = this.topPos + 10;
            int right = this.leftPos;
            int bottom = this.topPos + 80;
            // face
            graphics.fill(px, py, right, bottom, 0xFFC6C6C6);
            // raised bevel: light top + left, dark bottom + right
            graphics.fill(px, py, right, py + 1, 0xFFFFFFFF);
            graphics.fill(px, py, px + 1, bottom, 0xFFFFFFFF);
            graphics.fill(px, bottom - 1, right, bottom, 0xFF555555);
            graphics.fill(right - 1, py, right, bottom, 0xFF555555);
            // recessed title strip behind the "Ammo" label
            graphics.fill(px + 3, py + 2, right - 3, py + 12, 0xFF8B8B8B);
            graphics.fill(px + 3, py + 2, right - 3, py + 3, 0xFF555555);
            // slot wells
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    int sx = this.leftPos - 64 + col * 18;
                    int sy = this.topPos + 24 + row * 18;
                    graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String tname = this.turret.getDisplayName().getString();
        int budget = 84;
        int w = this.font.width(tname);
        if (w > budget) {
            float s = (float)budget / (float)w;
            graphics.pose().pushPose();
            graphics.pose().translate(8.0F, 6.0F, 0.0F);
            graphics.pose().scale(s, s, 1.0F);
            graphics.drawString(this.font, tname, 0, 0, 0x404040, false);
            graphics.pose().popPose();
        } else {
            graphics.drawString(this.font, tname, 8, 6, 0x404040, false);
        }
        if (this.menu.hasAmmoGrid()) {
            graphics.drawString(this.font, Component.translatable("utilitymobs.gui.ammo"), -63, 13, 0x404040, false);
        }
        TurretStats base = TurretStats.base(this.turret);
        ItemStack slot = this.turret.getEquipmentInSlot(0);
        EnumUpgrade up = EnumUpgrade.getUpgrade(this.turret.upgrades, slot);
        TurretStats cur = TurretStats.withUpgrade(this.turret, up);
        int x = 96;
        int y = 18;
        y = this.drawStatRow(graphics, "utilitymobs.stat.health", GuiTurretGolem.fmt(this.turret.getMaxHealth()), null, 0, x, y);
        y = this.drawStatRow(graphics, "utilitymobs.stat.armor", String.valueOf(this.turret.getArmorValue()), null, 0, x, y);
        // Damage (with projectiles)
        String baseDmg = GuiTurretGolem.dmgStr(base);
        String newDmg = GuiTurretGolem.dmgStr(cur);
        boolean dmgChanged = cur.damageDiffersFrom(base);
        y = this.drawStatRow(graphics, "utilitymobs.stat.damage", baseDmg, dmgChanged ? newDmg : null, up.gradientColor, x, y);
        // Fire rate
        y = this.drawStatRow(graphics, "utilitymobs.stat.firerate", GuiTurretGolem.fmt(base.shotsPerSecond()) + "/s", null, 0, x, y);
        // Range
        boolean rangeChanged = cur.rangeDiffersFrom(base);
        y = this.drawStatRow(graphics, "utilitymobs.stat.range", GuiTurretGolem.fmt(base.range), rangeChanged ? GuiTurretGolem.fmt(cur.range) : null, up.gradientColor, x, y);
        // Accuracy - drawn plain (no upgrade gradient): "near->far%" conveys range falloff, a single
        // value means flat accuracy at all ranges.
        int accNear = cur.accuracyNear();
        int accFar = cur.accuracyFar();
        String accLabel = Component.translatable("utilitymobs.stat.accuracy").getString() + ": ";
        graphics.drawString(this.font, accLabel, x, y, 0x404040, false);
        int accLw = this.font.width(accLabel);
        String accVal = accNear == accFar ? accNear + "%" : accNear + "→" + accFar + "%";
        // Upgrades that give accuracy a range falloff switch this to the longer "near→far%" form, which
        // is wide enough to push the trailing % past the panel's right edge. Clamp the value's right
        // edge to the inner margin so it stays inside the frame; the short form is unaffected because
        // it never reaches the limit.
        int accX = Math.min(x + accLw, this.statRight() - this.font.width(accVal));
        graphics.drawString(this.font, accVal, accX, y, 0x202020, false);
        y += 10;
        // Effect rows
        List<String> effects = cur.effects;
        for (int i = 0; i < effects.size(); i++) {
            int color = cur.effectColors.get(i);
            graphics.drawString(this.font, "+ " + Component.translatable(effects.get(i)).getString(), x, y, color & 0xFFFFFF, false);
            y += 10;
        }
        // Upgrade-slot indicator: label beside the slot (slot is at 24,24), brighter when empty so it
        // reads as "put something here". Split across two lines so "Upgrade Slot" sits entirely between
        // the slot (ends x=40) and the stat column (starts x=96) without overrunning into it.
        boolean slotEmpty = slot.isEmpty();
        int slotLabelColor = slotEmpty ? 0x707070 : 0x404040;
        graphics.drawString(this.font, "Upgrade", 44, 24, slotLabelColor, false);
        graphics.drawString(this.font, "Slot", 44, 33, slotLabelColor, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        int sx = this.leftPos + 24, sy = this.topPos + 24;
        if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
            graphics.renderComponentTooltip(this.font, Arrays.asList(
                Component.literal("Upgrade Slot"),
                Component.literal("Insert an upgrade item to boost").withStyle(ChatFormatting.GRAY),
                Component.literal("this turret's stats.").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    /** Draws "Label: old" or "Label: old -> new" with the new value gradient-tinted. Returns next y. */
    /// Right edge the stat column must not cross. The panel border is part of the background texture,
    /// so this is the inner margin measured against the GUI width rather than anything queryable.
    private int statRight() {
        return this.imageWidth - 8;
    }

    private int drawStatRow(GuiGraphics graphics, String labelKey, String oldVal, String newVal, int accent, int x, int y) {
        String label = Component.translatable(labelKey).getString() + ": ";
        graphics.drawString(this.font, label, x, y, 0x404040, false);
        int lw = this.font.width(label);
        if (newVal == null) {
            graphics.drawString(this.font, oldVal, x + lw, y, 0x202020, false);
        } else {
            graphics.drawString(this.font, oldVal, x + lw, y, 0x808080, false);
            int ow = this.font.width(oldVal);
            // An upgraded multi-projectile row ("3 x6 -> 1 x6") is wide enough to push the highlight cell
            // clean off the panel, so drop the padding around the arrow once the row stops fitting, then
            // clamp the cell to the margin. Rows with room keep the spaced arrow they always had.
            int cellW = this.font.width(newVal) + 2;
            String arrow = " → ";
            if (x + lw + ow + this.font.width(arrow) + cellW > this.statRight()) {
                arrow = "→";
            }
            int aw = this.font.width(arrow);
            graphics.drawString(this.font, arrow, x + lw + ow, y, 0x404040, false);
            int cellX = Math.min(x + lw + ow + aw, this.statRight() - cellW);
            this.drawGradientCell(graphics, cellX, y, cellW, 9, accent);
            graphics.drawString(this.font, newVal, cellX + 1, y, 0xFFFFFF, false);
        }
        return y + 10;
    }

    /** Dark->bright vertical gradient of the accent color behind the new value. */
    private void drawGradientCell(GuiGraphics graphics, int x, int y, int w, int h, int accent) {
        int r = (accent >> 16) & 0xFF, g = (accent >> 8) & 0xFF, b = accent & 0xFF;
        int dark = 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
        int bright = 0xFF000000 | (r << 16) | (g << 8) | b;
        graphics.fillGradient(x, y - 1, x + w, y + h, dark, bright);
    }

    private static String fmt(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.05) return String.valueOf((int)Math.rint(d));
        return String.format("%.1f", d);
    }

    private static String dmgStr(TurretStats s) {
        String d = GuiTurretGolem.fmt(s.displayedDamage());
        return s.projectiles > 1 ? d + " ×" + s.projectiles : d;
    }
}
