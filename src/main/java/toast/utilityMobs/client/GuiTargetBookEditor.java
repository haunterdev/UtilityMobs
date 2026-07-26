package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * A deliberately small book editor for the Utility Mobs target-list books. Vanilla's book editor
 * only appends/backspaces at the end of a page with no cursor, so you cannot edit an earlier line
 * without deleting everything after it. This screen adds a movable text cursor - Left/Right/Up/Down,
 * Home/End, plus insert/delete at the cursor - so individual lines can be edited in place. It keeps
 * everything else minimal: pages are split on '\n' and drawn line-by-line (the list entries are short
 * ids that never need word-wrap), and the edited book is pushed to the server when the screen closes
 * (ESC or Done), which our save-on-exit tick then parses.
 *
 * <p>Installed by swapping out vanilla's BookEditScreen via {@link OpenHandler} whenever the player
 * opens a writable target book (one carrying the "umt" tag). Registered in {@link ClientSetup}.
 *
 * <p>Two 1.20.1 changes show through. 1.12.2 shipped the page text as an NBT list inside a hand-built
 * "MC|BEdit" custom payload; ServerboundEditBookPacket now carries the pages as plain strings and an
 * inventory slot, so the pages are held as a String list and the packet does the rest - the server
 * rewrites only the "pages" tag, leaving our "umt" data intact exactly as the old payload did. And the
 * page-turn arrows no longer need a hand-drawn button: vanilla's PageButton is public and draws from
 * the same book texture the 1.12.2 inner class blitted by hand.
 */
public class GuiTargetBookEditor extends Screen {

    private static final int IMG_W = 192;
    private static final int MAX_PAGE_CHARS = 256;
    private static final int TEXT_LEFT_PAD = 36;
    private static final int TEXT_TOP = 34;
    private static final int TEXT_WIDTH = 116;

    private final Player editingPlayer;
    private final ItemStack book;
    private final InteractionHand hand;
    private final List<String> pages = new ArrayList<String>();
    private int currPage;
    private int cursor;
    private int updateCount;
    private boolean modified;

    private Button buttonNext;
    private Button buttonPrev;

    public GuiTargetBookEditor(Player player, ItemStack book, InteractionHand hand) {
        super(Component.empty());
        this.editingPlayer = player;
        this.book = book;
        this.hand = hand;
        if (book.getTag() != null && book.getTag().contains("pages")) {
            ListTag list = book.getTag().getList("pages", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.pages.add(list.getString(i));
            }
        }
        if (this.pages.isEmpty()) {
            this.pages.add("");
        }
        this.cursor = this.page().length();
    }

