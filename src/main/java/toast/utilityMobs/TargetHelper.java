package toast.utilityMobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.network.MessageFetchTargetHelper;
import toast.utilityMobs.network.MessageTargetHelper;
import toast.utilityMobs.network.UMChannel;

public class TargetHelper
{
    // All currently loaded target helpers.
    private static final HashMap<String, TargetHelper> TARGET_HELPERS = new HashMap<String, TargetHelper>();
    // The location of the target helper saves.
    public static File SAVE_DIRECTORY;
    // Permission values.
    public static final byte PERMISSION_TARGET = (byte)(1 << 0);
    public static final byte PERMISSION_USE = (byte)(1 << 1);
    public static final byte PERMISSION_OPEN = (byte)(1 << 2);
    // The highest permission value.
    public static final byte HIGHEST_PERMISSION = TargetHelper.PERMISSION_OPEN;
    // If true, mobs attack all players. (Non-final in 1.20.1: the TOML config is not readable at
    // class-init time, so Properties.reload() pushes the value here on every config (re)load.)
    public static boolean HOSTILE = false;

    /**
     * Lazy Class -> entity registry id cache. 1.12.2 could map both ways through EntityList; 1.20.1's
     * EntityType hides the entity class, so the reverse (class -> id) is learned from live entities as
     * they are seen (every entity passing through the list matchers or book interactions records itself).
     * Registry-id entries that cannot be resolved to a class yet are kept as raw id strings in the *Ids
     * sets and matched against an entity's OWN registry key at check time, which is order-independent.
     */
    private static final HashMap<Class<?>, String> CLASS_TO_ID = new HashMap<>();
    /// Learned id -> class mappings (populated alongside CLASS_TO_ID from live entities).
    private static final HashMap<String, Class<?>> ID_TO_CLASS = new HashMap<>();

