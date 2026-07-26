package toast.utilityMobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * This helper class automatically creates, stores, and retrieves properties.
 * 1.20.1: backed by a ForgeConfigSpec (TOML) instead of the old Configuration API,
 * but the accessor surface (category + field keyed lookups) is unchanged so ported
 * callers compile as-is. Option names and categories match the 1.12.2 config.
 *
 * Any non-String property can be retrieved as any other non-String property.
 * Retrieving a number as a boolean produces a randomized output depending on the value.
 */
public abstract class Properties
{
    // Mapping of all properties in the mod to their backing config values.
    private static final HashMap<String, ForgeConfigSpec.ConfigValue<?>> map = new HashMap<>();
    // Common category names.
    public static final String GENERAL = "_general";

    /// What kind of widget an option needs on the config screen.
    public enum OptionType { BOOLEAN, INT, DOUBLE, STRING_LIST }

    /**
     * Everything the config screen needs to draw one option. 1.12.2 got this for free: Forge's
     * GuiConfig walked the Configuration object and built its own widgets from the ConfigCategory
     * tree. 1.20.1 removed that UI entirely and a ForgeConfigSpec does not expose defaults, ranges or
     * comments in any usable form, so the metadata is recorded here as each option is declared.
     */
    public static final class Option {
        public final String category;
        public final String field;
        public final OptionType type;
        public final Object defaultValue;
        /// Null for BOOLEAN and STRING_LIST; the inclusive bounds otherwise.
        public final Double min;
        public final Double max;
        public final String comment;

        Option(String category, String field, OptionType type, Object defaultValue, Double min, Double max, String comment) {
            this.category = category;
            this.field = field;
            this.type = type;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.comment = comment;
        }

        public String key() {
            return this.category + "@" + this.field;
        }
    }

    /// Declaration order, which is the order the config screen lists them in.
    private static final List<Option> OPTIONS = new ArrayList<>();

    public static List<Option> options() {
        return java.util.Collections.unmodifiableList(OPTIONS);
    }

    /// Distinct categories, in declaration order.
    public static List<String> categories() {
        List<String> out = new ArrayList<>();
        for (Option o : OPTIONS) {
            if (!out.contains(o.category))
                out.add(o.category);
        }
        return out;
    }

    public static final ForgeConfigSpec SPEC = build();

    private static ForgeConfigSpec build() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        // ---- General ----
        b.comment("General and/or miscellaneous options.").push(GENERAL);
        add(b, GENERAL, "give_book_on_first_join", true, "If true, players are given the Utility Mobs guide book the first time they join a world. Modpack devs can set this to false.");
        add(b, GENERAL, "alternate_manuals", false, "If this is true, manual recipes will require a book and quill instead of just a book.");
        add(b, GENERAL, "heal_numbers", true, "If true, a floating green +N appears over a golem when it is healed (melon golem, food, repair item). Set false to hide the numbers - useful with damage-indicator mods.");
        add(b, GENERAL, "show_help_button", true, "If true, golem GUIs (steam golem, block-golem inventories) show a '?' button that opens the guide book. Set false to hide it. Has no effect when Patchouli is not installed (the button is hidden either way).");
        add(b, GENERAL, "creeper_head_rarity", 80, 0, Integer.MAX_VALUE, "The rarity for a creeper to drop its head when killed. Setting this to 0 disables skull drops. Drop chance is 1/(rarity - looting).");
        add(b, GENERAL, "hostile", false, "If this is true, all utility mobs added by this mod will be hostile towards players.");
        add(b, GENERAL, "wither_conversion", true, "Setting this to false disables the wither skull to skeleton skull recipe.");
        add(b, GENERAL, "skull_rarity", 60, 0, Integer.MAX_VALUE, "The rarity for a skeleton to drop its skull when killed. Setting this to 0 disables skull drops. Drop chance is 1/(rarity - looting).");
        // Global attack blacklist/whitelist. Resolved by TargetHelper on config (re)load.
        addList(b, GENERAL, "attack_blacklist",
            "Entity types that golems and turrets will NEVER attack, regardless of owner target books or the attack_* toggles. One entry per line: an entity registry id (e.g. minecraft:cow, minecraft:villager), the tokens Player or Hostiles, or a fully-qualified class name. Blacklisting a base class also covers its subclasses. Lets you protect animals/NPCs the in-game target book cannot add.");
        addList(b, GENERAL, "attack_whitelist",
            "Entity types that golems and turrets will ALWAYS attack, even when the matching attack_* category toggle is off (e.g. target one specific passive animal or modded mob without enabling all passives). One entry per line: an entity registry id (e.g. minecraft:cow), the tokens Player or Hostiles, or a fully-qualified class name. The attack_blacklist and owner/friendly protections still take precedence over this list.");
        b.pop();

