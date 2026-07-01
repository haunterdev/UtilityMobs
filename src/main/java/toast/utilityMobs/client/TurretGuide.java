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

/**
    Opens the guide book straight to the "Turret Upgrades" category page (the upgrade entry grid),
    used by the turret GUI's "?" help button.

    The turret GUI is a GuiContainer, not a Patchouli GuiBook, so opening a category GUI directly from
    it leaves Patchouli's back-arrow stack empty - the page has no way back (the old behaviour). We seed
    the book's landing page first, then open the category with {@code push = true}; that pushes the
    landing onto Patchouli's gui stack so the category's back arrow returns to the book root and full
    navigation is preserved.

    Lives in the client package and only touches public Patchouli members (BookRegistry.INSTANCE.books,
    Book.contents, BookContents.openLexiconGui/categories, the GuiBook* constructors) - the same surface
    {@link UMPatchouliLinks} already uses.
 */
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
