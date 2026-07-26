package toast.utilityMobs.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import vazkii.patchouli.client.book.ClientBookRegistry;

import toast.utilityMobs._UtilityMobs;

/**
 * Every client-side touch of Patchouli's INTERNALS lives behind this one class.
 *
 * <p>Patchouli is an optional dependency. Its API jar ships a no-op stub so {@code PatchouliAPI} is
 * always safe to call, but the classes below (ClientBookRegistry, the multiblock handler, the page
 * base classes) are implementation, not API - naming them at all when the mod is absent would be a
 * NoClassDefFoundError. Keeping them in a class that {@link ClientSetup} only loads after checking
 * ModList means the JVM never resolves them on a Patchouli-less install.
 *
 * <p>1.12.2 had no such split: everything was wired unconditionally in ClientProxy, because Patchouli
 * was a hard dependency there.
 */
public final class UMPatchouli {
    private UMPatchouli() {}

    private static final ResourceLocation BUILD_GUIDE =
        new ResourceLocation(_UtilityMobs.MODID, "build_guide");
    private static final ResourceLocation ENTITY_CAROUSEL =
        new ResourceLocation(_UtilityMobs.MODID, "entity_carousel");

    // Known limitation: the turrets category holds 12 entries and Patchouli only puts
    // ENTRIES_IN_FIRST_PAGE (11) on the landing page, so the 12th (Killer) spills onto a second page.
    // 1.12.2 worked around this with GuiTurretCategory, a subclass declared inside
    // vazkii.patchouli.client.book.gui so it could reach GuiBookEntryList's package-private paging
    // state. That state is still package-private in 1.20.1 and the trick still works at the language
    // level, but declaring our own class in Patchouli's package now means two modules owning one
    // package, which Forge's module layer rejects at load with a split-package error. Without a mixin
    // there is no way to reach it, so the category renders as stock Patchouli: correct, one page deeper.

    public static void init() {
        // Custom page types, keyed by the "type" the book entries name.
        ClientBookRegistry.INSTANCE.pageTypes.put(UMPatchouli.BUILD_GUIDE, PageBuildGuide.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(UMPatchouli.ENTITY_CAROUSEL, PageEntityCarousel.class);
        MinecraftForge.EVENT_BUS.register(new MultiblockBuildClearHandler());
    }

    /// The turret GUI's "?" button. Routed through here so the screen never names a Patchouli class.
    public static void openTurretUpgrades() {
        TurretGuide.openUpgrades();
    }
}
