package toast.utilityMobs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemFollow extends EntityAIBase
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

    public EntityPlayer owner;
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
        this.setMutexBits(3);
    }

    /// Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityPlayer player = this.golem.getOwner();
        if (player == null || this.golem.isSitting() || this.golem.getDistanceSq(player) < this.minDistance * this.minDistance)
            return false;
        this.owner = player;
        return true;
    }

    /// Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean shouldContinueExecuting() {
        return !this.golem.getNavigator().noPath() && this.golem.getDistanceSq(this.owner) > this.maxDistance * this.maxDistance && !this.golem.isSitting();
    }

    /// Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.pathDelay = 0;
    }

    /// Resets the task.
    @Override
    public void resetTask() {
        this.owner = null;
        this.golem.getNavigator().clearPath();
    }

    /// Updates the task
    @Override
    public void updateTask() {
        this.golem.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, this.golem.getVerticalFaceSpeed());
        if (this.golem.isSitting() || --this.pathDelay > 0) {
            return;
        }
        this.pathDelay = 10;
        // When too far to realistically path, teleport instead of running a doomed A* search first.
        // The old code A*'d every follow tick for far golems (the path almost always fails at range) and
        // only then teleported - with a big army that ran the player away, that was thousands of failed
        // pathfinds in one tick (the anvil-golem teleport lag spike). Now far golems skip straight to a
        // budgeted teleport, so the army trickles to the player instead of all pathing at once.
        if (this.golem.getDistanceSq(this.owner) >= EntityAIGolemFollow.TELEPORT_DIST_SQ) {
            if (EntityAIGolemFollow.teleportBudgetRemaining > 0 && this.tryTeleportToOwner()) {
                EntityAIGolemFollow.teleportBudgetRemaining--;
            }
            return;
        }
        // Close enough to walk: path normally.
        this.golem.getNavigator().tryMoveToEntityLiving(this.owner, this.moveSpeed);
    }

    /// Searches the ring of blocks around the owner for a safe standing spot and teleports there.
    /// Returns true if a spot was found and the golem moved. Mirrors the original block-scan placement.
    private boolean tryTeleportToOwner() {
        int i = MathHelper.floor(this.owner.posX) - 2;
        int j = MathHelper.floor(this.owner.posZ) - 2;
        int k = MathHelper.floor(this.owner.getEntityBoundingBox().minY);
        for (int l = 0; l <= 4; ++l) {
            for (int i1 = 0; i1 <= 4; ++i1) {
                if (l < 1 || i1 < 1 || l > 3 || i1 > 3) {
                    BlockPos posGround = new BlockPos(i + l, k - 1, j + i1);
                    BlockPos posBody = new BlockPos(i + l, k, j + i1);
                    BlockPos posHead = new BlockPos(i + l, k + 1, j + i1);
                    if (this.golem.world.getBlockState(posGround).isTopSolid() && !this.golem.world.getBlockState(posBody).isNormalCube() && !this.golem.world.getBlockState(posHead).isNormalCube()) {
                        this.golem.setLocationAndAngles(i + l + 0.5F, k, j + i1 + 0.5F, this.golem.rotationYaw, this.golem.rotationPitch);
                        this.golem.getNavigator().clearPath();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
