package toast.utilityMobs.ai;

import java.util.EnumSet;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIWeaponAttack extends Goal
{
    public final EntityUtilityGolem golem;
    public final double moveSpeed;
    public LivingEntity target;
    public Path path = null;
    public int pathDelay = 0;
    public int sightTime = 0;

    public int rodTime = 0;

    // Bow state, mirroring vanilla RangedBowAttackGoal so a bow-armed golem behaves like a 1.9+ skeleton:
    // keep its distance, circle-strafe, and actually DRAW the bow instead of firing out of a static pose
    // (1.12.2 issue #17).
    private static final double BOW_RANGE_SQ = 100.0;
    // Ticks between releasing an arrow and starting the next draw. The draw itself is 20 ticks, so the
    // full cycle stays at the 60 ticks per shot the golems fired at before.
    private static final int BOW_COOLDOWN = 40;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public EntityAIWeaponAttack(EntityUtilityGolem entity, double speed) {
        this.golem = entity;
        this.moveSpeed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity entity = this.golem.getTarget();
        if (entity == null)
            return false;
        this.target = entity;
        ItemStack weapon = this.golem.getEquipmentInSlot(0);
        if (!weapon.isEmpty() && (this.isRangedWeapon(weapon) || weapon.getItem() instanceof FishingRodItem))
            return true;
        this.path = this.golem.getNavigation().createPath(this.target, 0);
        return this.path != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.golem.getRandom().nextInt(200) != 0 && this.golem.canAttack(this.target) && (!this.golem.getNavigation().isDone() || this.isRangedWeapon(this.golem.getEquipmentInSlot(0)));
    }

    @Override
    public void start() {
        this.pathDelay = 0;
        if (!this.isRangedWeapon(this.golem.getEquipmentInSlot(0))) {
            this.golem.getNavigation().moveTo(this.path, this.moveSpeed);
        }
    }

    @Override
    public void stop() {
        boolean wasRanged = this.isRangedWeapon(this.golem.getEquipmentInSlot(0));
        this.target = null;
        this.sightTime = 0;
        this.stopStrafing();
        this.golem.stopUsingItem();
        this.golem.setTarget(null);
        this.golem.getNavigation().stop();
        // Vanilla's RangedBowAttackGoal parks its cooldown at -1 on stop, so a mob that has just killed
        // something starts drawing at the new target immediately instead of sitting out the leftover
        // post-shot cooldown. Ranged only: melee golems keep their swing timer, as MeleeAttackGoal does.
        if (wasRanged) {
            this.golem.golemAttackTime = 0;
        }
    }

    /// Clears the leftover sideways input from MoveControl.strafe().
    ///
    /// MoveControl only writes the strafe input while it is in the STRAFE operation; the WAIT operation it
    /// drops back to zeroes zza and nothing else. Once the bow AI stops strafing, the only thing touching xxa
    /// is LivingEntity.aiStep's `xxa *= 0.98F`, which takes several seconds to decay to nothing - that is the
    /// sideways "gliding" after a bow golem finishes off its target. Nothing else in this mod writes xxa.
    private void stopStrafing() {
        this.strafingTime = -1;
        this.golem.setXxa(0.0F);
    }

    @Override
    public void tick() {
        ItemStack weapon = this.golem.getEquipmentInSlot(0);
        this.golem.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (this.isRangedWeapon(weapon)) {
            this.tickRangedAttack(weapon);
        }
        else {
            if (!weapon.isEmpty() && weapon.getItem() instanceof FishingRodItem) {
                if (this.rodTime > 0) {
                    this.rodTime--;
                }
                if (this.rodTime <= 0 && this.golem.getFishingRod()) {
                    float distanceSq = (float)this.golem.distanceToSqr(this.target);
                    if (distanceSq > 9.0F && distanceSq < 100.0F && this.golem.getSensing().hasLineOfSight(this.target)) {
                        this.golem.level().addFreshEntity(new toast.utilityMobs.EntityGolemFishHook(this.golem.level(), this.golem, this.target));
                        // The cast used to play the bow-shot sound, a 1.7.10 holdover from before the game
                        // had bobber sounds. Matches SpecialMobs' fishing zombie now (1.12.2 issue #1.10).
                        UMSound.playAt(this.golem, SoundEvents.FISHING_BOBBER_THROW, 0.5F, 0.4F / (this.golem.getRandom().nextFloat() * 0.4F + 0.8F));
                        this.golem.setFishingRod(false);
                        this.rodTime = this.golem.getRandom().nextInt(11) + 32;
                    }
                }
            }

            if (this.golem.getSensing().hasLineOfSight(this.target) && --this.pathDelay <= 0) {
                this.pathDelay = 4 + this.golem.getRandom().nextInt(7);
                this.golem.getNavigation().moveTo(this.target, this.moveSpeed);
            }
            double reach = this.golem.getBbWidth() * this.golem.getBbWidth() * 4.0F + this.golem.getBbWidth();
            if (this.golem.distanceToSqr(this.target.getX(), this.target.getBoundingBox().minY, this.target.getZ()) <= reach) {
                if (this.golem.golemAttackTime <= 0) {
                    this.golem.golemAttackTime = 20;
                    // Broadcast the attack via entity-status 4 (the reliable status channel that already
                    // drives the large golems' hitTime arm-raise). Large golems turn it into hitTime; the
                    // biped-model golems turn it into a client-side swing (see EntityUtilityGolem).
                    this.golem.level().broadcastEntityEvent(this.golem, (byte)4);
                    this.golem.doHurtTarget(this.target);
                }
            }
        }
    }

    /// Ranged combat, modelled on vanilla's RangedBowAttackGoal (1.12.2 issue #17). A bow-armed golem now
    /// holds its ground at range, circle-strafes, and draws the bow for 20 ticks before releasing, which is
    /// what drives the 1.9+ aiming pose - the old code fired straight out of an idle stance and never moved.
    private void tickRangedAttack(ItemStack weapon) {
        boolean isBow = weapon.getItem() instanceof BowItem;
        double distanceSq = this.golem.distanceToSqr(this.target.getX(), this.target.getBoundingBox().minY, this.target.getZ());
        boolean canSee = this.golem.getSensing().hasLineOfSight(this.target);
        if (canSee != this.sightTime > 0) {
            this.sightTime = 0;
        }
        if (canSee) {
            this.sightTime++;
        }
        else {
            this.sightTime--;
        }

        if (distanceSq <= EntityAIWeaponAttack.BOW_RANGE_SQ && this.sightTime >= 20) {
            this.golem.getNavigation().stop();
            this.strafingTime++;
        }
        else {
            this.golem.getNavigation().moveTo(this.target, this.moveSpeed);
            // Closing the distance again: drop the sideways input, or the golem crabs towards its target.
            this.stopStrafing();
        }

        if (this.strafingTime >= 20) {
            if (this.golem.getRandom().nextFloat() < 0.3F) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if (this.golem.getRandom().nextFloat() < 0.3F) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }

        if (this.strafingTime > -1) {
            if (distanceSq > EntityAIWeaponAttack.BOW_RANGE_SQ * 0.75) {
                this.strafingBackwards = false;
            }
            else if (distanceSq < EntityAIWeaponAttack.BOW_RANGE_SQ * 0.25) {
                this.strafingBackwards = true;
            }
            this.golem.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.golem.lookAt(this.target, 30.0F, 30.0F);
        }
        else {
            this.golem.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        }

        if (isBow) {
            // isUsingItem() is already synced to clients, so the model reads it directly for the aim pose
            // - no extra EntityDataAccessor needed.
            if (this.golem.isUsingItem()) {
                if (!canSee && this.sightTime < -60) {
                    this.golem.stopUsingItem();
                }
                else if (canSee && this.golem.getTicksUsingItem() >= 20) {
                    this.golem.stopUsingItem();
                    this.golem.doRangedAttack(this.target);
                    this.golem.golemAttackTime = EntityAIWeaponAttack.BOW_COOLDOWN;
                }
            }
            else if (this.golem.golemAttackTime <= 0 && this.sightTime >= -60) {
                this.golem.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
        else {
            // Snowballs have no draw animation, so they keep the plain fire-on-cooldown cadence.
            if (this.golem.golemAttackTime > 0 || distanceSq > EntityAIWeaponAttack.BOW_RANGE_SQ || !canSee)
                return;
            this.golem.doRangedAttack(this.target);
            this.golem.golemAttackTime = 20;
        }
    }

    /// The attack timing/pathing must run every tick, not on the throttled goal cadence.
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean isRangedWeapon(ItemStack itemStack) {
        return !itemStack.isEmpty() && (itemStack.getItem() instanceof BowItem || itemStack.is(Items.SNOWBALL));
    }
}