    @Override
    protected void init() {
        int i = (this.width - GuiTargetBookEditor.IMG_W) / 2;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, 196, 200, 20).build());
        this.buttonNext = this.addRenderableWidget(new PageButton(i + 120, 156, true, b -> this.nextPage(), true));
        this.buttonPrev = this.addRenderableWidget(new PageButton(i + 38, 156, false, b -> this.prevPage(), true));
        this.updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        ++this.updateCount;
    }

    @Override
    public void removed() {
        // Save on exit (ESC or Done both route here), matching the rest of the target book flow.
        this.sendBookToServer();
    }

    private void updateButtons() {
        this.buttonNext.visible = true; // can always add a page at the end
        this.buttonPrev.visible = this.currPage > 0;
    }

    private void nextPage() {
        if (this.currPage < this.pages.size() - 1) {
            this.currPage++;
        }
        else if (this.pages.size() < 50) {
            this.pages.add("");
            this.currPage++;
            this.modified = true;
        }
        this.cursor = this.page().length();
        this.updateButtons();
    }

    private void prevPage() {
        if (this.currPage > 0) {
            this.currPage--;
            this.cursor = this.page().length();
            this.updateButtons();
        }
    }

    // --- page text helpers -------------------------------------------------

    private String page() {
        return this.currPage >= 0 && this.currPage < this.pages.size() ? this.pages.get(this.currPage) : "";
    }

    private void setPage(String text) {
        if (this.currPage >= 0 && this.currPage < this.pages.size()) {
            this.pages.set(this.currPage, text);
            this.modified = true;
        }
    }

    private int[] lineColOf(String s, int cur) {
        int line = 0;
        int col = 0;
        for (int i = 0; i < cur && i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                line++;
                col = 0;
            }
            else {
                col++;
            }
        }
        return new int[] { line, col };
    }

    private int indexOf(String s, int line, int col) {
        int idx = 0;
        int cur = 0;
        while (cur < line && idx < s.length()) {
            if (s.charAt(idx) == '\n') {
                cur++;
            }
            idx++;
        }
        int c = 0;
        while (c < col && idx < s.length() && s.charAt(idx) != '\n') {
            idx++;
            c++;
        }
        return idx;
    }

    private int lineCount(String s) {
        int n = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') n++;
        return n;
    }

    // Wraps every logical line to the page width, yielding visual rows {logicalLine, colStart, length}.
    private List<int[]> buildRows(String[] lines) {
        List<int[]> rows = new ArrayList<int[]>();
        for (int ll = 0; ll < lines.length; ll++) {
            this.wrapLine(lines[ll], GuiTargetBookEditor.TEXT_WIDTH, ll, rows);
        }
        return rows;
    }

    // Splits one logical line into width-bounded rows, breaking after spaces when possible and never
    // dropping a character, so a row's text == line.substring(colStart, colStart+length).
    private void wrapLine(String line, int width, int logicalIndex, List<int[]> rows) {
        int len = line.length();
        if (len == 0) {
            rows.add(new int[] { logicalIndex, 0, 0 });
            return;
        }
        int start = 0;
        while (start < len) {
            int end = start;
            int w = 0;
            int lastSpace = -1;
            while (end < len) {
                char ch = line.charAt(end);
                int cw = this.font.width(String.valueOf(ch));
                if (w + cw > width && end > start)
                    break;
                w += cw;
                if (ch == ' ')
                    lastSpace = end;
                end++;
            }
            int breakAt = end;
            if (end < len && lastSpace >= start)
                breakAt = lastSpace + 1; // keep the space on this row
            if (breakAt <= start)
                breakAt = start + 1;
            rows.add(new int[] { logicalIndex, start, breakAt - start });
            start = breakAt;
        }
    }

    // Maps the raw cursor index onto the wrapped rows: returns {rowIndex, pixelX within the text area}.
    private int[] cursorVisualPos(String[] lines, List<int[]> rows) {
        int[] lc = this.lineColOf(this.page(), this.cursor);
        int line = lc[0];
        int col = lc[1];
        for (int r = 0; r < rows.size(); r++) {
            int[] row = rows.get(r);
            if (row[0] != line)
                continue;
            if (col >= row[1] && col <= row[1] + row[2]) {
                return new int[] { r, this.font.width(lines[line].substring(row[1], col)) };
            }
        }
        return new int[] { Math.max(0, rows.size() - 1), 0 };
    }

    private void insert(String text) {
        String s = this.page();
        if (s.length() + text.length() > GuiTargetBookEditor.MAX_PAGE_CHARS)
            return;
        this.setPage(s.substring(0, this.cursor) + text + s.substring(this.cursor));
        this.cursor += text.length();
    }

    // --- input -------------------------------------------------------------

    /**
     * 1.12.2 handled navigation, editing and typing in one keyTyped. 1.20.1 splits them: keyPressed sees
     * key codes (so the arrows/Home/End/Backspace/Delete/Enter live here) and charTyped sees the typed
     * character (so plain text insertion lives there). The behaviour of each branch is unchanged.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String s = this.page();
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT:
                this.cursor = Math.max(0, this.cursor - 1);
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                this.cursor = Math.min(s.length(), this.cursor + 1);
                return true;
            case GLFW.GLFW_KEY_UP: {
                int[] lc = this.lineColOf(s, this.cursor);
                if (lc[0] > 0)
                    this.cursor = this.indexOf(s, lc[0] - 1, lc[1]);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN: {
                int[] lc = this.lineColOf(s, this.cursor);
                if (lc[0] < this.lineCount(s) - 1)
                    this.cursor = this.indexOf(s, lc[0] + 1, lc[1]);
                return true;
            }
            case GLFW.GLFW_KEY_HOME: {
                int[] lc = this.lineColOf(s, this.cursor);
                this.cursor = this.indexOf(s, lc[0], 0);
                return true;
            }
            case GLFW.GLFW_KEY_END: {
                int[] lc = this.lineColOf(s, this.cursor);
                this.cursor = this.indexOf(s, lc[0], Integer.MAX_VALUE);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE:
                if (this.cursor > 0) {
                    this.setPage(s.substring(0, this.cursor - 1) + s.substring(this.cursor));
                    this.cursor--;
                }
                return true;
            case GLFW.GLFW_KEY_DELETE:
                if (this.cursor < s.length()) {
                    this.setPage(s.substring(0, this.cursor) + s.substring(this.cursor + 1));
                }
                return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                this.insert("\n");
                return true;
            default:
                break;
        }
        if (Screen.isPaste(keyCode)) {
            this.insert(this.minecraft.keyboardHandler.getClipboard());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // ESC closes (then removed() saves)
    }

    @Override
    public boolean charTyped(char typedChar, int modifiers) {
        if (SharedConstants.isAllowedChatCharacter(typedChar)) {
            this.insert(Character.toString(typedChar));
            return true;
        }
        return false;
    }

    // --- rendering ---------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - GuiTargetBookEditor.IMG_W) / 2;
        graphics.blit(BookViewScreen.BOOK_LOCATION, i, 2, 0, 0, GuiTargetBookEditor.IMG_W, GuiTargetBookEditor.IMG_W);

        String indicator = I18n.get("book.pageIndicator", this.currPage + 1, this.pages.size());
        graphics.drawString(this.font, indicator,
                i - this.font.width(indicator) + GuiTargetBookEditor.IMG_W - 44, 18, 0, false);

        String s = this.page();
        int textLeft = i + GuiTargetBookEditor.TEXT_LEFT_PAD;
        int fh = this.font.lineHeight;
        String[] lines = s.split("\n", -1);
        // Wrap each logical line to the page width so prose pages don't run off the edge. Each row is
        // {logicalLine, colStart, length}; wrapping preserves every character so cursor indices stay exact.
        List<int[]> rows = this.buildRows(lines);

        for (int r = 0; r < rows.size(); r++) {
            int[] row = rows.get(r);
            graphics.drawString(this.font, lines[row[0]].substring(row[1], row[1] + row[2]),
                    textLeft, GuiTargetBookEditor.TEXT_TOP + r * fh, 0, false);
        }

        // Blinking cursor at its wrapped row + column.
        if (this.updateCount / 6 % 2 == 0) {
            int[] pos = this.cursorVisualPos(lines, rows); // {rowIndex, pixelX}
            graphics.drawString(this.font, ChatFormatting.BLACK + "_",
                    textLeft + pos[1], GuiTargetBookEditor.TEXT_TOP + pos[0] * fh, 0, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    // --- networking --------------------------------------------------------

    private void sendBookToServer() {
        if (!this.modified)
            return;
        // Trim trailing empty pages, mirroring vanilla's book editor.
        while (this.pages.size() > 1 && this.pages.get(this.pages.size() - 1).isEmpty()) {
            this.pages.remove(this.pages.size() - 1);
        }
        // Keep the local copy in step so the held stack shows the edit before the server echoes back.
        ListTag list = new ListTag();
        for (String text : this.pages) {
            list.add(StringTag.valueOf(text));
        }
        this.book.addTagElement("pages", list);
        try {
            int slot = this.hand == InteractionHand.MAIN_HAND
                ? this.editingPlayer.getInventory().selected : Inventory.SLOT_OFFHAND;
            Minecraft.getInstance().getConnection().getConnection()
                .send(new ServerboundEditBookPacket(slot, this.pages, Optional.empty()));
        }
        catch (Exception ex) {
            toast.utilityMobs._UtilityMobs.debugException("Could not send the edited target book: " + ex.getMessage());
        }
    }

    /** Swaps vanilla's book editor for this cursor-capable one when a writable target book is opened. */
    public static class OpenHandler {
        @SubscribeEvent
        public void onScreenOpening(ScreenEvent.Opening event) {
            if (!(event.getScreen() instanceof BookEditScreen))
                return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = mc.player.getItemInHand(hand);
                if (!held.isEmpty() && held.is(Items.WRITABLE_BOOK)
                        && held.getTag() != null && held.getTag().contains("umt")) {
                    event.setNewScreen(new GuiTargetBookEditor(mc.player, held, hand));
                    return;
                }
            }
        }
    }
}
