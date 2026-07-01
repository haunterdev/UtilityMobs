package toast.utilityMobs.golem;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import toast.utilityMobs.UMSound;

public class EntityLargeGolem extends EntityUtilityGolem
{
    private int hitTime;
    private int animationTime;

    public EntityLargeGolem(World world) {
        super(world);
        this.setSize(1.4F, 2.9F);
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.hitTime > 0) {
            this.hitTime--;
        }
        if (this.animationTime > 0) {
            this.animationTime--;
        }
        // Drive sprint (and its dirt-kick particles) off actual leg motion, not raw velocity:
        // limbSwingAmount decays to ~0 within a couple ticks of stopping, so particles stop promptly
        // instead of lingering on residual slide (the 2.5E-7 velocity threshold never cleared).
        this.setSprinting(this.limbSwingAmount > 0.1F);
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        this.hitTime = 10;
        this.world.setEntityState(this, (byte)4);
        UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        return super.attackEntityAsMob(entity);
    }

    @Override
    public void hitEffects(Entity entity) {
        entity.motionY += 0.4;
    }

    @Override
    public void handleStatusUpdate(byte b) {
        if (b == 4) {
            this.hitTime = 10;
            UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        }
        else if (b == 11) {
            this.animationTime = 400;
        }
        else {
            super.handleStatusUpdate(b);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRONGOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        UMSound.playAt(this, UMSound.IRONGOLEM_WALK, 1.0F, 1.0F);
    }

    public int getHitTime() {
        return this.hitTime;
    }

    public int getAnimationTime() {
        return this.animationTime;
    }
}
