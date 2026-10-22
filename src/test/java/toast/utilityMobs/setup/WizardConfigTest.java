package toast.utilityMobs.setup;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;

public class WizardConfigTest {

    @Test
    public void engineer_maps_to_expected_values() {
        Map<String, Object> m = WizardConfig.resolve(WizardState.engineer());
        assertEquals(Boolean.TRUE, m.get("turrets@require_ammo"));
        assertEquals(Boolean.TRUE, m.get("turrets@collision"));
        assertEquals(Boolean.FALSE, m.get("turrets@no_mob_aggro"));
        assertEquals(Double.valueOf(0.25), m.get("turrets@drop_chance"));
        assertEquals(Boolean.FALSE, m.get("golems@attack_passives"));
        assertEquals(Integer.valueOf(0), m.get("golems@collision_disable_density"));
        assertEquals(Integer.valueOf(1), m.get("golems@collision_interval"));
    }

    @Test
    public void warlord_maps_to_expected_values() {
        Map<String, Object> m = WizardConfig.resolve(WizardState.warlord());
        assertEquals(Boolean.FALSE, m.get("turrets@require_ammo"));
        assertEquals(Boolean.TRUE, m.get("turrets@no_mob_aggro"));
        assertEquals(Double.valueOf(1.0), m.get("turrets@drop_chance"));
        assertEquals(Boolean.TRUE, m.get("golems@attack_passives"));
        assertEquals(Boolean.TRUE, m.get("colossals@attack_passives"));
        assertEquals(Integer.valueOf(20), m.get("_general@creeper_head_rarity"));
        assertEquals(Integer.valueOf(15), m.get("_general@skull_rarity"));
        assertEquals(Integer.valueOf(24), m.get("golems@collision_disable_density"));
        assertEquals(Integer.valueOf(3), m.get("golems@collision_interval"));
    }

    @Test
    public void custom_matches_mod_defaults() {
        Map<String, Object> m = WizardConfig.resolve(WizardState.custom());
        assertEquals(Boolean.FALSE, m.get("turrets@require_ammo"));
        assertEquals(Boolean.FALSE, m.get("turrets@collision"));
        assertEquals(Double.valueOf(0.5), m.get("turrets@drop_chance"));
        assertEquals(Integer.valueOf(80), m.get("_general@creeper_head_rarity"));
    }

    @Test
    public void decoupling_warlord_with_ammo_flipped_back_on() {
        WizardState s = WizardState.warlord();
        s.requireAmmo = true;
        Map<String, Object> m = WizardConfig.resolve(s);
        assertEquals(Boolean.TRUE, m.get("turrets@require_ammo"));
        assertEquals(Boolean.TRUE, m.get("turrets@no_mob_aggro"));
        assertEquals(Double.valueOf(1.0), m.get("turrets@drop_chance"));
    }
}
