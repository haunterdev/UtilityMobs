package toast.utilityMobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Central mapping for the legacy (1.7.10) string sound/particle names this mod used.
 * Retargeted to the 1.20.1 constants; note block sounds are Holders in 1.20.1, unwrapped
 * here once so entity code keeps using plain {@link SoundEvent}s.
 */
public final class UMSound {
    private UMSound() {}

    // --- Sounds (legacy name -> SoundEvent) ---
    public static final SoundEvent GHAST_FIREBALL = SoundEvents.GHAST_SHOOT;          // "mob.ghast.fireball"
    public static final SoundEvent IRONGOLEM_DEATH = SoundEvents.IRON_GOLEM_DEATH;    // "mob.irongolem.death"
    public static final SoundEvent IRONGOLEM_HIT = SoundEvents.IRON_GOLEM_HURT;       // "mob.irongolem.hit"
    public static final SoundEvent IRONGOLEM_THROW = SoundEvents.IRON_GOLEM_ATTACK;   // "mob.irongolem.throw"
    public static final SoundEvent IRONGOLEM_WALK = SoundEvents.IRON_GOLEM_STEP;      // "mob.irongolem.walk"
    public static final SoundEvent NOTE_BASS = SoundEvents.NOTE_BLOCK_BASS.value();   // "note.bass"
    public static final SoundEvent NOTE_PLING = SoundEvents.NOTE_BLOCK_PLING.value(); // "note.pling"
    public static final SoundEvent BOW = SoundEvents.ARROW_SHOOT;                     // "random.bow"
    public static final SoundEvent CHEST_CLOSE = SoundEvents.CHEST_CLOSE;             // "random.chestclosed"
    public static final SoundEvent CHEST_OPEN = SoundEvents.CHEST_OPEN;               // "random.chestopen"
    public static final SoundEvent STEP_STONE = SoundEvents.STONE_STEP;               // "step.stone"
    public static final SoundEvent SPLASH = SoundEvents.GENERIC_SPLASH;               // "liquid.splash"

    // --- Particles (legacy name -> particle type) ---
    public static final SimpleParticleType BUBBLE = ParticleTypes.BUBBLE;             // "bubble"
    public static final SimpleParticleType EXPLODE = ParticleTypes.POOF;              // "explode"
    public static final SimpleParticleType HUGE_EXPLOSION = ParticleTypes.EXPLOSION_EMITTER; // "hugeexplosion"
    public static final SimpleParticleType LARGE_EXPLODE = ParticleTypes.EXPLOSION;   // "largeexplode"
    public static final SimpleParticleType PORTAL = ParticleTypes.PORTAL;             // "portal"
    public static final SimpleParticleType SMOKE = ParticleTypes.SMOKE;               // "smoke"
    public static final SimpleParticleType WATER_SPLASH = ParticleTypes.SPLASH;       // "splash"

    /** Legacy {@code World.playSoundAtEntity} replacement. */
    public static void playAt(Entity entity, SoundEvent sound, float volume, float pitch) {
        Level level = entity.level();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }
}
