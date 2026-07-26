package toast.utilityMobs.setup;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.RecipeSavePermissions;

/**
 * Recipe serializers. 1.12.2 registered its IRecipe instance straight into the recipe registry; 1.20.1
 * recipes are data files, so the code side is only the serializer that the file names.
 */
public final class ModRecipes {
    private ModRecipes() {}

    public static final RegistryObject<RecipeSerializer<?>> SAVE_PERMISSIONS =
        ModRegistries.RECIPE_SERIALIZERS.register("save_permissions",
            () -> new SimpleCraftingRecipeSerializer<>(RecipeSavePermissions::new));
}
