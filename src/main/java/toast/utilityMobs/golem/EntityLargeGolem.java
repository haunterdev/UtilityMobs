package toast.utilityMobs.golem;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import toast.utilityMobs.UMSound;

public class EntityLargeGolem extends EntityUtilityGolem
{
    private int hitTime;
    private int animationTime;

    /// Registered entity size: 1.4 x 2.9 (set via EntityType.Builder.sized at registration).
    public EntityLargeGolem(EntityType<? extends EntityLargeGolem> type, Level level) {
        super(type, level);
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 40.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.hitTime > 0) {
            this.hitTime--;
        }
        if (this.animationTime > 0) {
            this.animationTime--;
        }
        // Drive sprint (and its dirt-kick particles) off actual leg motion, not raw velocity:
        // walkAnimation speed decays to ~0 within a couple ticks of stopping, so particles stop
        // promptly instead of lingering on residual slide (the 2.5E-7 velocity threshold never cleared).
        this.setSprinting(this.walkAnimation.speed() > 0.1F);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        this.hitTime = 10;
        this.level().broadcastEntityEvent(this, (byte)4);
        UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        return super.doHurtTarget(entity);
    }

    @Override
    public void hitEffects(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, motion.y + 0.4, motion.z);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.hitTime = 10;
            UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        }
        else if (id == 11) {
            this.animationTime = 400;
        }
        else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UMSound.playAt(this, UMSound.IRONGOLEM_WALK, 1.0F, 1.0F);
    }

    public int getHitTime() {
        return this.hitTime;
    }

    public int getAnimationTime() {
        return this.animationTime;
    }
}
