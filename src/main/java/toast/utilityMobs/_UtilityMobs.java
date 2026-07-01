package toast.utilityMobs;

import java.io.File;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import toast.utilityMobs.block.BlockGolemLight;
import toast.utilityMobs.golem.EntityUMIronGolem;
import toast.utilityMobs.golem.EntityUMSnowGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.network.GuiHelper;
import toast.utilityMobs.network.MessageExplosion;
import toast.utilityMobs.network.MessageFetchTargetHelper;
import toast.utilityMobs.network.MessageHealNumber;
import toast.utilityMobs.network.MessageTargetHelper;
import toast.utilityMobs.network.MessageTurretToggle;
import toast.utilityMobs.network.MessageUseGolem;
import toast.utilityMobs.turret.EntityTurretArrow;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = _UtilityMobs.MODID, name = "Utility Mobs", version = _UtilityMobs.VERSION, guiFactory = "toast.utilityMobs.client.GuiFactory", dependencies = "required-after:patchouli")
public class _UtilityMobs
{
    // This mod's id (lowercased for 1.12.2 - registry/resource domains must be lowercase).
    public static final String MODID = "utilitymobs";
    // This mod's version.
    public static final String VERSION = "3.2.0";

    // If true, this mod starts up in debug mode.
    public static final boolean debug = false;
    // The mod instance (used for GUI handler lookup).
    public static _UtilityMobs instance;
    // The common proxy for this mod.
    @SidedProxy(clientSide = "toast.utilityMobs.client.ClientProxy", serverSide = "toast.utilityMobs.CommonProxy")
    public static CommonProxy proxy;
    // The mod's random number generator.
    public static final Random random = new Random();
    // The network channel for this mod.
    public static SimpleNetworkWrapper CHANNEL;

