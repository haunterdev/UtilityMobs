package toast.utilityMobs;

import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
    Central mapping for the legacy (1.7.10) string sound/particle names this mod used.
    In 1.7.10 sounds and particles were referenced by raw String; 1.12.2 uses
    {@link SoundEvent} constants and {@link EnumParticleTypes}. Mapped here once so the
    entity classes stay readable and consistent.
 */
public final class UMSound {
    private UMSound() {}

    // --- Sounds (legacy name -> SoundEvent) ---
    public static final SoundEvent GHAST_FIREBALL = SoundEvents.ENTITY_GHAST_SHOOT;     // "mob.ghast.fireball"
    public static final SoundEvent IRONGOLEM_DEATH = SoundEvents.ENTITY_IRONGOLEM_DEATH; // "mob.irongolem.death"
    public static final SoundEvent IRONGOLEM_HIT = SoundEvents.ENTITY_IRONGOLEM_HURT;    // "mob.irongolem.hit"
    public static final SoundEvent IRONGOLEM_THROW = SoundEvents.ENTITY_IRONGOLEM_ATTACK;// "mob.irongolem.throw"
    public static final SoundEvent IRONGOLEM_WALK = SoundEvents.ENTITY_IRONGOLEM_STEP;   // "mob.irongolem.walk"
    public static final SoundEvent NOTE_BASS = SoundEvents.BLOCK_NOTE_BASS;              // "note.bass"
    public static final SoundEvent NOTE_PLING = SoundEvents.BLOCK_NOTE_PLING;            // "note.pling"
    public static final SoundEvent BOW = SoundEvents.ENTITY_ARROW_SHOOT;                 // "random.bow"
    public static final SoundEvent CHEST_CLOSE = SoundEvents.BLOCK_CHEST_CLOSE;          // "random.chestclosed"
    public static final SoundEvent CHEST_OPEN = SoundEvents.BLOCK_CHEST_OPEN;            // "random.chestopen"
    public static final SoundEvent STEP_STONE = SoundEvents.BLOCK_STONE_STEP;            // "step.stone"
    public static final SoundEvent SPLASH = SoundEvents.ENTITY_GENERIC_SPLASH;           // "liquid.splash"

    // --- Particles (legacy name -> EnumParticleTypes) ---
    public static final EnumParticleTypes BUBBLE = EnumParticleTypes.WATER_BUBBLE;       // "bubble"
    public static final EnumParticleTypes EXPLODE = EnumParticleTypes.EXPLOSION_NORMAL;  // "explode"
    public static final EnumParticleTypes HUGE_EXPLOSION = EnumParticleTypes.EXPLOSION_HUGE;  // "hugeexplosion"
    public static final EnumParticleTypes LARGE_EXPLODE = EnumParticleTypes.EXPLOSION_LARGE;  // "largeexplode"
    public static final EnumParticleTypes PORTAL = EnumParticleTypes.PORTAL;             // "portal"
    public static final EnumParticleTypes SMOKE = EnumParticleTypes.SMOKE_NORMAL;        // "smoke"
    public static final EnumParticleTypes WATER_SPLASH = EnumParticleTypes.WATER_SPLASH; // "splash"

    /** Legacy {@code World.playSoundAtEntity} replacement (1.12.2 needs a SoundCategory). */
    public static void playAt(Entity entity, SoundEvent sound, float volume, float pitch) {
        World world = entity.world;
        world.playSound(null, entity.posX, entity.posY, entity.posZ, sound, SoundCategory.NEUTRAL, volume, pitch);
    }
}
