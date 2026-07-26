package toast.utilityMobs.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import toast.utilityMobs.turret.EntityTurretGolem;

public class EntityAITurretAttack extends Goal
{
    public final EntityTurretGolem golem;
    public LivingEntity target;

    public EntityAITurretAttack(EntityTurretGolem entity) {
        this.golem = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity entity = this.golem.getTarget();
        if (entity == null)
            return false;
        this.target = entity;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.golem.targetAI.canUse()) {
            this.golem.targetAI.start();
            this.target = this.golem.getTarget();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        this.target = null;
        this.golem.setTarget(null);
    }

    @Override
    public void tick() {
        // Aim the whole turret at the target directly. The look control only turns the head and is
        // clamped to ±limit of the (never-rotating) body, so a stationary turret's barrel ended up
        // stuck pointing backwards and only "snapped" toward the target on the firing tick. Setting
        // body + head + pitch (and their prev values, so there is no interp smear) every tick makes
        // the base and barrel track the target smoothly. ModelTurret maps foot=yBodyRot,
        // head=netHeadYaw(=0 here), headPitch=xRot.
        double dx = this.target.getX() - this.golem.getX();
        double dz = this.target.getZ() - this.golem.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double dy = (this.target.getY() + this.target.getEyeHeight())
                - (this.golem.getY() + this.golem.getEyeHeight());
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horiz) * (180.0 / Math.PI)));
        this.golem.setYRot(yaw);
        this.golem.yRotO = yaw;
        this.golem.yBodyRot = yaw;
        this.golem.yBodyRotO = yaw;
        this.golem.yHeadRot = yaw;
        this.golem.yHeadRotO = yaw;
        this.golem.setXRot(pitch);
        this.golem.xRotO = pitch;
        if (this.golem.getRandom().nextInt(40) == 0) {
            if (this.golem.getRandom().nextInt(2) == 0) {
                this.golem.golemAttackTime--;
            }
            else {
                this.golem.golemAttackTime++;
            }
        }
        if (this.golem.golemAttackTime > 0)
            return;
        if (this.golem.requiresAmmo() && !this.golem.hasAmmo())
            return; // hold fire until ammo is supplied
        this.golem.doRangedAttack(this.target);
        if (this.golem.requiresAmmo())
            this.golem.consumeAmmo(this.golem.getAmmoPerShot());
        this.golem.golemAttackTime = this.golem.maxAttackTime;
    }

    /// The turret aim + fire logic must run every tick, not on the throttled goal cadence.
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
