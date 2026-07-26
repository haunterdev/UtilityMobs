package toast.utilityMobs;

import java.io.File;
import java.util.Random;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toast.utilityMobs.setup.ModEntities;
import toast.utilityMobs.setup.ModRegistries;

@Mod(_UtilityMobs.MODID)
public class _UtilityMobs {
    public static final String MODID = "utilitymobs";
    public static final Logger LOG = LoggerFactory.getLogger("UtilityMobs");

    // If true, this mod starts up in debug mode.
    public static final boolean debug = false;
    // The mod's random number generator.
    public static final Random random = new Random();

    // The texture path prefix.
    public static final String TEXTURE = _UtilityMobs.MODID + ":textures/models/";

    // Spawn egg colors per individual mob (primary, secondary), parallel to UTILITY_NAMES[i][j].
    // Colors chosen to read as the block/material each mob is built from.
    public static final int[][][] EGG_COLORS = {
        { // Block golems (skeleton skull base)
            { 0x494949, 0x2B2B2B }, // AnvilGolem - anvil iron
            { 0x2E7D6B, 0x0A1A17 }, // ChestEnderGolem - ender teal
            { 0x8B5A2B, 0x5A3A1B }, // ChestGolem - wood
            { 0x8B5A2B, 0x8B0000 }, // ChestTrappedGolem - wood / redstone
            { 0x7A7A7A, 0x3A3A3A }, // FurnaceGolem - furnace stone
            { 0x4A2E12, 0xA0522D }, // JukeboxGolem - dark wood
            { 0xE8870E, 0xFFD56B }, // LanternGolem - jack o'lantern
            { 0x8B5A2B, 0xA0522D }, // WorkbenchGolem - crafting table
        },
        { // Golems (pumpkin base)
            { 0xC0C0C0, 0x6E6E6E }, // ArmorGolem - iron
            { 0x5B4636, 0x2E2218 }, // BoundSoul - soul sand
            { 0xFFD700, 0xB8860B }, // GildedGolem - gold
            { 0x2E7D32, 0x8BC34A }, // MelonGolem - melon green
            { 0x3B2E5A, 0x120A1F }, // ObsidianGolem - obsidian
            { 0xD9C36B, 0x8B7B3A }, // Scarecrow - hay / wool
            { 0x6E6E6E, 0xE8870E }, // SteamGolem - furnace / fire
            { 0x8A8A8A, 0x5A5A5A }, // StoneGolem - cobblestone
            { 0x6E6E6E, 0x3A3A3A }, // StoneLargeGolem - dark cobble
        },
        { // Hostile (none registered)
        },
        { // Turrets (dispenser base)
            { 0x8A8A8A, 0x6A6A6A }, // BrickTurret - stone brick
            { 0x6E2B2B, 0x3A1414 }, // FireballTurret - netherrack
            { 0xC02020, 0x6E0000 }, // FireTurret - redstone block
            { 0xFFD700, 0xB8860B }, // GatlingTurret - gold
            { 0x3A2020, 0x1A0E0E }, // GhastTurret - nether brick
            { 0x4AEDD9, 0x1FA89A }, // KillerTurret - diamond
            { 0x3B2E5A, 0x120A1F }, // ObsidianTurret - obsidian
            { 0xC0C0C0, 0x6E6E6E }, // ShotgunTurret - iron block
            { 0x1F4FA8, 0x0A2350 }, // SniperTurret - lapis
            { 0xEAF6FF, 0xA9C6E0 }, // SnowTurret - snow
            { 0x8A8A8A, 0x5A5A5A }, // StoneTurret - cobblestone
            { 0x2ECC71, 0x148F4E }, // VolleyTurret - emerald
        },
        { // Colossal (creeper head base)
            { 0xC0C0C0, 0x6E6E6E }, // ArmorColossus - iron
            { 0x3B2E5A, 0x120A1F }, // ObsidianColossus - obsidian
            { 0x8A8A8A, 0x5A5A5A }, // StoneColossus - cobblestone
        },
    };

