package toast.utilityMobs.client;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.gui.GuiBookCategory;
import vazkii.patchouli.client.book.text.BookTextParser;
import vazkii.patchouli.client.book.text.Span;
import vazkii.patchouli.client.book.text.SpanState;
import vazkii.patchouli.common.book.Book;

@SideOnly(Side.CLIENT)
public final class UMPatchouliLinks {

    private UMPatchouliLinks() {}

    @SuppressWarnings("unchecked")
    public static void register() {
        try {
            Field field = BookTextParser.class.getDeclaredField("FUNCTIONS");
            field.setAccessible(true);
            Map<String, BookTextParser.FunctionProcessor> functions =
                    (Map<String, BookTextParser.FunctionProcessor>) field.get(null);
            functions.put("lc", UMPatchouliLinks::categoryLink);
        } catch (Exception e) {
            System.err.println("[UtilityMobs] Could not register the $(lc:) category-link function:");
            e.printStackTrace();
        }
    }

    private static String categoryLink(String param, SpanState state) {
        state.cluster = new LinkedList<Span>();
        state.prevColor = state.color;
        state.color = state.book.linkColor;
        final Book book = state.book;
        final BookCategory category =
                book.contents.categories.get(new ResourceLocation(book.getModNamespace(), param));
        if (category == null) {
            state.tooltip = "BAD CATEGORY: " + param;
            return "";
        }
        state.tooltip = category.getName();
        state.onClick = () -> book.contents.openLexiconGui(new GuiBookCategory(book, category), true);
        return "";
    }
}