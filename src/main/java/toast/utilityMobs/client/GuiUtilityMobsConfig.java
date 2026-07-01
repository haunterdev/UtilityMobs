package toast.utilityMobs.client;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import toast.utilityMobs.setup.WizardState;

/** The mod's config screen, plus a button that reopens the first-launch setup wizard. */
public class GuiUtilityMobsConfig extends GuiConfig {

    private static final int ID_WIZARD = 9100;

    public GuiUtilityMobsConfig(GuiScreen parent, List<IConfigElement> elements, String modId, String title) {
        super(parent, elements, modId, false, false, title);
    }

    @Override
    public void initGui() {
        super.initGui();
        // Compact button pinned to the top-left corner so it clears the centered title and the config
        // list rows (the old full-width button at center-top overlapped the "Utility Mobs" title).
        this.buttonList.add(new GuiButton(ID_WIZARD, 4, 4, 110, 18,
                I18n.format("utilitymobs.setup.reopen")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_WIZARD) {
            this.mc.displayGuiScreen(new GuiSetupWizard(this, WizardState.engineer()));
            return;
        }
        super.actionPerformed(button);
    }
}
