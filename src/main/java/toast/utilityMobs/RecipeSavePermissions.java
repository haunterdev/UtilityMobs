package toast.utilityMobs;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import toast.utilityMobs.setup.ModRecipes;

/**
 * Puts a target book back on the crafting grid on its own to get an editable copy: the result is a
 * book and quill carrying the same target data, tagged "umu" so TickHandler knows to parse it once the
 * player takes it.
 *
 * <p>1.12.2 implemented IRecipe directly. 1.20.1's CustomRecipe is the same idea with the boilerplate
 * removed, and it needs a serializer registered so the data-driven recipe file can point at it.
 */
public class RecipeSavePermissions extends CustomRecipe
{
    public RecipeSavePermissions(net.minecraft.resources.ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    // Used to check if a recipe matches current crafting inventory.
    @Override
    public boolean matches(CraftingContainer craftMatrix, Level level) {
        ItemStack targetBook = ItemStack.EMPTY;
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack ingredient = craftMatrix.getItem(i);
            if (ingredient.isEmpty()) {
                // Do nothing
            }
            else if (targetBook.isEmpty() && (ingredient.is(Items.WRITABLE_BOOK) || ingredient.is(Items.WRITTEN_BOOK))
                    && ingredient.getTag() != null && ingredient.getTag().contains("umt")) {
                targetBook = ingredient;
            }
            else
                return false;
        }
        return !targetBook.isEmpty();
    }

    // Returns an item stack that is the result of this recipe.
    @Override
    public ItemStack assemble(CraftingContainer craftMatrix, RegistryAccess registries) {
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack ingredient = craftMatrix.getItem(i);
            if (!ingredient.isEmpty() && ingredient.getTag() != null) {
                ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
                book.setTag((CompoundTag)ingredient.getTag().copy());
                book.getOrCreateTag().putByte("umu", (byte)0);
                return book;
            }
        }
        return ItemStack.EMPTY;
    }

    // Whether this recipe fits in the given crafting grid (single ingredient, fits any grid).
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SAVE_PERMISSIONS.get();
    }
}
