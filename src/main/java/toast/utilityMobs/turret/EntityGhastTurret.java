package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityGhastTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/ghastturret.png");

    {
        this.maxAttackTime = 100;
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.POISON
        };
    }

    public EntityGhastTurret(World world) {
        super(world);
        this.texture = EntityGhastTurret.TEXTURE;
    }

    @Override
    public net.minecraft.item.Item getAmmoItem() { return net.minecraft.init.Items.FIRE_CHARGE; }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 8);
    }

    // Fireballs fly a direct trajectory with no spread roll - perfect accuracy at any range.
    @Override
    public float getBaseInaccuracy() { return 0.0F; }
    @Override
    public float getInaccuracyFalloff() { return 0.0F; }
    @Override
    public float getMaxInaccuracy() { return 0.0F; }

    @Override
    public boolean isArrowBased() { return false; }
    @Override
    public double getDisplayDamageOverride() { return 6.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.explode"); }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.NETHER_BRICK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            EntityLargeFireball fireball = new EntityLargeFireball(this.world, this, target.posX - this.posX, target.getEntityBoundingBox().minY + target.height / 2.0F - (this.posY + this.height / 2.0F), target.posZ - this.posZ);
            this.targetHelper.setOwned(fireball);
            this.upgrade.applyTo(fireball);
            fireball.posY = this.posY + this.height - 0.5;
            this.world.spawnEntity(fireball);
        }
        UMSound.playAt(this, UMSound.GHAST_FIREBALL, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
