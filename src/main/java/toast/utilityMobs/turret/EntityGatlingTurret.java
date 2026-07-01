package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityGatlingTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/gatlingturret.png");

    {
        this.maxAttackTime = 15;
    }

    public EntityGatlingTurret(World world) {
        super(world);
        this.texture = EntityGatlingTurret.TEXTURE;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 6);
    }

    @Override
    public double getProjectileDamage() { return Double.MIN_VALUE; }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.GOLD_BLOCK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            EntityTippedArrow arrow = new EntityTurretArrow(this.world, this);
            double dx = target.posX - this.posX;
            double dy = (target.getEntityBoundingBox().minY + target.height * 0.5F) - arrow.posY;
            double dz = target.posZ - this.posZ;
            double dist = (double)MathHelper.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + dist * 0.15, dz, 1.6F, this.inaccuracyAt(dist));
            arrow.setDamage(this.getProjectileDamage());
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            EnumUpgrade.MULTISHOT.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.world.spawnEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
