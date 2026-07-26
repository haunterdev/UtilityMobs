package toast.utilityMobs.setup;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.EntityGolemFishHook;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityChestEnderGolem;
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityChestTrappedGolem;
import toast.utilityMobs.block.EntityFurnaceGolem;
import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.block.EntityLanternGolem;
import toast.utilityMobs.block.EntityWorkbenchGolem;
import toast.utilityMobs.golem.EntityArmorGolem;
import toast.utilityMobs.golem.EntityBoundSoul;
import toast.utilityMobs.golem.EntityGildedGolem;
import toast.utilityMobs.golem.EntityMelonGolem;
import toast.utilityMobs.golem.EntityObsidianGolem;
import toast.utilityMobs.golem.EntityScarecrow;
import toast.utilityMobs.golem.EntityStackGolem;
import toast.utilityMobs.golem.EntitySteamGolem;
import toast.utilityMobs.golem.EntityStoneGolem;
import toast.utilityMobs.golem.EntityStoneLargeGolem;
import toast.utilityMobs.golem.EntityUMIronGolem;
import toast.utilityMobs.golem.EntityUMSnowGolem;
import toast.utilityMobs.block.EntityBlockGolem;
import toast.utilityMobs.colossal.EntityArmorColossus;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.colossal.EntityObsidianColossus;
import toast.utilityMobs.colossal.EntityStoneColossus;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.turret.EntityBrickTurret;
import toast.utilityMobs.turret.EntityFireballTurret;
import toast.utilityMobs.turret.EntityFireTurret;
import toast.utilityMobs.turret.EntityGatlingTurret;
import toast.utilityMobs.turret.EntityGhastTurret;
import toast.utilityMobs.turret.EntityKillerTurret;
import toast.utilityMobs.turret.EntityObsidianTurret;
import toast.utilityMobs.turret.EntityShotgunTurret;
import toast.utilityMobs.turret.EntitySniperTurret;
import toast.utilityMobs.turret.EntitySnowTurret;
import toast.utilityMobs.turret.EntityVolleyTurret;
import toast.utilityMobs.turret.EntityStoneTurret;
import toast.utilityMobs.turret.EntityTurretGolem;
import toast.utilityMobs.turret.EntityTurretArrow;

/**
 * Entity types, their spawn eggs, and their attribute suppliers.
 *
 * <p>The 1.12.2 mod registered every mob in one reflective loop over
 * {@code _UtilityMobs.UTILITY_NAMES} with tracking range 80 blocks / update interval 3.
 * DeferredRegister needs concrete constructor references, so the loop is unrolled here;
 * the tracking values are carried over verbatim (80 blocks = 5 chunks).
 *
 * <p>Registry names are lowercase snake case of the 1.12.2 class names.
 */
public final class ModEntities {
    private ModEntities() {}

    /// 1.12.2 EntityRegistry.registerModEntity tracking args: range 80 blocks, update interval 3.
    private static final int TRACK_CHUNKS = 5;
    private static final int TRACK_INTERVAL = 3;

    public static final RegistryObject<EntityType<EntityStoneGolem>> STONE_GOLEM =
        ModRegistries.ENTITY_TYPES.register("stone_golem", () ->
            EntityType.Builder.<EntityStoneGolem>of(EntityStoneGolem::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":stone_golem"));

    public static final RegistryObject<EntityType<EntityStoneTurret>> STONE_TURRET =
        ModRegistries.ENTITY_TYPES.register("stone_turret", () ->
            EntityType.Builder.<EntityStoneTurret>of(EntityStoneTurret::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":stone_turret"));

    public static final RegistryObject<EntityType<EntityArmorGolem>> ARMOR_GOLEM =
        golem("armor_golem", EntityArmorGolem::new);
    public static final RegistryObject<EntityType<EntityBoundSoul>> BOUND_SOUL =
        golem("bound_soul", EntityBoundSoul::new);
    public static final RegistryObject<EntityType<EntityGildedGolem>> GILDED_GOLEM =
        golem("gilded_golem", EntityGildedGolem::new);
    public static final RegistryObject<EntityType<EntityScarecrow>> SCARECROW =
        golem("scarecrow", EntityScarecrow::new);
    public static final RegistryObject<EntityType<EntityStackGolem>> STACK_GOLEM =
        golem("stack_golem", EntityStackGolem::new);
    public static final RegistryObject<EntityType<EntityMelonGolem>> MELON_GOLEM =
        golem("melon_golem", EntityMelonGolem::new);
    public static final RegistryObject<EntityType<EntityUMSnowGolem>> UM_SNOW_GOLEM =
        golem("umsnowgolem", EntityUMSnowGolem::new);

