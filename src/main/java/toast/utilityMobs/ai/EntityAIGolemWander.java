package toast.utilityMobs.ai;

import net.minecraft.entity.ai.EntityAIWander;
import toast.utilityMobs.UMProfiler;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * Performance-aware replacement for vanilla EntityAIWander on golems.
 *
 * Golems are protectors and should roam, but a huge army all pathfinding to roam would reintroduce the
 * cost we just removed. Two guards keep it cheap at scale:
 *   - Activation gate: a golem with no player nearby does not roam (distant armies stay idle/free).
 *   - Global per-tick budget: only a capped number of golems may BEGIN a wander (the pathfinding part)
 *     on any single tick, so a large army trickles into motion instead of all pathing at once.
 *
 * The vanilla random execution chance (120, unchanged from vanilla) still rate-limits per-golem, so the
 * budget is only a ceiling for the worst case. Roam frequency and speed match the original exactly; only
 * the activation gate and budget are added. A wandering golem has an active path, so its collision
 * resumes (see EntityUtilityGolem.collideWithNearbyEntities) only while it is actually moving.
 */
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
