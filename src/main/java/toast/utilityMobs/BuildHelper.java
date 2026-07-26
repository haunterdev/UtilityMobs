package toast.utilityMobs;

import java.util.HashSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityChestEnderGolem;
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityChestTrappedGolem;
import toast.utilityMobs.block.EntityContainerGolem;
import toast.utilityMobs.block.EntityFurnaceGolem;
import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.block.EntityLanternGolem;
import toast.utilityMobs.block.EntityWorkbenchGolem;
import toast.utilityMobs.colossal.EntityArmorColossus;
import toast.utilityMobs.colossal.EntityObsidianColossus;
import toast.utilityMobs.colossal.EntityStoneColossus;
import toast.utilityMobs.event.BlockEvent;
import toast.utilityMobs.golem.EntityArmorGolem;
import toast.utilityMobs.golem.EntityBoundSoul;
import toast.utilityMobs.golem.EntityGildedGolem;
import toast.utilityMobs.golem.EntityMelonGolem;
import toast.utilityMobs.golem.EntityObsidianGolem;
import toast.utilityMobs.golem.EntityScarecrow;
import toast.utilityMobs.golem.EntitySteamGolem;
import toast.utilityMobs.golem.EntityStoneGolem;
import toast.utilityMobs.golem.EntityStoneLargeGolem;
import toast.utilityMobs.golem.EntityUMIronGolem;
import toast.utilityMobs.golem.EntityUMSnowGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.setup.ModEntities;
import toast.utilityMobs.turret.EntityBrickTurret;
import toast.utilityMobs.turret.EntityFireTurret;
import toast.utilityMobs.turret.EntityFireballTurret;
import toast.utilityMobs.turret.EntityGatlingTurret;
import toast.utilityMobs.turret.EntityGhastTurret;
import toast.utilityMobs.turret.EntityKillerTurret;
import toast.utilityMobs.turret.EntityObsidianTurret;
import toast.utilityMobs.turret.EntityShotgunTurret;
import toast.utilityMobs.turret.EntitySniperTurret;
import toast.utilityMobs.turret.EntitySnowTurret;
import toast.utilityMobs.turret.EntityStoneTurret;
import toast.utilityMobs.turret.EntityVolleyTurret;

/**
 * Turns a block structure topped with a head into a golem. This is the mod's main way of obtaining
 * anything; spawn eggs are the shortcut.
 *
 * <p>Two 1.12.2 concepts have no 1.20.1 equivalent and are replaced by tags rather than reproduced.
 * The wooden-fence set came from the "fenceWood" ore dictionary, which is now {@link BlockTags#WOODEN_FENCES}.
 * Wool was the single Blocks.WOOL with a colour metadata; it is sixteen blocks now, so scarecrow heads
 * are matched against {@link BlockTags#WOOL}.
 *
 * <p>Skulls were likewise one block with a type field. Colossal golems accepted any head, which is any
 * {@link AbstractSkullBlock}, while block golems accepted only the skeleton skull, which is now its own
 * pair of standing and wall blocks.
 */
public class BuildHelper
{
    /// A set of usernames for players right-clicking a Block.
    public final HashSet<String> clickingBlock = new HashSet<String>();

    /// True if the block is any wooden fence. 1.12.2 built this set lazily from the "fenceWood" ore
    /// dictionary so modded fences counted too; the wooden-fences tag is that set, maintained by the game.
    public static boolean isFenceWood(BlockState state) {
        return state.is(BlockTags.WOODEN_FENCES);
    }

