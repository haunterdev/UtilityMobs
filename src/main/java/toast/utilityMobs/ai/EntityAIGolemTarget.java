package toast.utilityMobs.ai;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import toast.utilityMobs.UMProfiler;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityAIGolemTarget extends EntityAINearestAttackableTarget<EntityLivingBase>
{
    /// Ticks between target searches for an untargeted golem, cached from golems.target_scan_interval.
    /// The scan (AABB query + per-candidate filter) is the per-tick cost; running it every Nth tick
    /// instead of every tick is the single biggest win for large golem counts. >= 1.
    public static int scanInterval = 10;
    /// Max line-of-sight raytraces per search, cached from golems.target_raytrace_cap. canSee() is the
    /// most expensive single op; we only raytrace the top-priority candidates until one is visible.
    public static int raytraceCap = 5;

    private final EntityAINearestAttackableTarget.Sorter sorter;
    private final Predicate<net.minecraft.entity.Entity> targetSelector;
    public final EntityUtilityGolem golem;
    public EntityLivingBase targetEntity;

    public EntityAIGolemTarget(EntityUtilityGolem entity) {
        super(entity, EntityLivingBase.class, 0, true, false, (Predicate<EntityLivingBase>)null);
        this.golem = entity;
        this.sorter = new EntityAINearestAttackableTarget.Sorter(entity);
        this.targetSelector = new EntityAIGolemTargetSelector(entity);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
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
        double range = this.getTargetDistance();
        List<EntityLivingBase> entityList = this.golem.world.getEntitiesWithinAABB(EntityLivingBase.class, this.golem.getEntityBoundingBox().grow(range, range, range), this.targetSelector);
        UMProfiler.end("target_scan", t0);
        UMProfiler.count("target_scan_calls", 1);
        UMProfiler.count("target_scan_candidates", entityList.size());
        if (entityList.isEmpty()) {
            this.targetEntity = null;
            return false;
        }
        int mode = 0;
        if (this.golem instanceof toast.utilityMobs.turret.EntityTurretGolem) {
            mode = ((toast.utilityMobs.turret.EntityTurretGolem) this.golem).getTargetMode();
        }
        this.targetEntity = this.pickTarget(entityList, mode);
        return this.targetEntity != null;
    }

    /// Selects a target by mode: 0=CLOSE, 1=FAR, 2=STRONG, 3=WEAK. Candidates are ordered by the mode's
    /// priority, then line-of-sight is checked in priority order, raytracing at most raytraceCap entities
    /// and returning the first visible one. This bounds the per-search raytrace cost while still
    /// preferring the highest-priority *visible* target.
    private EntityLivingBase pickTarget(List<EntityLivingBase> list, int mode) {
        if (mode == 0) {
            Collections.sort(list, this.sorter);    // CLOSE - vanilla nearest-first (Comparator<Entity>)
        } else {
            Collections.sort(list, this.comparatorFor(mode));
        }
        long t0 = UMProfiler.start();
        EntityLivingBase result = null;
        int rays = 0;
        for (EntityLivingBase e : list) {
            if (rays >= EntityAIGolemTarget.raytraceCap) {
                break;
            }
            rays++;
            UMProfiler.count("target_raytrace", 1);
            if (this.golem.getEntitySenses().canSee(e)) {
                result = e;
                break;
            }
        }
        UMProfiler.end("target_raytrace_time", t0);
        return result;
    }

    /// Orders candidates highest-priority-first for FAR/STRONG/WEAK modes. CLOSE is handled separately
    /// with the vanilla Sorter (which is a raw Comparator<Entity>).
    private Comparator<EntityLivingBase> comparatorFor(int mode) {
        if (mode == 1) {            // FAR - farthest first
            return new Comparator<EntityLivingBase>() {
                @Override public int compare(EntityLivingBase a, EntityLivingBase b) {
                    return Double.compare(golem.getDistanceSq(b), golem.getDistanceSq(a));
                }
            };
        }
        if (mode == 2) {            // STRONG - most health first, tie -> nearest
            return new Comparator<EntityLivingBase>() {
                @Override public int compare(EntityLivingBase a, EntityLivingBase b) {
                    int h = Float.compare(b.getHealth(), a.getHealth());
                    return h != 0 ? h : Double.compare(golem.getDistanceSq(a), golem.getDistanceSq(b));
                }
            };
        }
        if (mode == 3) {            // WEAK - least health first, tie -> nearest
            return new Comparator<EntityLivingBase>() {
                @Override public int compare(EntityLivingBase a, EntityLivingBase b) {
                    int h = Float.compare(a.getHealth(), b.getHealth());
                    return h != 0 ? h : Double.compare(golem.getDistanceSq(a), golem.getDistanceSq(b));
                }
            };
        }
        // Unreachable for mode 0 (handled by caller); typed nearest-first fallback keeps the compiler happy.
        return new Comparator<EntityLivingBase>() {
            @Override public int compare(EntityLivingBase a, EntityLivingBase b) {
                return Double.compare(golem.getDistanceSq(a), golem.getDistanceSq(b));
            }
        };
    }

    @Override
    public void startExecuting() {
        this.golem.setAttackTarget(this.targetEntity);
    }
}
