package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntitySnowTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/snowturret.png");

    {
        this.maxAttackTime = 20;
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.POISON
        };
    }

    public EntitySnowTurret(World world) {
        super(world);
        this.texture = EntitySnowTurret.TEXTURE;
    }

    @Override
    public net.minecraft.item.Item getAmmoItem() { return net.minecraft.init.Items.SNOWBALL; }

    @Override
    public boolean isArrowBased() { return false; }
    @Override
    public double getDisplayDamageOverride() { return 0.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.knockback"); }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.SNOW);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            EntitySnowball snowball = new EntitySnowball(this.world, this);
            this.targetHelper.setOwned(snowball);
            this.upgrade.applyTo(snowball);
            double dX = target.posX - this.posX;
            // Aim at centre mass so point-blank / hugging mobs are hit (see EntityTurretGolem).
            double dY = (target.getEntityBoundingBox().minY + target.height * 0.5F) - snowball.posY;
            double dZ = target.posZ - this.posZ;
            double v = Math.sqrt(dX * dX + dZ * dZ) * 0.15;
            snowball.shoot(dX, dY + v, dZ, 1.6F, this.inaccuracyAt(Math.sqrt(dX * dX + dZ * dZ)));
            this.world.spawnEntity(snowball);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
