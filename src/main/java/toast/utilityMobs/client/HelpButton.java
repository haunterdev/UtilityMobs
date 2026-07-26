package toast.utilityMobs.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * The small square "?" button the golem GUIs put in their top-right corner.
 *
 * <p>1.12.2 built this from GuiBorderedButton and hand-drew its tooltip through HelpTooltipHandler: a
 * plain button that short clipped its own bottom border, and a tooltip drawn inside drawScreen landed
 * UNDER JEI's item column, which paints in DrawScreenEvent.Post. 1.20.1 removes both problems - buttons
 * are nine-sliced so a 14px one closes its frame, and Button.setTooltip hands the tooltip to the
 * screen's own tooltip pass instead of painting it inline. So neither helper class is ported.
 */
public final class HelpButton {
    private HelpButton() {}

    public static Button build(int x, int y, int size, Button.OnPress onPress, Component title, Component description) {
        Button button = Button.builder(Component.literal("?"), onPress).bounds(x, y, size, size).build();
        button.setTooltip(Tooltip.create(Component.empty()
            .append(title.copy().withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n"))
            .append(description.copy().withStyle(ChatFormatting.GRAY))));
        return button;
    }
}
