package toast.utilityMobs.turret;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import toast.utilityMobs.UMSound;

public class EntitySniperTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/sniperturret.png");

    {
        this.maxAttackTime = 70;
    }

    public EntitySniperTurret(World world) {
        super(world);
        this.texture = EntitySniperTurret.TEXTURE;
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(20.0);
    }

    @Override
    public double getProjectileDamage() { return 3.0; }

    // Sniper is pinpoint at any range: flat tight spread, no distance falloff.
    @Override
    public float getBaseInaccuracy() { return 3.0F; }
    @Override
    public float getInaccuracyFalloff() { return 0.0F; }
    @Override
    public float getMaxInaccuracy() { return 3.0F; }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.LAPIS_BLOCK);
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            float power = 1.6F;
            float distance = this.getDistance(target);
            if (20.0F < distance) {
                power += (distance - 20.0F) * 3.0F / 100.0F;
            }
            EntityTippedArrow arrow = new EntityTurretArrow(this.world, this);
            double dx = target.posX - this.posX;
            double dy = (target.getEntityBoundingBox().minY + target.height * 0.5F) - arrow.posY;
            double dz = target.posZ - this.posZ;
            double dist = (double)MathHelper.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + dist * 0.15, dz, power, this.inaccuracyAt(dist));
            arrow.setDamage(this.getProjectileDamage());
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.world.spawnEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
