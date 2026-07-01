package toast.utilityMobs;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public abstract class BookHelper
{
    /// Checks the player's book and updates it.
    public static boolean checkBook(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getTagCompound() == null || held.getItem() != Items.WRITTEN_BOOK && held.getItem() != Items.WRITABLE_BOOK)
            return false;
        if (held.getTagCompound().hasKey("umt")) {
            TargetHelper.write(player.getName(), held, held.getTagCompound().getByte("umt"));
            // Mark the freshly-written pages as known so the save-on-exit tick treats them as the
            // baseline rather than a player edit.
            TargetHelper.stampSignature(held);
            return true;
        }
        return false;
    }

    /// Called when a player right clicks a living entity. If this returns true, the event is canceled.
    public static boolean interact(EntityPlayer player, EntityLivingBase entity) {
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getTagCompound() == null || held.getItem() != Items.WRITTEN_BOOK && held.getItem() != Items.WRITABLE_BOOK || !held.getTagCompound().hasKey("umt"))
            return false;
        if (player.world.isRemote) {
            TargetHelper.interact(player.getName(), held, held.getTagCompound().getByte("umt"), entity, player.isSneaking());
        }
        return true;
    }

    /// Sets the book's title and author.
    public static ItemStack setTitleAndAuthor(ItemStack book, String title, String author) {
        if (book.getTagCompound() == null) {
            book.setTagCompound(new NBTTagCompound());
        }
        book.getTagCompound().setString("title", title);
        book.getTagCompound().setString("author", author);
        return book;
    }
    public static ItemStack setTitle(ItemStack book, String title) {
        if (book.getTagCompound() == null) {
            book.setTagCompound(new NBTTagCompound());
        }
        book.getTagCompound().setString("title", title);
        return book;
    }
    public static ItemStack setAuthor(ItemStack book, String author) {
        if (book.getTagCompound() == null) {
            book.setTagCompound(new NBTTagCompound());
        }
        book.getTagCompound().setString("author", author);
        return book;
    }

    /// Removes all pages from a book.
    public static ItemStack removePages(ItemStack book) {
        if (book.getTagCompound() != null && book.getTagCompound().hasKey("pages")) {
            book.getTagCompound().removeTag("pages");
        }
        return book;
    }

    /// Adds new pages to a book.
    public static ItemStack addPages(ItemStack book, String... pages) {
        if (pages.length > 0) {
            if (book.getTagCompound() == null) {
                book.setTagCompound(new NBTTagCompound());
            }
            if (!book.getTagCompound().hasKey("pages")) {
                book.getTagCompound().setTag("pages", new NBTTagList());
            }
            NBTTagList tag = book.getTagCompound().getTagList("pages", new NBTTagString("").getId());
            // Written (signed) books require each page to be a JSON text component string in 1.12.2;
            // writable (book & quill) pages stay as plain strings.
            boolean written = book.getItem() == Items.WRITTEN_BOOK;
            for (int p = 0; p < pages.length; p++) if (pages[p] != null) {
                String content = written ? ITextComponent.Serializer.componentToJson(new TextComponentString(pages[p])) : pages[p];
                tag.appendTag(new NBTTagString(content));
            }
        }
        return book;
    }
}
