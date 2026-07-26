package toast.utilityMobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public abstract class BookHelper
{
    /// Checks the player's book and updates it.
    public static boolean checkBook(Player player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getTag() == null || held.getItem() != Items.WRITTEN_BOOK && held.getItem() != Items.WRITABLE_BOOK)
            return false;
        if (held.getTag().contains("umt")) {
            TargetHelper.write(player.getScoreboardName(), held, held.getTag().getByte("umt"));
            // Mark the freshly-written pages as known so the save-on-exit tick treats them as the
            // baseline rather than a player edit.
            TargetHelper.stampSignature(held);
            return true;
        }
        return false;
    }

    /// Called when a player right clicks a living entity. If this returns true, the event is canceled.
    public static boolean interact(Player player, LivingEntity entity) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getTag() == null || held.getItem() != Items.WRITTEN_BOOK && held.getItem() != Items.WRITABLE_BOOK || !held.getTag().contains("umt"))
            return false;
        if (player.level().isClientSide) {
            TargetHelper.interact(player.getScoreboardName(), held, held.getTag().getByte("umt"), entity, player.isShiftKeyDown());
        }
        return true;
    }

    /// Sets the book's title and author.
    public static ItemStack setTitleAndAuthor(ItemStack book, String title, String author) {
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        return book;
    }
    public static ItemStack setTitle(ItemStack book, String title) {
        book.getOrCreateTag().putString("title", title);
        return book;
    }
    public static ItemStack setAuthor(ItemStack book, String author) {
        book.getOrCreateTag().putString("author", author);
        return book;
    }

    /// Removes all pages from a book.
    public static ItemStack removePages(ItemStack book) {
        if (book.getTag() != null && book.getTag().contains("pages")) {
            book.getTag().remove("pages");
        }
        return book;
    }

    /// Adds new pages to a book.
    public static ItemStack addPages(ItemStack book, String... pages) {
        if (pages.length > 0) {
            CompoundTag bookTag = book.getOrCreateTag();
            if (!bookTag.contains("pages", Tag.TAG_LIST)) {
                bookTag.put("pages", new ListTag());
            }
            ListTag tag = bookTag.getList("pages", Tag.TAG_STRING);
            // Written (signed) books require each page to be a JSON text component string;
            // writable (book & quill) pages stay as plain strings.
            boolean written = book.getItem() == Items.WRITTEN_BOOK;
            for (String page : pages) if (page != null) {
                String content = written ? Component.Serializer.toJson(Component.literal(page)) : page;
                tag.add(StringTag.valueOf(content));
            }
        }
        return book;
    }
}
