package toast.utilityMobs.turret;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityKillerTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/killerturret.png");

    public EntityKillerTurret(World world) {
        super(world);
        this.texture = EntityKillerTurret.TEXTURE;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 18);
    }

    @Override
    public double getProjectileDamage() { return 3.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.aoe"); }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.DIAMOND_BLOCK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            double range = this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            List<Entity> entityList = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(range * 1.5, range * 1.5, range * 1.5));
            EntityTippedArrow arrow = null;
            for (Entity entity : entityList) {
                if (this.canAttack(entity)) {
                    arrow = new EntityTurretArrow(this.world, this);
                    double dx = entity.posX - this.posX;
                    double dy = (entity.getEntityBoundingBox().minY + entity.height * 0.5F) - arrow.posY;
                    double dz = entity.posZ - this.posZ;
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
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
