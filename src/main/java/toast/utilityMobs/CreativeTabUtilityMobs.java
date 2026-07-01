package toast.utilityMobs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
    The mod's creative tab. The mod registers no items of its own - its content is the utility
    mob spawn eggs (one per registered entity) plus the NBT manual/upgrade/target books, all
    injected here via displayAllRelevantItems.
 */
public class CreativeTabUtilityMobs extends CreativeTabs
{
    public static final CreativeTabUtilityMobs INSTANCE = new CreativeTabUtilityMobs();

    private CreativeTabUtilityMobs() {
        super(_UtilityMobs.MODID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        // Hidden throwaway item textured with the stone golem's face (see _UtilityMobs.TAB_ICON).
        return new ItemStack(_UtilityMobs.TAB_ICON);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(NonNullList<ItemStack> list) {
        // A spawn egg for every registered utility mob.
        for (int i = 0; i < _UtilityMobs.UTILITY_NAMES.length; i++) {
            for (int j = 0; j < _UtilityMobs.UTILITY_NAMES[i].length; j++) {
                ItemStack egg = new ItemStack(Items.SPAWN_EGG);
                ItemMonsterPlacer.applyEntityIdToItemStack(egg, new ResourceLocation(_UtilityMobs.MODID, _UtilityMobs.UTILITY_NAMES[i][j].toLowerCase()));
                list.add(egg);
            }
        }
        // The UM iron/snow golems live outside UTILITY_NAMES (vanilla-mob variants) but still get eggs.
        for (String variant : new String[] { "umirongolem", "umsnowgolem" }) {
            ItemStack egg = new ItemStack(Items.SPAWN_EGG);
            ItemMonsterPlacer.applyEntityIdToItemStack(egg, new ResourceLocation(_UtilityMobs.MODID, variant));
            list.add(egg);
        }
        // The Patchouli guide book and the two target books.
        ItemStack guide = GuideBook.stack();
        if (guide != null && !guide.isEmpty()) {
            list.add(guide);
        }
        list.add(TargetHelper.book(0));
        list.add(TargetHelper.book(1));
    }
}
