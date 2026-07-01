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

public class EntityShotgunTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/shotgunturret.png");

    public EntityShotgunTurret(World world) {
        super(world);
        this.texture = EntityShotgunTurret.TEXTURE;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 12);
    }

    @Override
    public double getProjectileDamage() { return 1.0; }
    @Override
    public int getProjectileCount() { return 6; }

    // Shotgun is a deliberate wide-cone spread weapon: flat large spread at all ranges.
    @Override
    public float getBaseInaccuracy() { return 16.0F; }
    @Override
    public float getInaccuracyFalloff() { return 0.0F; }
    @Override
    public float getMaxInaccuracy() { return 16.0F; }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.IRON_BLOCK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            EntityTippedArrow arrow;
            for (int i = this.getProjectileCount(); i-- > 0;) {
                arrow = new EntityTurretArrow(this.world, this);
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
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