    // The texture path prefix.
    public static final String TEXTURE = _UtilityMobs.MODID + ":textures/models/";
    // The mod's creative tab (referencing it triggers its CreativeTabs registration).
    public static final net.minecraft.creativetab.CreativeTabs CREATIVE_TAB = CreativeTabUtilityMobs.INSTANCE;
    // Throwaway item used ONLY as the creative-tab icon (stone golem face). Not in any tab.
    public static Item TAB_ICON;
    // Invisible full-bright block the Jack o'Lantern Golem walks to light the area around itself.
    public static Block GOLEM_LIGHT;
    // Spawn egg colors per individual mob (primary, secondary), parallel to UTILITY_NAMES[i][j].
    // Colors chosen to read as the block/material each mob is built from.
    private static final int[][][] EGG_COLORS = {
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

    // Registers the entities in this mod.
    private void registerMobs() {
        int id = 0;
        String path;
        EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, "genericgolem"), EntityUtilityGolem.class, "GenericGolem", id++, this, 80, 3, true);
        // UM iron/snow golems are vanilla-mob variants. They get spawn eggs too (listed explicitly in the
        // creative tab, since they're not part of UTILITY_NAMES).
        EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, "umirongolem"), EntityUMIronGolem.class, "UMIronGolem", id++, this, 80, 3, true, 0xDCDCDC, 0xC9A0A0);
        EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, "umsnowgolem"), EntityUMSnowGolem.class, "UMSnowGolem", id++, this, 80, 3, true, 0xEAF6FF, 0xE8870E);
        for (int i = 0; i < _UtilityMobs.UTILITY_NAMES.length; i++) {
            path = "toast.utilityMobs." + _UtilityMobs.decap(_UtilityMobs.UTILITY_TYPES[i]) + ".Entity";
            for (int j = 0; j < _UtilityMobs.UTILITY_NAMES[i].length; j++) {
                try {
                    String name = _UtilityMobs.UTILITY_NAMES[i][j];
                    @SuppressWarnings("unchecked")
                    Class<? extends Entity> clazz = (Class<? extends Entity>)Class.forName(path + name);
                    EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, name.toLowerCase()), clazz, name, id++, this, 80, 3, true, _UtilityMobs.EGG_COLORS[i][j][0], _UtilityMobs.EGG_COLORS[i][j][1]);
                }
                catch (ClassNotFoundException ex) {
                    _UtilityMobs.debugException("@Entity" + _UtilityMobs.UTILITY_NAMES[i][j] + ": class not found!");
                }
            }
        }
        EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, "umfishhook"), EntityGolemFishHook.class, "UMFishHook", id++, this, 64, 5, true);
        // Turret-fired arrow (EntityTippedArrow subclass) so despawning arrows can puff into particles
        // instead of popping. Renders with the vanilla arrow renderer.
        EntityRegistry.registerModEntity(new ResourceLocation(_UtilityMobs.MODID, "turret_arrow"), EntityTurretArrow.class, "TurretArrow", id++, this, 64, 3, true);
    }

    // Registers the recipes for this mod (1.12.2 registers IRecipe objects into the registry).
    @Mod.EventBusSubscriber(modid = _UtilityMobs.MODID)
    public static class RegistrationHandler {
        // The only registered item: the hidden tab-icon item (stone golem face). No creative tab set,
        // so it never appears in the inventory - it exists solely to render as the CreativeTab icon.
        @SubscribeEvent
        public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
            Block light = new BlockGolemLight();
            light.setRegistryName(new ResourceLocation(_UtilityMobs.MODID, "golem_light"));
            _UtilityMobs.GOLEM_LIGHT = light;
            event.getRegistry().register(light);
        }

        @SubscribeEvent
        public static void onRegisterItems(RegistryEvent.Register<Item> event) {
            Item icon = new Item();
            icon.setRegistryName(new ResourceLocation(_UtilityMobs.MODID, "stonegolem_face"));
            icon.setTranslationKey(_UtilityMobs.MODID + ".stonegolem_face");
            _UtilityMobs.TAB_ICON = icon;
            event.getRegistry().register(icon);

            // Client-side: register the models now that the items exist.
            _UtilityMobs.proxy.registerTabIconModel();
        }

        @SubscribeEvent
        @SuppressWarnings("boxing")
        public static void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
            IForgeRegistry<IRecipe> registry = event.getRegistry();
            Item book = Properties.getBoolean(Properties.GENERAL, "alternate_manuals") ? Items.WRITABLE_BOOK : Items.BOOK;
            // The mod's content is documented in a Patchouli guide book - vanilla book + pumpkin.
            shapeless(registry, "guide_book", GuideBook.stack(), Items.BOOK, new ItemStack(Blocks.PUMPKIN));
            shapeless(registry, "target_book_0", TargetHelper.book(0), book, new ItemStack(Items.BONE));
            shapeless(registry, "target_book_1", TargetHelper.book(1), book, new ItemStack(Items.ROTTEN_FLESH));

            RecipeSavePermissions savePermissions = new RecipeSavePermissions();
            savePermissions.setRegistryName(new ResourceLocation(_UtilityMobs.MODID, "save_permissions"));
            registry.register(savePermissions);

            if (Properties.getBoolean(Properties.GENERAL, "wither_conversion")) {
                ShapedOreRecipe wither = new ShapedOreRecipe(new ResourceLocation(_UtilityMobs.MODID, "wither_conversion"), new ItemStack(Items.SKULL),
                        "&&&", "&@&", "&&&", '@', new ItemStack(Items.SKULL, 1, 1), '&', new ItemStack(Items.SPECKLED_MELON));
                wither.setRegistryName(new ResourceLocation(_UtilityMobs.MODID, "wither_conversion"));
                registry.register(wither);
            }
        }

        // Registers a shapeless recipe with a (possibly NBT-bearing) fixed output.
        private static void shapeless(IForgeRegistry<IRecipe> registry, String name, ItemStack output, Object... inputs) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (Object input : inputs) {
                if (input instanceof Item) {
                    ingredients.add(Ingredient.fromItem((Item)input));
                }
                else {
                    ingredients.add(Ingredient.fromStacks((ItemStack)input));
                }
            }
            ShapelessRecipes recipe = new ShapelessRecipes(_UtilityMobs.MODID, output, ingredients);
            recipe.setRegistryName(new ResourceLocation(_UtilityMobs.MODID, name));
            registry.register(recipe);
        }
    }

    // Called before initialization. Loads the properties/configurations.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        _UtilityMobs.instance = this;
        _UtilityMobs.debugConsole("Loading in debug mode!");
        Properties.init(new Configuration(event.getSuggestedConfigurationFile()));
        TargetHelper.SAVE_DIRECTORY = new File(event.getModConfigurationDirectory(), "UtilityMobs");

        _UtilityMobs.CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("UM|Info");
        int id = 0;
        _UtilityMobs.CHANNEL.registerMessage(MessageUseGolem.Handler.class, MessageUseGolem.class, id++, Side.SERVER);
        _UtilityMobs.CHANNEL.registerMessage(MessageTargetHelper.Handler.class, MessageTargetHelper.class, id, Side.SERVER);
        _UtilityMobs.CHANNEL.registerMessage(MessageTurretToggle.Handler.class, MessageTurretToggle.class, 4, Side.SERVER);
        if (event.getSide() == Side.CLIENT) {
            _UtilityMobs.CHANNEL.registerMessage(MessageTargetHelper.Handler.class, MessageTargetHelper.class, id++, Side.CLIENT);
            _UtilityMobs.CHANNEL.registerMessage(MessageFetchTargetHelper.Handler.class, MessageFetchTargetHelper.class, id++, Side.CLIENT);
            _UtilityMobs.CHANNEL.registerMessage(MessageExplosion.Handler.class, MessageExplosion.class, id++, Side.CLIENT);
            _UtilityMobs.CHANNEL.registerMessage(MessageHealNumber.Handler.class, MessageHealNumber.class, 5, Side.CLIENT);
        }
        // Entity renderers MUST register in preInit: RenderManager runs loadEntityRenderers at the end of
        // its constructor, which happens after preInit but before init. Registering in init is too late
        // (factories never apply, entities fall back to RenderEntity = white box).
        _UtilityMobs.proxy.registerRenderers();
    }

    // Called during initialization. Registers entities, mob spawns, and renderers.
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        this.registerMobs();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHelper());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuideBook());
        new BuildHelper();
        new TickHandler();
        new EventHandler();
    }

    // Registers this mod's server commands.
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandUMSummon());
        event.registerServerCommand(new CommandUMBlacklist());
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
        String message = "[" + _UtilityMobs.MODID + "] [" + FMLCommonHandler.instance().getSide().name() + "] ";
        for (String part : messages) {
            message += part;
        }
        System.out.println(message);
    }

    // Prints the message to the console with this mod's name tag if debugging is enabled.
    public static void debugConsole(String... messages) {
        if (_UtilityMobs.debug) {
            String message = "[" + _UtilityMobs.MODID + "] [" + FMLCommonHandler.instance().getSide().name() + "] ";
            for (String part : messages) {
                message += part;
            }
            System.out.println(message);
        }
    }

    // Throws a runtime exception with a message and this mod's name tag if debugging is enabled.
    public static void debugException(String... messages) {
        if (_UtilityMobs.debug) {
            String message = "[" + _UtilityMobs.MODID + "] [" + FMLCommonHandler.instance().getSide().name() + "] ";
            for (String part : messages) {
                message += part;
            }
            throw new RuntimeException(message);
        }
        if (messages.length > 0) {
            messages[0] = "[ERROR] " + messages[0];
        }
        _UtilityMobs.console(messages);
    }
}
