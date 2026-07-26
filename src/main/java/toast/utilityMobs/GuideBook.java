package toast.utilityMobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import vazkii.patchouli.api.PatchouliAPI;

/**
 * Glue for the Patchouli guide book ("utilitymobs:guide"). Provides the book stack, a server-side
 * open helper, and the config-gated give-on-first-join handler.
 *
 * <p>Patchouli is an optional dependency (mods.toml marks it {@code mandatory=false}), and its API
 * jar is built for exactly that: {@link PatchouliAPI#get()} hands back a no-op stub when the mod is
 * absent, so every call here is safe without a mod-loaded guard. The stub returns an empty stack,
 * which the give-on-join handler already skips.
 */
public final class GuideBook
{
    // Patchouli book id: <namespace>:<book folder under assets/<ns>/patchouli_books/>.
    public static final String BOOK_ID = "utilitymobs:guide";
    public static final ResourceLocation BOOK_RL = new ResourceLocation(BOOK_ID);
    // Key on the player's PERSISTED NBT marking the book was already granted (survives death/dim change).
    private static final String GIVEN_TAG = "utilitymobs_book_given";

    public GuideBook() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // A fresh guide-book stack (Patchouli guide_book item carrying this book's id).
    public static ItemStack stack() {
        return PatchouliAPI.get().getBookStack(GuideBook.BOOK_RL);
    }

    // Server-side: open the guide book GUI for a player.
    public static void open(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PatchouliAPI.get().openBookGUI(serverPlayer, GuideBook.BOOK_RL);
        }
    }

    /// Client-side: open the guide book from a screen. Only the API is touched, so this stays callable
    /// even when Patchouli is missing - it just does nothing.
    public static void openClient() {
        PatchouliAPI.get().openBookGUI(GuideBook.BOOK_RL);
    }

    /**
     * Whether a GUI should draw its "?" help button. The config option is the player's switch; the
     * ModList check is what the option's own description promises - with no Patchouli installed there
     * is no book to open, so the button is hidden either way rather than being a dead control.
     */
    public static boolean showHelpButton() {
        return Properties.getBoolean(Properties.GENERAL, "show_help_button")
            && net.minecraftforge.fml.ModList.get().isLoaded("patchouli");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Properties.getBoolean(Properties.GENERAL, "give_book_on_first_join")) return;
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        CompoundTag persist = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persist.getBoolean(GuideBook.GIVEN_TAG)) return;

        ItemStack book = GuideBook.stack();
        if (!book.isEmpty()) {
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
        persist.putBoolean(GuideBook.GIVEN_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persist);
    }
}
