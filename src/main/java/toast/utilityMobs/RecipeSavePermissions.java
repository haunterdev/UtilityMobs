package toast.utilityMobs;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RecipeSavePermissions extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{
    // Used to check if a recipe matches current crafting inventory.
    @Override
    public boolean matches(InventoryCrafting craftMatrix, World world) {
        ItemStack targetBook = ItemStack.EMPTY;
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack ingredient = craftMatrix.getStackInSlot(i);
            if (ingredient.isEmpty()) {
                // Do nothing
            }
            else if (targetBook.isEmpty() && (ingredient.getItem() == Items.WRITABLE_BOOK || ingredient.getItem() == Items.WRITTEN_BOOK) && ingredient.getTagCompound() != null && ingredient.getTagCompound().hasKey("umt")) {
                targetBook = ingredient;
            }
            else
                return false;
        }
        return !targetBook.isEmpty();
    }

    // Returns an item stack that is the result of this recipe.
    @Override
    public ItemStack getCraftingResult(InventoryCrafting craftMatrix) {
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack ingredient = craftMatrix.getStackInSlot(i);
            if (!ingredient.isEmpty()) {
                ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
                book.setTagCompound((NBTTagCompound)ingredient.getTagCompound().copy());
                book.getTagCompound().setByte("umu", (byte)0);
                return book;
            }
        }
        return ItemStack.EMPTY;
    }

    // Whether this recipe fits in the given crafting grid (single ingredient, fits any grid).
    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 1;
    }

    // Returns the output for the recipe.
    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
