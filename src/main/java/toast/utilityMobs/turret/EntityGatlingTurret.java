package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityGatlingTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/gatlingturret.png");

    public EntityGatlingTurret(EntityType<? extends EntityGatlingTurret> type, Level level) {
        super(type, level);
        this.maxAttackTime = 15;
        this.texture = EntityGatlingTurret.TEXTURE;
    }

    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 6);
    }

    @Override
    public double getProjectileDamage() { return Double.MIN_VALUE; }

    @Override
    protected Item getDropItem() {
        return Items.GOLD_BLOCK;
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            EntityTurretArrow arrow = this.atMuzzle(new EntityTurretArrow(this.level(), this), target);
            double dx = target.getX() - arrow.getX();
            double dy = (target.getBoundingBox().minY + target.getBbHeight() * 0.5F) - arrow.getY();
            double dz = target.getZ() - arrow.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + dist * 0.15, dz, 1.6F, this.inaccuracyAt(dist));
            arrow.setBaseDamage(this.getProjectileDamage());
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            EnumUpgrade.MULTISHOT.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.level().addFreshEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
