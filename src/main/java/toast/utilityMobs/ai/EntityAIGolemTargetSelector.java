package toast.utilityMobs.ai;

import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemTargetSelector implements Predicate<Entity>
{
    public final EntityUtilityGolem golem;

    public EntityAIGolemTargetSelector(EntityUtilityGolem entity) {
        this.golem = entity;
    }

    @Override
    public boolean test(Entity entity) {
        // Cheap filter only (no line-of-sight raytrace) - this runs against every entity in the scan box.
        // EntityAIGolemTarget applies the expensive hasLineOfSight() check to just the best few survivors.
        return this.golem.canAttackNoSight(entity);
    }
}
