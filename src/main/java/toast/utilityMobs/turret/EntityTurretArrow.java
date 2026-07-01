package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/**
    A tipped-arrow fired by a turret. Identical to {@link EntityTippedArrow} in every way except that
    it puffs into a small cloud of particles when it is removed, instead of popping out of existence.
    Covers the two cases where turret arrows otherwise vanish without feedback: timing out after
    sticking in the ground (the 1200-tick in-ground despawn), and the server destroying the entity
    once it leaves the tracker. The poof is client-only and fires exactly once (guarded by isDead).
 */
public class EntityTurretArrow extends EntityTippedArrow
{
    public EntityTurretArrow(World world) {
        super(world);
    }

    public EntityTurretArrow(World world, EntityLivingBase shooter) {
        super(world, shooter);
    }

    public EntityTurretArrow(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void setDead() {
        if (this.world.isRemote && !this.isDead) {
            for (int i = 0; i < 5; i++) {
                double ox = (this.rand.nextDouble() - 0.5) * 0.35;
                double oy = this.rand.nextDouble() * 0.2;
                double oz = (this.rand.nextDouble() - 0.5) * 0.35;
                this.world.spawnParticle(EnumParticleTypes.CRIT, this.posX + ox, this.posY + oy, this.posZ + oz, 0.0, 0.04, 0.0);
            }
        }
        super.setDead();
    }
}
