package toast.utilityMobs.ai;

import net.minecraft.entity.ai.EntityAIWander;
import toast.utilityMobs.UMProfiler;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemWander extends EntityAIWander {
    /// Max golems that may begin a wander per tick, cached from golems.wander_budget.
    public static int budget = 40;
    /// Remaining wander starts allowed this tick; reset each server tick by TickHandler.
    public static int budgetRemaining = 40;

    private final EntityUtilityGolem golem;

    public EntityAIGolemWander(EntityUtilityGolem entity, double speed) {
        super(entity, speed, 120);
        this.golem = entity;
    }

    /// Refills the per-tick wander budget. Called once per server tick.
    public static void resetBudget() {
        EntityAIGolemWander.budgetRemaining = EntityAIGolemWander.budget;
    }

    @Override
    public boolean shouldExecute() {
        // Only roam near a player, and never exceed the global per-tick wander budget. Both checks are
        // cheap and run BEFORE vanilla's findRandomTarget pathfinding cost.
        if (!this.golem.isPerfActive() || EntityAIGolemWander.budgetRemaining <= 0) {
            return false;
        }
        boolean execute = super.shouldExecute();
        if (execute) {
            EntityAIGolemWander.budgetRemaining--;
            UMProfiler.count("wander_started", 1);
        }
        return execute;
    }
}