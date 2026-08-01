package toast.utilityMobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Every direct touch of the Patchouli API lives here, so that a game without Patchouli installed never
 * loads a class that names one of its types.
 *
 * <p>Patchouli is a soft dependency (1.12.2 issue #1.6): the guide book, its recipe, the creative-tab entry
 * and the '?' help buttons all disappear when it is absent, and the rest of the mod runs unchanged.
 *
 * <p>{@code mods.toml} marking the dependency {@code mandatory=false} is necessary but NOT sufficient. It
 * only stops Forge refusing to load; it does not put Patchouli's classes on the classpath. {@code GuideBook}
 * used to call {@code PatchouliAPI.get()} directly on the assumption that the API hands back a no-op stub
 * when the mod is missing, which is only true when the API classes themselves are present. Without the mod
 * they are not, and the first login threw {@code NoClassDefFoundError: vazkii/patchouli/api/PatchouliAPI}
 * out of the give-book-on-join handler, taking the server tick loop with it.
 */
public final class PatchouliCompat
{
    // Resolved once. ModList.isLoaded is cheap, but this is read from GUI init and on every login.
    private static boolean loaded;
    private static boolean resolved;

    private PatchouliCompat() {}

    public static boolean isLoaded() {
        if (!PatchouliCompat.resolved) {
            PatchouliCompat.loaded = ModList.get().isLoaded("patchouli");
            PatchouliCompat.resolved = true;
        }
        return PatchouliCompat.loaded;
    }

    /** A guide-book stack, or an empty stack when Patchouli is not installed. */
    public static ItemStack bookStack(ResourceLocation book) {
        if (!PatchouliCompat.isLoaded())
            return ItemStack.EMPTY;
        return PatchouliCompat.Api.bookStack(book);
    }

    /** Server-side: opens the guide book for a player. No-op without Patchouli. */
    public static void openBook(ServerPlayer player, ResourceLocation book) {
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
     * The only class in the mod that names a Patchouli type on the common side. Kept as a nested class with
     * no static state so the JVM does not load (and therefore does not have to resolve) any of it until one
     * of the guarded methods above actually calls in.
     */
    private static final class Api
    {
        private Api() {}

        static ItemStack bookStack(ResourceLocation book) {
            return vazkii.patchouli.api.PatchouliAPI.get().getBookStack(book);
        }

        static void openBook(ServerPlayer player, ResourceLocation book) {
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(player, book);
        }

        static void openBookClient(ResourceLocation book) {
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(book);
        }
    }
}
