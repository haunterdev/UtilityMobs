package toast.utilityMobs.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import toast.utilityMobs.EntityGolemFishHook;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIWeaponAttack extends EntityAIBase
{
    public final EntityUtilityGolem golem;
    public final double moveSpeed;
    public EntityLivingBase target;
    public Path path = null;
    public int pathDelay = 0;
    public int sightTime = 0;

    public int rodTime = 0;

    // Bow state, mirroring vanilla EntityAIAttackRangedBow so a bow-armed golem behaves like a 1.9+
    // skeleton: keep its distance, circle-strafe, and actually DRAW the bow instead of teleporting an
    // arrow out of a static pose (issue #17).
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
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase entity = this.golem.getAttackTarget();
        if (entity == null)
            return false;
        this.target = entity;
        ItemStack weapon = this.golem.getEquipmentInSlot(0);
        if (!weapon.isEmpty() && (this.isRangedWeapon(weapon) || weapon.getItem() instanceof ItemFishingRod))
            return true;
        this.path = this.golem.getNavigator().getPathToEntityLiving(this.target);
        return this.path != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.golem.getRNG().nextInt(200) != 0 && this.golem.canAttack(this.target) && (!this.golem.getNavigator().noPath() || this.isRangedWeapon(this.golem.getEquipmentInSlot(0)));
    }

    @Override
    public void startExecuting() {
        this.pathDelay = 0;
        if (!this.isRangedWeapon(this.golem.getEquipmentInSlot(0))) {
            this.golem.getNavigator().setPath(this.path, this.moveSpeed);
        }
    }

    @Override
    public void resetTask() {
        boolean wasRanged = this.isRangedWeapon(this.golem.getEquipmentInSlot(0));
        this.target = null;
        this.sightTime = 0;
        this.stopStrafing();
        this.golem.resetActiveHand();
        this.golem.setAttackTarget(null);
        this.golem.getNavigator().clearPath();
        // Vanilla's EntityAIAttackRangedBow parks its cooldown at -1 on reset, so a mob that has just
        // killed something starts drawing at the new target immediately instead of sitting out the
        // leftover post-shot cooldown. Ranged only: melee golems keep their swing timer, as vanilla's
        // EntityAIAttackMelee does.
        if (wasRanged) {
            this.golem.golemAttackTime = 0;
        }
    }

    /// Clears the leftover sideways input from EntityMoveHelper.strafe().
    ///
    /// EntityMoveHelper only writes moveStrafing while it is in the STRAFE action; the WAIT branch it drops
    /// back to zeroes moveForward and nothing else. Once the bow AI stops strafing, the only thing touching
    /// moveStrafing is EntityLivingBase.onLivingUpdate's `moveStrafing *= 0.98F`, which takes several seconds
    /// to decay to nothing - that is the sideways "gliding" after a bow golem finishes off its target.
    /// Nothing else in this mod writes moveStrafing, so clearing it here is safe.
    private void stopStrafing() {
        this.strafingTime = -1;
        this.golem.setMoveStrafing(0.0F);
    }

    @Override
    public void updateTask() {
        ItemStack weapon = this.golem.getEquipmentInSlot(0);
        this.golem.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
        if (this.isRangedWeapon(weapon)) {
            this.updateRangedAttack(weapon);
        }
        else {
            if (!weapon.isEmpty() && weapon.getItem() instanceof ItemFishingRod) {
                if (this.rodTime > 0) {
                    this.rodTime--;
                }
                if (this.rodTime <= 0 && this.golem.getFishingRod()) {
                    float distanceSq = (float)this.golem.getDistanceSq(this.target);
                    if (distanceSq > 9.0F && distanceSq < 100.0F && this.golem.getEntitySenses().canSee(this.target)) {
                        this.golem.world.spawnEntity(new EntityGolemFishHook(this.golem.world, this.golem, this.target));
                        // The cast used to play the bow-shot sound, a 1.7.10 holdover from before the game
                        // had bobber sounds. Matches SpecialMobs' fishing zombie now (issue #1.10 / #11).
                        UMSound.playAt(this.golem, SoundEvents.ENTITY_BOBBER_THROW, 0.5F, 0.4F / (this.golem.getRNG().nextFloat() * 0.4F + 0.8F));
                        this.golem.setFishingRod(false);
                        this.rodTime = this.golem.getRNG().nextInt(11) + 32;
                    }
                }
            }

            if (this.golem.getEntitySenses().canSee(this.target) && --this.pathDelay <= 0) {
                this.pathDelay = 4 + this.golem.getRNG().nextInt(7);
                this.golem.getNavigator().tryMoveToEntityLiving(this.target, this.moveSpeed);
            }
            double reach = this.golem.width * this.golem.width * 4.0F + this.golem.width;
            if (this.golem.getDistanceSq(this.target.posX, this.target.getEntityBoundingBox().minY, this.target.posZ) <= reach) {
                if (this.golem.golemAttackTime <= 0) {
                    this.golem.golemAttackTime = 20;
                    // Broadcast the attack via entity-status 4 (the reliable status channel that already
                    // drives the large golems' hitTime arm-raise). Large golems turn it into hitTime; the
                    // biped-model golems turn it into a client-side swingArm (see EntityUtilityGolem). The
                    // bare server swingArm/SPacketAnimation was not animating the biped models in-game.
                    this.golem.world.setEntityState(this.golem, (byte)4);
                    this.golem.attackEntityAsMob(this.target);
                }
            }
        }
    }

    /// Ranged combat, modelled on vanilla's EntityAIAttackRangedBow (issue #17). A bow-armed golem now
    /// holds its ground at range, circle-strafes, and draws the bow for 20 ticks before releasing, which is
    /// what drives the 1.9+ aiming pose - the old code fired straight out of an idle stance and never moved.
    private void updateRangedAttack(ItemStack weapon) {
        boolean isBow = weapon.getItem() instanceof ItemBow;
        double distanceSq = this.golem.getDistanceSq(this.target.posX, this.target.getEntityBoundingBox().minY, this.target.posZ);
        boolean canSee = this.golem.getEntitySenses().canSee(this.target);
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
            this.golem.getNavigator().clearPath();
            this.strafingTime++;
        }
        else {
            this.golem.getNavigator().tryMoveToEntityLiving(this.target, this.moveSpeed);
            // Closing the distance again: drop the sideways input, or the golem crabs towards its target.
            this.stopStrafing();
        }

        if (this.strafingTime >= 20) {
            if (this.golem.getRNG().nextFloat() < 0.3F) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if (this.golem.getRNG().nextFloat() < 0.3F) {
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
            this.golem.getMoveHelper().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.golem.faceEntity(this.target, 30.0F, 30.0F);
        }
        else {
            this.golem.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
        }

        if (isBow) {
            // isHandActive() is already synced to clients, so the model reads it directly for the aim pose
            // - no extra DataParameter needed.
            if (this.golem.isHandActive()) {
                if (!canSee && this.sightTime < -60) {
                    this.golem.resetActiveHand();
                }
                else if (canSee && this.golem.getItemInUseMaxCount() >= 20) {
                    this.golem.resetActiveHand();
                    this.golem.doRangedAttack(this.target);
                    this.golem.golemAttackTime = EntityAIWeaponAttack.BOW_COOLDOWN;
                }
            }
            else if (this.golem.golemAttackTime <= 0 && this.sightTime >= -60) {
                this.golem.setActiveHand(EnumHand.MAIN_HAND);
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

    public boolean isRangedWeapon(ItemStack itemStack) {
        return !itemStack.isEmpty() && (itemStack.getItem() instanceof ItemBow || itemStack.getItem() instanceof ItemSnowball);
    }
}
