package toast.utilityMobs;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
    Every direct touch of the Patchouli API lives here, so that a game without Patchouli installed never
    loads a class that names one of its types.

    Patchouli is a soft dependency (issue #1.6): the guide book, its recipe, the creative-tab entry and the
    '?' help buttons all disappear when it is absent, and the rest of the mod runs unchanged.
 */
public final class PatchouliCompat
{
    // Resolved once in preInit. Loader.isModLoaded is cheap but this is read from GUI init and per-login.
    private static boolean loaded;
    private static boolean resolved;

    private PatchouliCompat() {}

    public static boolean isLoaded() {
        if (!PatchouliCompat.resolved) {
            PatchouliCompat.loaded = Loader.isModLoaded("patchouli");
            PatchouliCompat.resolved = true;
        }
        return PatchouliCompat.loaded;
    }

    /** A guide-book stack, or an empty stack when Patchouli is not installed. */
    public static ItemStack bookStack(String bookId) {
        if (!PatchouliCompat.isLoaded())
            return ItemStack.EMPTY;
        return PatchouliCompat.Api.bookStack(bookId);
    }

    /** Server-side: opens the guide book for a player. No-op without Patchouli. */
    public static void openBook(EntityPlayerMP player, ResourceLocation book) {
        if (!PatchouliCompat.isLoaded())
            return;
        PatchouliCompat.Api.openBook(player, book);
    }

    /** Client-side: opens the guide book for the local player. No-op without Patchouli. */
    public static void openBookClient(ResourceLocation book) {
        if (!PatchouliCompat.isLoaded())
            return;
        PatchouliCompat.Api.openBookClient(book);
    }

    /**
        The only class in the mod that names a Patchouli type on the common side. Kept as a nested class
        with no static state so the JVM does not load (and therefore does not have to resolve) any of it
        until one of the guarded methods above actually calls in.
     */
    private static final class Api
    {
        private Api() {}

        static ItemStack bookStack(String bookId) {
            return vazkii.patchouli.api.PatchouliAPI.instance.getBookStack(bookId);
        }

        static void openBook(EntityPlayerMP player, ResourceLocation book) {
            vazkii.patchouli.api.PatchouliAPI.instance.openBookGUI(player, book);
        }

        static void openBookClient(ResourceLocation book) {
            vazkii.patchouli.api.PatchouliAPI.instance.openBookGUI(book);
        }
    }
}
