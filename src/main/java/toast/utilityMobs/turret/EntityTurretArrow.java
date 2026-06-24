package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

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