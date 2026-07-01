package toast.utilityMobs.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.input.Keyboard;

/**
    A deliberately small book editor for the Utility Mobs target-list books. Vanilla's GuiScreenBook
    only appends/backspaces at the end of a page with no cursor, so you cannot edit an earlier line
    without deleting everything after it. This screen adds a movable text cursor - Left/Right/Up/Down,
    Home/End, plus insert/delete at the cursor - so individual lines can be edited in place. It keeps
    everything else minimal: pages are split on '\n' and drawn line-by-line (the list entries are short
    ids that never need word-wrap), and the edited book is pushed to the server when the screen closes
    (ESC or Done), which our save-on-exit tick then parses.

    Installed by swapping out vanilla's GuiScreenBook via {@link OpenHandler} whenever the player opens
    a writable target book (one carrying the "umt" tag). Registered in ClientProxy.
 */
public class GuiTargetBookEditor extends GuiScreen {

    private static final ResourceLocation BOOK_TEXTURES = new ResourceLocation("textures/gui/book.png");
    private static final int IMG_W = 192;
    private static final int MAX_PAGE_CHARS = 256;
    private static final int TEXT_LEFT_PAD = 36;
    private static final int TEXT_TOP = 34;
    private static final int TEXT_WIDTH = 116;

    private final EntityPlayer editingPlayer;
    private final ItemStack book;
    private NBTTagList pages;
    private int currPage;
    private int cursor;
    private int updateCount;
    private boolean modified;

    private GuiButton buttonDone;
    private GuiButton buttonNext;
    private GuiButton buttonPrev;

    public GuiTargetBookEditor(EntityPlayer player, ItemStack book) {
        this.editingPlayer = player;
        this.book = book;
        if (book.hasTagCompound() && book.getTagCompound().hasKey("pages")) {
            this.pages = book.getTagCompound().getTagList("pages", new NBTTagString("").getId()).copy();
        }
        if (this.pages == null || this.pages.tagCount() < 1) {
            this.pages = new NBTTagList();
            this.pages.appendTag(new NBTTagString(""));
        }
        this.cursor = this.page().length();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        int i = (this.width - IMG_W) / 2;
        this.buttonDone = this.addButton(new GuiButton(0, this.width / 2 - 100, 196, 200, 20, I18n.format("gui.done")));
        this.buttonNext = this.addButton(new PageButton(1, i + 120, 156, true));
        this.buttonPrev = this.addButton(new PageButton(2, i + 38, 156, false));
        this.updateButtons();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ++this.updateCount;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        // Save on exit (ESC or Done both route here), matching the rest of the target book flow.
        this.sendBookToServer();
    }