    public BuildHelper() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Convenience: block state at integer coords.
    private static BlockState state(Level level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z));
    }

    /**
     * Called when the player right-clicks a block; queues a golem build server-side.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        boolean targetBook = false;
        if (!event.getLevel().getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
            targetBook = BookHelper.checkBook(player);
        }
        if (!player.level().isClientSide) {
            new BlockEvent(player, pos.getX(), pos.getY(), pos.getZ(), event.getFace());
        }
        else if (targetBook) {
            this.clickingBlock.add(player.getGameProfile().getName());
        }
    }

    /**
     * Called when the player right-clicks with an item in the air; handles target-book sync.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        // Refresh the target list into the book, then let the book GUI open normally - including when
        // clicking air, so the target list is no longer gated behind aiming at a block.
        BookHelper.checkBook(player);
        if (player.level().isClientSide) {
            this.clickingBlock.remove(player.getGameProfile().getName());
        }
    }

    /**
     * Called when the player right-clicks an entity; lets the target book interact with it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof LivingEntity target && BookHelper.interact(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    /// Spawns a golem, if possible.
    public static boolean place(Level level, Player player, boolean holdingGolemHead, int x, int y, int z) {
        BlockState state = BuildHelper.state(level, x, y, z);
        if (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN))
            return BuildHelper.placeGolem(level, player, x, y, z);
        if (state.is(Blocks.DISPENSER))
            return BuildHelper.placeTurret(level, player, x, y, z);
        if (state.getBlock() instanceof AbstractSkullBlock) {
            // Colossal golems: ANY mob head works (the structure beneath decides which golem is built).
            if (BuildHelper.placeColossal(level, player, x, y, z))
                return true;
            // Block golems: SKELETON skull only. Other heads won't animate a block golem.
            if (BuildHelper.isSkeletonSkull(state) && BuildHelper.placeBlock(level, player, x, y, z))
                return true;
            return BuildHelper.placeHostile(level, player, x, y, z);
        }
        if (holdingGolemHead)
            return BuildHelper.replaceGolem(level, player, x, y, z);
        return false;
    }

    /// 1.12.2's getSkullType(...) == 0.
    public static boolean isSkeletonSkull(BlockState state) {
        return state.is(Blocks.SKELETON_SKULL) || state.is(Blocks.SKELETON_WALL_SKULL);
    }

    /// Spawns a golem, if possible.
    public static boolean placeGolem(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.golems", "_all"))
            return false;
        EntityUtilityGolem golem;
        BlockState top = BuildHelper.state(level, x, y - 1, z);
        BlockState bottom = BuildHelper.state(level, x, y - 2, z);
        BlockState armLX = BuildHelper.state(level, x - 1, y - 1, z);
        BlockState armRX = BuildHelper.state(level, x + 1, y - 1, z);
        BlockState armLZ = BuildHelper.state(level, x, y - 1, z - 1);
        BlockState armRZ = BuildHelper.state(level, x, y - 1, z + 1);
        String owner = null;
        if (player != null) {
            owner = player.getGameProfile().getName();
        }
        if (top.is(Blocks.COBBLESTONE) && bottom.is(Blocks.COBBLESTONE)) {
            boolean xAxis = armLX.is(Blocks.COBBLESTONE) && armRX.is(Blocks.COBBLESTONE);
            boolean zAxis = armLZ.is(Blocks.COBBLESTONE) && armRZ.is(Blocks.COBBLESTONE);
            if (!Properties.getBoolean("build.golems", "StoneLargeGolem")) {
                xAxis = false;
                zAxis = false;
            }
            if (xAxis || zAxis) {
                golem = new EntityStoneLargeGolem(ModEntities.STONE_LARGE_GOLEM.get(), level);
                golem.setOwner(owner);
                BuildHelper.init(golem, level, x, y, z);
                BuildHelper.removeLarge(level, xAxis, x, y, z);
                BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
                return true;
            }
            else if (!Properties.getBoolean("build.golems", "StoneGolem"))
                return false;
            golem = new EntityStoneGolem(ModEntities.STONE_GOLEM.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x, y, z);
            BuildHelper.removeStandard(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.SNOWFLAKE, x, y, z);
            return true;
        }
        else if (top.is(Blocks.FURNACE) && bottom.is(Blocks.COBBLESTONE)) {
            if (!Properties.getBoolean("build.golems", "SteamGolem"))
                return false;
            boolean xAxis = armLX.is(Blocks.COBBLESTONE) && armRX.is(Blocks.COBBLESTONE);
            boolean zAxis = armLZ.is(Blocks.COBBLESTONE) && armRZ.is(Blocks.COBBLESTONE);
            if (xAxis || zAxis) {
                golem = new EntitySteamGolem(ModEntities.STEAM_GOLEM.get(), level);
                golem.setOwner(owner);
                BuildHelper.init(golem, level, x, y, z);
                BuildHelper.removeLarge(level, xAxis, x, y, z);
                BuildHelper.particleEffect(level, ParticleTypes.LARGE_SMOKE, x, y, z);
                return true;
            }
            return false;
        }
        else if (top.is(Blocks.IRON_BLOCK) && bottom.is(Blocks.IRON_BLOCK)) {
            if (armLX.is(Blocks.IRON_BLOCK) && armRX.is(Blocks.IRON_BLOCK) || armLZ.is(Blocks.IRON_BLOCK) && armRZ.is(Blocks.IRON_BLOCK))
                return false;
            if (!Properties.getBoolean("build.golems", "ArmorGolem"))
                return false;
            golem = new EntityArmorGolem(ModEntities.ARMOR_GOLEM.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x, y, z);
            BuildHelper.removeStandard(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
            return true;
        }
        else if (Properties.getBoolean("build.golems", "Scarecrow") && top.is(BlockTags.WOOL) && BuildHelper.isFenceWood(bottom)) {
            boolean xAxis = BuildHelper.isFenceWood(armLX) && BuildHelper.isFenceWood(armRX);
            boolean zAxis = BuildHelper.isFenceWood(armLZ) && BuildHelper.isFenceWood(armRZ);
            if (xAxis || zAxis) {
                golem = new EntityScarecrow(ModEntities.SCARECROW.get(), level);
                golem.setOwner(owner);
                BuildHelper.init(golem, level, x, y, z);
                BuildHelper.removeLarge(level, xAxis, x, y, z);
                BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
                return true;
            }
            return false;
        }
        else if (top.is(Blocks.GOLD_BLOCK) && bottom.is(Blocks.GOLD_BLOCK)) {
            if (!Properties.getBoolean("build.golems", "GildedGolem"))
                return false;
            golem = new EntityGildedGolem(ModEntities.GILDED_GOLEM.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x, y, z);
            BuildHelper.removeStandard(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.CRIT, x, y, z);
            return true;
        }
        else if (top.is(Blocks.MELON) && bottom.is(Blocks.MELON)) {
            if (!Properties.getBoolean("build.golems", "MelonGolem"))
                return false;
            golem = new EntityMelonGolem(ModEntities.MELON_GOLEM.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x, y, z);
            BuildHelper.removeStandard(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
            return true;
        }
        else if (top.is(Blocks.SOUL_SAND) && bottom.is(Blocks.SOUL_SAND)) {
            if (!Properties.getBoolean("build.golems", "BoundSoul"))
                return false;
            golem = new EntityBoundSoul(ModEntities.BOUND_SOUL.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x, y, z);
            BuildHelper.removeStandard(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.ENCHANTED_HIT, x, y, z);
            return true;
        }
        else if (top.is(Blocks.OBSIDIAN) && bottom.is(Blocks.OBSIDIAN)) {
            if (!Properties.getBoolean("build.golems", "ObsidianGolem"))
                return false;
            boolean xAxis = armLX.is(Blocks.OBSIDIAN) && armRX.is(Blocks.OBSIDIAN);
            boolean zAxis = armLZ.is(Blocks.OBSIDIAN) && armRZ.is(Blocks.OBSIDIAN);
            if (xAxis || zAxis) {
                golem = new EntityObsidianGolem(ModEntities.OBSIDIAN_GOLEM.get(), level);
                golem.setOwner(owner);
                BuildHelper.init(golem, level, x, y, z);
                BuildHelper.removeLarge(level, xAxis, x, y, z);
                BuildHelper.particleEffect(level, ParticleTypes.ENCHANTED_HIT, x, y, z);
                return true;
            }
            return false;
        }
        return false;
    }

    /// Spawns a turret, if possible.
    public static boolean placeTurret(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.turrets", "_all"))
            return false;
        EntityUtilityGolem golem;
        BlockState top = BuildHelper.state(level, x, y - 1, z);
        if (!top.is(Blocks.OAK_FENCE) && !top.is(Blocks.NETHER_BRICK_FENCE))
            return false;
        BlockState bottom = BuildHelper.state(level, x, y - 2, z);
        String owner = null;
        if (player != null) {
            owner = player.getGameProfile().getName();
        }
        if (bottom.is(Blocks.COBBLESTONE))
            return BuildHelper.turret(level, owner, "StoneTurret", new EntityStoneTurret(ModEntities.STONE_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.SNOW_BLOCK))
            return BuildHelper.turret(level, owner, "SnowTurret", new EntitySnowTurret(ModEntities.SNOW_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.STONE_BRICKS))
            return BuildHelper.turret(level, owner, "BrickTurret", new EntityBrickTurret(ModEntities.BRICK_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.GOLD_BLOCK))
            return BuildHelper.turret(level, owner, "GatlingTurret", new EntityGatlingTurret(ModEntities.GATLING_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.IRON_BLOCK))
            return BuildHelper.turret(level, owner, "ShotgunTurret", new EntityShotgunTurret(ModEntities.SHOTGUN_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.LAPIS_BLOCK))
            return BuildHelper.turret(level, owner, "SniperTurret", new EntitySniperTurret(ModEntities.SNIPER_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.REDSTONE_BLOCK))
            return BuildHelper.turret(level, owner, "FireTurret", new EntityFireTurret(ModEntities.FIRE_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.NETHERRACK))
            return BuildHelper.turret(level, owner, "FireballTurret", new EntityFireballTurret(ModEntities.FIREBALL_TURRET.get(), level), x, y, z);
        if (top.is(Blocks.NETHER_BRICK_FENCE) && bottom.is(Blocks.NETHER_BRICKS))
            return BuildHelper.turret(level, owner, "GhastTurret", new EntityGhastTurret(ModEntities.GHAST_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.EMERALD_BLOCK))
            return BuildHelper.turret(level, owner, "VolleyTurret", new EntityVolleyTurret(ModEntities.VOLLEY_TURRET.get(), level), x, y, z);
        if (bottom.is(Blocks.DIAMOND_BLOCK))
            return BuildHelper.turret(level, owner, "KillerTurret", new EntityKillerTurret(ModEntities.KILLER_TURRET.get(), level), x, y, z);
        if (top.is(Blocks.NETHER_BRICK_FENCE) && bottom.is(Blocks.OBSIDIAN))
            return BuildHelper.turret(level, owner, "ObsidianTurret", new EntityObsidianTurret(ModEntities.OBSIDIAN_TURRET.get(), level), x, y, z);
        return false;
    }

    /// The shared tail of every turret branch: config gate, spawn, clear the blocks, puff of particles.
    /// 1.12.2 repeated these seven lines twelve times over.
    private static boolean turret(Level level, String owner, String configKey, EntityUtilityGolem golem, int x, int y, int z) {
        if (!Properties.getBoolean("build.turrets", configKey))
            return false;
        golem.setOwner(owner);
        BuildHelper.init(golem, level, x, y, z);
        BuildHelper.removeStandard(level, x, y, z);
        BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
        return true;
    }

    /// Spawns a block golem, if possible.
    public static boolean placeBlock(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.blocks", "_all"))
            return false;
        EntityUtilityGolem golem;
        BlockState state = BuildHelper.state(level, x, y - 1, z);
        String owner = null;
        if (player != null) {
            owner = player.getGameProfile().getName();
        }
        if (state.is(Blocks.CRAFTING_TABLE))
            return BuildHelper.blockGolem(level, owner, "WorkbenchGolem", new EntityWorkbenchGolem(ModEntities.WORKBENCH_GOLEM.get(), level), false, x, y, z);
        if (state.is(Blocks.JACK_O_LANTERN))
            return BuildHelper.blockGolem(level, owner, "LanternGolem", new EntityLanternGolem(ModEntities.LANTERN_GOLEM.get(), level), false, x, y, z);
        if (state.is(Blocks.CHEST))
            return BuildHelper.blockGolem(level, owner, "ChestGolem", new EntityChestGolem(ModEntities.CHEST_GOLEM.get(), level), true, x, y, z);
        if (state.is(Blocks.TRAPPED_CHEST))
            return BuildHelper.blockGolem(level, owner, "ChestTrappedGolem", new EntityChestTrappedGolem(ModEntities.CHEST_TRAPPED_GOLEM.get(), level), true, x, y, z);
        if (state.is(Blocks.ENDER_CHEST))
            return BuildHelper.blockGolem(level, owner, "ChestEnderGolem", new EntityChestEnderGolem(ModEntities.CHEST_ENDER_GOLEM.get(), level), true, x, y, z);
        if (state.is(Blocks.FURNACE))
            return BuildHelper.blockGolem(level, owner, "FurnaceGolem", new EntityFurnaceGolem(ModEntities.FURNACE_GOLEM.get(), level), true, x, y, z);
        if (state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)) {
            if (!Properties.getBoolean("build.blocks", "AnvilGolem"))
                return false;
            // 1.12.2 read the anvil's damage out of its metadata; the three stages are three blocks now.
            int damage = state.is(Blocks.DAMAGED_ANVIL) ? 2 : state.is(Blocks.CHIPPED_ANVIL) ? 1 : 0;
            EntityAnvilGolem anvil = new EntityAnvilGolem(ModEntities.ANVIL_GOLEM.get(), level);
            anvil.setOwner(owner);
            anvil.setDamage(damage);
            BuildHelper.getContents(level, anvil, x, y - 1, z);
            anvil.sitAI().sit = true;
            BuildHelper.init(anvil, level, x, y + 1, z);
            BuildHelper.removeShort(level, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
            return true;
        }
        if (state.is(Blocks.JUKEBOX))
            return BuildHelper.blockGolem(level, owner, "JukeboxGolem", new EntityJukeboxGolem(ModEntities.JUKEBOX_GOLEM.get(), level), false, x, y, z);
        return false;
    }

    /// The shared tail of every block-golem branch. takeContents is true for the ones that inherit the
    /// block's inventory.
    private static boolean blockGolem(Level level, String owner, String configKey, EntityUtilityGolem golem, boolean takeContents, int x, int y, int z) {
        if (!Properties.getBoolean("build.blocks", configKey))
            return false;
        golem.setOwner(owner);
        if (takeContents) {
            BuildHelper.getContents(level, (EntityContainerGolem)golem, x, y - 1, z);
        }
        golem.sitAI().sit = true;
        BuildHelper.init(golem, level, x, y + 1, z);
        BuildHelper.removeShort(level, x, y, z);
        BuildHelper.particleEffect(level, ParticleTypes.ITEM_SNOWBALL, x, y, z);
        return true;
    }

    /// Spawns a hostile golem, if possible.
    public static boolean placeHostile(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.hostiles", "_all"))
            return false;
        /// There are no hostile golems yet. (Except the Wither.)
        return false;
    }

    /// Spawns a colossal golem, if possible.
    public static boolean placeColossal(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.colossals", "_all"))
            return false;
        EntityUtilityGolem golem;
        int direction;
        int[] xOff = { -1, 1, 0, 0 };
        int[] zOff = { 0, 0, -1, 1 };

        String owner = null;
        if (player != null) {
            owner = player.getGameProfile().getName();
        }
        direction = BuildHelper.checkColossal(level, Blocks.COBBLESTONE.defaultBlockState(), x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "StoneColossus"))
                return false;
            golem = new EntityStoneColossus(ModEntities.STONE_COLOSSUS.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(level, direction, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.CRIT, x, y, z);
            return true;
        }
        direction = BuildHelper.checkColossal(level, Blocks.OBSIDIAN.defaultBlockState(), x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "ObsidianColossus"))
                return false;
            golem = new EntityObsidianColossus(ModEntities.OBSIDIAN_COLOSSUS.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(level, direction, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.CRIT, x, y, z);
            return true;
        }
        direction = BuildHelper.checkColossal(level, Blocks.IRON_BLOCK.defaultBlockState(), x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "ArmorColossus"))
                return false;
            golem = new EntityArmorColossus(ModEntities.ARMOR_COLOSSUS.get(), level);
            golem.setOwner(owner);
            BuildHelper.init(golem, level, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(level, direction, x, y, z);
            BuildHelper.particleEffect(level, ParticleTypes.CRIT, x, y, z);
            return true;
        }
        return false;
    }

    // Attempts to replace a vanilla golem just placed by the player.
    public static boolean replaceGolem(Level level, Player player, int x, int y, int z) {
        if (!Properties.getBoolean("build.golems", "_all"))
            return false;
        String owner = null;
        if (player != null) {
            owner = player.getGameProfile().getName();
        }
        // 1.12.2 scanned the whole loaded entity list. The golem must be standing in this exact spot, so
        // a box around it finds the same entity without walking every entity in the world.
        AABB area = new AABB(x - 1.0, y - 3.0, z - 1.0, x + 2.0, y + 1.0, z + 2.0);
        List<net.minecraft.world.entity.animal.AbstractGolem> golems =
            level.getEntitiesOfClass(net.minecraft.world.entity.animal.AbstractGolem.class, area);
        for (net.minecraft.world.entity.animal.AbstractGolem golem : golems) {
            if (golem.getY() == y - 1.95 && golem.getX() == x + 0.5 && golem.getZ() == z + 0.5) {
                EntityUtilityGolem newGolem;
                if (golem instanceof IronGolem ironGolem && ironGolem.isPlayerCreated()) {
                    if (!Properties.getBoolean("build.golems", "UMIronGolem"))
                        return false;
                    newGolem = new EntityUMIronGolem(ModEntities.UM_IRON_GOLEM.get(), level);
                    newGolem.setOwner(owner);
                    BuildHelper.init(newGolem, level, x, y, z);
                    golem.discard();
                    return true;
                }
                if (golem instanceof SnowGolem) {
                    if (!Properties.getBoolean("build.golems", "UMSnowGolem"))
                        return false;
                    newGolem = new EntityUMSnowGolem(ModEntities.UM_SNOW_GOLEM.get(), level);
                    newGolem.setOwner(owner);
                    BuildHelper.init(newGolem, level, x, y, z);
                    golem.discard();
                    return true;
                }
            }
        }
        return false;
    }

    /// Sets the golem to a standard position based on the head block.
    public static void init(EntityUtilityGolem golem, Level level, int x, int y, int z) {
        golem.moveTo(x + 0.5, y - 1.95, z + 0.5, 0.0F, 0.0F);
        if (level instanceof ServerLevel serverLevel) {
            golem.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.MOB_SUMMONED, null, null);
        }
        level.addFreshEntity(golem);
    }

    /// Checks the area around the given creeper head for a colossal golem made of the given block.
    public static int checkColossal(Level level, BlockState targetBlock, int x, int y, int z) {
        boolean failedXn = false;
        boolean failedXp = false;
        boolean failedZn = false;
        boolean failedZp = false;
        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j >= -3; j--) {
                if (j < -1 && (i == -2 || i == 2)) {
                    continue;
                }
                if (j == -3 && i == 0) {
                    continue;
                }
                if (!failedXn && !BuildHelper.state(level, x - 1, y + j, z + i).is(targetBlock.getBlock())) {
                    failedXn = true;
                }
                if (!failedXp && !BuildHelper.state(level, x + 1, y + j, z + i).is(targetBlock.getBlock())) {
                    failedXp = true;
                }
                if (!failedZn && !BuildHelper.state(level, x + i, y + j, z - 1).is(targetBlock.getBlock())) {
                    failedZn = true;
                }
                if (!failedZp && !BuildHelper.state(level, x + i, y + j, z + 1).is(targetBlock.getBlock())) {
                    failedZp = true;
                }
            }
        }
        if (!failedXn)
            return 0;
        if (!failedXp)
            return 1;
        if (!failedZn)
            return 2;
        if (!failedZp)
            return 3;
        return -1;
    }

    /// Removes the blocks to make a colossus.
    public static void removeColossal(Level level, int direction, int x, int y, int z) {
        BuildHelper.removeBlock(level, x, y, z);
        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j >= -3; j--) {
                if (j < -1 && (i == -2 || i == 2)) {
                    continue;
                }
                if (j == -3 && i == 0) {
                    continue;
                }
                switch (direction) {
                    case 0:
                        BuildHelper.removeBlock(level, x - 1, y + j, z + i);
                        break;
                    case 1:
                        BuildHelper.removeBlock(level, x + 1, y + j, z + i);
                        break;
                    case 2:
                        BuildHelper.removeBlock(level, x + i, y + j, z - 1);
                        break;
                    case 3:
                        BuildHelper.removeBlock(level, x + i, y + j, z + 1);
                }
            }
        }
    }

    /// Removes the block and marks it to be updated.
    public static void removeBlock(Level level, int x, int y, int z) {
        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    }

    /// Removes the standard two blocks used to make short golems, the coords passed being the head.
    public static void removeShort(Level level, int x, int y, int z) {
        BuildHelper.removeBlock(level, x, y, z);
        BuildHelper.removeBlock(level, x, y - 1, z);
    }

    /// Removes the standard three blocks used to make golems, the coords passed being the head.
    public static void removeStandard(Level level, int x, int y, int z) {
        BuildHelper.removeBlock(level, x, y, z);
        BuildHelper.removeBlock(level, x, y - 1, z);
        BuildHelper.removeBlock(level, x, y - 2, z);
    }

    /// Removes the standard five blocks used to make large golems, the coords passed being the head.
    public static void removeLarge(Level level, boolean xAxis, int x, int y, int z) {
        BuildHelper.removeStandard(level, x, y, z);
        if (xAxis) {
            BuildHelper.removeBlock(level, x - 1, y - 1, z);
            BuildHelper.removeBlock(level, x + 1, y - 1, z);
        }
        else {
            BuildHelper.removeBlock(level, x, y - 1, z - 1);
            BuildHelper.removeBlock(level, x, y - 1, z + 1);
        }
    }

    /// Loads the given block entity's data to the golem.
    public static void getContents(Level level, EntityContainerGolem golem, int x, int y, int z) {
        BlockEntity blockEntity = level.getBlockEntity(new BlockPos(x, y, z));
        if (blockEntity != null) {
            CompoundTag tag = blockEntity.saveWithoutMetadata();
            golem.takeContentsFromNBT(tag);
            blockEntity.load(tag);
        }
    }

    /// Creates the particle effect when a golem is spawned.
    public static void particleEffect(Level level, ParticleOptions particle, int x, int y, int z) {
        for (int i = 120; i-- > 0;) {
            level.addParticle(particle, x + level.random.nextDouble(), y - 2 + level.random.nextDouble() * 2.5, z + level.random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    /// 1.12.2 passed the face as an int index; the event hands over a Direction directly now.
    public static BlockPos offset(BlockPos pos, Direction face) {
        return pos.relative(face);
    }
}
