package toast.utilityMobs.client;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.setup.WizardState;

/** Auto-opens the setup wizard over the main menu on first launch (gated by a marker file). */
public class SetupWizardHandler {

    private static boolean shownThisSession;

    private static File markerFile() {
        return new File(Loader.instance().getConfigDir(), _UtilityMobs.MODID + "/setup_done.flag");
    }

    public static boolean markerExists() {
        return markerFile().exists();
    }

    public static void writeMarker() {
        try {
            File f = markerFile();
            File dir = f.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            f.createNewFile();
        } catch (IOException e) {
            _UtilityMobs.debugException("Could not write setup-wizard marker: " + e.getMessage());
        }
        shownThisSession = true;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen gui = event.getGui();
        if (gui == null || gui.getClass() != GuiMainMenu.class) {
            return;
        }
        if (shownThisSession || markerExists()) {
            return;
        }
        shownThisSession = true;
        event.setGui(new GuiSetupWizard((GuiMainMenu) gui, WizardState.engineer()));
    }
}
