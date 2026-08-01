package toast.utilityMobs;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.setup.ModEntities;
import toast.utilityMobs.setup.ModRegistries;

/**
    The mod's creative tab. The mod registers no items of its own beyond spawn eggs - its content is
    the utility mob spawn eggs (one per registered entity) plus the NBT manual/upgrade/target books.

    <p>1.12.2 injected the eggs at display time via {@code displayAllRelevantItems}, building each
    from {@code Items.SPAWN_EGG} + an entity id NBT tag. 1.20.1 has no generic egg item, so each mob
    owns a real {@link net.minecraftforge.common.ForgeSpawnEggItem} (see {@link ModEntities}) and the
    tab simply lists them. Egg colors are unchanged.
 */
public final class CreativeTabUtilityMobs {
    private CreativeTabUtilityMobs() {}

    /// Eggs listed in the tab, in registration order. Leaf waves append here.
    private static final RegistryObject<?>[] EGGS = {
        ModEntities.ANVIL_GOLEM_EGG,
        ModEntities.CHEST_ENDER_GOLEM_EGG,
        ModEntities.CHEST_GOLEM_EGG,
        ModEntities.CHEST_TRAPPED_GOLEM_EGG,
        ModEntities.FURNACE_GOLEM_EGG,
        ModEntities.JUKEBOX_GOLEM_EGG,
        ModEntities.LANTERN_GOLEM_EGG,
        ModEntities.WORKBENCH_GOLEM_EGG,
        ModEntities.ARMOR_GOLEM_EGG,
        ModEntities.BOUND_SOUL_EGG,
        ModEntities.GILDED_GOLEM_EGG,
        ModEntities.MELON_GOLEM_EGG,
        ModEntities.OBSIDIAN_GOLEM_EGG,
        ModEntities.SCARECROW_EGG,
        ModEntities.STEAM_GOLEM_EGG,
        ModEntities.STONE_GOLEM_EGG,
        ModEntities.STONE_LARGE_GOLEM_EGG,
        ModEntities.BRICK_TURRET_EGG,
        ModEntities.FIREBALL_TURRET_EGG,
        ModEntities.FIRE_TURRET_EGG,
        ModEntities.GATLING_TURRET_EGG,
        ModEntities.GHAST_TURRET_EGG,
        ModEntities.KILLER_TURRET_EGG,
        ModEntities.OBSIDIAN_TURRET_EGG,
        ModEntities.SHOTGUN_TURRET_EGG,
        ModEntities.SNIPER_TURRET_EGG,
        ModEntities.SNOW_TURRET_EGG,
        ModEntities.VOLLEY_TURRET_EGG,
        ModEntities.STONE_TURRET_EGG,
        ModEntities.ARMOR_COLOSSUS_EGG,
        ModEntities.OBSIDIAN_COLOSSUS_EGG,
        ModEntities.STONE_COLOSSUS_EGG,
    };

    public static final RegistryObject<CreativeModeTab> TAB = ModRegistries.TABS.register("utilitymobs", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + _UtilityMobs.MODID))
            // 1.12.2 used a hidden throwaway item textured with the stone golem's face. The stone
            // golem egg carries the same identity without needing a dead registry entry.
            .icon(() -> new ItemStack(ModEntities.STONE_GOLEM_EGG.get()))
            .displayItems((params, output) -> {
                for (RegistryObject<?> egg : EGGS) {
                    output.accept(new ItemStack((net.minecraft.world.item.Item)egg.get()));
                }
                // The Patchouli guide book. PatchouliCompat hands back an empty stack when Patchouli is not
                // installed, in which case the tab simply has no book entry.
                ItemStack guide = GuideBook.stack();
                if (!guide.isEmpty()) {
                    output.accept(guide);
                }
                // The two target helper books: player permissions and mob target list.
                output.accept(TargetHelper.book(0));
                output.accept(TargetHelper.book(1));
                // Testing tool, not part of the 1.12.2 mod.
                output.accept(new ItemStack(toast.utilityMobs.setup.ModItems.ADMIN_SWORD.get()));
            })
            .build());
}
