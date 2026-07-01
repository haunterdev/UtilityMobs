package toast.utilityMobs;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import vazkii.patchouli.api.PatchouliAPI;

/**
    Glue for the Patchouli guide book ("utilitymobs:guide"). Provides the book
    stack, a server-side open helper, and the config-gated give-on-first-join handler.
 */
public final class GuideBook
{
    // Patchouli book id: <namespace>:<book folder under assets/<ns>/patchouli_books/>.
    public static final String BOOK_ID = "utilitymobs:guide";
    public static final ResourceLocation BOOK_RL = new ResourceLocation(BOOK_ID);
    // Key on the player's PERSISTED NBT marking the book was already granted (survives death/dim change).
    private static final String GIVEN_TAG = "utilitymobs_book_given";

    public GuideBook() {}

    // A fresh guide-book stack (Patchouli guide_book item carrying this book's id).
    public static ItemStack stack() {
        return PatchouliAPI.instance.getBookStack(BOOK_ID);
    }

    // Server-side: open the guide book GUI for a player.
    public static void open(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            PatchouliAPI.instance.openBookGUI((EntityPlayerMP) player, BOOK_RL);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Properties.getBoolean(Properties.GENERAL, "give_book_on_first_join")) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        NBTTagCompound persist = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persist.getBoolean(GIVEN_TAG)) return;

        ItemStack book = GuideBook.stack();
        if (!book.isEmpty()) {
            if (!player.inventory.addItemStackToInventory(book)) {
                player.dropItem(book, false);
            }
        }
        persist.setBoolean(GIVEN_TAG, true);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persist);
    }
}
