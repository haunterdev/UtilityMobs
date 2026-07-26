package toast.utilityMobs.ai;

import java.util.EnumSet;

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
        this.target = null;
        this.sightTime = 0;
        this.golem.setTarget(null);
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        ItemStack weapon = this.golem.getEquipmentInSlot(0);
        this.golem.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (this.isRangedWeapon(weapon)) {
            double distanceSq = this.golem.distanceToSqr(this.target);
            boolean canSee = this.golem.getSensing().hasLineOfSight(this.target);
            if (distanceSq <= 100.0 && canSee) {
                this.sightTime++;
            }
            else {
                this.sightTime = 0;
            }
            if (this.sightTime < 20) {
                this.golem.getNavigation().moveTo(this.target, this.moveSpeed);
            }
            else {
                this.golem.getNavigation().stop();
            }
            if (this.golem.golemAttackTime > 0)
                return;
            if (distanceSq > 100.0 || !canSee)
                return;
            this.golem.doRangedAttack(this.target);
            if (weapon.getItem() instanceof BowItem) {
                this.golem.golemAttackTime = 60;
            }
            else {
                this.golem.golemAttackTime = 20;
            }
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
                        UMSound.playAt(this.golem, UMSound.BOW, 0.5F, 0.4F / (this.golem.getRandom().nextFloat() * 0.4F + 0.8F));
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

    /// The attack timing/pathing must run every tick, not on the throttled goal cadence.
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean isRangedWeapon(ItemStack itemStack) {
        return !itemStack.isEmpty() && (itemStack.getItem() instanceof BowItem || itemStack.is(Items.SNOWBALL));
    }
}
