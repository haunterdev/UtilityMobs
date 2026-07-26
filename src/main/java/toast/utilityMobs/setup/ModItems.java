package toast.utilityMobs.setup;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.ItemAdminSword;

/**
 * Plain items. Spawn eggs live in {@link ModEntities} next to the entity types they spawn.
 *
 * <p>Touch this holder from the mod constructor, or its static initialiser never runs and the
 * DeferredRegister entry is never created.
 */
public final class ModItems {
    private ModItems() {}

    /// Testing tool, not part of the 1.12.2 mod. See ItemAdminSword.
    public static final RegistryObject<Item> ADMIN_SWORD =
        ModRegistries.ITEMS.register("admin_sword", ItemAdminSword::new);
}
