package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityFireTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/fireturret.png");

    public EntityFireTurret(EntityType<? extends EntityFireTurret> type, Level level) {
        super(type, level);
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.KILLER, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
        };
        this.texture = EntityFireTurret.TEXTURE;
    }

    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.ignite"); }

    @Override
    protected Item getDropItem() {
        return Items.REDSTONE_BLOCK;
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
            arrow.setSecondsOnFire(100);
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.level().addFreshEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
