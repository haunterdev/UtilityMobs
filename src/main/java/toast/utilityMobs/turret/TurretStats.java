package toast.utilityMobs.turret;

import java.util.ArrayList;
import java.util.List;

import toast.utilityMobs.EnumUpgrade;

/** Single source of truth for a turret's displayed combat stats, base and upgraded. */
public class TurretStats {
    public double rawDamage;       // arrow damage before velocity scaling
    public int projectiles;
    public int fireRateTicks;
    public double range;
    public boolean arrowBased;
    public double displayDamageOverride; // used when !arrowBased
    public final List<String> effects = new ArrayList<String>();
    public final List<Integer> effectColors = new ArrayList<Integer>();

    /// Spread model captured from the turret (see EntityTurretGolem.inaccuracyAt). Drives the accuracy stat.
    public float baseInacc;
    public float falloff;
    public float maxInacc;
    /// Spread value that maps to 0% accuracy. Worst turret spread is the shotgun/volley at 16; 20 keeps
    /// even those readably above zero while pinpoint turrets sit near 100%.
    private static final double ACCURACY_REF = 20.0;

    private final double velocity;

    private TurretStats(double velocity) {
        this.velocity = velocity;
    }

    /** Per-hit damage shown to the player. */
    public double displayedDamage() {
        if (!this.arrowBased) {
            return this.displayDamageOverride;
        }
        double d = this.velocity * this.rawDamage;
        return Math.max(1.0, Math.ceil(d));
    }

    public double shotsPerSecond() {
        return this.fireRateTicks <= 0 ? 0.0 : 20.0 / this.fireRateTicks;
    }

    /// Accuracy as a 0-100% reading derived from the spread at horizontal distance d.
    private int accuracyPct(double dist) {
        double inacc = Math.min(this.maxInacc, this.baseInacc + dist * this.falloff);
        int pct = (int)Math.round(100.0 * (1.0 - inacc / ACCURACY_REF));
        return Math.max(0, Math.min(100, pct));
    }
    /// Accuracy at point blank.
    public int accuracyNear() { return this.accuracyPct(0.0); }
    /// Accuracy at this stat block's max range (reflects the sight upgrade's doubled range). When this
    /// differs from accuracyNear the turret has range falloff; equal means flat accuracy at all ranges.
    public int accuracyFar() { return this.accuracyPct(this.range); }

    public static TurretStats base(EntityTurretGolem turret) {
        TurretStats s = new TurretStats(turret.getProjectileVelocity());
        s.rawDamage = turret.getProjectileDamage();
        s.projectiles = turret.getProjectileCount();
        s.fireRateTicks = turret.getMaxAttackTime();
        s.range = turret.getBaseRange();
        s.arrowBased = turret.isArrowBased();
        s.displayDamageOverride = turret.getDisplayDamageOverride();
        s.baseInacc = turret.getBaseInaccuracy();
        s.falloff = turret.getInaccuracyFalloff();
        s.maxInacc = turret.getMaxInaccuracy();
        for (String key : turret.getBaseEffectKeys()) {
            s.effects.add(key);
            s.effectColors.add(0x404040); // dark gray - readable on the light panel
        }
        return s;
    }

    /** Returns a NEW stats object with the upgrade applied. Mirrors EnumUpgrade.applyToArrow. */
    public static TurretStats withUpgrade(EntityTurretGolem turret, EnumUpgrade upgrade) {
        TurretStats s = base(turret);
        if (upgrade == null || upgrade == EnumUpgrade.DEFAULT) {
            return s;
        }
        int c = upgrade.gradientColor;
        if (upgrade == EnumUpgrade.KILLER) {
            s.rawDamage = s.rawDamage * 1.5 + 1.0;
        }
        else if (upgrade == EnumUpgrade.FIRE) {
            s.rawDamage -= 1.0;
            addEffect(s, "utilitymobs.effect.ignite", c);
        }
        else if (upgrade == EnumUpgrade.EXPLOSIVE) {
            s.rawDamage -= 1.0;
            addEffect(s, "utilitymobs.effect.explode", c);
        }
        else if (upgrade == EnumUpgrade.FIRE_EXPLOSIVE) {
            s.rawDamage -= 2.0;
            addEffect(s, "utilitymobs.effect.ignite", c);
            addEffect(s, "utilitymobs.effect.explode_fire", c);
        }
        else if (upgrade == EnumUpgrade.SIGHT) {
            s.range *= 2.0;   // matches the multiplicative sightBoost attribute modifier (doubles range)
        }
        else if (upgrade == EnumUpgrade.POISON) {
            addEffect(s, "utilitymobs.effect.poison", c);
        }
        else if (upgrade == EnumUpgrade.SLOW) {
            addEffect(s, "utilitymobs.effect.slow", c);
        }
        else if (upgrade == EnumUpgrade.EGG) {
            addEffect(s, "utilitymobs.effect.egg", c);
        }
        else if (upgrade == EnumUpgrade.FEATHER) {
            addEffect(s, "utilitymobs.effect.mobile", c);
        }
        if (s.arrowBased && s.rawDamage <= 0.0) {
            s.rawDamage = Double.MIN_VALUE;
        }
        return s;
    }

    private static void addEffect(TurretStats s, String key, int color) {
        s.effects.add(key);
        s.effectColors.add(color);
    }

    /** Convenience: build stats for the turret's currently equipped upgrade (slot 0). */
    public static TurretStats current(EntityTurretGolem turret) {
        EnumUpgrade up = EnumUpgrade.getUpgrade(turret.upgrades, turret.getEquipmentInSlot(0));
        return withUpgrade(turret, up);
    }

    /** True when an upgrade changed a numeric stat vs base (for arrow direction display). */
    public boolean damageDiffersFrom(TurretStats other) {
        return Math.abs(this.displayedDamage() - other.displayedDamage()) > 0.0001 || this.projectiles != other.projectiles;
    }

    public boolean rangeDiffersFrom(TurretStats other) {
        return Math.abs(this.range - other.range) > 0.0001;
    }
}
