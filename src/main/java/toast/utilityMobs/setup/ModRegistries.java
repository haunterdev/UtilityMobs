package toast.utilityMobs.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import toast.utilityMobs._UtilityMobs;

/** All deferred registers. Entries live next to their content classes; this just wires the buses. */
public final class ModRegistries {
    private ModRegistries() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, _UtilityMobs.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, _UtilityMobs.MODID);
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, _UtilityMobs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, _UtilityMobs.MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, _UtilityMobs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, _UtilityMobs.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, _UtilityMobs.MODID);

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
    }
}
