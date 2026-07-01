package toast.utilityMobs;

import java.util.ArrayList;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.oredict.OreDictionary;
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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BuildHelper
{
    /// A set of usernames for players right-clicking a Block.
    public final HashSet<String> clickingBlock = new HashSet<String>();

    /// Lazily-built set of every block registered under the "fenceWood" ore dictionary.
    private static HashSet<Block> fenceWoodBlocks;

    /// True if the block is any wooden fence (oak, spruce, birch, ... and modded ones via the
    /// "fenceWood" ore dictionary). Used so golem builds accept any wood fence, not just oak.
    public static boolean isFenceWood(Block block) {
        if (block == null)
            return false;
        if (BuildHelper.fenceWoodBlocks == null) {
            HashSet<Block> set = new HashSet<Block>();
            for (ItemStack stack : OreDictionary.getOres("fenceWood")) {
                Block b = Block.getBlockFromItem(stack.getItem());
                if (b != Blocks.AIR) {
                    set.add(b);
                }
            }
            set.add(Blocks.OAK_FENCE); // safety net in case the ore dict entry is ever missing
            BuildHelper.fenceWoodBlocks = set;
        }
        return BuildHelper.fenceWoodBlocks.contains(block);
    }

    public BuildHelper() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Convenience: block at integer coords (1.12.2 is IBlockState/BlockPos based).
    private static Block block(World world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    /**
     * Called when the player right-clicks a block; queues a golem build server-side.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        BlockPos pos = event.getPos();
        boolean targetBook = false;
        if (event.getWorld().getBlockState(pos).getBlock() != Blocks.CRAFTING_TABLE) {
            targetBook = BookHelper.checkBook(player);
        }
        if (!player.world.isRemote) {
            new BlockEvent(player, pos.getX(), pos.getY(), pos.getZ(), event.getFace().getIndex());
        }
        else if (targetBook) {
            this.clickingBlock.add(player.getName());
        }
    }

    /**
     * Called when the player right-clicks with an item in the air; handles target-book sync.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        // Refresh the target list into the book, then let the book GUI open normally - including when
        // clicking air, so the target list is no longer gated behind aiming at a block.
        BookHelper.checkBook(player);
        if (player.world.isRemote) {
            this.clickingBlock.remove(player.getName());
        }
    }

    /**
     * Called when the player right-clicks an entity; lets the target book interact with it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof EntityLivingBase && BookHelper.interact(event.getEntityPlayer(), (EntityLivingBase)event.getTarget())) {
            event.setCanceled(true);
        }
    }

    /// Spawns a golem, if possible.
    public static boolean place(World world, EntityPlayer player, boolean holdingGolemHead, int x, int y, int z) {
        Block block = block(world, x, y, z);
        if (block == Blocks.PUMPKIN || block == Blocks.LIT_PUMPKIN)
            return BuildHelper.placeGolem(world, player, x, y, z);
        if (block == Blocks.DISPENSER)
            return BuildHelper.placeTurret(world, player, x, y, z);
        if (block == Blocks.SKULL) {
            // Colossal golems: ANY mob head works (the structure beneath decides which golem is built).
            if (BuildHelper.placeColossal(world, player, x, y, z))
                return true;
            // Block golems: SKELETON skull only (skull type 0). Other heads won't animate a block golem.
            if (BuildHelper.getSkullType(world, x, y, z) == 0 && BuildHelper.placeBlock(world, player, x, y, z))
                return true;
            return BuildHelper.placeHostile(world, player, x, y, z);
        }
        if (holdingGolemHead)
            return BuildHelper.replaceGolem(world, player, x, y, z);
        return false;
    }

    /// Returns the skull type.
    public static int getSkullType(World world, int x, int y, int z) {
        try {
            return ((TileEntitySkull)world.getTileEntity(new BlockPos(x, y, z))).getSkullType();
        }
        catch (Exception ex) {
            // Do nothing
        }
        return -1;
    }

    /// Spawns a golem, if possible.
    public static boolean placeGolem(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.golems", "_all"))
            return false;
        EntityUtilityGolem golem;
        Block top = block(world, x, y - 1, z);
        Block bottom = block(world, x, y - 2, z);
        Block armLX = block(world, x - 1, y - 1, z);
        Block armRX = block(world, x + 1, y - 1, z);
        Block armLZ = block(world, x, y - 1, z - 1);
        Block armRZ = block(world, x, y - 1, z + 1);
        String owner = null;
        if (player != null) {
            owner = player.getName();
        }
        if (top == Blocks.COBBLESTONE && bottom == Blocks.COBBLESTONE) {
            boolean xAxis = armLX == Blocks.COBBLESTONE && armRX == Blocks.COBBLESTONE;
            boolean zAxis = armLZ == Blocks.COBBLESTONE && armRZ == Blocks.COBBLESTONE;
            if (!Properties.getBoolean("build.golems", "StoneLargeGolem")) {
                xAxis = false;
                zAxis = false;
            }
            if (xAxis || zAxis) {
                golem = new EntityStoneLargeGolem(world);
                golem.setOwner(owner);
                BuildHelper.init(golem, world, x, y, z);
                BuildHelper.removeLarge(world, xAxis, x, y, z);
                BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
                return true;
            }
            else if (!Properties.getBoolean("build.golems", "StoneGolem"))
                return false;
            golem = new EntityStoneGolem(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowshovel", x, y, z);
            return true;
        }
        else if ((top == Blocks.FURNACE || top == Blocks.LIT_FURNACE) && bottom == Blocks.COBBLESTONE) {
            if (!Properties.getBoolean("build.golems", "SteamGolem"))
                return false;
            boolean xAxis = armLX == Blocks.COBBLESTONE && armRX == Blocks.COBBLESTONE;
            boolean zAxis = armLZ == Blocks.COBBLESTONE && armRZ == Blocks.COBBLESTONE;
            if (xAxis || zAxis) {
                golem = new EntitySteamGolem(world);
                golem.setOwner(owner);
                BuildHelper.init(golem, world, x, y, z);
                BuildHelper.removeLarge(world, xAxis, x, y, z);
                BuildHelper.particleEffect(world, "largesmoke", x, y, z);
                return true;
            }
            return false;
        }
        else if (top == Blocks.IRON_BLOCK && bottom == Blocks.IRON_BLOCK) {
            if (armLX == Blocks.IRON_BLOCK && armRX == Blocks.IRON_BLOCK || armLZ == Blocks.IRON_BLOCK && armRZ == Blocks.IRON_BLOCK)
                return false;
            if (!Properties.getBoolean("build.golems", "ArmorGolem"))
                return false;
            golem = new EntityArmorGolem(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        else if (Properties.getBoolean("build.golems", "Scarecrow") && top == Blocks.WOOL && BuildHelper.isFenceWood(bottom)) {
            boolean xAxis = BuildHelper.isFenceWood(armLX) && BuildHelper.isFenceWood(armRX);
            boolean zAxis = BuildHelper.isFenceWood(armLZ) && BuildHelper.isFenceWood(armRZ);
            if (xAxis || zAxis) {
                golem = new EntityScarecrow(world);
                golem.setOwner(owner);
                BuildHelper.init(golem, world, x, y, z);
                BuildHelper.removeLarge(world, xAxis, x, y, z);
                BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
                return true;
            }
            return false;
        }
        else if (top == Blocks.GOLD_BLOCK && bottom == Blocks.GOLD_BLOCK) {
            if (!Properties.getBoolean("build.golems", "GildedGolem"))
                return false;
            golem = new EntityGildedGolem(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "critical", x, y, z);
            return true;
        }
        else if (top == Blocks.MELON_BLOCK && bottom == Blocks.MELON_BLOCK) {
            if (!Properties.getBoolean("build.golems", "MelonGolem"))
                return false;
            golem = new EntityMelonGolem(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        else if (top == Blocks.SOUL_SAND && bottom == Blocks.SOUL_SAND) {
            if (!Properties.getBoolean("build.golems", "BoundSoul"))
                return false;
            golem = new EntityBoundSoul(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "magicCrit", x, y, z);
            return true;
        }
        else if (top == Blocks.OBSIDIAN && bottom == Blocks.OBSIDIAN) {
            if (!Properties.getBoolean("build.golems", "ObsidianGolem"))
                return false;
            boolean xAxis = armLX == Blocks.OBSIDIAN && armRX == Blocks.OBSIDIAN;
            boolean zAxis = armLZ == Blocks.OBSIDIAN && armRZ == Blocks.OBSIDIAN;
            if (xAxis || zAxis) {
                golem = new EntityObsidianGolem(world);
                golem.setOwner(owner);
                BuildHelper.init(golem, world, x, y, z);
                BuildHelper.removeLarge(world, xAxis, x, y, z);
                BuildHelper.particleEffect(world, "magicCrit", x, y, z);
                return true;
            }
            return false;
        }
        return false;
    }

    /// Spawns a turret, if possible.
    public static boolean placeTurret(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.turrets", "_all"))
            return false;
        EntityUtilityGolem golem;
        Block top = block(world, x, y - 1, z);
        if (top != Blocks.OAK_FENCE && top != Blocks.NETHER_BRICK_FENCE)
            return false;
        Block bottom = block(world, x, y - 2, z);
        String owner = null;
        if (player != null) {
            owner = player.getName();
        }
        if (bottom == Blocks.COBBLESTONE) {
            if (!Properties.getBoolean("build.turrets", "StoneTurret"))
                return false;
            golem = new EntityStoneTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.SNOW) {
            if (!Properties.getBoolean("build.turrets", "SnowTurret"))
                return false;
            golem = new EntitySnowTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.STONEBRICK) {
            if (!Properties.getBoolean("build.turrets", "BrickTurret"))
                return false;
            golem = new EntityBrickTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.GOLD_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "GatlingTurret"))
                return false;
            golem = new EntityGatlingTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.IRON_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "ShotgunTurret"))
                return false;
            golem = new EntityShotgunTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.LAPIS_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "SniperTurret"))
                return false;
            golem = new EntitySniperTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.REDSTONE_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "FireTurret"))
                return false;
            golem = new EntityFireTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.NETHERRACK) {
            if (!Properties.getBoolean("build.turrets", "FireballTurret"))
                return false;
            golem = new EntityFireballTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (top == Blocks.NETHER_BRICK_FENCE && bottom == Blocks.NETHER_BRICK) {
            if (!Properties.getBoolean("build.turrets", "GhastTurret"))
                return false;
            golem = new EntityGhastTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.EMERALD_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "VolleyTurret"))
                return false;
            golem = new EntityVolleyTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (bottom == Blocks.DIAMOND_BLOCK) {
            if (!Properties.getBoolean("build.turrets", "KillerTurret"))
                return false;
            golem = new EntityKillerTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (top == Blocks.NETHER_BRICK_FENCE && bottom == Blocks.OBSIDIAN) {
            if (!Properties.getBoolean("build.turrets", "ObsidianTurret"))
                return false;
            golem = new EntityObsidianTurret(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x, y, z);
            BuildHelper.removeStandard(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        return false;
    }

    /// Spawns a block golem, if possible.
    public static boolean placeBlock(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.blocks", "_all"))
            return false;
        EntityUtilityGolem golem;
        Block block = block(world, x, y - 1, z);
        String owner = null;
        if (player != null) {
            owner = player.getName();
        }
        if (block == Blocks.CRAFTING_TABLE) {
            if (!Properties.getBoolean("build.blocks", "WorkbenchGolem"))
                return false;
            golem = new EntityWorkbenchGolem(world);
            golem.setOwner(owner);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.LIT_PUMPKIN) {
            if (!Properties.getBoolean("build.blocks", "LanternGolem"))
                return false;
            golem = new EntityLanternGolem(world);
            golem.setOwner(owner);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.CHEST) {
            if (!Properties.getBoolean("build.blocks", "ChestGolem"))
                return false;
            golem = new EntityChestGolem(world);
            golem.setOwner(owner);
            BuildHelper.getContents(world, (EntityContainerGolem)golem, x, y - 1, z);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.TRAPPED_CHEST) {
            if (!Properties.getBoolean("build.blocks", "ChestTrappedGolem"))
                return false;
            golem = new EntityChestTrappedGolem(world);
            golem.setOwner(owner);
            BuildHelper.getContents(world, (EntityContainerGolem)golem, x, y - 1, z);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.ENDER_CHEST) {
            if (!Properties.getBoolean("build.blocks", "ChestEnderGolem"))
                return false;
            golem = new EntityChestEnderGolem(world);
            golem.setOwner(owner);
            BuildHelper.getContents(world, (EntityContainerGolem)golem, x, y - 1, z);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.FURNACE || block == Blocks.LIT_FURNACE) {
            if (!Properties.getBoolean("build.blocks", "FurnaceGolem"))
                return false;
            golem = new EntityFurnaceGolem(world);
            golem.setOwner(owner);
            BuildHelper.getContents(world, (EntityContainerGolem)golem, x, y - 1, z);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.ANVIL) {
            if (!Properties.getBoolean("build.blocks", "AnvilGolem"))
                return false;
            int damage = Blocks.ANVIL.damageDropped(world.getBlockState(new BlockPos(x, y - 1, z)));
            golem = new EntityAnvilGolem(world);
            golem.setOwner(owner);
            ((EntityAnvilGolem)golem).setDamage(damage);
            BuildHelper.getContents(world, (EntityContainerGolem)golem, x, y - 1, z);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        if (block == Blocks.JUKEBOX) {
            if (!Properties.getBoolean("build.blocks", "JukeboxGolem"))
                return false;
            golem = new EntityJukeboxGolem(world);
            golem.setOwner(owner);
            golem.sitAI.sit = true;
            BuildHelper.init(golem, world, x, y + 1, z);
            BuildHelper.removeShort(world, x, y, z);
            BuildHelper.particleEffect(world, "snowballpoof", x, y, z);
            return true;
        }
        return false;
    }

    /// Spawns a hostile golem, if possible.
    public static boolean placeHostile(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.hostiles", "_all"))
            return false;
        /// There are no hostile golems yet. (Except the Wither.)
        return false;
    }

    /// Spawns a colossal golem, if possible.
    public static boolean placeColossal(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.colossals", "_all"))
            return false;
        EntityUtilityGolem golem;
        int direction;
        int[] xOff = { -1, 1, 0, 0 };
        int[] zOff = { 0, 0, -1, 1 };

        String owner = null;
        if (player != null) {
            owner = player.getName();
        }
        direction = BuildHelper.checkColossal(world, Blocks.COBBLESTONE, x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "StoneColossus"))
                return false;
            golem = new EntityStoneColossus(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(world, direction, x, y, z);
            BuildHelper.particleEffect(world, "critical", x, y, z);
            return true;
        }
        direction = BuildHelper.checkColossal(world, Blocks.OBSIDIAN, x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "ObsidianColossus"))
                return false;
            golem = new EntityObsidianColossus(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(world, direction, x, y, z);
            BuildHelper.particleEffect(world, "critical", x, y, z);
            return true;
        }
        direction = BuildHelper.checkColossal(world, Blocks.IRON_BLOCK, x, y, z);
        if (direction >= 0) {
            if (!Properties.getBoolean("build.colossals", "ArmorColossus"))
                return false;
            golem = new EntityArmorColossus(world);
            golem.setOwner(owner);
            BuildHelper.init(golem, world, x + xOff[direction], y - 1, z + zOff[direction]);
            BuildHelper.removeColossal(world, direction, x, y, z);
            BuildHelper.particleEffect(world, "critical", x, y, z);
            return true;
        }
        return false;
    }

    // Attempts to replace a vanilla golem just placed by the player.
    public static boolean replaceGolem(World world, EntityPlayer player, int x, int y, int z) {
        if (!Properties.getBoolean("build.golems", "_all"))
            return false;
        String owner = null;
        if (player != null) {
            owner = player.getName();
        }
        for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
            if (entity instanceof EntityGolem) {
                EntityGolem golem = (EntityGolem) entity;
                if (golem.posY == y - 1.95 && golem.posX == x + 0.5 && golem.posZ == z + 0.5) {
                    EntityUtilityGolem newGolem;
                    if (golem instanceof EntityIronGolem && ((EntityIronGolem)golem).isPlayerCreated()) {
                        if (!Properties.getBoolean("build.golems", "UMIronGolem"))
                            return false;
                        newGolem = new EntityUMIronGolem(world);
                        newGolem.setOwner(owner);
                        BuildHelper.init(newGolem, world, x, y, z);
                        golem.setDead();
                        return true;
                    }
                    if (golem instanceof EntitySnowman) {
                        if (!Properties.getBoolean("build.golems", "UMSnowGolem"))
                            return false;
                        newGolem = new EntityUMSnowGolem(world);
                        newGolem.setOwner(owner);
                        BuildHelper.init(newGolem, world, x, y, z);
                        golem.setDead();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Sets the golem to a standard position based on the head block.
    public static void init(EntityUtilityGolem golem, World world, int x, int y, int z) {
        golem.setLocationAndAngles(x + 0.5, y - 1.95, z + 0.5, 0.0F, 0.0F);
        golem.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), (IEntityLivingData)null);
        world.spawnEntity(golem);
    }

    /// Checks the area around the given creeper head for a colossal golem made of the given block.
    public static int checkColossal(World world, Block targetBlock, int x, int y, int z) {
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
                if (!failedXn && block(world, x - 1, y + j, z + i) != targetBlock) {
                    failedXn = true;
                }
                if (!failedXp && block(world, x + 1, y + j, z + i) != targetBlock) {
                    failedXp = true;
                }
                if (!failedZn && block(world, x + i, y + j, z - 1) != targetBlock) {
                    failedZn = true;
                }
                if (!failedZp && block(world, x + i, y + j, z + 1) != targetBlock) {
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
    public static void removeColossal(World world, int direction, int x, int y, int z) {
        BuildHelper.removeBlock(world, x, y, z);
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
                        BuildHelper.removeBlock(world, x - 1, y + j, z + i);
                        break;
                    case 1:
                        BuildHelper.removeBlock(world, x + 1, y + j, z + i);
                        break;
                    case 2:
                        BuildHelper.removeBlock(world, x + i, y + j, z - 1);
                        break;
                    case 3:
                        BuildHelper.removeBlock(world, x + i, y + j, z + 1);
                }
            }
        }
    }

    /// Removes the block and marks it to be updated.
    public static void removeBlock(World world, int x, int y, int z) {
        world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
    }

    /// Removes the standard two blocks used to make short golems, the coords passed being the head.
    public static void removeShort(World world, int x, int y, int z) {
        BuildHelper.removeBlock(world, x, y, z);
        BuildHelper.removeBlock(world, x, y - 1, z);
    }

    /// Removes the standard three blocks used to make golems, the coords passed being the head.
    public static void removeStandard(World world, int x, int y, int z) {
        BuildHelper.removeBlock(world, x, y, z);
        BuildHelper.removeBlock(world, x, y - 1, z);
        BuildHelper.removeBlock(world, x, y - 2, z);
    }

    /// Removes the standard five blocks used to make large golems, the coords passed being the head.
    public static void removeLarge(World world, boolean xAxis, int x, int y, int z) {
        BuildHelper.removeStandard(world, x, y, z);
        if (xAxis) {
            BuildHelper.removeBlock(world, x - 1, y - 1, z);
            BuildHelper.removeBlock(world, x + 1, y - 1, z);
        }
        else {
            BuildHelper.removeBlock(world, x, y - 1, z - 1);
            BuildHelper.removeBlock(world, x, y - 1, z + 1);
        }
    }

    /// Loads the given tile entity's data to the golem.
    public static void getContents(World world, EntityContainerGolem golem, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (tileEntity != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag = tileEntity.writeToNBT(tag);
            golem.takeContentsFromNBT(tag);
            tileEntity.readFromNBT(tag);
        }
    }

    // Maps the legacy particle name to the 1.12.2 particle type.
    private static EnumParticleTypes particle(String name) {
        switch (name) {
            case "snowshovel": return EnumParticleTypes.SNOW_SHOVEL;
            case "largesmoke": return EnumParticleTypes.SMOKE_LARGE;
            case "critical": return EnumParticleTypes.CRIT;
            case "magicCrit": return EnumParticleTypes.CRIT_MAGIC;
            case "snowballpoof":
            default: return EnumParticleTypes.SNOWBALL;
        }
    }

    /// Creates the particle effect when a golem is spawned.
    public static void particleEffect(World world, String particle, int x, int y, int z) {
        EnumParticleTypes type = particle(particle);
        for (int i = 120; i-- > 0;) {
            world.spawnParticle(type, x + world.rand.nextDouble(), y - 2 + world.rand.nextDouble() * 2.5, z + world.rand.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
