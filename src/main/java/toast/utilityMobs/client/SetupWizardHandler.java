package toast.utilityMobs.client;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.setup.WizardState;

/** Auto-opens the setup wizard over the main menu on first launch (gated by a marker file). */
public class SetupWizardHandler {

    private static boolean shownThisSession;

    private static File markerFile() {
        return new File(FMLPaths.CONFIGDIR.get().toFile(), _UtilityMobs.MODID + "/setup_done.flag");
    }

    public static boolean markerExists() {
        return SetupWizardHandler.markerFile().exists();
    }

    public static void writeMarker() {
        try {
            File f = SetupWizardHandler.markerFile();
            File dir = f.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            f.createNewFile();
        } catch (IOException e) {
            _UtilityMobs.debugException("Could not write setup-wizard marker: " + e.getMessage());
        }
        SetupWizardHandler.shownThisSession = true;
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        Screen gui = event.getScreen();
        if (gui == null || gui.getClass() != TitleScreen.class) {
            return;
        }
        if (SetupWizardHandler.shownThisSession || SetupWizardHandler.markerExists()) {
            return;
        }
        SetupWizardHandler.shownThisSession = true;
        event.setNewScreen(new GuiSetupWizard((TitleScreen) gui, WizardState.engineer()));
    }
}