    /// Large golems use the 1.12.2 setSize(1.4F, 2.9F) hitbox.
    public static final RegistryObject<EntityType<EntityObsidianGolem>> OBSIDIAN_GOLEM =
        largeGolem("obsidian_golem", EntityObsidianGolem::new);
    public static final RegistryObject<EntityType<EntityStoneLargeGolem>> STONE_LARGE_GOLEM =
        largeGolem("stone_large_golem", EntityStoneLargeGolem::new);
    public static final RegistryObject<EntityType<EntityUMIronGolem>> UM_IRON_GOLEM =
        largeGolem("umirongolem", EntityUMIronGolem::new);

    public static final RegistryObject<EntityType<EntityBrickTurret>> BRICK_TURRET =
        golem("brick_turret", EntityBrickTurret::new);
    public static final RegistryObject<EntityType<EntityFireballTurret>> FIREBALL_TURRET =
        golem("fireball_turret", EntityFireballTurret::new);
    public static final RegistryObject<EntityType<EntityFireTurret>> FIRE_TURRET =
        golem("fire_turret", EntityFireTurret::new);
    public static final RegistryObject<EntityType<EntityGatlingTurret>> GATLING_TURRET =
        golem("gatling_turret", EntityGatlingTurret::new);
    public static final RegistryObject<EntityType<EntityGhastTurret>> GHAST_TURRET =
        golem("ghast_turret", EntityGhastTurret::new);
    public static final RegistryObject<EntityType<EntityKillerTurret>> KILLER_TURRET =
        golem("killer_turret", EntityKillerTurret::new);
    public static final RegistryObject<EntityType<EntityObsidianTurret>> OBSIDIAN_TURRET =
        golem("obsidian_turret", EntityObsidianTurret::new);
    public static final RegistryObject<EntityType<EntityShotgunTurret>> SHOTGUN_TURRET =
        golem("shotgun_turret", EntityShotgunTurret::new);
    public static final RegistryObject<EntityType<EntitySniperTurret>> SNIPER_TURRET =
        golem("sniper_turret", EntitySniperTurret::new);
    public static final RegistryObject<EntityType<EntitySnowTurret>> SNOW_TURRET =
        golem("snow_turret", EntitySnowTurret::new);
    public static final RegistryObject<EntityType<EntityVolleyTurret>> VOLLEY_TURRET =
        golem("volley_turret", EntityVolleyTurret::new);

    public static final RegistryObject<EntityType<EntityAnvilGolem>> ANVIL_GOLEM =
        blockGolem("anvil_golem", EntityAnvilGolem::new, 0.9375F);
    public static final RegistryObject<EntityType<EntityFurnaceGolem>> FURNACE_GOLEM =
        blockGolem("furnace_golem", EntityFurnaceGolem::new, 0.9375F);
    public static final RegistryObject<EntityType<EntityJukeboxGolem>> JUKEBOX_GOLEM =
        blockGolem("jukebox_golem", EntityJukeboxGolem::new, 0.9375F);
    public static final RegistryObject<EntityType<EntityLanternGolem>> LANTERN_GOLEM =
        blockGolem("lantern_golem", EntityLanternGolem::new, 0.9375F);
    public static final RegistryObject<EntityType<EntityChestEnderGolem>> CHEST_ENDER_GOLEM =
        blockGolem("chest_ender_golem", EntityChestEnderGolem::new, 0.875F);
    public static final RegistryObject<EntityType<EntityChestGolem>> CHEST_GOLEM =
        blockGolem("chest_golem", EntityChestGolem::new, 0.875F);
    public static final RegistryObject<EntityType<EntityChestTrappedGolem>> CHEST_TRAPPED_GOLEM =
        blockGolem("chest_trapped_golem", EntityChestTrappedGolem::new, 0.875F);
    public static final RegistryObject<EntityType<EntityWorkbenchGolem>> WORKBENCH_GOLEM =
        blockGolem("workbench_golem", EntityWorkbenchGolem::new, 0.9375F);

