package toast.utilityMobs.setup;

/** Plain, Minecraft-free holder for the setup wizard's selections. Unit-testable. */
public class WizardState {

    public enum DropChance {
        ALWAYS(1.0), HIGH(0.75), HALF(0.5), LOW(0.25), NEVER(0.0);
        public final double value;
        DropChance(double v) { this.value = v; }
    }

    public enum SkullDrops {
        COMMON(20, 15), NORMAL(80, 60), OFF(0, 0);
        public final int creeper;
        public final int skull;
        SkullDrops(int creeper, int skull) { this.creeper = creeper; this.skull = skull; }
    }

    public boolean requireAmmo;
    public boolean mobsIgnoreTurrets;
    public boolean friendlyPassthrough;
    public boolean walkableTurrets;
    public boolean attackPassives;
    public boolean attackNeutrals;
    public boolean hostile;
    public boolean alternateManuals;
    public boolean giveBook = true;
    public boolean hugeArmies;
    public DropChance dropChance = DropChance.HALF;
    public SkullDrops skullDrops = SkullDrops.NORMAL;

    public static WizardState engineer() {
        WizardState s = new WizardState();
        s.requireAmmo = true;
        s.walkableTurrets = true;
        s.dropChance = DropChance.LOW;       // 0.25
        s.giveBook = true;
        return s;
    }

    public static WizardState warlord() {
        WizardState s = new WizardState();
        s.mobsIgnoreTurrets = true;
        s.friendlyPassthrough = true;
        s.walkableTurrets = true;
        s.dropChance = DropChance.ALWAYS;    // 1.0
        s.attackPassives = true;
        s.attackNeutrals = true;
        s.skullDrops = SkullDrops.COMMON;
        s.giveBook = true;
        s.hugeArmies = true;
        return s;
    }

    public static WizardState survivor() {
        WizardState s = new WizardState();
        s.requireAmmo = true;
        s.dropChance = DropChance.NEVER;     // 0.0
        s.alternateManuals = true;
        s.giveBook = true;
        return s;
    }

    /** Mod defaults - matches the unmodified config so picking this changes nothing surprising. */
    public static WizardState custom() {
        WizardState s = new WizardState();
        s.dropChance = DropChance.HALF;      // 0.5 default
        s.giveBook = true;
        return s;
    }

    public DropChance nextDrop() {
        DropChance[] v = DropChance.values();
        dropChance = v[(dropChance.ordinal() + 1) % v.length];
        return dropChance;
    }

    public SkullDrops nextSkull() {
        SkullDrops[] v = SkullDrops.values();
        skullDrops = v[(skullDrops.ordinal() + 1) % v.length];
        return skullDrops;
    }
}
