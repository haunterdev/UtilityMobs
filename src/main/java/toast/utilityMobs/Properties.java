package toast.utilityMobs;

import java.util.HashMap;
import java.util.Random;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiConfigEntries;

/**
    This helper class automatically creates, stores, and retrieves properties.
    Supported data types:
        String, boolean, int, double

    Any property can be retrieved as an Object or String.
    Any non-String property can also be retrieved as any other non-String property.
    Retrieving a number as a boolean will produce a randomized output depending on the value.
 */
public abstract class Properties
{
    // Mapping of all properties in the mod to their values.
    private static final HashMap<String, Object> map = new HashMap();
    // Common category names.
    public static final String GENERAL = "_general";
    // The mod's Configuration instance (held so the in-game GUI and reload can re-read it).
    public static net.minecraftforge.common.config.Configuration config;

    // Initializes these properties.
    public static void init(Configuration config) {
        Properties.config = config;
        config.load();
        Properties.load();
        config.save();
    }

    // Reads all property values from the current config into the map. Safe to call repeatedly (idempotent).
    public static void load() {
        Configuration config = Properties.config;
        Property prop;

        // ---- General ----
        prop = Properties.add(config, Properties.GENERAL, "give_book_on_first_join", true, "If true, players are given the Utility Mobs guide book the first time they join a world. Modpack devs can set this to false.");
        prop.setLanguageKey("utilitymobs.cfg.general.give_book_on_first_join");
        prop = Properties.add(config, Properties.GENERAL, "alternate_manuals", false, "If this is true, manual recipes will require a book and quill instead of just a book.");
        prop.setLanguageKey("utilitymobs.cfg.general.alternate_manuals");
        prop = Properties.add(config, Properties.GENERAL, "heal_numbers", true, "If true, a floating green +N appears over a golem when it is healed (melon golem, food, repair item). Set false to hide the numbers - useful with damage-indicator mods.");
        prop.setLanguageKey("utilitymobs.cfg.general.heal_numbers");
        prop = Properties.add(config, Properties.GENERAL, "show_help_button", true, "If true, golem GUIs (steam golem, block-golem inventories) show a '?' button that opens the guide book. Set false to hide it. Has no effect when Patchouli is not installed (the button is hidden either way).");
        prop.setLanguageKey("utilitymobs.cfg.general.show_help_button");
        prop = Properties.add(config, Properties.GENERAL, "creeper_head_rarity", 80, "The rarity for a creeper to drop its head when killed. Setting this to 0 disables skull drops. Drop chance is 1/(rarity - looting).");
        prop.setLanguageKey("utilitymobs.cfg.general.creeper_head_rarity").setMinValue(0);
        prop = Properties.add(config, Properties.GENERAL, "hostile", false, "If this is true, all utility mobs added by this mod will be hostile towards players.");
        prop.setLanguageKey("utilitymobs.cfg.general.hostile").setRequiresWorldRestart(true);
        prop = Properties.add(config, Properties.GENERAL, "wither_conversion", true, "Setting this to false disables the wither skull to skeleton skull recipe.");
        prop.setLanguageKey("utilitymobs.cfg.general.wither_conversion");
        prop = Properties.add(config, Properties.GENERAL, "skull_rarity", 60, "The rarity for a skeleton to drop its skull when killed. Setting this to 0 disables skull drops. Drop chance is 1/(rarity - looting).");
        prop.setLanguageKey("utilitymobs.cfg.general.skull_rarity").setMinValue(0);
        // Global attack blacklist: entity types golems/turrets will NEVER attack. Native Forge string-list
        // (one entry per line) so it renders as an editable list in the GUI. Resolved by TargetHelper.
        Property attackBlacklist = config.get(Properties.GENERAL, "attack_blacklist", new String[0],
            "Entity types that golems and turrets will NEVER attack, regardless of owner target books or the attack_* toggles. One entry per line: an entity registry id (e.g. minecraft:cow, minecraft:villager), the tokens Player or Hostiles, or a fully-qualified class name. Blacklisting a base class also covers its subclasses. Lets you protect animals/NPCs the in-game target book cannot add.");
        attackBlacklist.setLanguageKey("utilitymobs.cfg.general.attack_blacklist");
        TargetHelper.loadGlobalBlacklist(attackBlacklist.getStringList());

        // ---- Turrets (behavior) ----
        prop = Properties.add(config, "turrets", "require_ammo", false, "If true, turrets must hold matching ammo in their 9-slot ammo inventory to fire (arrow turrets need arrows, fireball/ghast need fire charges, snow needs snowballs). Adds an ammo panel to the turret GUI.");
        prop.setLanguageKey("utilitymobs.cfg.turrets.require_ammo");
        prop = Properties.add(config, "turrets", "friendly_passthrough", false, "If true, projectiles fired by a turret/golem pass through friendly utility golems instead of colliding with them - so turrets placed in rows/layers can shoot through each other. Off by default; turn on if you want friendly turret formations.");
        prop.setLanguageKey("utilitymobs.cfg.turrets.friendly_passthrough");
        prop = Properties.add(config, "turrets", "no_mob_aggro", false, "If true, hostile mobs will never target or retaliate against turrets (worker golems are unaffected).");
        prop.setLanguageKey("utilitymobs.cfg.turrets.no_mob_aggro");
        prop = Properties.add(config, "turrets", "drop_chance", 0.5, "Chance (0.0-1.0) that a turret drops its building block when killed. 1.0 = always, 0.5 = coin flip, 0.0 = never. Does not affect ammo drops.");
        prop.setLanguageKey("utilitymobs.cfg.turrets.drop_chance").setMinValue(0.0).setMaxValue(1.0).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "turrets", "collision", false, "If true, turrets become solid and can be stood on and walked across (like colossal golems) - place a row of turrets to build a turret walkway. Off by default; turrets are pass-through.");
        prop.setLanguageKey("utilitymobs.cfg.turrets.collision");