        // ---- Turrets (behavior) ----
        b.push("turrets");
        add(b, "turrets", "require_ammo", false, "If true, turrets must hold matching ammo in their 9-slot ammo inventory to fire (arrow turrets need arrows, fireball/ghast need fire charges, snow needs snowballs). Adds an ammo panel to the turret GUI.");
        add(b, "turrets", "friendly_passthrough", false, "If true, projectiles fired by a turret/golem pass through friendly utility golems instead of colliding with them - so turrets placed in rows/layers can shoot through each other. Off by default; turn on if you want friendly turret formations.");
        add(b, "turrets", "no_mob_aggro", false, "If true, hostile mobs will never target or retaliate against turrets (worker golems are unaffected).");
        add(b, "turrets", "drop_chance", 0.5, 0.0, 1.0, "Chance (0.0-1.0) that a turret drops its building block when killed. 1.0 = always, 0.5 = coin flip, 0.0 = never. Does not affect ammo drops.");
        add(b, "turrets", "collision", false, "If true, turrets become solid and can be stood on and walked across (like colossal golems) - place a row of turrets to build a turret walkway. Off by default; turrets are pass-through.");
        b.pop();

        // ---- Worker Golems (behavior) ----
        b.push("golems");
        add(b, "golems", "block_collision", false, "If true, block golems (chest/furnace/crafting table/anvil/jukebox) become solid and can be stood on and walked across - line them up to build a walkway. Off by default; block golems are pass-through.");
        add(b, "golems", "attack_hostiles", true, "If true, worker golems and colossal golems may target hostile mobs.");
        add(b, "golems", "attack_passives", false, "If true, worker golems and colossal golems may target passive mobs.");
        add(b, "golems", "attack_neutrals", false, "If true, worker golems and colossal golems may target neutral mobs (endermen, zombified piglins, spiders, wolves, polar bears, llamas, iron golems).");
        add(b, "golems", "melon_heal_range", 16.0, 1.0, 64.0, "How close (in blocks) a melon golem must be to another golem before it heals it. Lower = the melon golem has to come nearer, which is more balanced than healing anything it can see from afar.");
        // Performance tuning for large golem armies. See UMProfiler / EntityAIGolemTarget / EntityUtilityGolem.
        add(b, "golems", "performance_logging", false, "If true, logs golem AI and collision timing stats to the server console every 10s. Diagnostic only - leave off in normal play.");
        add(b, "golems", "target_scan_interval", 10, 1, 200, "Ticks between target searches for a golem with no target. Higher = much better TPS with many golems, at a small delay to acquire new targets. 1 = vanilla (scan every tick).");
        add(b, "golems", "target_raytrace_cap", 5, 1, 50, "Max line-of-sight raytraces a golem does per target search. Caps the most expensive part of targeting. Lower = cheaper, may miss a visible target behind closer blocked ones.");
        add(b, "golems", "collision_push_cap", 8, -1, 64, "Max entities a golem pushes per tick. Caps the O(n^2) shove cost when golems clump. 0 = unlimited (scan still profiled), -1 = pure vanilla (no override).");
        add(b, "golems", "collision_disable_density", 0, 0, 128, "If a golem is crowded by at least this many entities, it stops colliding entirely (no scan, no push) until the crowd thins - it just stacks instead of bouncing. The mob-bumping cost is O(n^2) in a pile, so this is the big win for huge armies. 0 = never disable. Try ~24 for dense armies.");
        add(b, "golems", "collision_interval", 1, 1, 20, "Run a golem's collision check only every N ticks (staggered across golems). The collision scan is the single most expensive thing at high golem counts; 3-4 is visually fine and cuts that cost N-fold. 1 = every tick (vanilla cadence). Mounted golems skip collision entirely regardless.");
        add(b, "golems", "active_range", 64, 0, 256, "A golem with no player within this many blocks skips its expensive scans (targeting AND collision) until a player approaches. This is the main lever for supporting very large armies - far-off golems cost almost nothing. 0 = always active (no gating).");
        add(b, "golems", "follow_teleports_per_tick", 20, 1, 200, "Max golems that may teleport to their owner per tick. Prevents the lag spike when a large following army all teleports at once after the owner moves far. Lower = smoother but the army regroups slower.");
        add(b, "golems", "wander_budget", 40, 1, 200, "Max golems that may begin a roam (wander pathfind) per tick across the whole world. Keeps a big roaming army cheap; the rest simply wait their turn. Roaming also only happens near a player (see active_range).");
        b.pop();

