package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import toast.utilityMobs.Properties;
import toast.utilityMobs._UtilityMobs;

/** Provides the in-game "Mods > Utility Mobs > Config" screen, backed by the mod Configuration. */
public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraft) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        if (Properties.config != null) {
            for (String category : Properties.config.getCategoryNames()) {
                ConfigCategory cc = Properties.config.getCategory(category);
                if (!cc.isChild()) {
                    elements.add(new ConfigElement(cc));
                }
            }
        }
        return new GuiUtilityMobsConfig(parent, elements, _UtilityMobs.MODID, "Utility Mobs");
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}
