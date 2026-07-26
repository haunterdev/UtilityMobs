package toast.utilityMobs.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import toast.utilityMobs.block.EntityContainerGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemSit extends Goal
{
    // The golem using this AI.
    public final EntityUtilityGolem golem;
    // If set to true, this golem will sit wherever it is.
    public boolean sitAnywhere = false;
    // If this AI should execute.
    public boolean sit = false;

    public EntityAIGolemSit(EntityUtilityGolem entity) {
        this.golem = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    /// Returns whether the Goal should begin execution.
    @Override
    public boolean canUse() {
        if (this.sitAnywhere || !this.golem.isInWater() && this.golem.onGround())
            return this.sit || this.golem instanceof EntityContainerGolem container && container.isOpen();
        return false;
    }

    /// Execute a one shot task or start executing a continuous task.
    @Override
    public void start() {
        this.golem.getNavigation().stop();
        this.golem.setSitting(true);
    }

    /// Resets the task.
    @Override
    public void stop() {
        this.golem.setSitting(false);
    }
}