        // ---- Colossals (behavior) ----
        b.push("colossals");
        add(b, "colossals", "attack_hostiles", true, "If true, colossal golems may target hostile mobs.");
        add(b, "colossals", "attack_passives", false, "If true, colossal golems may target passive mobs.");
        add(b, "colossals", "attack_neutrals", false, "If true, colossal golems may target neutral mobs.");
        add(b, "colossals", "wander_while_ridden", false, "If true, a ridden colossus keeps its normal wandering AI while the rider is idle. Pressing a movement key immediately takes manual control until the rider stops moving.");
        b.pop();

        // ---- Build toggles ----
        // UM iron/snow golems are built by upgrading a vanilla golem (no spawn egg, not in UTILITY_NAMES),
        // so their build-toggle keys are registered explicitly.
        b.push("build");
        b.comment("Options to disable the building of specific golems or all golems of this type.").push("golems");
        add(b, "build.golems", "UMIronGolem", true, null);
        add(b, "build.golems", "UMSnowGolem", true, null);
        b.pop();
        for (int i = 0; i < _UtilityMobs.UTILITY_TYPES.length; i++) {
            String type = _UtilityMobs.UTILITY_TYPES[i].toLowerCase();
            String category = "build." + type + "s";
            b.comment("Options to disable the building of specific golems or all golems of this type.").push(type + "s");
            add(b, category, "_all", true, "If false, " + (type.equals("golem") ? "standard" : type) + " golems will not be buildable.");
            for (int j = 0; j < _UtilityMobs.UTILITY_NAMES[i].length; j++) {
                add(b, category, _UtilityMobs.UTILITY_NAMES[i][j], true, null);
            }
            b.pop();
        }
        b.pop();

