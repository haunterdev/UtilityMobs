package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import toast.utilityMobs.setup.WizardConfig;
import toast.utilityMobs.setup.WizardState;

/**
 * First-launch experience picker. Independent decisions + one-click tier pre-fills.
 *
 * <p>1.12.2 gave every GuiButton a numeric id and dispatched on it in actionPerformed. 1.20.1 buttons
 * carry an onPress callback instead, so the ids survive only as the row keys the label and description
 * lookups already switch on - the toggle itself is now the callback. Layout, wording, tier presets and
 * the tooltip line are unchanged.
 */
public class GuiSetupWizard extends Screen {

    private final Screen parent;
    private final WizardState state;

    private static final int[] ROWS = {
        10, 11, 12, 13, 14, 15,   // left
        16, 17, 18, 19, 20, 21    // right
    };

    /// Row id -> its button, so refreshLabels can rewrite the label after a toggle.
    private final List<Button> rowButtons = new ArrayList<Button>();

    public GuiSetupWizard(Screen parent, WizardState initial) {
        super(Component.translatable("utilitymobs.setup.title"));
        this.parent = parent;
        this.state = initial;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        this.rowButtons.clear();
        int cx = this.width / 2;

        int tierY = 34;
        int tw = 86, tgap = 4;
        int tierStart = cx - (tw * 2 + tgap + tgap / 2);
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.tier.engineer"),
                b -> this.copyInto(WizardState.engineer())).bounds(tierStart, tierY, tw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.tier.warlord"),
                b -> this.copyInto(WizardState.warlord())).bounds(tierStart + tw + tgap, tierY, tw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.tier.survivor"),
                b -> this.copyInto(WizardState.survivor())).bounds(tierStart + 2 * (tw + tgap), tierY, tw, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.tier.custom"),
                b -> this.copyInto(WizardState.custom())).bounds(tierStart + 3 * (tw + tgap), tierY, tw, 20).build());

        int bw = 158, bh = 20, vgap = 2;
        int leftX = cx - bw - 4;
        int rightX = cx + 4;
        int firstY = 66;
        for (int i = 0; i < GuiSetupWizard.ROWS.length; i++) {
            int id = GuiSetupWizard.ROWS[i];
            boolean left = i < 6;
            int col = left ? leftX : rightX;
            int rowInCol = left ? i : i - 6;
            int y = firstY + rowInCol * (bh + vgap);
            Button button = Button.builder(Component.empty(), b -> {
                this.toggle(id);
                this.refreshLabels();
            }).bounds(col, y, bw, bh).build();
            this.addRenderableWidget(button);
            this.rowButtons.add(button);
        }

        int botY = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.confirm"), b -> {
            WizardConfig.apply(this.state);
            SetupWizardHandler.writeMarker();
            this.minecraft.setScreen(this.parent);
        }).bounds(cx - 154, botY, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.skip"), b -> {
            SetupWizardHandler.writeMarker();
            this.minecraft.setScreen(this.parent);
        }).bounds(cx + 4, botY, 150, 20).build());

        this.refreshLabels();
    }

    private void refreshLabels() {
        for (int i = 0; i < this.rowButtons.size(); i++) {
            this.rowButtons.get(i).setMessage(Component.literal(this.labelFor(GuiSetupWizard.ROWS[i])));
        }
    }

    private String onOff(boolean v) {
        return I18n.get(v ? "utilitymobs.setup.on" : "utilitymobs.setup.off");
    }

    private String labelFor(int id) {
        switch (id) {
            case 10: return I18n.get("utilitymobs.setup.row.attack_passives") + ": " + this.onOff(this.state.attackPassives);
            case 11: return I18n.get("utilitymobs.setup.row.attack_neutrals") + ": " + this.onOff(this.state.attackNeutrals);
            case 12: return I18n.get("utilitymobs.setup.row.hostile") + ": " + this.onOff(this.state.hostile);
            case 13: return I18n.get("utilitymobs.setup.row.require_ammo") + ": " + this.onOff(this.state.requireAmmo);
            case 14: return I18n.get("utilitymobs.setup.row.no_mob_aggro") + ": " + this.onOff(this.state.mobsIgnoreTurrets);
            case 15: return I18n.get("utilitymobs.setup.row.passthrough") + ": " + this.onOff(this.state.friendlyPassthrough);
            case 16: return I18n.get("utilitymobs.setup.row.skull") + ": " + I18n.get("utilitymobs.setup.skull." + this.state.skullDrops.name().toLowerCase());
            case 17: return I18n.get("utilitymobs.setup.row.alt_manuals") + ": " + this.onOff(this.state.alternateManuals);
            case 18: return I18n.get("utilitymobs.setup.row.give_book") + ": " + this.onOff(this.state.giveBook);
            case 19: return I18n.get("utilitymobs.setup.row.collision") + ": " + this.onOff(this.state.walkableTurrets);
            case 20: return I18n.get("utilitymobs.setup.row.drop_chance") + ": " + I18n.get("utilitymobs.setup.drop." + this.state.dropChance.name().toLowerCase());
            case 21: return I18n.get("utilitymobs.setup.row.huge_armies") + ": " + this.onOff(this.state.hugeArmies);
            default: return "";
        }
    }

    private String descKeyFor(int id) {
        switch (id) {
            case 10: return "utilitymobs.setup.desc.attack_passives";
            case 11: return "utilitymobs.setup.desc.attack_neutrals";
            case 12: return "utilitymobs.setup.desc.hostile";
            case 13: return "utilitymobs.setup.desc.require_ammo";
            case 14: return "utilitymobs.setup.desc.no_mob_aggro";
            case 15: return "utilitymobs.setup.desc.passthrough";
            case 16: return "utilitymobs.setup.desc.skull";
            case 17: return "utilitymobs.setup.desc.alt_manuals";
            case 18: return "utilitymobs.setup.desc.give_book";
            case 19: return "utilitymobs.setup.desc.collision";
            case 20: return "utilitymobs.setup.desc.drop_chance";
            case 21: return "utilitymobs.setup.desc.huge_armies";
            default: return "";
        }
    }

    private void toggle(int id) {
        switch (id) {
            case 10: this.state.attackPassives = !this.state.attackPassives; break;
            case 11: this.state.attackNeutrals = !this.state.attackNeutrals; break;
            case 12: this.state.hostile = !this.state.hostile; break;
            case 13: this.state.requireAmmo = !this.state.requireAmmo; break;
            case 14: this.state.mobsIgnoreTurrets = !this.state.mobsIgnoreTurrets; break;
            case 15: this.state.friendlyPassthrough = !this.state.friendlyPassthrough; break;
            case 16: this.state.nextSkull(); break;
            case 17: this.state.alternateManuals = !this.state.alternateManuals; break;
            case 18: this.state.giveBook = !this.state.giveBook; break;
            case 19: this.state.walkableTurrets = !this.state.walkableTurrets; break;
            case 20: this.state.nextDrop(); break;
            case 21: this.state.hugeArmies = !this.state.hugeArmies; break;
            default: break;
        }
    }

    private void copyInto(WizardState t) {
        this.state.requireAmmo = t.requireAmmo;
        this.state.mobsIgnoreTurrets = t.mobsIgnoreTurrets;
        this.state.friendlyPassthrough = t.friendlyPassthrough;
        this.state.walkableTurrets = t.walkableTurrets;
        this.state.attackPassives = t.attackPassives;
        this.state.attackNeutrals = t.attackNeutrals;
        this.state.hostile = t.hostile;
        this.state.alternateManuals = t.alternateManuals;
        this.state.giveBook = t.giveBook;
        this.state.hugeArmies = t.hugeArmies;
        this.state.dropChance = t.dropChance;
        this.state.skullDrops = t.skullDrops;
        this.refreshLabels();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, I18n.get("utilitymobs.setup.title"), this.width / 2, 14, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.rowButtons.size(); i++) {
            if (this.rowButtons.get(i).isHovered()) {
                graphics.drawCenteredString(this.font, I18n.get(this.descKeyFor(GuiSetupWizard.ROWS[i])),
                        this.width / 2, this.height - 44, 0xA0A0A0);
                break;
            }
        }
    }
}
