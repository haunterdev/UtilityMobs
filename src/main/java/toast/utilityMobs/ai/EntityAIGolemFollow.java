package toast.utilityMobs.ai;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemFollow extends Goal
{
    /// Distance (squared) beyond which a golem teleports to its owner instead of pathing. 12 blocks.
    private static final double TELEPORT_DIST_SQ = 144.0;

    /// Max golems that may teleport to their owner per tick, cached from golems.follow_teleports_per_tick.
    public static int teleportBudget = 20;
    /// Remaining teleports allowed this tick; reset each server tick by TickHandler.
    public static int teleportBudgetRemaining = 20;

    public final EntityUtilityGolem golem;
    public double moveSpeed;
    public float minDistance, maxDistance;

    public Player owner;
    public int pathDelay = 0;

    /// Refills the per-tick teleport budget. Called once per server tick.
    public static void resetBudget() {
        EntityAIGolemFollow.teleportBudgetRemaining = EntityAIGolemFollow.teleportBudget;
    }

    public EntityAIGolemFollow(EntityUtilityGolem entity, double speed, float min, float max) {
        this.golem = entity;
        this.moveSpeed = speed;
        this.minDistance = min;
        this.maxDistance = max;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /// Returns whether the Goal should begin execution.
    @Override
    public boolean canUse() {
        Player player = this.golem.getOwner();
        if (player == null || this.golem.isSitting() || this.golem.distanceToSqr(player) < this.minDistance * this.minDistance)
            return false;
        this.owner = player;
        return true;
    }

    /// Returns whether an in-progress Goal should continue executing.
    @Override
    public boolean canContinueToUse() {
        return !this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.owner) > this.maxDistance * this.maxDistance && !this.golem.isSitting();
    }

    /// Execute a one shot task or start executing a continuous task.
    @Override
    public void start() {
        this.pathDelay = 0;
    }

    /// Resets the task.
    @Override
    public void stop() {
        this.owner = null;
        this.golem.getNavigation().stop();
    }

    /// Updates the task
    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.owner, 10.0F, this.golem.getMaxHeadXRot());
        if (this.golem.isSitting() || --this.pathDelay > 0) {
            return;
        }
        this.pathDelay = 10;
        // When too far to realistically path, teleport instead of running a doomed A* search first.
        // Far golems skip straight to a budgeted teleport, so the army trickles to the player instead
        // of all pathing at once (the anvil-golem teleport lag spike).
        if (this.golem.distanceToSqr(this.owner) >= EntityAIGolemFollow.TELEPORT_DIST_SQ) {
            if (EntityAIGolemFollow.teleportBudgetRemaining > 0 && this.tryTeleportToOwner()) {
                EntityAIGolemFollow.teleportBudgetRemaining--;
            }
            return;
        }
        // Close enough to walk: path normally.
        this.golem.getNavigation().moveTo(this.owner, this.moveSpeed);
    }

    /// Searches the ring of blocks around the owner for a safe standing spot and teleports there.
    /// Returns true if a spot was found and the golem moved. Mirrors the original block-scan placement.
    private boolean tryTeleportToOwner() {
        int i = Mth.floor(this.owner.getX()) - 2;
        int j = Mth.floor(this.owner.getZ()) - 2;
        int k = Mth.floor(this.owner.getBoundingBox().minY);
        for (int l = 0; l <= 4; ++l) {
            for (int i1 = 0; i1 <= 4; ++i1) {
                if (l < 1 || i1 < 1 || l > 3 || i1 > 3) {
                    BlockPos posGround = new BlockPos(i + l, k - 1, j + i1);
                    BlockPos posBody = new BlockPos(i + l, k, j + i1);
                    BlockPos posHead = new BlockPos(i + l, k + 1, j + i1);
                    if (this.golem.level().getBlockState(posGround).isFaceSturdy(this.golem.level(), posGround, Direction.UP)
                            && !this.golem.level().getBlockState(posBody).blocksMotion()
                            && !this.golem.level().getBlockState(posHead).blocksMotion()) {
                        this.golem.moveTo(i + l + 0.5, k, j + i1 + 0.5, this.golem.getYRot(), this.golem.getXRot());
                        this.golem.getNavigation().stop();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
