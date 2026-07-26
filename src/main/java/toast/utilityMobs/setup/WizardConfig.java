package toast.utilityMobs.setup;

import java.util.HashMap;
import java.util.Map;

import toast.utilityMobs.Properties;

/** Maps wizard selections to config key/value pairs, and writes them to the live config. */
public abstract class WizardConfig {

    /** Pure, Minecraft-free: WizardState -> { "category@field" : Boolean|Integer|Double }. */
    public static Map<String, Object> resolve(WizardState s) {
        Map<String, Object> m = new HashMap<String, Object>();

        // Turrets
        m.put("turrets@require_ammo", Boolean.valueOf(s.requireAmmo));
        m.put("turrets@no_mob_aggro", Boolean.valueOf(s.mobsIgnoreTurrets));
        m.put("turrets@friendly_passthrough", Boolean.valueOf(s.friendlyPassthrough));
        m.put("turrets@collision", Boolean.valueOf(s.walkableTurrets));
        m.put("turrets@drop_chance", Double.valueOf(s.dropChance.value));

        // Combat - applied to golems AND colossals together
        m.put("golems@attack_passives", Boolean.valueOf(s.attackPassives));
        m.put("colossals@attack_passives", Boolean.valueOf(s.attackPassives));
        m.put("golems@attack_neutrals", Boolean.valueOf(s.attackNeutrals));
        m.put("colossals@attack_neutrals", Boolean.valueOf(s.attackNeutrals));
        m.put("_general@hostile", Boolean.valueOf(s.hostile));

        // World & recipes
        m.put("_general@creeper_head_rarity", Integer.valueOf(s.skullDrops.creeper));
        m.put("_general@skull_rarity", Integer.valueOf(s.skullDrops.skull));
        m.put("_general@alternate_manuals", Boolean.valueOf(s.alternateManuals));
        m.put("_general@give_book_on_first_join", Boolean.valueOf(s.giveBook));

        // Performance
        m.put("golems@collision_disable_density", Integer.valueOf(s.hugeArmies ? 24 : 0));
        m.put("golems@collision_interval", Integer.valueOf(s.hugeArmies ? 3 : 1));

        return m;
    }

    /**
     * Writes the resolved selections into the live mod config, saves, and reloads.
     *
     * <p>1.12.2 walked the Configuration's categories and set each Property by hand, then called
     * config.save() once. A ForgeConfigSpec has no category objects to walk, so Properties resolves
     * "category@field" straight to the backing ConfigValue; the single save+reload at the end is
     * unchanged, so a half-applied config is still not observable.
     */
    public static void apply(WizardState s) {
        for (Map.Entry<String, Object> e : resolve(s).entrySet()) {
            int at = e.getKey().indexOf('@');
            String category = e.getKey().substring(0, at);
            String field = e.getKey().substring(at + 1);
            Properties.setValue(category, field, e.getValue());
        }
        Properties.save();
    }
}