    private void updateButtons() {
        this.buttonNext.visible = true; // can always add a page at the end
        this.buttonPrev.visible = this.currPage > 0;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled)
            return;
        if (button.id == 0) {
            this.mc.displayGuiScreen(null); // triggers onGuiClosed -> save
        }
        else if (button.id == 1) {
            if (this.currPage < this.pages.tagCount() - 1) {
                this.currPage++;
            }
            else if (this.pages.tagCount() < 50) {
                this.pages.appendTag(new NBTTagString(""));
                this.currPage++;
                this.modified = true;
            }
            this.cursor = this.page().length();
            this.updateButtons();
        }
        else if (button.id == 2 && this.currPage > 0) {
            this.currPage--;
            this.cursor = this.page().length();
            this.updateButtons();
        }
    }

    // --- page text helpers -------------------------------------------------

    private String page() {
        return this.currPage >= 0 && this.currPage < this.pages.tagCount() ? this.pages.getStringTagAt(this.currPage) : "";
    }

    private void setPage(String text) {
        if (this.currPage >= 0 && this.currPage < this.pages.tagCount()) {
            this.pages.set(this.currPage, new NBTTagString(text));
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
            this.wrapLine(lines[ll], TEXT_WIDTH, ll, rows);
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
                int cw = this.fontRenderer.getCharWidth(ch);
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
                return new int[] { r, this.fontRenderer.getStringWidth(lines[line].substring(row[1], col)) };
            }
        }
        return new int[] { Math.max(0, rows.size() - 1), 0 };
    }

    private void insert(String text) {
        String s = this.page();
        if (s.length() + text.length() > MAX_PAGE_CHARS)
            return;
        this.setPage(s.substring(0, this.cursor) + text + s.substring(this.cursor));
        this.cursor += text.length();
    }

    // --- input -------------------------------------------------------------

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode); // ESC closes (then onGuiClosed saves)
        String s = this.page();
        switch (keyCode) {
            case Keyboard.KEY_LEFT:
                this.cursor = Math.max(0, this.cursor - 1);
                return;
            case Keyboard.KEY_RIGHT:
                this.cursor = Math.min(s.length(), this.cursor + 1);
                return;
            case Keyboard.KEY_UP: {
                int[] lc = this.lineColOf(s, this.cursor);
                if (lc[0] > 0)
                    this.cursor = this.indexOf(s, lc[0] - 1, lc[1]);
                return;
            }
            case Keyboard.KEY_DOWN: {
                int[] lc = this.lineColOf(s, this.cursor);
                if (lc[0] < this.lineCount(s) - 1)
                    this.cursor = this.indexOf(s, lc[0] + 1, lc[1]);
                return;
            }
            case Keyboard.KEY_HOME: {
                int[] lc = this.lineColOf(s, this.cursor);
                this.cursor = this.indexOf(s, lc[0], 0);
                return;
            }
            case Keyboard.KEY_END: {
                int[] lc = this.lineColOf(s, this.cursor);
                this.cursor = this.indexOf(s, lc[0], Integer.MAX_VALUE);
                return;
            }
            case Keyboard.KEY_BACK:
                if (this.cursor > 0) {
                    this.setPage(s.substring(0, this.cursor - 1) + s.substring(this.cursor));
                    this.cursor--;
                }
                return;
            case Keyboard.KEY_DELETE:
                if (this.cursor < s.length()) {
                    this.setPage(s.substring(0, this.cursor) + s.substring(this.cursor + 1));
                }
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                this.insert("\n");
                return;
            default:
                break;
        }
        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            this.insert(GuiScreen.getClipboardString());
        }
        else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            this.insert(Character.toString(typedChar));
        }
    }

    // --- rendering ---------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BOOK_TEXTURES);
        int i = (this.width - IMG_W) / 2;
        this.drawTexturedModalRect(i, 2, 0, 0, IMG_W, IMG_W);

        String indicator = I18n.format("book.pageIndicator", this.currPage + 1, this.pages.tagCount());
        this.fontRenderer.drawString(indicator, i - this.fontRenderer.getStringWidth(indicator) + IMG_W - 44, 18, 0);

        String s = this.page();
        int textLeft = i + TEXT_LEFT_PAD;
        int fh = this.fontRenderer.FONT_HEIGHT;
        String[] lines = s.split("\n", -1);
        // Wrap each logical line to the page width so prose pages don't run off the edge. Each row is
        // {logicalLine, colStart, length}; wrapping preserves every character so cursor indices stay exact.
        List<int[]> rows = this.buildRows(lines);

        for (int r = 0; r < rows.size(); r++) {
            int[] row = rows.get(r);
            this.fontRenderer.drawString(lines[row[0]].substring(row[1], row[1] + row[2]), textLeft, TEXT_TOP + r * fh, 0);
        }

        // Blinking cursor at its wrapped row + column.
        if (this.updateCount / 6 % 2 == 0) {
            int[] pos = this.cursorVisualPos(lines, rows); // {rowIndex, pixelX}
            this.fontRenderer.drawString(TextFormatting.BLACK + "_", textLeft + pos[1], TEXT_TOP + pos[0] * fh, 0);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // --- networking --------------------------------------------------------

    private void sendBookToServer() {
        if (!this.modified || this.pages == null)
            return;
        // Trim trailing empty pages, mirroring vanilla GuiScreenBook.
        while (this.pages.tagCount() > 1 && this.pages.getStringTagAt(this.pages.tagCount() - 1).isEmpty()) {
            this.pages.removeTag(this.pages.tagCount() - 1);
        }
        if (this.book.hasTagCompound()) {
            this.book.getTagCompound().setTag("pages", this.pages);
        }
        else {
            this.book.setTagInfo("pages", this.pages);
        }
        try {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeItemStack(this.book);
            this.mc.getConnection().sendPacket(new CPacketCustomPayload("MC|BEdit", buf));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Book-style page-turn arrow, drawn from the book texture (matches vanilla's look). */
    static class PageButton extends GuiButton {
        private final boolean forward;

        PageButton(int id, int x, int y, boolean forward) {
            super(id, x, y, 23, 13, "");
            this.forward = forward;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible)
                return;
            boolean hover = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BOOK_TEXTURES);
            int u = hover ? 23 : 0;
            int v = this.forward ? 192 : 205;
            this.drawTexturedModalRect(this.x, this.y, u, v, 23, 13);
        }
    }

    /** Swaps vanilla's book editor for this cursor-capable one when a writable target book is opened. */
    public static class OpenHandler {
        @SubscribeEvent
        public void onGuiOpen(GuiOpenEvent event) {
            if (!(event.getGui() instanceof GuiScreenBook))
                return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null)
                return;
            for (EnumHand hand : EnumHand.values()) {
                ItemStack held = mc.player.getHeldItem(hand);
                if (!held.isEmpty() && held.getItem() == Items.WRITABLE_BOOK
                        && held.getTagCompound() != null && held.getTagCompound().hasKey("umt")) {
                    event.setGui(new GuiTargetBookEditor(mc.player, held));
                    return;
                }
            }
        }
    }
}
