package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor for the two global attack lists (attack_blacklist / attack_whitelist), which are the only
 * list-valued options in the config. One entry per line, exactly the format the config file uses:
 * an entity registry id, the tokens Player or Hostiles, or a fully-qualified class name.
 *
 * <p>Nothing is committed to the config here. The edited list is handed back to the calling row,
 * which writes it only when the config screen's Done button is pressed.
 */
public class GuiStringListEditor extends Screen {

    private static final int VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 22;

    private final Screen parent;
    private final String field;
    private final Consumer<List<String>> onDone;
    private final List<String> entries;
    private final List<EditBox> boxes = new ArrayList<>();
    private int scroll;

    public GuiStringListEditor(Screen parent, String field, List<String> initial, Consumer<List<String>> onDone) {
        super(Component.literal(field));
        this.parent = parent;
        this.field = field;
        this.onDone = onDone;
        this.entries = new ArrayList<>(initial);
    }

    @Override
    protected void init() {
        this.boxes.clear();
        int left = this.width / 2 - 150;
        int top = 40;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int index = this.scroll + i;
            EditBox box = new EditBox(this.font, left, top + i * ROW_HEIGHT, 250, 20,
                Component.literal(this.field + " " + index));
            box.setMaxLength(256);
            box.setValue(index < this.entries.size() ? this.entries.get(index) : "");
            box.setResponder(text -> this.set(index, text));
            this.addRenderableWidget(box);
            this.boxes.add(box);

            this.addRenderableWidget(Button.builder(Component.literal("x"), b -> this.remove(index))
                .bounds(left + 254, top + i * ROW_HEIGHT, 20, 20).build());
        }

        int y = this.height - 27;
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.config.list_add"),
            b -> this.addEntry()).bounds(this.width / 2 - 154, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
            b -> this.minecraft.setScreen(this.parent)).bounds(this.width / 2 - 50, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> this.finish()).bounds(this.width / 2 + 54, y, 100, 20).build());
    }

    /// Grows the backing list on demand so typing into a trailing blank row appends rather than
    /// throwing. Blank rows are dropped again on Done.
    private void set(int index, String text) {
        while (this.entries.size() <= index) {
            this.entries.add("");
        }
        this.entries.set(index, text);
    }

    private void remove(int index) {
        if (index < this.entries.size()) {
            this.entries.remove(index);
        }
        this.rebuild();
    }

    private void addEntry() {
        this.entries.add("");
        // Keep the new row on screen.
        if (this.entries.size() > this.scroll + VISIBLE_ROWS) {
            this.scroll = this.entries.size() - VISIBLE_ROWS;
        }
        this.rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void finish() {
        List<String> cleaned = new ArrayList<>();
        for (String entry : this.entries) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        this.onDone.accept(cleaned);
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, this.entries.size() + 1 - VISIBLE_ROWS);
        int updated = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(delta)));
        if (updated != this.scroll) {
            this.scroll = updated;
            this.rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDirtBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
            Component.translatable("utilitymobs.config.list_hint"), this.width / 2, 24, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
