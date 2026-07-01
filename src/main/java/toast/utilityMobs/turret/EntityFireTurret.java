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

public class EntityFireTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/fireturret.png");

    {
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.KILLER, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
        };
    }

    public EntityFireTurret(World world) {
        super(world);
        this.texture = EntityFireTurret.TEXTURE;
    }

    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.ignite"); }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.REDSTONE_BLOCK);
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
            arrow.setFire(100);
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.world.spawnEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
