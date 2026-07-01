package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityFireballTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/fireballturret.png");

    {
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
        };
    }

    public EntityFireballTurret(World world) {
        super(world);
        this.texture = EntityFireballTurret.TEXTURE;
    }

    @Override
    public net.minecraft.item.Item getAmmoItem() { return net.minecraft.init.Items.FIRE_CHARGE; }

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
    public double getDisplayDamageOverride() { return 4.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.ignite"); }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.NETHERRACK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            EntitySmallFireball fireball = new EntitySmallFireball(this.world, this, target.posX - this.posX, target.getEntityBoundingBox().minY + target.height / 2.0F - (this.posY + this.height / 2.0F), target.posZ - this.posZ);
            this.targetHelper.setOwned(fireball);
            this.upgrade.applyTo(fireball);
            fireball.posY = this.posY + this.height - 0.5;
            this.world.spawnEntity(fireball);
        }
        UMSound.playAt(this, UMSound.GHAST_FIREBALL, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