    /// Records a live entity's class <-> registry id mapping.
    private static void learn(Entity entity) {
        Class<?> entityClass = entity.getClass();
        if (!CLASS_TO_ID.containsKey(entityClass)) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null) {
                String id = key.toString().toLowerCase(Locale.ROOT);
                CLASS_TO_ID.put(entityClass, id);
                ID_TO_CLASS.putIfAbsent(id, entityClass);
            }
        }
    }

    // The owner of the golems using this target helper.
    public String owner;
    // If this is set to true, then this instance will be destroyed.
    private boolean destroy;
    // Player permissions for this target helper.
    private HashMap<String, Byte> permissions = new HashMap<String, Byte>();
    // Mob class blacklist for this target helper.
    private HashSet<Class<?>> mobBlacklist = new HashSet<>();
    // Registry-id form of the per-helper blacklist (entries whose class is not yet known).
    private LinkedHashSet<String> mobBlacklistIds = new LinkedHashSet<>();

    // Global, config-driven attack blacklist (general.attack_blacklist).
    private static final HashSet<Class<?>> GLOBAL_BLACKLIST = new HashSet<>();
    private static final HashSet<String> GLOBAL_BLACKLIST_IDS = new HashSet<>();
    // Global, config-driven attack WHITELIST (general.attack_whitelist).
    private static final HashSet<Class<?>> GLOBAL_WHITELIST = new HashSet<>();
    private static final HashSet<String> GLOBAL_WHITELIST_IDS = new HashSet<>();
    // Mob class whitelist for this target helper.
    private ArrayList<Class<?>> mobWhitelist = new ArrayList<>();
    // Registry-id form of the per-helper whitelist (entries whose class is not yet known).
    private LinkedHashSet<String> mobWhitelistIds = new LinkedHashSet<>();
    // Memoizes isWhitelisted() results per concrete entity Class (hot path at high golem counts).
    // Cleared whenever the whitelist changes (whitelist/unwhitelist/load).
    private final HashMap<Class<?>, Boolean> whitelistCache = new HashMap<>();

    // Per-class cache of a mod's reflective boolean isHostile() method (Optional-style: absent key = not
    // looked up yet, mapped-to-null = looked up, no such method). Lets us detect modded hostiles that
    // track hostility on their own base class instead of implementing vanilla Enemy.
    private static final HashMap<Class<?>, java.lang.reflect.Method> HOSTILE_METHOD_CACHE = new HashMap<>();

    /** Broad "is this a hostile mob" test used by the target-category gate so modded hostiles are attacked
        out of the box, not just vanilla Enemy. Matches, in order: vanilla Enemy; anything registered in the
        MONSTER category; and a reflective no-arg boolean isHostile() returning true. */
    public static boolean isHostileMob(Entity entity) {
        if (entity instanceof Enemy)
            return true;
        if (entity instanceof Mob && entity.getType().getCategory() == MobCategory.MONSTER)
            return true;
        Boolean reflective = TargetHelper.reflectiveIsHostile(entity);
        return reflective != null && reflective.booleanValue();
    }

    // Invokes a cached, reflectively-found no-arg boolean isHostile() on the entity, or null if the class
    // has no such method (or the call fails). Method handle is resolved once per class and cached.
    private static Boolean reflectiveIsHostile(Entity entity) {
        Class<?> entityClass = entity.getClass();
        java.lang.reflect.Method method;
        if (HOSTILE_METHOD_CACHE.containsKey(entityClass)) {
            method = HOSTILE_METHOD_CACHE.get(entityClass);
        }
        else {
            method = null;
            try {
                java.lang.reflect.Method m = entityClass.getMethod("isHostile");
                if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                    m.setAccessible(true);
                    method = m;
                }
            }
            catch (Throwable ex) {
                // No isHostile() on this class (the common vanilla case) - cache the miss.
            }
            HOSTILE_METHOD_CACHE.put(entityClass, method);
        }
        if (method == null)
            return null;
        try {
            return (Boolean) method.invoke(entity);
        }
        catch (Throwable ex) {
            return null;
        }
    }

    /** Fixed set of vanilla "neutral" mobs (don't attack unprovoked). Used by the hostile/
        neutral/passive target gate so players can choose to leave these alone. */
    public static boolean isNeutralMob(Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.EnderMan
            || entity instanceof net.minecraft.world.entity.monster.ZombifiedPiglin
            || entity instanceof net.minecraft.world.entity.monster.Spider      // + CaveSpider (subclass)
            || entity instanceof net.minecraft.world.entity.animal.PolarBear
            || entity instanceof net.minecraft.world.entity.animal.IronGolem
            || entity instanceof net.minecraft.world.entity.animal.Wolf
            || entity instanceof net.minecraft.world.entity.animal.horse.Llama
            || entity instanceof net.minecraft.world.entity.animal.Bee
            || entity instanceof net.minecraft.world.entity.monster.piglin.Piglin
            || entity instanceof net.minecraft.world.entity.animal.Panda;
    }

    private TargetHelper(String username) {
        this.owner = username;
        TargetHelper.TARGET_HELPERS.put(this.owner, this);
        if (this.owner != null) {
            if (this.hasSave()) {
                this.load();
            }
            else {
                this.setPermissions(this.owner, 7);
                this.whitelist(Player.class);
                this.whitelist(EntityUtilityGolem.class);
                // Whitelist every living thing. isValidTarget() requires whitelist membership; the actual
                // hostile/passive split is then decided by the category gate (passesTargetFilter). The
                // Enemy entry below is redundant but kept for documentation ("Hostiles" maps to it).
                this.whitelist(LivingEntity.class);
                this.whitelist(Enemy.class);
            }
        }
    }

    // Gets the target helper for the owner and loads it, if needed.
    public static TargetHelper getTargetHelper(String owner) {
        if ("".equals(owner)) {
            owner = null;
        }
        TargetHelper targetHelper = TargetHelper.TARGET_HELPERS.get(owner);
        if (targetHelper == null) {
            targetHelper = new TargetHelper(owner);
        }
        return targetHelper;
    }

    // Returns true if the entity has an owner tag.
    public static boolean hasOwner(Entity entity) {
        return entity.getPersistentData().contains("UM|owner");
    }

    // Gets the target helper for the owner of the entity and loads it, if needed.
    public static TargetHelper getOwnerTargetHelper(Entity entity) {
        return TargetHelper.getTargetHelper(entity.getPersistentData().getString("UM|owner"));
    }

    // Resolves the owner username of an ownable entity (golems track owner by name).
    private static String ownerNameOf(Entity entity) {
        if (entity instanceof EntityUtilityGolem golem)
            return golem.getOwnerName();
        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            return owner == null ? null : owner.getScoreboardName();
        }
        return null;
    }

    // Attempts to find the player on the server. Returns null if the player cannot be found.
    public Player getOwner() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || this.owner == null)
            return null;
        return server.getPlayerList().getPlayerByName(this.owner);
    }

    // Sets the owner of a particular entity, usually an arrow or snowball.
    public void setOwned(Entity entity) {
        if (this.owner != null) {
            entity.getPersistentData().putString("UM|owner", this.owner);
        }
    }

    // Returns true if the player can be damaged.
    public boolean canDamagePlayer(String username) {
        if (TargetHelper.HOSTILE)
            return true;
        else if (!this.permissions.containsKey(username))
            return !this.isBlacklisted(Player.class) && this.isWhitelisted(Player.class);
        else
            return (this.permissions.get(username).byteValue() & TargetHelper.PERMISSION_TARGET) == 0;
    }

    // Returns the player's permissions.
    public byte getPermissions(String username) {
        if (this.owner == null || !this.permissions.containsKey(username))
            return (byte)0;
        return this.permissions.get(username).byteValue();
    }

    // Returns true if the player has the given permissions (at least).
    public boolean playerHasPermission(String username, int value) {
        return this.owner == null || (this.getPermissions(username) & value) == value;
    }

    // Returns true if the given entity should continue to be attacked.
    public boolean maintainTarget(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive())
            return false;
        return true;
    }

    // Returns true if the given entity should be attacked.
    public boolean isValidTarget(Entity entity) {
        if (!this.maintainTarget(entity))
            return false;
        TargetHelper.learn(entity);
        if (TargetHelper.isGloballyBlacklisted(entity))
            return false;
        if (this.owner == null)
            return entity instanceof EntityUtilityGolem golem ? golem.getOwner() != null : true;
        if (entity instanceof OwnableEntity) {
            String ownerName = TargetHelper.ownerNameOf(entity);
            if (this.owner.equals(ownerName) || !this.canDamagePlayer(ownerName))
                return false;
        }
        if (entity instanceof Player)
            return this.canDamagePlayer(entity.getScoreboardName());
        Class<?> entityClass = entity.getClass();
        // The per-player blacklist (target book "!entity") always wins - the owner explicitly said no.
        if (this.isBlacklisted(entityClass) || this.matchesIdList(entity, this.mobBlacklistIds))
            return false;
        // The global attack whitelist forces the target through the per-player whitelist requirement; the
        // category gate (passive/neutral) is bypassed separately in EntityUtilityGolem.canAttackNoSight.
        if (TargetHelper.isGloballyWhitelisted(entity))
            return true;
        return this.isWhitelisted(entityClass) || this.matchesIdList(entity, this.mobWhitelistIds);
    }

    // True if the entity's own registry id appears in the given raw-id list.
    private boolean matchesIdList(Entity entity, LinkedHashSet<String> ids) {
        if (ids.isEmpty())
            return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && ids.contains(key.toString().toLowerCase(Locale.ROOT));
    }

    // Marks this target helper for deletion. Called every now-and-then on every target helper. They will reload themselves if still being used.
    public static void destroyAll() {
        for (Map.Entry<String, TargetHelper> entry : TargetHelper.TARGET_HELPERS.entrySet()) {
            entry.getValue().softDestroy();
        }
        TargetHelper.TARGET_HELPERS.clear();
    }
    private void softDestroy() {
        this.destroy = true;
    }
    public void destroy() {
        this.destroy = true;
        TargetHelper.TARGET_HELPERS.remove(this);
    }
    public boolean destroyed() {
        return this.destroy;
    }

    // Functions for setting and removing player permissions.
    public void setPermissions(String username, int value) {
        if (value > 0) {
            this.permissions.put(username, Byte.valueOf((byte)value));
        }
        else {
            this.permissions.remove(username);
        }
    }
    public void addPermissions(String username, int value) {
        if (this.permissions.containsKey(username)) {
            byte prev = this.permissions.get(username).byteValue();
            value |= prev;
        }
        this.permissions.put(username, Byte.valueOf((byte)value));
    }
    public void remPermissions(String username, int value) {
        if (this.permissions.containsKey(username)) {
            byte prev = this.permissions.get(username).byteValue();
            prev &= ~value;
            if (prev > 0) {
                this.permissions.put(username, Byte.valueOf(prev));
            }
            else {
                this.permissions.remove(username);
            }
        }
    }

    // Functions for blacklisting/whitelisting mobs.
    public void blacklist(Class<?> entityClass) {
        if (!this.isBlacklisted(entityClass) && this.isWhitelisted(entityClass)) {
            this.mobBlacklist.add(entityClass);
        }
    }
    public void unblacklist(Class<?> entityClass) {
        this.mobBlacklist.remove(entityClass);
    }
    public void toggleBlacklist(Class<?> entityClass) {
        if (this.isBlacklisted(entityClass)) {
            this.unblacklist(entityClass);
        }
        else {
            this.blacklist(entityClass);
        }
    }
    public void whitelist(Class<?> entityClass) {
        this.whitelistCache.clear();
        // Accept LivingEntity subclasses and the Enemy interface (used to target all hostiles).
        if (!this.mobWhitelist.contains(entityClass) && (LivingEntity.class.isAssignableFrom(entityClass) || Enemy.class.isAssignableFrom(entityClass)) && this.clearWhitelistFor(entityClass)) {
            this.mobWhitelist.add(entityClass);
        }
    }
    public void unwhitelist(Class<?> entityClass) {
        this.whitelistCache.clear();
        this.clearBlacklistFor(entityClass);
        this.mobWhitelist.remove(entityClass);
    }
    public void toggleWhitelist(Class<?> entityClass) {
        if (this.mobWhitelist.contains(entityClass)) {
            this.unwhitelist(entityClass);
        }
        else {
            this.whitelist(entityClass);
        }
    }

    // Returns true if the entity class is whitelisted or extends a whitelisted class.
    public boolean isWhitelisted(Class<?> entityClass) {
        Boolean cached = this.whitelistCache.get(entityClass);
        if (cached != null)
            return cached.booleanValue();
        boolean result = false;
        for (Class<?> allowedClass : this.mobWhitelist) if (allowedClass.isAssignableFrom(entityClass)) {
            result = true;
            break;
        }
        this.whitelistCache.put(entityClass, Boolean.valueOf(result));
        return result;
    }

    // Returns true if the entity class is blacklisted.
    public boolean isBlacklisted(Class<?> entityClass) {
        return this.mobBlacklist.contains(entityClass);
    }

    // Ensures that there is no over-definition in the whitelist when a whitelist entry is added.
    private boolean clearWhitelistFor(Class<?> entityClass) {
        Class<?> allowedClass;
        for (Iterator<Class<?>> iterator = this.mobWhitelist.iterator(); iterator.hasNext() && (allowedClass = iterator.next()) != null;) {
            if (allowedClass.isAssignableFrom(entityClass))
                return false;
            if (entityClass.isAssignableFrom(allowedClass)) {
                iterator.remove();
            }
        }
        return true;
    }

    // Ensures that there are no unneeded blacklist entries when a whitelist entry is removed.
    private void clearBlacklistFor(Class<?> entityClass) {
        Class<?> disallowedClass;
        for (Iterator<Class<?>> iterator = this.mobBlacklist.iterator(); iterator.hasNext() && (disallowedClass = iterator.next()) != null;) if (entityClass.isAssignableFrom(disallowedClass)) {
            iterator.remove();
        }
    }

    // Returns true if this target helper has a save.
    public boolean hasSave() {
        if (this.owner == null)
            return false;
        try {
            return new File(TargetHelper.SAVE_DIRECTORY, this.owner + ".txt").exists();
        }
        catch (Exception ex) {
            _UtilityMobs.console("Failed to fetch target save data (" + this.owner + ".txt)!");
            ex.printStackTrace();
        }
        return false;
    }

    // Saves this target helper to the config.
    public void save() {
        if (this.owner == null || this.destroyed())
            return;

        this.updateTargetHelper();

        try {
            File saveTmp = new File(TargetHelper.SAVE_DIRECTORY, this.owner + ".txt.tmp");
            File save = new File(TargetHelper.SAVE_DIRECTORY, this.owner + ".txt");
            TargetHelper.SAVE_DIRECTORY.mkdirs();
            saveTmp.createNewFile();
            FileWriter out = new FileWriter(saveTmp);
            out.write("player_permissions");
            for (Map.Entry<String, Byte> entry : this.permissions.entrySet()) {
                out.write("\n" + Integer.toBinaryString(entry.getValue().intValue()) + " " + entry.getKey());
            }
            out.write("\n\nwhitelist");
            for (Class<?> entityClass : this.mobWhitelist) {
                out.write("\n" + TargetHelper.classToString(entityClass));
            }
            for (String id : this.mobWhitelistIds) {
                out.write("\n" + id);
            }
            out.write("\n\nblacklist");
            for (Class<?> entityClass : this.mobBlacklist) {
                out.write("\n" + TargetHelper.classToString(entityClass));
            }
            for (String id : this.mobBlacklistIds) {
                out.write("\n" + id);
            }
            out.close();
            save.delete();
            saveTmp.renameTo(save);
        }
        catch (Exception ex) {
            _UtilityMobs.console("Failed to save target data (" + this.owner + ".txt)!");
            ex.printStackTrace();
        }
    }

    // Saves this target helper to the byte buffer to send to the client.
    public void save(FriendlyByteBuf buf) {
        if (this.owner == null || this.destroyed()) {
            buf.writeUtf("");
            buf.writeUtf("");
            buf.writeUtf("");
            return;
        }
        StringBuilder list;

        list = new StringBuilder();
        for (Map.Entry<String, Byte> entry : this.permissions.entrySet()) {
            list.append("\n").append(Integer.toBinaryString(entry.getValue().intValue())).append(" ").append(entry.getKey());
        }
        buf.writeUtf(list.toString());

        list = new StringBuilder();
        for (Class<?> entityClass : this.mobWhitelist) {
            list.append("\n").append(TargetHelper.classToString(entityClass));
        }
        for (String id : this.mobWhitelistIds) {
            list.append("\n").append(id);
        }
        buf.writeUtf(list.toString());

        list = new StringBuilder();
        for (Class<?> entityClass : this.mobBlacklist) {
            list.append("\n").append(TargetHelper.classToString(entityClass));
        }
        for (String id : this.mobBlacklistIds) {
            list.append("\n").append(id);
        }
        buf.writeUtf(list.toString());
    }

    // Loads this target helper from the config.
    public void load() {
        if (this.owner == null || this.destroyed())
            return;
        try {
            this.permissions.clear();
            this.mobBlacklist.clear();
            this.mobBlacklistIds.clear();
            this.mobWhitelist.clear();
            this.mobWhitelistIds.clear();
            this.whitelistCache.clear();
            File save = new File(TargetHelper.SAVE_DIRECTORY, this.owner + ".txt");
            if (!save.exists())
                return;
            FileInputStream in = new FileInputStream(save);
            int dat;
            byte status = 0;
            String key = "";
            String value = "";
            while ((dat = in.read()) >= 0) {
                if (dat == 13) {
                    continue;
                }
                if (dat == 10) {
                    if (status == 0) {
                        if ("".equals(key)) {
                            continue;
                        }
                        if (!key.equalsIgnoreCase("player_permissions") && !key.equalsIgnoreCase("whitelist") && !key.equalsIgnoreCase("blacklist")) {
                            _UtilityMobs.console("Unrecognized value in player config: " + key + " (" + this.owner + ".txt)!");
                            key = "";
                            continue;
                        }
                        status = 1;
                    }
                    else if (status == 1) {
                        if ("".equals(value)) {
                            key = "";
                            status = 0;
                            continue;
                        }
                        if (key.equalsIgnoreCase("player_permissions")) {
                            for (int i = 0; i < value.length(); i++) {
                                if (value.charAt(i) == ' ') {
                                    try {
                                        this.setPermissions(value.substring(i + 1), Integer.parseInt(value.substring(0, i), 2));
                                    }
                                    catch (Exception ex) {
                                        _UtilityMobs.console("Invalid player permissions entry: " + value + " (" + this.owner + ".txt)!");
                                    }
                                }
                            }
                        }
                        else if (key.equalsIgnoreCase("whitelist")) {
                            this.addListEntry(value, false);
                        }
                        else if (key.equalsIgnoreCase("blacklist")) {
                            this.addListEntry(value, true);
                        }
                        value = "";
                    }
                    continue;
                }
                if (status == 0) {
                    key += Character.toString((char)dat);
                }
                else if (status == 1) {
                    value += Character.toString((char)dat);
                }
            }
            in.close();
        }
        catch (Exception ex) {
            _UtilityMobs.console("Failed to load target data (" + this.owner + ".txt)!");
            ex.printStackTrace();
        }
        // Keep the broad default present so the hostile/neutral/passive gate is what actually decides.
        this.whitelist(LivingEntity.class);
    }

    /// Adds a parsed save/book/packet line to the white- or blacklist, falling back to the raw-id
    /// sets when the class cannot be resolved (unseen modded/vanilla entity - matched by id instead).
    private void addListEntry(String value, boolean toBlacklist) {
        Class<?> entry = TargetHelper.stringToClass(value);
        if (entry != null) {
            if (toBlacklist) {
                this.blacklist(entry);
            }
            else {
                this.whitelist(entry);
            }
        }
        else if (value.indexOf(':') >= 0) {
            if (toBlacklist) {
                this.mobBlacklistIds.add(value.toLowerCase(Locale.ROOT));
            }
            else {
                this.mobWhitelistIds.add(value.toLowerCase(Locale.ROOT));
            }
        }
        else {
            _UtilityMobs.console("Invalid " + (toBlacklist ? "blacklist" : "whitelist") + " entry: " + value + " (" + this.owner + ")!");
        }
    }

    // Loads this target helper from the byte buffer.
    public void load(FriendlyByteBuf buf) {
        if (this.owner == null || this.destroyed())
            return;
        try {
            this.permissions.clear();
            this.mobBlacklist.clear();
            this.mobBlacklistIds.clear();
            this.mobWhitelist.clear();
            this.mobWhitelistIds.clear();
            this.whitelistCache.clear();
            String list = "";
            list += "player_permissions" + buf.readUtf();
            list += "\n\nwhitelist" + buf.readUtf();
            list += "\n\nblacklist" + buf.readUtf();

            byte status = 0;
            String key = "";
            String value = "";
            for (int dat : list.toCharArray()) {
                if (dat == 13) {
                    continue;
                }
                if (dat == 10) {
                    if (status == 0) {
                        if ("".equals(key)) {
                            continue;
                        }
                        if (!key.equalsIgnoreCase("player_permissions") && !key.equalsIgnoreCase("whitelist") && !key.equalsIgnoreCase("blacklist")) {
                            _UtilityMobs.console("Unrecognized value in player config: " + key + " (" + this.owner + " packet)!");
                            key = "";
                            continue;
                        }
                        status = 1;
                    }
                    else if (status == 1) {
                        if ("".equals(value)) {
                            key = "";
                            status = 0;
                            continue;
                        }
                        if (key.equalsIgnoreCase("player_permissions")) {
                            for (int i = 0; i < value.length(); i++) {
                                if (value.charAt(i) == ' ') {
                                    try {
                                        this.setPermissions(value.substring(i + 1), Integer.parseInt(value.substring(0, i), 2));
                                    }
                                    catch (Exception ex) {
                                        _UtilityMobs.console("Invalid player permissions entry: " + value + " (" + this.owner + " packet)!");
                                    }
                                }
                            }
                        }
                        else if (key.equalsIgnoreCase("whitelist")) {
                            this.addListEntry(value, false);
                        }
                        else if (key.equalsIgnoreCase("blacklist")) {
                            this.addListEntry(value, true);
                        }
                        value = "";
                    }
                    continue;
                }
                if (status == 0) {
                    key += Character.toString((char)dat);
                }
                else if (status == 1) {
                    value += Character.toString((char)dat);
                }
            }
            this.save();
        }
        catch (Exception ex) {
            _UtilityMobs.console("Failed to load target data (" + this.owner + " packet)!");
            ex.printStackTrace();
        }
    }

    // Returns an empty target helper book.
    public static ItemStack book(int id) {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        BookHelper.addPages(book, "");
        book.getOrCreateTag().putByte("umt", (byte)id);
        if (id == 0) {
            EffectHelper.setItemName(book, 0xb, "Player Permissions");
        }
        else if (id == 1) {
            EffectHelper.setItemName(book, 0xb, "Mob Target List");
        }
        EffectHelper.setItemGlowing(book);
        return book;
    }

    // Writes a target helper's specs to a book.
    public static ItemStack write(String username, int id) {
        return TargetHelper.write(username, new ItemStack(id == 0 ? Items.WRITABLE_BOOK : Items.WRITTEN_BOOK), id);
    }
    public static ItemStack write(String username, ItemStack book, int id) {
        return TargetHelper.getTargetHelper(username).writeTo(book, id);
    }
    private ItemStack writeTo(ItemStack book, int id) {
        if (book == null || book.isEmpty())
            return book;
        BookHelper.removePages(book);
        if (id == 0) {
            book = TargetHelper.toWritable(book); // Cannot mutate an ItemStack's item in place.
            EffectHelper.setItemName(book, 0xb, "Player Permissions");
            EffectHelper.setItemText(book, 0x7, "by " + this.owner);
            EffectHelper.setItemGlowing(book);
            BookHelper.addPages(book,
                    " §7Player Permissions§0\n\nTo change a player's permissions, simply add a new line with the permissions you want that player to have - any previous lines will be overwritten when the book is saved.\nTo save any changes, simply craft this book by itself.",
                    " §7Player Permissions§0\n\nIf you do not save your changes, they will be erased the next time this book is right clicked!\n\nYou may also modify permissions by right clicking a player with this book, much like the Mob Target List.",
                    " §7Player Permissions§0\n\nRight clicking a player will grant that player the lowest permission he or she does not have.\n\nIf you are sneaking, instead it will remove the highest permission that player has!",
                    " §7Quick Permission Guide\n\nid:                    name§0\n\n000:                 none\n\n001:               target\n\n010:                   use\n\n100:                 open\n"
                    );
            if (this.permissions.size() <= 0) {
                BookHelper.addPages(book, " §lPermissions:§r\n<no permissions>");
            }
            else {
                String[] pages = new String[this.permissions.size()];
                int page = 0;
                byte line = 1;
                pages[0] = " §lPermissions:§r\n";
                for (Map.Entry<String, Byte> entry : this.permissions.entrySet()) {
                    if (line++ == 10) {
                        line = 0;
                        pages[++page] = "";
                    }
                    pages[page] += Integer.toBinaryString(entry.getValue().intValue()) + " " + entry.getKey() + "\n";
                }
                BookHelper.addPages(book, pages);
            }
        }
        else if (id == 1) {
            book = TargetHelper.toWritable(book);
            EffectHelper.setItemName(book, 0xb, "Mob Target List");
            EffectHelper.setItemText(book, 0x7, "by " + this.owner);
            EffectHelper.setItemGlowing(book);
            BookHelper.addPages(book,
                    " §7Mob Target List§0\n\nTo change your target list, simply add a new line with the entity you want to toggle - no need to delete lines.\nAn entity starting with \'!\' will NOT be targeted.\nTo save any changes, simply craft this book by itself.",
                    " §7Mob Target List§0\n\nIf you do not save your changes, they will be erased the next time this book is right clicked!\nYou may also toggle entities by right clicking them.",
                    " §7Mob Target List§0\n\nIf you right click while sneaking, the entity will be toggled with a \'!\'.\nOtherwise, it will be toggled normally."
                    );
            int totalEntries = this.mobWhitelist.size() + this.mobWhitelistIds.size() + this.mobBlacklist.size() + this.mobBlacklistIds.size();
            if (totalEntries <= 0) {
                BookHelper.addPages(book, " §lTarget List:§r\n<no entries>");
            }
            else {
                // One line per entry, ~10 lines per page, +1 for the header line and +1 slack so the
                // page index can never overrun.
                String[] pages = new String[totalEntries / 10 + 2];
                int page = 0;
                byte line = 1;
                pages[0] = " §lTarget List:§r\n";
                for (Class<?> entityClass : this.mobWhitelist) {
                    if (line++ == 10) {
                        line = 0;
                        pages[++page] = "";
                    }
                    pages[page] += TargetHelper.classToString(entityClass) + "\n";
                }
                for (String rawId : this.mobWhitelistIds) {
                    if (line++ == 10) {
                        line = 0;
                        pages[++page] = "";
                    }
                    pages[page] += rawId + "\n";
                }
                for (Class<?> entityClass : this.mobBlacklist) {
                    if (++line == 10) {
                        line = 0;
                        pages[++page] = "";
                    }
                    pages[page] += "!" + TargetHelper.classToString(entityClass) + "\n";
                }
                for (String rawId : this.mobBlacklistIds) {
                    if (++line == 10) {
                        line = 0;
                        pages[++page] = "";
                    }
                    pages[page] += "!" + rawId + "\n";
                }
                BookHelper.addPages(book, pages);
            }
        }
        book.getOrCreateTag().putByte("umt", (byte)id);
        return book;
    }

    // Returns a writable_book carrying the given book's NBT (ItemStack items are immutable).
    private static ItemStack toWritable(ItemStack book) {
        if (book.getItem() == Items.WRITABLE_BOOK)
            return book;
        ItemStack writable = new ItemStack(Items.WRITABLE_BOOK);
        if (book.getTag() != null) {
            writable.setTag(book.getTag());
        }
        return writable;
    }

    // Reads a target helper's specs from a book.
    public static void read(String username, ItemStack book) {
        TargetHelper.getTargetHelper(username).readFrom(book);
    }
    private void readFrom(ItemStack book) {
        if (book == null || book.isEmpty() || book.getTag() == null || !book.getTag().contains("pages"))
            return;
        ListTag pages = book.getTag().getList("pages", Tag.TAG_STRING);
        byte id = book.getTag().getByte("umt");
        String page;
        String line;
        int index;
        if (id == 0) {
            this.permissions.clear();
            for (int p = 0; p < pages.size(); p++) {
                page = pages.getString(p);
                while ((index = page.indexOf("\n")) >= 0) {
                    line = page.substring(0, index);
                    page = page.substring(index + 1);
                    this.readPermissionsLine(line);
                }
                this.readPermissionsLine(page);
            }
        }
        else if (id == 1) {
            this.mobBlacklist.clear();
            this.mobBlacklistIds.clear();
            this.mobWhitelist.clear();
            this.mobWhitelistIds.clear();
            this.whitelistCache.clear();
            for (int p = 0; p < pages.size(); p++) {
                page = pages.getString(p);
                while ((index = page.indexOf("\n")) >= 0) {
                    line = page.substring(0, index);
                    page = page.substring(index + 1);
                    this.readTargetListLine(line);
                }
                this.readTargetListLine(page);
            }
            // Keep the broad default present even if an old book's pages don't list it (see load()).
            this.whitelist(LivingEntity.class);
        }
        this.writeTo(book, id);
        this.save();
    }
    private void readPermissionsLine(String line) {
        int index = line.indexOf(" ");
        if (index <= 0)
            return;
        try {
            byte permission = (byte)Math.max(0, Integer.parseInt(line.substring(0, index), 2));
            String username = line.substring(index + 1);
            if (username.indexOf(" ") < 0) {
                this.setPermissions(username, permission);
            }
        }
        catch (Exception ex) {
            // Do nothing
        }
    }
    private void readTargetListLine(String line) {
        if (line.contains(" "))
            return;
        boolean blacklist = line.startsWith("!");
        if (blacklist) {
            line = line.substring(1);
        }
        if (line.isEmpty() || line.startsWith("§"))
            return;
        // Declarative add (not toggle): readFrom() clears the lists first, so the saved list
        // ends up exactly mirroring the book's pages.
        this.addListEntry(line, blacklist);
    }

    // Hash of a book's pages, used to detect when a player has edited and closed a target book.
    public static int signatureOf(ItemStack book) {
        if (book == null || book.isEmpty() || book.getTag() == null || !book.getTag().contains("pages"))
            return 0;
        return book.getTag().getList("pages", Tag.TAG_STRING).toString().hashCode();
    }

    // Stamps the current page signature onto the book so the next tick won't re-parse unchanged pages.
    public static void stampSignature(ItemStack book) {
        if (book != null && !book.isEmpty() && book.getTag() != null) {
            book.getTag().putInt("umh", TargetHelper.signatureOf(book));
        }
    }

    // Updates a target helper based on the entity interacted with.
    public static void interact(String username, ItemStack book, int id, LivingEntity entity, boolean sneaking) {
        TargetHelper.getTargetHelper(username).interactWith(book, id, entity, sneaking);
    }
    private void interactWith(ItemStack book, int id, LivingEntity entity, boolean sneaking) {
        TargetHelper.learn(entity);
        if (id == 0) {
            if (!(entity instanceof Player))
                return;
            byte playerPermissions = this.getPermissions(entity.getScoreboardName());
            if (sneaking) {
                if (playerPermissions > 0) {
                    for (byte permission = TargetHelper.HIGHEST_PERMISSION; permission > 0; permission >>= 1) if ((permission & playerPermissions) > 0) {
                        this.remPermissions(entity.getScoreboardName(), permission);
                        this.save();
                        break;
                    }
                }
            }
            else {
                for (byte permission = 1; permission <= TargetHelper.HIGHEST_PERMISSION; permission <<= 1) if ((permission & playerPermissions) == 0) {
                    this.addPermissions(entity.getScoreboardName(), permission);
                    this.save();
                    break;
                }
            }
        }
        else if (id == 1) {
            if (sneaking) {
                this.toggleBlacklist(entity.getClass());
            }
            else {
                this.toggleWhitelist(entity.getClass());
            }
            this.save();
        }
        this.writeTo(book, id);
    }

    // Called when a player logs in, to send his/her target helper to the server and send the server's handlers to the player.
    public static void fetchTargetHelpers(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            UMChannel.sendToPlayer(new MessageFetchTargetHelper(), serverPlayer);
            for (Map.Entry<String, TargetHelper> entry : TargetHelper.TARGET_HELPERS.entrySet()) {
                if (entry.getKey() != null && !entry.getValue().destroyed()) {
                    UMChannel.sendToPlayer(new MessageTargetHelper(entry.getValue()), serverPlayer);
                }
            }
        }
    }

    // Called when the target handler is updated to send changes to the server/other players.
    private void updateTargetHelper() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            // Pure client: push our own helper up to the server. 1.12.2 also required that no integrated
            // server was running, which is the same null check.
            if (this.owner != null && this.owner.equals(TargetHelper.localPlayerName())) {
                UMChannel.sendToServer(new MessageTargetHelper(this));
            }
        }
        else {
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : new ArrayList<>(level.players())) {
                    if (!this.owner.equals(player.getGameProfile().getName())) {
                        UMChannel.sendToPlayer(new MessageTargetHelper(this), player);
                    }
                }
            }
        }
    }

    /// The logged-in client player's name, or null off the client. Kept behind DistExecutor so the
    /// dedicated server never loads Minecraft.
    private static String localPlayerName() {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> toast.utilityMobs.client.UMClientNetwork::localPlayerName);
    }

    // Returns a loadable string from the class, if possible.
    private static String classToString(Class<?> entityClass) {
        String name = null;
        if (entityClass == Player.class || Player.class.isAssignableFrom(entityClass)) {
            name = "Player";
        }
        else if (entityClass == Enemy.class) {
            name = "Hostiles";
        }
        else {
            name = TargetHelper.CLASS_TO_ID.get(entityClass);
            if (name == null) {
                name = entityClass.getName();
            }
        }
        return name;
    }

    // Rebuilds the global attack blacklist from the config's general.attack_blacklist list.
    public static void loadGlobalBlacklist(String[] names) {
        TargetHelper.loadGlobalList(names, TargetHelper.GLOBAL_BLACKLIST, TargetHelper.GLOBAL_BLACKLIST_IDS, "attack_blacklist");
    }

    // Rebuilds the global attack whitelist from general.attack_whitelist. Same entry syntax as the blacklist.
    public static void loadGlobalWhitelist(String[] names) {
        TargetHelper.loadGlobalList(names, TargetHelper.GLOBAL_WHITELIST, TargetHelper.GLOBAL_WHITELIST_IDS, "attack_whitelist");
    }

    // Shared parser for both global lists. Resolves each entry to a class when possible (keeps subclass
    // coverage) AND remembers the registry-id form for any namespaced entry, so a modded id still matches
    // by the entity's own key even if its class can't be resolved at this lifecycle stage.
    private static void loadGlobalList(String[] names, HashSet<Class<?>> classes, HashSet<String> ids, String label) {
        classes.clear();
        ids.clear();
        if (names == null)
            return;
        for (String raw : names) {
            if (raw == null || raw.trim().isEmpty())
                continue;
            String name = raw.trim();
            Class<?> entry = TargetHelper.stringToClass(name);
            if (entry != null) {
                classes.add(entry);
            }
            if (name.indexOf(':') >= 0) {
                // Registry id (has a namespace) - remember it verbatim for order-independent key matching.
                ids.add(name.toLowerCase(Locale.ROOT));
            }
            else if (entry == null && !"Player".equals(name) && !"Hostiles".equals(name)) {
                _UtilityMobs.console(label + ": could not resolve entity '" + name + "' (skipped).");
            }
        }
    }

    // True if the entity is covered by the global attack blacklist.
    public static boolean isGloballyBlacklisted(Entity entity) {
        return TargetHelper.matchesGlobalList(entity, TargetHelper.GLOBAL_BLACKLIST, TargetHelper.GLOBAL_BLACKLIST_IDS);
    }

    // True if the entity is covered by the global attack whitelist.
    public static boolean isGloballyWhitelisted(Entity entity) {
        return TargetHelper.matchesGlobalList(entity, TargetHelper.GLOBAL_WHITELIST, TargetHelper.GLOBAL_WHITELIST_IDS);
    }

    // Matches an entity against a global list by class (covers subclasses of a resolved entry) OR by its
    // own registry id (covers entries whose class never resolved). Order-independent.
    private static boolean matchesGlobalList(Entity entity, HashSet<Class<?>> classes, HashSet<String> ids) {
        if (classes.isEmpty() && ids.isEmpty())
            return false;
        TargetHelper.learn(entity);
        Class<?> entityClass = entity.getClass();
        for (Class<?> listed : classes) {
            if (listed.isAssignableFrom(entityClass))
                return true;
        }
        if (!ids.isEmpty()) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null && ids.contains(key.toString().toLowerCase(Locale.ROOT)))
                return true;
        }
        return false;
    }

    // Attempts to load a class from the given string. Registry ids resolve only after their entity class
    // has been learned from a live instance (see CLASS_TO_ID); callers fall back to raw-id matching.
    private static Class<?> stringToClass(String line) {
        Class<?> entityClass = null;
        if (line.equals("Player")) {
            entityClass = Player.class;
        }
        else if (line.equals("Hostiles")) {
            entityClass = Enemy.class;
        }
        else {
            if (line.indexOf(':') >= 0) {
                entityClass = TargetHelper.ID_TO_CLASS.get(line.toLowerCase(Locale.ROOT));
            }
            if (entityClass == null && line.indexOf(':') < 0) {
                try {
                    entityClass = Class.forName(line);
                }
                catch (Exception ex) {
                    // Do nothing
                }
            }
        }
        return entityClass;
    }
}
