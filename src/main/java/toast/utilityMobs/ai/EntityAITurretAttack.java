package toast.utilityMobs.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import toast.utilityMobs.turret.EntityTurretGolem;

public class EntityAITurretAttack extends EntityAIBase
{
    public final EntityTurretGolem golem;
    public EntityLivingBase target;

    public EntityAITurretAttack(EntityTurretGolem entity) {
        this.golem = entity;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase entity = this.golem.getAttackTarget();
        if (entity == null)
            return false;
        this.target = entity;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.golem.targetAI.shouldExecute()) {
            this.golem.targetAI.startExecuting();
            this.target = this.golem.getAttackTarget();
            return true;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        // Do nothing
    }

    @Override
    public void resetTask() {
        this.target = null;
        this.golem.setAttackTarget(null);
    }

    @Override
    public void updateTask() {
        // Aim the whole turret at the target directly. The look helper only turns the head and is
        // clamped to ±limit of the (never-rotating) body, so a stationary turret's barrel ended up
        // stuck pointing backwards and only "snapped" toward the target on the firing tick. Setting
        // body + head + pitch (and their prev values, so there is no interp smear) every tick makes
        // the base and barrel track the target smoothly. ModelTurret maps foot=renderYawOffset,
        // head=netHeadYaw(=0 here), headPitch=rotationPitch.
        double dx = this.target.posX - this.golem.posX;
        double dz = this.target.posZ - this.golem.posZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double dy = (this.target.posY + this.target.getEyeHeight())
                - (this.golem.posY + this.golem.getEyeHeight());
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horiz) * (180.0 / Math.PI)));
        this.golem.rotationYaw = yaw;
        this.golem.prevRotationYaw = yaw;
        this.golem.renderYawOffset = yaw;
        this.golem.prevRenderYawOffset = yaw;
        this.golem.rotationYawHead = yaw;
        this.golem.prevRotationYawHead = yaw;
        this.golem.rotationPitch = pitch;
        this.golem.prevRotationPitch = pitch;
        if (this.golem.getRNG().nextInt(40) == 0) {
            if (this.golem.getRNG().nextInt(2) == 0) {
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
}
