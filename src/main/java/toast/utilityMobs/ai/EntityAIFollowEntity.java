package toast.utilityMobs.ai;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class EntityAIFollowEntity extends Goal
{
    private final Class<? extends LivingEntity> followClass;
    // rangeMax is fixed; rangeMin (the stop/park distance) is settable so a follower can tighten how close
    // it gets - the melon golem tracks this to its heal range so it can reach a golem it needs to heal.
    private float rangeMin;
    private final float rangeMax;
    private final Mob golem;
    private LivingEntity followEntity;
    private double moveSpeed;
    private boolean isFollowing;
    /// Optional preference. Candidates matching it win over ones that do not, and the nearest match
    /// wins among equals. Null means "first candidate in the list", which is what 1.12.2 did.
    private java.util.function.Predicate<LivingEntity> preferred;

    public EntityAIFollowEntity(Mob entity, Class<? extends LivingEntity> target, double speed, float min, float max) {
        this.isFollowing = false;
        this.followEntity = null;
        this.golem = entity;
        this.followClass = target;
        this.moveSpeed = speed;
        this.rangeMin = min;
        this.rangeMax = max;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /** Sets the park distance (how close the follower gets before it stops pathing). Clamped to stay
        positive and below rangeMax so the follow logic can't invert. */
    public void setRangeMin(float min) {
        this.rangeMin = Math.max(0.5F, Math.min(min, this.rangeMax - 1.0F));
    }

    /** Sets the preference used when choosing who to follow. Differs from 1.12.2, which always took
        whichever candidate the entity list happened to return first: that is why a melon golem would
        walk off to a distant healthy golem while one next to it died. */
    public void setPreferred(java.util.function.Predicate<LivingEntity> preferred) {
        this.preferred = preferred;
    }

    @Override
    public boolean canUse() {
        if (this.followClass == null || this.golem.getTarget() != null)
            return false;
        List<? extends LivingEntity> l = this.golem.level().getEntitiesOfClass(this.followClass, this.golem.getBoundingBox().inflate(this.rangeMax, this.rangeMax, this.rangeMax));
        if (l.isEmpty())
            return false;
        this.followEntity = null;
        boolean havePreferred = false;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : l) {
            // Don't trail creative/spectator players (abilities.invulnerable covers both).
            if (candidate instanceof Player player && player.getAbilities().invulnerable)
                continue;
            if (this.preferred == null) {
                this.followEntity = candidate;
                break;
            }
            boolean matches = this.preferred.test(candidate);
            // A match always beats a non-match; among equals, take the nearest.
            if (havePreferred && !matches)
                continue;
            double distance = this.golem.distanceToSqr(candidate);
            if (matches && !havePreferred || distance < bestDistance) {
                this.followEntity = candidate;
                bestDistance = distance;
                havePreferred = matches;
            }
        }
        if (this.followEntity == null)
            return false;
        if (this.golem.distanceToSqr(this.followEntity) < this.rangeMin * this.rangeMin || !this.golem.getSensing().hasLineOfSight(this.followEntity))
            return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !(this.golem.getNavigation().isDone() || this.golem.getTarget() != null);
    }

    @Override
    public void start() {
        this.isFollowing = false;
        this.golem.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.followEntity = null;
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.followEntity, 30.0F, 30.0F);
        this.golem.getNavigation().moveTo(this.followEntity, this.moveSpeed);
        this.isFollowing = true;
        if (this.isFollowing && this.golem.distanceToSqr(this.followEntity) < this.rangeMin * this.rangeMin && this.golem.getRandom().nextInt(10) == 0) {
            this.golem.getNavigation().stop();
        }
        else if ((this.isFollowing && this.golem.distanceToSqr(this.followEntity) > this.rangeMax * this.rangeMax || !this.golem.getSensing().hasLineOfSight(this.followEntity)) && this.golem.getRandom().nextInt(60) == 0) {
            this.golem.getNavigation().stop();
        }
    }
}
