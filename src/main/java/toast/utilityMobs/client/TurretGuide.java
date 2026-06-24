package toast.utilityMobs.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.gui.GuiBookCategory;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import toast.utilityMobs.GuideBook;
import toast.utilityMobs._UtilityMobs;

@SideOnly(Side.CLIENT)
public final class TurretGuide {

    /// The "Turret Upgrades" category (assets/.../categories/upgrades.json).
    private static final ResourceLocation UPGRADES = new ResourceLocation(_UtilityMobs.MODID, "upgrades");

    private TurretGuide() {}

    public static void openUpgrades() {
        Book book = BookRegistry.INSTANCE.books.get(GuideBook.BOOK_RL);
        if (book == null || book.contents == null) return;
        BookCategory category = book.contents.categories.get(UPGRADES);
        if (category == null) {
            // Category missing for some reason - fall back to the book landing rather than nothing.
            book.contents.openLexiconGui(new GuiBookLanding(book), false);
            return;
        }
        // Seed the landing page, then open the category pushing the landing so "back" returns to it.
        book.contents.openLexiconGui(new GuiBookLanding(book), false);
        book.contents.openLexiconGui(new GuiBookCategory(book, category), true);
    }
}