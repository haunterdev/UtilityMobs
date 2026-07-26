package toast.utilityMobs.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import toast.utilityMobs.Properties;
import toast.utilityMobs.setup.WizardState;

/**
 * The mod's config screen, plus a button that reopens the first-launch setup wizard.
 *
 * <p>1.12.2 got the whole screen for free: it subclassed Forge's GuiConfig, which walked the
 * Configuration object and generated a widget per property, organised as a landing page of category
 * buttons that each opened a sub-screen. Forge 1.20.1 deleted that UI outright, so the layout, the
 * widgets and the edit/validate/save cycle are written out here, keeping that two-level shape.
 *
 * <p>The option list comes from {@link Properties}, so this screen never needs updating when an
 * option is added: declare it there and it appears here, under its category.
 *
 * <p>Nothing is written to disk until Done is pressed on a category page. Cancel discards edits.
 */
public class GuiUtilityMobsConfig extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 100;

    private final Screen parent;
    /// Null on the landing page, otherwise the category whose options are being edited.
    private final String category;
    private OptionList list;

    public GuiUtilityMobsConfig(Screen parent) {
        this(parent, null);
    }

    private GuiUtilityMobsConfig(Screen parent, String category) {
        super(category == null
            ? Component.translatable("utilitymobs.config.title")
            : Component.literal(GuiUtilityMobsConfig.categoryLabel(category)));
        this.parent = parent;
        this.category = category;
    }

    /// "_general" is the internal name of the untitled top category, exactly as in 1.12.2.
    private static String categoryLabel(String category) {
        return Properties.GENERAL.equals(category) ? "general" : category;
    }

    @Override
    protected void init() {
        this.list = new OptionList(this, this.category);
        this.addWidget(this.list);

        if (this.category == null) {
            // Matches 1.12.2's compact button pinned clear of the centered title.
            this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.setup.reopen"),
                b -> this.minecraft.setScreen(new GuiSetupWizard(this, WizardState.engineer())))
                .bounds(4, 4, 110, 18).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 50, this.height - 27, 100, 20).build());
            return;
        }

        int y = this.height - 27;
        this.addRenderableWidget(Button.builder(Component.translatable("utilitymobs.config.reset"),
            b -> this.resetAll()).bounds(this.width / 2 - 154, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
            b -> this.minecraft.setScreen(this.parent)).bounds(this.width / 2 - 50, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> this.saveAndClose()).bounds(this.width / 2 + 54, y, 100, 20).build());
    }

    private void resetAll() {
        for (Row row : this.list.rows) {
            row.resetToDefault();
        }
    }

    /// Applies every pending edit in one batch, then writes the file once. Properties.save() reloads
    /// afterwards, so the cached hot-path statics pick the new values up immediately.
    private void saveAndClose() {
        for (Row row : this.list.rows) {
            row.apply();
        }
        Properties.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDirtBackground(graphics);
        this.list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    /// A single line of the scrolling list.
    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        /// Writes this row's pending value into the config.
        void apply() {}
        /// Restores the option's declared default into the widget, without saving.
        void resetToDefault() {}
    }

    /// A landing-page entry: one button per category, opening that category's page.
    private static final class CategoryButtonRow extends Row {
        private final Button button;

        CategoryButtonRow(GuiUtilityMobsConfig screen, String category, int optionCount) {
            this.button = Button.builder(
                Component.literal(GuiUtilityMobsConfig.categoryLabel(category)),
                b -> screen.minecraft.setScreen(new GuiUtilityMobsConfig(screen, category)))
                .bounds(0, 0, 200, 20).build();
            this.button.setTooltip(Tooltip.create(
                Component.translatable("utilitymobs.config.category_options", Integer.valueOf(optionCount))));
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.button.setX(left + width / 2 - 100);
            this.button.setY(top + 2);
            this.button.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }
    }

    /// An option row: its name on the left, one editing widget on the right, the config comment as a
    /// hover tooltip on that widget.
    private abstract static class OptionRow extends Row {
        final GuiUtilityMobsConfig screen;
        final Properties.Option option;

        OptionRow(GuiUtilityMobsConfig screen, Properties.Option option) {
            this.screen = screen;
            this.option = option;
        }

        abstract AbstractWidget widget();

        Component tooltip() {
            StringBuilder text = new StringBuilder();
            if (this.option.comment != null) {
                text.append(this.option.comment).append("\n\n");
            }
            text.append("Default: ").append(this.option.defaultValue);
            if (this.option.min != null && this.option.max != null) {
                text.append("\nRange: ").append(this.format(this.option.min))
                    .append(" to ").append(this.format(this.option.max));
            }
            return Component.literal(text.toString());
        }

        private String format(Double value) {
            if (this.option.type == Properties.OptionType.INT) {
                long rounded = value.longValue();
                // The two rarity options use Integer.MAX_VALUE as "no ceiling"; printing it is noise.
                return rounded == Integer.MAX_VALUE ? "no limit" : Long.toString(rounded);
            }
            return String.valueOf(value.doubleValue());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            AbstractWidget widget = this.widget();
            graphics.drawString(this.screen.font, this.option.field, left + 4, top + height / 2 - 4, 0xE0E0E0);
            widget.setX(left + width - WIDGET_WIDTH - 8);
            widget.setY(top + 2);
            widget.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget());
        }
    }

    private static final class BooleanRow extends OptionRow {
        private final Button button;
        private boolean value;

        BooleanRow(GuiUtilityMobsConfig screen, Properties.Option option) {
            super(screen, option);
            this.value = Properties.getBoolean(option.category, option.field);
            this.button = Button.builder(this.label(), b -> {
                this.value = !this.value;
                b.setMessage(this.label());
            }).bounds(0, 0, WIDGET_WIDTH, 20).build();
            this.button.setTooltip(Tooltip.create(this.tooltip()));
        }

        private Component label() {
            return this.value
                ? Component.literal("true").withStyle(ChatFormatting.GREEN)
                : Component.literal("false").withStyle(ChatFormatting.RED);
        }

        @Override
        AbstractWidget widget() {
            return this.button;
        }

        @Override
        void apply() {
            Properties.setValue(this.option.category, this.option.field, Boolean.valueOf(this.value));
        }

        @Override
        void resetToDefault() {
            this.value = ((Boolean) this.option.defaultValue).booleanValue();
            this.button.setMessage(this.label());
        }
    }

    /// Numeric options. A text box rather than a slider: two of these options use Integer.MAX_VALUE as
    /// their upper bound, which no slider can represent usefully.
    private static final class NumberRow extends OptionRow {
        private final EditBox box;

        NumberRow(GuiUtilityMobsConfig screen, Properties.Option option) {
            super(screen, option);
            this.box = new EditBox(screen.font, 0, 0, WIDGET_WIDTH, 20, Component.literal(option.field));
            this.box.setValue(this.current());
            this.box.setResponder(text -> this.box.setTextColor(this.parse(text) == null ? 0xFF5555 : 0xE0E0E0));
            this.box.setTooltip(Tooltip.create(this.tooltip()));
        }

        private String current() {
            return this.option.type == Properties.OptionType.INT
                ? Integer.toString(Properties.getInt(this.option.category, this.option.field))
                : Double.toString(Properties.getDouble(this.option.category, this.option.field));
        }

        /// Returns the in-range value, or null when the text is not a number or falls outside the
        /// declared range. An out-of-range value would be silently corrected by ForgeConfigSpec on the
        /// next load, so it is rejected here instead, while the box is still red and editable.
        private Object parse(String text) {
            try {
                if (this.option.type == Properties.OptionType.INT) {
                    int parsed = Integer.parseInt(text.trim());
                    if (parsed < this.option.min.intValue() || parsed > this.option.max.intValue())
                        return null;
                    return Integer.valueOf(parsed);
                }
                double parsed = Double.parseDouble(text.trim());
                if (parsed < this.option.min.doubleValue() || parsed > this.option.max.doubleValue())
                    return null;
                return Double.valueOf(parsed);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        AbstractWidget widget() {
            return this.box;
        }

        @Override
        void apply() {
            Object parsed = this.parse(this.box.getValue());
            if (parsed != null) {
                Properties.setValue(this.option.category, this.option.field, parsed);
            }
        }

        @Override
        void resetToDefault() {
            this.box.setValue(String.valueOf(this.option.defaultValue));
        }
    }

    /// The two global attack lists. Editing happens on its own screen, since a row cannot hold a
    /// multi-line editor.
    private static final class StringListRow extends OptionRow {
        private final Button button;
        private List<String> value;

        @SuppressWarnings("unchecked")
        StringListRow(GuiUtilityMobsConfig screen, Properties.Option option) {
            super(screen, option);
            Object raw = Properties.getProperty(option.category, option.field);
            this.value = raw instanceof List ? new ArrayList<>((List<String>) raw) : new ArrayList<>();
            this.button = Button.builder(this.label(), b -> screen.minecraft.setScreen(
                new GuiStringListEditor(screen, option.field, this.value, edited -> {
                    this.value = edited;
                    b.setMessage(this.label());
                    // Persist immediately rather than waiting for Done. Closing the editor returns to
                    // this screen via setScreen, which re-runs its init() and rebuilds every row from
                    // Properties - so a value held only in this row would be thrown away before Done
                    // could ever read it. Writing here also matches what /umblacklist and /umwhitelist
                    // do, so the two paths stay in step.
                    Properties.setList(option.category, option.field, edited);
                }))).bounds(0, 0, WIDGET_WIDTH, 20).build();
            this.button.setTooltip(Tooltip.create(this.tooltip()));
        }

        private Component label() {
            return Component.translatable("utilitymobs.config.edit_list", Integer.valueOf(this.value.size()));
        }

        @Override
        AbstractWidget widget() {
            return this.button;
        }

        @Override
        void apply() {
            Properties.setList(this.option.category, this.option.field, this.value);
        }

        @Override
        void resetToDefault() {
            this.value = new ArrayList<>();
            this.button.setMessage(this.label());
        }
    }

    private static final class OptionList extends ContainerObjectSelectionList<Row> {
        final List<Row> rows = new ArrayList<>();

        OptionList(GuiUtilityMobsConfig screen, String category) {
            super(screen.minecraft, screen.width, screen.height, 32, screen.height - 32, ROW_HEIGHT);
            if (category == null) {
                for (String name : Properties.categories()) {
                    int count = 0;
                    for (Properties.Option option : Properties.options()) {
                        if (option.category.equals(name)) {
                            count++;
                        }
                    }
                    this.add(new CategoryButtonRow(screen, name, count));
                }
                return;
            }
            for (Properties.Option option : Properties.options()) {
                if (!option.category.equals(category)) {
                    continue;
                }
                switch (option.type) {
                    case BOOLEAN -> this.add(new BooleanRow(screen, option));
                    case INT, DOUBLE -> this.add(new NumberRow(screen, option));
                    case STRING_LIST -> this.add(new StringListRow(screen, option));
                }
            }
        }

        private void add(Row row) {
            this.rows.add(row);
            this.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 160;
        }
    }
}