    public static final RegistryObject<EntityType<EntitySteamGolem>> STEAM_GOLEM =
        largeGolem("steam_golem", EntitySteamGolem::new);

    /// Colossal golems: 1.12.2 setSize(1.8F, 3.2F).
    public static final RegistryObject<EntityType<EntityArmorColossus>> ARMOR_COLOSSUS =
        colossus("armorcolossus", EntityArmorColossus::new);
    public static final RegistryObject<EntityType<EntityObsidianColossus>> OBSIDIAN_COLOSSUS =
        colossus("obsidiancolossus", EntityObsidianColossus::new);
    public static final RegistryObject<EntityType<EntityStoneColossus>> STONE_COLOSSUS =
        colossus("stonecolossus", EntityStoneColossus::new);

    /// The golem's fishing hook. 1.12.2 tracked it at 64 blocks / interval 5.
    public static final RegistryObject<EntityType<EntityGolemFishHook>> GOLEM_FISH_HOOK =
        ModRegistries.ENTITY_TYPES.register("umfishhook", () ->
            EntityType.Builder.<EntityGolemFishHook>of(EntityGolemFishHook::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(5)
                .build(_UtilityMobs.MODID + ":umfishhook"));

    /// Turret-fired arrow so despawning arrows puff into particles instead of popping.
    /// 1.12.2 tracked it at 64 blocks / interval 3.
    public static final RegistryObject<EntityType<EntityTurretArrow>> TURRET_ARROW =
        ModRegistries.ENTITY_TYPES.register("turret_arrow", () ->
            EntityType.Builder.<EntityTurretArrow>of(EntityTurretArrow::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":turret_arrow"));

    // --- Spawn eggs. Colors come from _UtilityMobs.EGG_COLORS (index [type][mob]). ---

    public static final RegistryObject<Item> STONE_GOLEM_EGG =
        egg("stone_golem_spawn_egg", STONE_GOLEM, 1, 7);
    public static final RegistryObject<Item> STONE_TURRET_EGG =
        egg("stone_turret_spawn_egg", STONE_TURRET, 3, 10);
    public static final RegistryObject<Item> ARMOR_GOLEM_EGG =
        egg("armor_golem_spawn_egg", ARMOR_GOLEM, 1, 0);
    public static final RegistryObject<Item> BOUND_SOUL_EGG =
        egg("bound_soul_spawn_egg", BOUND_SOUL, 1, 1);
    public static final RegistryObject<Item> GILDED_GOLEM_EGG =
        egg("gilded_golem_spawn_egg", GILDED_GOLEM, 1, 2);
    public static final RegistryObject<Item> OBSIDIAN_GOLEM_EGG =
        egg("obsidian_golem_spawn_egg", OBSIDIAN_GOLEM, 1, 4);
    public static final RegistryObject<Item> SCARECROW_EGG =
        egg("scarecrow_spawn_egg", SCARECROW, 1, 5);
    public static final RegistryObject<Item> STONE_LARGE_GOLEM_EGG =
        egg("stone_large_golem_spawn_egg", STONE_LARGE_GOLEM, 1, 8);
    public static final RegistryObject<Item> BRICK_TURRET_EGG =
        egg("brick_turret_spawn_egg", BRICK_TURRET, 3, 0);
    public static final RegistryObject<Item> FIREBALL_TURRET_EGG =
        egg("fireball_turret_spawn_egg", FIREBALL_TURRET, 3, 1);
    public static final RegistryObject<Item> FIRE_TURRET_EGG =
        egg("fire_turret_spawn_egg", FIRE_TURRET, 3, 2);
    public static final RegistryObject<Item> GATLING_TURRET_EGG =
        egg("gatling_turret_spawn_egg", GATLING_TURRET, 3, 3);
    public static final RegistryObject<Item> GHAST_TURRET_EGG =
        egg("ghast_turret_spawn_egg", GHAST_TURRET, 3, 4);
    public static final RegistryObject<Item> KILLER_TURRET_EGG =
        egg("killer_turret_spawn_egg", KILLER_TURRET, 3, 5);
    public static final RegistryObject<Item> OBSIDIAN_TURRET_EGG =
        egg("obsidian_turret_spawn_egg", OBSIDIAN_TURRET, 3, 6);
    public static final RegistryObject<Item> SHOTGUN_TURRET_EGG =
        egg("shotgun_turret_spawn_egg", SHOTGUN_TURRET, 3, 7);
    public static final RegistryObject<Item> SNIPER_TURRET_EGG =
        egg("sniper_turret_spawn_egg", SNIPER_TURRET, 3, 8);
    public static final RegistryObject<Item> SNOW_TURRET_EGG =
        egg("snow_turret_spawn_egg", SNOW_TURRET, 3, 9);
    public static final RegistryObject<Item> VOLLEY_TURRET_EGG =
        egg("volley_turret_spawn_egg", VOLLEY_TURRET, 3, 11);
    public static final RegistryObject<Item> CHEST_ENDER_GOLEM_EGG =
        egg("chest_ender_golem_spawn_egg", CHEST_ENDER_GOLEM, 0, 1);
    public static final RegistryObject<Item> CHEST_GOLEM_EGG =
        egg("chest_golem_spawn_egg", CHEST_GOLEM, 0, 2);
    public static final RegistryObject<Item> CHEST_TRAPPED_GOLEM_EGG =
        egg("chest_trapped_golem_spawn_egg", CHEST_TRAPPED_GOLEM, 0, 3);
    public static final RegistryObject<Item> WORKBENCH_GOLEM_EGG =
        egg("workbench_golem_spawn_egg", WORKBENCH_GOLEM, 0, 7);
    public static final RegistryObject<Item> ANVIL_GOLEM_EGG =
        egg("anvil_golem_spawn_egg", ANVIL_GOLEM, 0, 0);
    public static final RegistryObject<Item> FURNACE_GOLEM_EGG =
        egg("furnace_golem_spawn_egg", FURNACE_GOLEM, 0, 4);
    public static final RegistryObject<Item> JUKEBOX_GOLEM_EGG =
        egg("jukebox_golem_spawn_egg", JUKEBOX_GOLEM, 0, 5);
    public static final RegistryObject<Item> STEAM_GOLEM_EGG =
        egg("steam_golem_spawn_egg", STEAM_GOLEM, 1, 6);
    public static final RegistryObject<Item> LANTERN_GOLEM_EGG =
        egg("lantern_golem_spawn_egg", LANTERN_GOLEM, 0, 6);
    // Not ported: the UM iron and snow golem spawn eggs. 1.12.2 needed them because vanilla 1.12.2 had no
    // iron/snow golem eggs at all; 1.20.1 ships both. The UM variants are still obtained the way the
    // 1.12.2 comment on Properties' build.golems block describes, by upgrading a vanilla golem through
    // BuildHelper, so nothing is lost but the duplicate item.
    public static final RegistryObject<Item> MELON_GOLEM_EGG =
        egg("melon_golem_spawn_egg", MELON_GOLEM, 1, 3);
    /// Stack golem has no 1.12.2 egg (it is a construction artifact, not a spawnable mob).

    /// Colossal golem eggs. Colors from EGG_COLORS[4] (creeper-head base).
    public static final RegistryObject<Item> ARMOR_COLOSSUS_EGG =
        egg("armorcolossus_spawn_egg", ARMOR_COLOSSUS, 4, 0);
    public static final RegistryObject<Item> OBSIDIAN_COLOSSUS_EGG =
        egg("obsidiancolossus_spawn_egg", OBSIDIAN_COLOSSUS, 4, 1);
    public static final RegistryObject<Item> STONE_COLOSSUS_EGG =
        egg("stonecolossus_spawn_egg", STONE_COLOSSUS, 4, 2);

    /// Block golems. 1.12.2 EntityBlockGolem used setSize(0.9375F, 0.9375F); the chest family
    /// narrowed that to 0.875 in its own constructor.
    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<EntityType<T>> blockGolem(String name, EntityType.EntityFactory<T> factory, float size) {
        return ModRegistries.ENTITY_TYPES.register(name, () ->
            EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(size, size)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":" + name));
    }