        return b.build();
    }

    // Builder helpers. Keys into the map as "category@field", exactly like 1.12.2.
    private static void add(ForgeConfigSpec.Builder b, String category, String field, boolean def, String comment) {
        ForgeConfigSpec.ConfigValue<?> v = comment == null ? b.define(field, def) : b.comment(comment).define(field, def);
        map.put(category + "@" + field, v);
        OPTIONS.add(new Option(category, field, OptionType.BOOLEAN, Boolean.valueOf(def), null, null, comment));
    }
    private static void add(ForgeConfigSpec.Builder b, String category, String field, int def, int min, int max, String comment) {
        map.put(category + "@" + field, b.comment(comment).defineInRange(field, def, min, max));
        OPTIONS.add(new Option(category, field, OptionType.INT, Integer.valueOf(def),
            Double.valueOf(min), Double.valueOf(max), comment));
    }
    private static void add(ForgeConfigSpec.Builder b, String category, String field, double def, double min, double max, String comment) {
        map.put(category + "@" + field, b.comment(comment).defineInRange(field, def, min, max));
        OPTIONS.add(new Option(category, field, OptionType.DOUBLE, Double.valueOf(def),
            Double.valueOf(min), Double.valueOf(max), comment));
    }
    private static void addList(ForgeConfigSpec.Builder b, String category, String field, String comment) {
        map.put(category + "@" + field, b.comment(comment).defineList(field, new ArrayList<String>(), o -> o instanceof String));
        OPTIONS.add(new Option(category, field, OptionType.STRING_LIST, new ArrayList<String>(), null, null, comment));
    }

    /** Called on ModConfigEvent (loading + reloading): resolves lists and re-caches hot-path statics. */
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC)
            return;
        reload();
    }

    // Resolves the attack lists and caches the perf tunables into static fields so the
    // per-tick/per-golem hot paths avoid a config lookup. Safe to call repeatedly.
    @SuppressWarnings("unchecked")
    public static void reload() {
        TargetHelper.HOSTILE = getBoolean(GENERAL, "hostile");
        TargetHelper.loadGlobalBlacklist(((List<String>)getProperty(GENERAL, "attack_blacklist")).toArray(new String[0]));
        TargetHelper.loadGlobalWhitelist(((List<String>)getProperty(GENERAL, "attack_whitelist")).toArray(new String[0]));

        UMProfiler.enabled = getBoolean("golems", "performance_logging");
        toast.utilityMobs.turret.EntityTurretGolem.collision = getBoolean("turrets", "collision");
        toast.utilityMobs.block.EntityBlockGolem.collision = getBoolean("golems", "block_collision");
        toast.utilityMobs.ai.EntityAIGolemTarget.scanInterval = Math.max(1, getInt("golems", "target_scan_interval"));
        toast.utilityMobs.ai.EntityAIGolemTarget.raytraceCap = Math.max(1, getInt("golems", "target_raytrace_cap"));
        toast.utilityMobs.golem.EntityUtilityGolem.collisionPushCap = getInt("golems", "collision_push_cap");
        toast.utilityMobs.golem.EntityUtilityGolem.collisionDisableDensity = getInt("golems", "collision_disable_density");
        toast.utilityMobs.golem.EntityUtilityGolem.collisionInterval = Math.max(1, getInt("golems", "collision_interval"));
        toast.utilityMobs.golem.EntityUtilityGolem.activeRange = getInt("golems", "active_range");
        toast.utilityMobs.ai.EntityAIGolemFollow.teleportBudget = Math.max(1, getInt("golems", "follow_teleports_per_tick"));
        toast.utilityMobs.ai.EntityAIGolemWander.budget = Math.max(1, getInt("golems", "wander_budget"));
        toast.utilityMobs.colossal.EntityColossalGolem.wanderWhileRidden = getBoolean("colossals", "wander_while_ridden");
        toast.utilityMobs.golem.EntityMelonGolem.healRange = (float)getDouble("golems", "melon_heal_range");
    }

    // Gets the mod's random number generator.
    public static Random random() {
        return _UtilityMobs.random;
    }

    // Passes to the mod.
    public static void debugException(String message) {
        _UtilityMobs.debugException(message);
    }

    // Gets the raw property value.
    public static Object getProperty(String category, String field) {
        ForgeConfigSpec.ConfigValue<?> v = Properties.map.get(category + "@" + field);
        return v == null ? null : v.get();
    }

    /// Gets the config value itself rather than its contents, so a caller can write to it. 1.12.2 handed
    /// out the Configuration's Property object for the same reason; the whitelist/blacklist commands are
    /// the only callers.
    public static ForgeConfigSpec.ConfigValue<?> getConfigValue(String category, String field) {
        return Properties.map.get(category + "@" + field);
    }

    /// Sets a scalar option WITHOUT saving, so a batch of writes costs one file write. Callers must
    /// follow the batch with {@link #save()}. The setup wizard is the only caller.
    @SuppressWarnings("unchecked")
    public static void setValue(String category, String field, Object value) {
        ForgeConfigSpec.ConfigValue<?> v = Properties.getConfigValue(category, field);
        if (v == null)
            return;
        ((ForgeConfigSpec.ConfigValue<Object>)v).set(value);
    }

    /// Writes the config file and re-reads everything so pending {@link #setValue} calls go live.
    public static void save() {
        for (ForgeConfigSpec.ConfigValue<?> v : Properties.map.values()) {
            v.save();
            break; // save() flushes the whole file; one call is enough.
        }
        Properties.reload();
    }

    /// Replaces a list option and writes the file, then re-reads everything so the change is live.
    @SuppressWarnings("unchecked")
    public static void setList(String category, String field, java.util.List<String> entries) {
        ForgeConfigSpec.ConfigValue<?> v = Properties.getConfigValue(category, field);
        if (v == null)
            return;
        ((ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>)v).set(entries);
        v.save();
        Properties.reload();
    }

    // Gets the value of the property (instead of an Object representing it).
    public static String getString(String category, String field) {
        return String.valueOf(Properties.getProperty(category, field));
    }
    public static boolean getBoolean(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Boolean bool)
            return bool.booleanValue();
        if (property instanceof Integer num)
            return Properties.random().nextInt(num.intValue()) == 0;
        if (property instanceof Double num)
            return Properties.random().nextDouble() < num.doubleValue();
        Properties.debugException("Tried to get boolean for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return false;
    }
    public static int getInt(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number num)
            return num.intValue();
        if (property instanceof Boolean bool)
            return bool.booleanValue() ? 1 : 0;
        Properties.debugException("Tried to get int for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return 0;
    }
    public static double getDouble(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number num)
            return num.doubleValue();
        if (property instanceof Boolean bool)
            return bool.booleanValue() ? 1.0 : 0.0;
        Properties.debugException("Tried to get double for invalid property! @" + (property == null ? "(null)" : property.getClass().getName()));
        return 0.0;
    }
}