    // Utility mob type array. Based on the block used to create them.
    public static final String[] UTILITY_TYPES = {
        "Block", "Golem", "Hostile", "Turret", "Colossal"
    };
    // Utility mob sub-type array. First dimension is the UTILITY_TYPES[].
    public static final String[][] UTILITY_NAMES = {
        {/* skeleton skull */ "AnvilGolem", "ChestEnderGolem", "ChestGolem", "ChestTrappedGolem", "FurnaceGolem", "JukeboxGolem", "LanternGolem", "WorkbenchGolem" },
        {/* pumpkin */ "ArmorGolem", "BoundSoul", "GildedGolem", "MelonGolem", "ObsidianGolem", "Scarecrow", "SteamGolem", "StoneGolem", "StoneLargeGolem" },
        {/* wither skull */ },
        {/* dispenser */ "BrickTurret", "FireballTurret", "FireTurret", "GatlingTurret", "GhastTurret", "KillerTurret", "ObsidianTurret", "ShotgunTurret", "SniperTurret", "SnowTurret", "StoneTurret", "VolleyTurret" },
        {/* creeper head */ "ArmorColossus", "ObsidianColossus", "StoneColossus" }
    };

    public _UtilityMobs() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistries.register(modBus);
        // Touch the registration holders so their static DeferredRegister entries are created before
        // the registry events fire.
        ModEntities.STONE_GOLEM.getId();
        toast.utilityMobs.setup.ModBlocks.GOLEM_LIGHT.getId();
        toast.utilityMobs.setup.ModMenus.LANTERN_GOLEM.getId();
        toast.utilityMobs.setup.ModMenus.TURRET_GOLEM.getId();
        toast.utilityMobs.setup.ModMenus.STEAM_GOLEM.getId();
        toast.utilityMobs.setup.ModRecipes.SAVE_PERMISSIONS.getId();
        toast.utilityMobs.setup.ModItems.ADMIN_SWORD.getId();
        CreativeTabUtilityMobs.TAB.getId();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Properties.SPEC);
        modBus.addListener(Properties::onConfigLoad);
        // 1.12.2 set this in preInit from event.getModConfigurationDirectory(). FMLPaths.CONFIGDIR is
        // the same directory (run/config), so the save path is unchanged: run/config/UtilityMobs.
        // Must be assigned before any TargetHelper save/load runs, or every save throws and is swallowed.
        TargetHelper.SAVE_DIRECTORY = new File(FMLPaths.CONFIGDIR.get().toFile(), "UtilityMobs");
        toast.utilityMobs.network.UMChannel.register();
        // Forge-bus subscribers. 1.12.2 constructed these in init; each still registers itself.
        new BuildHelper();
        new EventHandler();
        new TickHandler();
        new GuideBook();
    }

    // Inserts a space before every capital letter (except the first).
    public static String parseName(String name) {
        if (name.length() > 1) {
            for (int i = 1; i < name.length(); i++) {
                if (Character.isUpperCase(name.charAt(i)))
                    return name.substring(0, i) + " " + _UtilityMobs.parseName(name.substring(i));
            }
        }
        return name;
    }

    // Capitalizes or decapitalizes the given string.
    public static String cap(String string) {
        if (string.length() > 0)
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        return string;
    }
    public static String decap(String string) {
        if (string.length() > 0)
            return string.substring(0, 1).toLowerCase() + string.substring(1);
        return string;
    }

    // Prints the message to the console with this mod's name tag.
    public static void console(String... messages) {
        StringBuilder message = new StringBuilder();
        for (String part : messages) {
            message.append(part);
        }
        LOG.info(message.toString());
    }

    // Prints the message to the console with this mod's name tag if debugging is enabled.
    public static void debugConsole(String... messages) {
        if (_UtilityMobs.debug) {
            _UtilityMobs.console(messages);
        }
    }

    // Throws a runtime exception with a message and this mod's name tag if debugging is enabled.
    public static void debugException(String... messages) {
        if (_UtilityMobs.debug) {
            StringBuilder message = new StringBuilder();
            for (String part : messages) {
                message.append(part);
            }
            throw new RuntimeException(message.toString());
        }
        if (messages.length > 0) {
            messages[0] = "[ERROR] " + messages[0];
        }
        _UtilityMobs.console(messages);
    }
}