    /// Standard humanoid golem: 1.12.2 left these at the default EntityLiving size.
    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<EntityType<T>> golem(String name, EntityType.EntityFactory<T> factory) {
        return ModRegistries.ENTITY_TYPES.register(name, () ->
            EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":" + name));
    }

    /// EntityLargeGolem and its subclasses: 1.12.2 setSize(1.4F, 2.9F).
    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<EntityType<T>> largeGolem(String name, EntityType.EntityFactory<T> factory) {
        return ModRegistries.ENTITY_TYPES.register(name, () ->
            EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(1.4F, 2.9F)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":" + name));
    }

    /// EntityColossalGolem and its subclasses: 1.12.2 setSize(1.8F, 3.2F).
    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<EntityType<T>> colossus(String name, EntityType.EntityFactory<T> factory) {
        return ModRegistries.ENTITY_TYPES.register(name, () ->
            EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(1.8F, 3.2F)
                .clientTrackingRange(TRACK_CHUNKS)
                .updateInterval(TRACK_INTERVAL)
                .build(_UtilityMobs.MODID + ":" + name));
    }

    private static RegistryObject<Item> egg(String name, RegistryObject<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int typeIndex, int mobIndex) {
        int[] colors = _UtilityMobs.EGG_COLORS[typeIndex][mobIndex];
        return ModRegistries.ITEMS.register(name, () -> new ForgeSpawnEggItem(type, colors[0], colors[1], new Item.Properties()));
    }

    /// Attribute suppliers. Mod bus - EntityAttributeCreationEvent fires during common setup.
    @Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {}

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(STONE_GOLEM.get(), EntityStoneGolem.createAttributes().build());
            event.put(STONE_TURRET.get(), EntityStoneTurret.createAttributes().build());
            event.put(ARMOR_GOLEM.get(), EntityArmorGolem.createAttributes().build());
            event.put(BOUND_SOUL.get(), EntityBoundSoul.createAttributes().build());
            event.put(GILDED_GOLEM.get(), EntityUtilityGolem.createAttributes().build());
            event.put(SCARECROW.get(), EntityScarecrow.createAttributes().build());
            event.put(STACK_GOLEM.get(), EntityStackGolem.createAttributes().build());
            event.put(OBSIDIAN_GOLEM.get(), EntityObsidianGolem.createAttributes().build());
            event.put(STONE_LARGE_GOLEM.get(), EntityStoneLargeGolem.createAttributes().build());
            event.put(UM_IRON_GOLEM.get(), EntityUMIronGolem.createAttributes().build());
            event.put(BRICK_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(FIREBALL_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(FIRE_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(GATLING_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(GHAST_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(KILLER_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(OBSIDIAN_TURRET.get(), EntityObsidianTurret.createAttributes().build());
            event.put(SHOTGUN_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(SNIPER_TURRET.get(), EntitySniperTurret.createAttributes().build());
            event.put(SNOW_TURRET.get(), EntityTurretGolem.createAttributes().build());
            event.put(VOLLEY_TURRET.get(), EntityVolleyTurret.createAttributes().build());
            event.put(CHEST_ENDER_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(CHEST_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(CHEST_TRAPPED_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(WORKBENCH_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(ANVIL_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(FURNACE_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(JUKEBOX_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(LANTERN_GOLEM.get(), EntityBlockGolem.createAttributes().build());
            event.put(STEAM_GOLEM.get(), EntitySteamGolem.createAttributes().build());
            event.put(MELON_GOLEM.get(), EntityMelonGolem.createAttributes().build());
            event.put(UM_SNOW_GOLEM.get(), EntityUMSnowGolem.createAttributes().build());
            event.put(ARMOR_COLOSSUS.get(), EntityArmorColossus.createAttributes().build());
            event.put(OBSIDIAN_COLOSSUS.get(), EntityObsidianColossus.createAttributes().build());
            event.put(STONE_COLOSSUS.get(), EntityColossalGolem.createAttributes().build());
        }
    }
}