        // ---- Worker Golems (behavior) ----
        prop = Properties.add(config, "golems", "block_collision", false, "If true, block golems (chest/furnace/crafting table/anvil/jukebox) become solid and can be stood on and walked across - line them up to build a walkway. Off by default; block golems are pass-through.");
        prop.setLanguageKey("utilitymobs.cfg.golems.block_collision");
        prop = Properties.add(config, "golems", "attack_hostiles", true, "If true, worker golems and colossal golems may target hostile mobs.");
        prop.setLanguageKey("utilitymobs.cfg.golems.attack_hostiles");
        prop = Properties.add(config, "golems", "attack_passives", false, "If true, worker golems and colossal golems may target passive mobs.");
        prop.setLanguageKey("utilitymobs.cfg.golems.attack_passives");
        prop = Properties.add(config, "golems", "attack_neutrals", false, "If true, worker golems and colossal golems may target neutral mobs (endermen, zombie pigmen, spiders, wolves, polar bears, llamas, iron golems).");
        prop.setLanguageKey("utilitymobs.cfg.golems.attack_neutrals");

        // Performance tuning for large golem armies. See UMProfiler / EntityAIGolemTarget / EntityUtilityGolem.
        prop = Properties.add(config, "golems", "performance_logging", false, "If true, logs golem AI and collision timing stats to the server console every 10s. Diagnostic only - leave off in normal play.");
        prop.setLanguageKey("utilitymobs.cfg.golems.performance_logging");
        prop = Properties.add(config, "golems", "target_scan_interval", 10, "Ticks between target searches for a golem with no target. Higher = much better TPS with many golems, at a small delay to acquire new targets. 1 = vanilla (scan every tick).");
        prop.setLanguageKey("utilitymobs.cfg.golems.target_scan_interval").setMinValue(1).setMaxValue(200).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "target_raytrace_cap", 5, "Max line-of-sight raytraces a golem does per target search. Caps the most expensive part of targeting. Lower = cheaper, may miss a visible target behind closer blocked ones.");
        prop.setLanguageKey("utilitymobs.cfg.golems.target_raytrace_cap").setMinValue(1).setMaxValue(50).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "collision_push_cap", 8, "Max entities a golem pushes per tick. Caps the O(n^2) shove cost when golems clump. 0 = unlimited (scan still profiled), -1 = pure vanilla (no override).");
        prop.setLanguageKey("utilitymobs.cfg.golems.collision_push_cap").setMinValue(-1).setMaxValue(64).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "collision_disable_density", 0, "If a golem is crowded by at least this many entities, it stops colliding entirely (no scan, no push) until the crowd thins - it just stacks instead of bouncing. The mob-bumping cost is O(n^2) in a pile, so this is the big win for huge armies. 0 = never disable. Try ~24 for dense armies.");
        prop.setLanguageKey("utilitymobs.cfg.golems.collision_disable_density").setMinValue(0).setMaxValue(128).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "collision_interval", 1, "Run a golem's collision check only every N ticks (staggered across golems). The collision scan is the single most expensive thing at high golem counts; 3-4 is visually fine and cuts that cost N-fold. 1 = every tick (vanilla cadence). Mounted golems skip collision entirely regardless.");
        prop.setLanguageKey("utilitymobs.cfg.golems.collision_interval").setMinValue(1).setMaxValue(20).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "active_range", 64, "A golem with no player within this many blocks skips its expensive scans (targeting AND collision) until a player approaches. This is the main lever for supporting very large armies - far-off golems cost almost nothing. 0 = always active (no gating).");
        prop.setLanguageKey("utilitymobs.cfg.golems.active_range").setMinValue(0).setMaxValue(256).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "follow_teleports_per_tick", 20, "Max golems that may teleport to their owner per tick. Prevents the lag spike when a large following army all teleports at once after the owner moves far. Lower = smoother but the army regroups slower.");
        prop.setLanguageKey("utilitymobs.cfg.golems.follow_teleports_per_tick").setMinValue(1).setMaxValue(200).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        prop = Properties.add(config, "golems", "wander_budget", 40, "Max golems that may begin a roam (wander pathfind) per tick across the whole world. Keeps a big roaming army cheap; the rest simply wait their turn. Roaming also only happens near a player (see active_range).");
        prop.setLanguageKey("utilitymobs.cfg.golems.wander_budget").setMinValue(1).setMaxValue(200).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);

        // ---- Colossals (behavior) ----
        prop = Properties.add(config, "colossals", "attack_hostiles", true, "If true, colossal golems may target hostile mobs.");
        prop.setLanguageKey("utilitymobs.cfg.colossals.attack_hostiles");
        prop = Properties.add(config, "colossals", "attack_passives", false, "If true, colossal golems may target passive mobs.");
        prop.setLanguageKey("utilitymobs.cfg.colossals.attack_passives");
        prop = Properties.add(config, "colossals", "attack_neutrals", false, "If true, colossal golems may target neutral mobs.");
        prop.setLanguageKey("utilitymobs.cfg.colossals.attack_neutrals");
        prop = Properties.add(config, "colossals", "wander_while_ridden", false, "If true, a ridden colossus keeps its normal wandering AI while the rider is idle. Pressing a movement key immediately takes manual control until the rider stops moving.");
        prop.setLanguageKey("utilitymobs.cfg.colossals.wander_while_ridden");

        // ---- Build toggles: UM iron/snow golems (explicit) ----
        // These are built by upgrading a vanilla golem (no spawn egg, intentionally not in UTILITY_NAMES).
        // Their build-toggle keys must be registered explicitly, or replaceGolem() looks up a null property.
        prop = Properties.add(config, "build.golems", "UMIronGolem", true);
        prop.setLanguageKey("utilitymobs.cfg.build.golems.UMIronGolem");
        prop = Properties.add(config, "build.golems", "UMSnowGolem", true);
        prop.setLanguageKey("utilitymobs.cfg.build.golems.UMSnowGolem");

        // ---- Build toggles: loop over UTILITY_TYPES ----
        String category;
        for (int i = _UtilityMobs.UTILITY_TYPES.length; i-- > 0;) {
            String type = _UtilityMobs.UTILITY_TYPES[i].toLowerCase();
            category = "build." + type + "s";

            prop = Properties.add(config, category, "_all", true, "If false, " + (type.equals("golem") ? "standard" : type) + " golems will not be buildable.");
            prop.setLanguageKey("utilitymobs.cfg.master_toggle");
            for (int j = _UtilityMobs.UTILITY_NAMES[i].length; j-- > 0;) {
                prop = Properties.add(config, category, _UtilityMobs.UTILITY_NAMES[i][j], true);
                prop.setLanguageKey(prettify(_UtilityMobs.UTILITY_NAMES[i][j]));
            }

            config.addCustomCategoryComment(category, "Options to disable the building of specific golems or all golems of this type.");
        }

        config.addCustomCategoryComment(Properties.GENERAL, "General and/or miscellaneous options.");

        // ---- Category display names ----
        config.getCategory(Properties.GENERAL).setLanguageKey("utilitymobs.cfg.cat.general");
        config.getCategory("turrets").setLanguageKey("utilitymobs.cfg.cat.turrets");
        config.getCategory("golems").setLanguageKey("utilitymobs.cfg.cat.golems");
        config.getCategory("colossals").setLanguageKey("utilitymobs.cfg.cat.colossals");
        config.getCategory("build").setLanguageKey("utilitymobs.cfg.cat.build");
        config.getCategory("build.golems").setLanguageKey("utilitymobs.cfg.cat.build.golems");
        config.getCategory("build.turrets").setLanguageKey("utilitymobs.cfg.cat.build.turrets");
        config.getCategory("build.blocks").setLanguageKey("utilitymobs.cfg.cat.build.blocks");
        config.getCategory("build.hostiles").setLanguageKey("utilitymobs.cfg.cat.build.hostiles");
        config.getCategory("build.colossals").setLanguageKey("utilitymobs.cfg.cat.build.colossals");

        // Migration: pre-build.* config files kept the per-mob build toggles in the flat behavior
        // categories; Forge re-writes those file-loaded keys even though we no longer register them
        // there, so the GUI would show stale "_all" entries. Strip them. Idempotent once clean.
        purgeStaleBuildToggles(config);

        // Cache the perf tunables into static fields so the per-tick/per-golem hot paths avoid a map lookup.
        UMProfiler.enabled = Properties.getBoolean("golems", "performance_logging");
        toast.utilityMobs.turret.EntityTurretGolem.collision = Properties.getBoolean("turrets", "collision");
        toast.utilityMobs.block.EntityBlockGolem.collision = Properties.getBoolean("golems", "block_collision");
        toast.utilityMobs.ai.EntityAIGolemTarget.scanInterval = Math.max(1, Properties.getInt("golems", "target_scan_interval"));
        toast.utilityMobs.ai.EntityAIGolemTarget.raytraceCap = Math.max(1, Properties.getInt("golems", "target_raytrace_cap"));
        toast.utilityMobs.golem.EntityUtilityGolem.collisionPushCap = Properties.getInt("golems", "collision_push_cap");
        toast.utilityMobs.golem.EntityUtilityGolem.collisionDisableDensity = Properties.getInt("golems", "collision_disable_density");
        toast.utilityMobs.golem.EntityUtilityGolem.collisionInterval = Math.max(1, Properties.getInt("golems", "collision_interval"));
        toast.utilityMobs.golem.EntityUtilityGolem.activeRange = Properties.getInt("golems", "active_range");
        toast.utilityMobs.ai.EntityAIGolemFollow.teleportBudget = Math.max(1, Properties.getInt("golems", "follow_teleports_per_tick"));
        toast.utilityMobs.ai.EntityAIGolemWander.budget = Math.max(1, Properties.getInt("golems", "wander_budget"));
        toast.utilityMobs.colossal.EntityColossalGolem.wanderWhileRidden = Properties.getBoolean("colossals", "wander_while_ridden");
    }

    // Re-reads config values into the map (called after the in-game config GUI saves changes).
    public static void reload() {
        Properties.load();
    }

    // Gets the mod's random number generator.
    public static Random random() {
        return _UtilityMobs.random;
    }

    // Passes to the mod.
    public static void debugException(String message) {
        _UtilityMobs.debugException(message);
    }

    // Loads the property as the specified value. Returns the Property for GUI metadata chaining.
    public static Property add(Configuration config, String category, String field, String defaultValue, String comment) {
        Property prop = config.get(category, field, defaultValue, comment);
        Properties.map.put(category + "@" + field, prop.getString());
        return prop;
    }
    public static Property add(Configuration config, String category, String field, int defaultValue, String comment) {
        Property prop = config.get(category, field, defaultValue, comment);
        Properties.map.put(category + "@" + field, Integer.valueOf(prop.getInt(defaultValue)));
        return prop;
    }
    public static Property add(Configuration config, String category, String field, boolean defaultValue) {
        Property prop = config.get(category, field, defaultValue);
        Properties.map.put(category + "@" + field, Boolean.valueOf(prop.getBoolean(defaultValue)));
        return prop;
    }
    public static Property add(Configuration config, String category, String field, boolean defaultValue, String comment) {
        Property prop = config.get(category, field, defaultValue, comment);
        Properties.map.put(category + "@" + field, Boolean.valueOf(prop.getBoolean(defaultValue)));
        return prop;
    }
    public static Property add(Configuration config, String category, String field, double defaultValue, String comment) {
        Property prop = config.get(category, field, defaultValue, comment);
        Properties.map.put(category + "@" + field, Double.valueOf(prop.getDouble(defaultValue)));
        return prop;
    }

    // Removes build toggles (_all + per-mob names + the explicit vanilla-golem upgrades) that older
    // configs left in the flat behavior categories. Empties left behind (blocks, hostiles) are dropped.
    private static void purgeStaleBuildToggles(Configuration config) {
        for (int i = 0; i < _UtilityMobs.UTILITY_TYPES.length; i++) {
            String flat = _UtilityMobs.UTILITY_TYPES[i].toLowerCase() + "s";
            if (!config.hasCategory(flat))
                continue;
            net.minecraftforge.common.config.ConfigCategory cc = config.getCategory(flat);
            cc.remove("_all");
            for (int j = 0; j < _UtilityMobs.UTILITY_NAMES[i].length; j++)
                cc.remove(_UtilityMobs.UTILITY_NAMES[i][j]);
            if (cc.isEmpty())
                config.removeCategory(cc);
        }
        if (config.hasCategory("golems")) {
            config.getCategory("golems").remove("UMIronGolem");
            config.getCategory("golems").remove("UMSnowGolem");
        }
    }

    // Inserts a space before each interior capital letter. "StoneLargeGolem" -> "Stone Large Golem".
    private static String prettify(String name) {
        return name.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    // Gets the Object property.
    public static Object getProperty(String category, String field) {
        return Properties.map.get(category + "@" + field);
    }

    // Gets the value of the property (instead of an Object representing it).
    public static String getString(String category, String field) {
        return Properties.getProperty(category, field).toString();
    }
    public static boolean getBoolean(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Boolean)
            return ((Boolean)property).booleanValue();
        if (property instanceof Integer)
            return Properties.random().nextInt(((Number)property).intValue()) == 0;
        if (property instanceof Double)
            return Properties.random().nextDouble() < ((Number)property).doubleValue();
        Properties.debugException("Tried to get boolean for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return false;
    }
    public static int getInt(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number)property).intValue();
        if (property instanceof Boolean)
            return ((Boolean)property).booleanValue() ? 1 : 0;
        Properties.debugException("Tried to get int for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return 0;
    }
    public static double getDouble(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number)property).doubleValue();
        if (property instanceof Boolean)
            return ((Boolean)property).booleanValue() ? 1.0 : 0.0;
        Properties.debugException("Tried to get double for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return 0.0;
    }
}
