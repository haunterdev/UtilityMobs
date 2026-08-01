package toast.utilityMobs.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
    Every client-side Patchouli registration, in one class that is only ever loaded when Patchouli is
    actually installed. Several of these types extend Patchouli classes, so merely naming them from
    ClientProxy would blow up on a game without it. Patchouli is a soft dependency (issue #1.6).
 */
@SideOnly(Side.CLIENT)
public final class PatchouliClientHooks
{
    private PatchouliClientHooks() {}

    public static void install() {
        // Custom Patchouli page type for build guides - adds offset/scale knobs the stock
        // "multiblock" page lacks. The pageTypes map only ever gains the built-in defaults, so
        // inserting here (regardless of order vs Patchouli's own init) is safe.
        vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes.put(
                "utilitymobs:build_guide", PageBuildGuide.class);
        vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes.put(
                "utilitymobs:entity_carousel", PageEntityCarousel.class);
        // Adds the $(lc:category) book-text link so entries can point at a whole category grid.
        UMPatchouliLinks.register();
        // Fit all 12 turret entries on the category landing page (Patchouli caps it at 11, spilling
        // the Killer turret onto a second page). Swaps the stock GUI for our 12-entry variant.
        MinecraftForge.EVENT_BUS.register(
                new vazkii.patchouli.client.book.gui.GuiTurretCategory.OpenHandler());
        // Dismiss Patchouli's projected multiblock ghost once the golem actually spawns (our builds
        // become an entity the instant they complete, so Patchouli's own "all blocks present" clear
        // never fires). See MultiblockBuildClearHandler.
        MinecraftForge.EVENT_BUS.register(new MultiblockBuildClearHandler());
    }
}
