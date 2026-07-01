package vazkii.patchouli.client.book.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;
import vazkii.patchouli.common.book.Book;

/**
    Patchouli's category landing page hardcodes {@link GuiBookEntryList#ENTRIES_IN_FIRST_PAGE} (11)
    entries on the first page; the turrets category has 12 entries, so the 12th (Killer) spilled onto
    a second page that required flipping forward to see. This subclass - deliberately declared in
    Patchouli's own package so it can touch GuiBookEntryList's package-private list/page state - lets
    the first page hold the 12th entry and collapses the now-empty second page.

    The referenced Patchouli members (book, page, maxpages, bookLeft, bookTop, visibleEntries,
    dependentButtons, buildEntryButtons, GuiBookCategory.category) are Patchouli's own names and are
    NOT remapped by reobf, so this compiles and runs identically in dev and in a packaged jar. The
    OpenHandler swaps the stock GUI for this one; it is registered in ClientProxy.
 */
public class GuiTurretCategory extends GuiBookCategory {

    public static final ResourceLocation TURRETS = new ResourceLocation("utilitymobs", "turrets");

    public GuiTurretCategory(Book book, BookCategory category) {
        super(book, category);
    }

    @Override
    void buildEntryButtons() {
        super.buildEntryButtons();
        // Only act when all 12 entries are visible (no active search filter narrowing the list) and
        // we are on the landing page. Reuse super's filtering/layout for entries 0-10, then append
        // the 12th directly under the 11th and force a single page so the forward arrow disappears.
        if (this.page == 0 && this.visibleEntries.size() == GuiBookEntryList.ENTRIES_IN_FIRST_PAGE + 1) {
            int index = GuiBookEntryList.ENTRIES_IN_FIRST_PAGE; // 11 -> the 12th entry
            BookEntry entry = this.visibleEntries.get(index);
            // Matches super's page-0 placement: addEntryButtons(141, 38, 0, 11) => y = top + 38 + i*11.
            GuiButtonEntry button = new GuiButtonEntry(this,
                    this.bookLeft + 141, this.bookTop + 38 + index * 11, entry, index);
            this.buttonList.add(button);
            this.dependentButtons.add(button);
            this.maxpages = 1;
        }
    }

    /** Replaces Patchouli's stock turrets-category page with the 12-entry variant. */
    public static class OpenHandler {
        @SubscribeEvent
        public void onGuiOpen(GuiOpenEvent event) {
            GuiScreen gui = event.getGui();
            // Exact-class check so we never re-wrap our own GuiTurretCategory (infinite swap).
            if (gui != null && gui.getClass() == GuiBookCategory.class) {
                GuiBookCategory cat = (GuiBookCategory) gui;
                if (TURRETS.equals(cat.category.getResource())) {
                    event.setGui(new GuiTurretCategory(cat.book, cat.category));
                }
            }
        }
    }
}
