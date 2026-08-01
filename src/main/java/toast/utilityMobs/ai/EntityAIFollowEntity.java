package toast.utilityMobs.ai;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;

public class EntityAIFollowEntity extends EntityAIBase
{
    private final Class<? extends EntityLivingBase> followClass;
    // rangeMax is fixed; rangeMin (the stop/park distance) is settable so a follower can tighten how close
    // it gets - the melon golem tracks this to its heal range so it can reach a golem it needs to heal.
    private float rangeMin;
    private final float rangeMax;
    private final EntityGolem golem;
    private EntityLivingBase followEntity;
    private double moveSpeed;
    private boolean isFollowing;
    // Extra acceptance test on top of followClass. Used by the melon golem to skip other melon golems.
    private Predicate<EntityLivingBase> filter;
    // If set, a hurt candidate always wins over a healthy one, distance only breaking ties within a group.
    private boolean preferInjured;
    private int recheckTimer;

    public EntityAIFollowEntity(EntityGolem entity, Class<? extends EntityLivingBase> target, double speed, float min, float max) {
        this.isFollowing = false;
        this.followEntity = null;
        this.golem = entity;
        this.followClass = target;
        this.moveSpeed = speed;
        this.rangeMin = min;
        this.rangeMax = max;
        this.setMutexBits(3);
    }

    /** Sets the park distance (how close the follower gets before it stops pathing). Clamped to stay
        positive and below rangeMax so the follow logic can't invert. */
    public void setRangeMin(float min) {
        this.rangeMin = Math.max(0.5F, Math.min(min, this.rangeMax - 1.0F));
    }

    /** Narrows what counts as a follow target beyond followClass. */
    public EntityAIFollowEntity setFilter(Predicate<EntityLivingBase> filter) {
        this.filter = filter;
        return this;
    }

    /** Makes hurt candidates outrank healthy ones, and re-picks periodically so a healer moves on
        once whoever it was escorting is topped up. */
    public EntityAIFollowEntity setPreferInjured(boolean preferInjured) {
        this.preferInjured = preferInjured;
        return this;
    }

    private boolean isValidCandidate(EntityLivingBase candidate) {
        // world.getEntitiesWithinAABB does NOT exclude the searcher, so a golem could pick ITSELF, land
        // at distance 0 (inside rangeMin) and conclude there was nobody to follow - that is why melon
        // golems sometimes ignored every golem around them and just wandered (issue #14).
        if (candidate == this.golem || !candidate.isEntityAlive())
            return false;
        // Don't trail creative/spectator players (disableDamage covers both).
        if (candidate instanceof EntityPlayer && ((EntityPlayer)candidate).capabilities.disableDamage)
            return false;
        return this.filter == null || this.filter.test(candidate);
    }

    @Override
    public boolean shouldExecute() {
        if (this.followClass == null || this.golem.getAttackTarget() != null)
            return false;
        List<EntityLivingBase> l = this.golem.world.getEntitiesWithinAABB(this.followClass, this.golem.getEntityBoundingBox().grow(this.rangeMax, this.rangeMax, this.rangeMax));
        if (l.isEmpty())
            return false;

        // Pick the BEST candidate, not simply the first one the world handed back. Scanning the whole list
        // is what lets a healer switch to the remaining golems when the one it was escorting dies.
        this.followEntity = null;
        double bestScore = Double.MAX_VALUE;
        for (EntityLivingBase candidate : l) {
            if (!this.isValidCandidate(candidate))
                continue;
            double score = this.golem.getDistanceSq(candidate);
            if (this.preferInjured && candidate.getHealth() >= candidate.getMaxHealth()) {
                // Healthy golems are still worth escorting, just never in preference to a hurt one.
                score += 1.0E6;
            }
            if (score < bestScore) {
                bestScore = score;
                this.followEntity = candidate;
            }
        }
        if (this.followEntity == null)
            return false;
        if (this.golem.getDistanceSq(this.followEntity) < this.rangeMin * this.rangeMin || !this.golem.getEntitySenses().canSee(this.followEntity))
            return false;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        // A dead or no-longer-valid target used to keep the task alive until the navigator ran out of path,
        // which is the other half of "the melon golem never picks a new golem to follow".
        if (this.followEntity == null || !this.isValidCandidate(this.followEntity))
            return false;
        if (this.golem.getAttackTarget() != null)
            return false;
        // A healer re-evaluates every few seconds so it moves on to whoever needs it now.
        if (this.preferInjured && ++this.recheckTimer > 60)
            return false;
        return !this.golem.getNavigator().noPath();
    }

    @Override
    public void startExecuting() {
        this.isFollowing = false;
        this.recheckTimer = 0;
        this.golem.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        this.followEntity = null;
        this.recheckTimer = 0;
        this.golem.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        this.golem.getLookHelper().setLookPositionWithEntity(this.followEntity, 30.0F, 30.0F);
        this.golem.getNavigator().tryMoveToEntityLiving(this.followEntity, this.moveSpeed);
        this.isFollowing = true;
        if (this.isFollowing && this.golem.getDistanceSq(this.followEntity) < this.rangeMin * this.rangeMin && this.golem.getRNG().nextInt(10) == 0) {
            this.golem.getNavigator().clearPath();
        }
        else if ((this.isFollowing && this.golem.getDistanceSq(this.followEntity) > this.rangeMax * this.rangeMax || !this.golem.getEntitySenses().canSee(this.followEntity)) && this.golem.getRNG().nextInt(60) == 0) {
            this.golem.getNavigator().clearPath();
        }
    }
}
