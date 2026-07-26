package toast.utilityMobs.ai;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import toast.utilityMobs.UMProfiler;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemTarget extends NearestAttackableTargetGoal<LivingEntity>
{
    /// Ticks between target searches for an untargeted golem, cached from golems.target_scan_interval. >= 1.
    public static int scanInterval = 10;
    /// Max line-of-sight raytraces per search, cached from golems.target_raytrace_cap. hasLineOfSight()
    /// is the most expensive single op; we only raytrace the top-priority candidates until one is visible.
    public static int raytraceCap = 5;

    private final Predicate<Entity> targetSelector;
    public final EntityUtilityGolem golem;
    public LivingEntity targetEntity;

    public EntityAIGolemTarget(EntityUtilityGolem entity) {
        super(entity, LivingEntity.class, 0, true, false, null);
        this.golem = entity;
        this.targetSelector = new EntityAIGolemTargetSelector(entity);
    }

    @Override
    public boolean canUse() {
        // NOTE: the scan-interval throttle (scanInterval/id-stagger) was REVERTED - it added a
        // 10-40 tick "detection delay" before a golem/colossus noticed a target, which felt sluggish.
        // We now scan every tick the golem is perf-active. raytraceCap + the activation gate below
        // still bound the cost; the per-tick AABB query is cheap enough without the extra delay.
        // Activation gate: no player nearby -> do not scan for targets (matches collision gating).
        if (!this.golem.isPerfActive()) {
            UMProfiler.count("target_scan_inactive", 1);
            return false;
        }
        long t0 = UMProfiler.start();
        double range = this.getFollowDistance();
        List<LivingEntity> entityList = this.golem.level().getEntitiesOfClass(LivingEntity.class, this.golem.getBoundingBox().inflate(range, range, range), this.targetSelector::test);
        UMProfiler.end("target_scan", t0);
        UMProfiler.count("target_scan_calls", 1);
        UMProfiler.count("target_scan_candidates", entityList.size());
        if (entityList.isEmpty()) {
            this.targetEntity = null;
            return false;
        }
        int mode = 0;
        if (this.golem instanceof toast.utilityMobs.turret.EntityTurretGolem turret) {
            mode = turret.getTargetMode();
        }
        this.targetEntity = this.pickTarget(entityList, mode);
        return this.targetEntity != null;
    }

    /// Selects a target by mode: 0=CLOSE, 1=FAR, 2=STRONG, 3=WEAK. Candidates are ordered by the mode's
    /// priority, then line-of-sight is checked in priority order, raytracing at most raytraceCap entities
    /// and returning the first visible one. This bounds the per-search raytrace cost while still
    /// preferring the highest-priority *visible* target.
    private LivingEntity pickTarget(List<LivingEntity> list, int mode) {
        Collections.sort(list, this.comparatorFor(mode));
        long t0 = UMProfiler.start();
        LivingEntity result = null;
        int rays = 0;
        for (LivingEntity e : list) {
            if (rays >= EntityAIGolemTarget.raytraceCap) {
                break;
            }
            rays++;
            UMProfiler.count("target_raytrace", 1);
            if (this.golem.getSensing().hasLineOfSight(e)) {
                result = e;
                break;
            }
        }
        UMProfiler.end("target_raytrace_time", t0);
        return result;
    }

    /// Orders candidates highest-priority-first. Mode 0 (CLOSE) is nearest-first, matching the vanilla
    /// Sorter the 1.12.2 version used (the Sorter class no longer exists in 1.20.1).
    private Comparator<LivingEntity> comparatorFor(int mode) {
        if (mode == 1) {            // FAR - farthest first
            return (a, b) -> Double.compare(this.golem.distanceToSqr(b), this.golem.distanceToSqr(a));
        }
        if (mode == 2) {            // STRONG - most health first, tie -> nearest
            return (a, b) -> {
                int h = Float.compare(b.getHealth(), a.getHealth());
                return h != 0 ? h : Double.compare(this.golem.distanceToSqr(a), this.golem.distanceToSqr(b));
            };
        }
        if (mode == 3) {            // WEAK - least health first, tie -> nearest
            return (a, b) -> {
                int h = Float.compare(a.getHealth(), b.getHealth());
                return h != 0 ? h : Double.compare(this.golem.distanceToSqr(a), this.golem.distanceToSqr(b));
            };
        }
        // CLOSE (mode 0) and fallback: nearest first.
        return (a, b) -> Double.compare(this.golem.distanceToSqr(a), this.golem.distanceToSqr(b));
    }

    @Override
    public void start() {
        this.golem.setTarget(this.targetEntity);
    }
}
