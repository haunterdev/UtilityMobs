package toast.utilityMobs.client;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import toast.utilityMobs.setup.WizardConfig;
import toast.utilityMobs.setup.WizardState;

/** First-launch experience picker. Independent decisions + one-click tier pre-fills. */
public class GuiSetupWizard extends GuiScreen {

    private final GuiScreen parent;
    private final WizardState state;

    private static final int ID_ENGINEER = 1, ID_WARLORD = 2, ID_SURVIVOR = 3, ID_CUSTOM = 4;
    private static final int ID_CONFIRM = 100, ID_SKIP = 101;
    private static final int[] ROWS = {
        10, 11, 12, 13, 14, 15,   // left
        16, 17, 18, 19, 20, 21    // right
    };

    public GuiSetupWizard(GuiScreen parent, WizardState initial) {
        this.parent = parent;
        this.state = initial;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int cx = this.width / 2;

        int tierY = 34;
        int tw = 86, tgap = 4;
        int tierStart = cx - (tw * 2 + tgap + tgap / 2);
        this.buttonList.add(new GuiButton(ID_ENGINEER, tierStart, tierY, tw, 20, I18n.format("utilitymobs.setup.tier.engineer")));
        this.buttonList.add(new GuiButton(ID_WARLORD, tierStart + tw + tgap, tierY, tw, 20, I18n.format("utilitymobs.setup.tier.warlord")));
        this.buttonList.add(new GuiButton(ID_SURVIVOR, tierStart + 2 * (tw + tgap), tierY, tw, 20, I18n.format("utilitymobs.setup.tier.survivor")));
        this.buttonList.add(new GuiButton(ID_CUSTOM, tierStart + 3 * (tw + tgap), tierY, tw, 20, I18n.format("utilitymobs.setup.tier.custom")));

        int bw = 158, bh = 20, vgap = 2;
        int leftX = cx - bw - 4;
        int rightX = cx + 4;
        int firstY = 66;
        for (int i = 0; i < ROWS.length; i++) {
            int id = ROWS[i];
            boolean left = i < 6;
            int col = left ? leftX : rightX;
            int rowInCol = left ? i : i - 6;
            int y = firstY + rowInCol * (bh + vgap);
            this.buttonList.add(new GuiButton(id, col, y, bw, bh, ""));
        }

        int botY = this.height - 30;
        this.buttonList.add(new GuiButton(ID_CONFIRM, cx - 154, botY, 150, 20, I18n.format("utilitymobs.setup.confirm")));
        this.buttonList.add(new GuiButton(ID_SKIP, cx + 4, botY, 150, 20, I18n.format("utilitymobs.setup.skip")));

        refreshLabels();
    }

    private void refreshLabels() {
        for (GuiButton b : this.buttonList) {
            if (b.id >= 10 && b.id <= 21) {
                b.displayString = labelFor(b.id);
            }
        }
    }

    private String onOff(boolean v) {
        return I18n.format(v ? "utilitymobs.setup.on" : "utilitymobs.setup.off");
    }

    private String labelFor(int id) {
        switch (id) {
            case 10: return I18n.format("utilitymobs.setup.row.attack_passives") + ": " + onOff(state.attackPassives);
            case 11: return I18n.format("utilitymobs.setup.row.attack_neutrals") + ": " + onOff(state.attackNeutrals);
            case 12: return I18n.format("utilitymobs.setup.row.hostile") + ": " + onOff(state.hostile);
            case 13: return I18n.format("utilitymobs.setup.row.require_ammo") + ": " + onOff(state.requireAmmo);
            case 14: return I18n.format("utilitymobs.setup.row.no_mob_aggro") + ": " + onOff(state.mobsIgnoreTurrets);
            case 15: return I18n.format("utilitymobs.setup.row.passthrough") + ": " + onOff(state.friendlyPassthrough);
            case 16: return I18n.format("utilitymobs.setup.row.skull") + ": " + I18n.format("utilitymobs.setup.skull." + state.skullDrops.name().toLowerCase());
            case 17: return I18n.format("utilitymobs.setup.row.alt_manuals") + ": " + onOff(state.alternateManuals);
            case 18: return I18n.format("utilitymobs.setup.row.give_book") + ": " + onOff(state.giveBook);
            case 19: return I18n.format("utilitymobs.setup.row.collision") + ": " + onOff(state.walkableTurrets);
            case 20: return I18n.format("utilitymobs.setup.row.drop_chance") + ": " + I18n.format("utilitymobs.setup.drop." + state.dropChance.name().toLowerCase());
            case 21: return I18n.format("utilitymobs.setup.row.huge_armies") + ": " + onOff(state.hugeArmies);
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

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case ID_ENGINEER: copyInto(WizardState.engineer()); break;
            case ID_WARLORD:  copyInto(WizardState.warlord());  break;
            case ID_SURVIVOR: copyInto(WizardState.survivor()); break;
            case ID_CUSTOM:   copyInto(WizardState.custom());   break;

            case 10: state.attackPassives = !state.attackPassives; break;
            case 11: state.attackNeutrals = !state.attackNeutrals; break;
            case 12: state.hostile = !state.hostile; break;
            case 13: state.requireAmmo = !state.requireAmmo; break;
            case 14: state.mobsIgnoreTurrets = !state.mobsIgnoreTurrets; break;
            case 15: state.friendlyPassthrough = !state.friendlyPassthrough; break;
            case 16: state.nextSkull(); break;
            case 17: state.alternateManuals = !state.alternateManuals; break;
            case 18: state.giveBook = !state.giveBook; break;
            case 19: state.walkableTurrets = !state.walkableTurrets; break;
            case 20: state.nextDrop(); break;
            case 21: state.hugeArmies = !state.hugeArmies; break;

            case ID_CONFIRM:
                WizardConfig.apply(state);
                SetupWizardHandler.writeMarker();
                this.mc.displayGuiScreen(parent);
                return;
            case ID_SKIP:
                SetupWizardHandler.writeMarker();
                this.mc.displayGuiScreen(parent);
                return;
            default:
                return;
        }
        refreshLabels();
    }

    private void copyInto(WizardState t) {
        state.requireAmmo = t.requireAmmo;
        state.mobsIgnoreTurrets = t.mobsIgnoreTurrets;
        state.friendlyPassthrough = t.friendlyPassthrough;
        state.walkableTurrets = t.walkableTurrets;
        state.attackPassives = t.attackPassives;
        state.attackNeutrals = t.attackNeutrals;
        state.hostile = t.hostile;
        state.alternateManuals = t.alternateManuals;
        state.giveBook = t.giveBook;
        state.hugeArmies = t.hugeArmies;
        state.dropChance = t.dropChance;
        state.skullDrops = t.skullDrops;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, I18n.format("utilitymobs.setup.title"), this.width / 2, 14, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiButton b : this.buttonList) {
            if (b.id >= 10 && b.id <= 21 && b.isMouseOver()) {
                this.drawCenteredString(this.fontRenderer, I18n.format(descKeyFor(b.id)),
                        this.width / 2, this.height - 44, 0xA0A0A0);
                break;
            }
        }
    }
}
