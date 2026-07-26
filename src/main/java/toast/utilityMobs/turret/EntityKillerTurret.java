package toast.utilityMobs.turret;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityKillerTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/killerturret.png");

    public EntityKillerTurret(EntityType<? extends EntityKillerTurret> type, Level level) {
        super(type, level);
        this.texture = EntityKillerTurret.TEXTURE;
    }

    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 18);
    }

    @Override
    public double getProjectileDamage() { return 3.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.aoe"); }

    @Override
    protected Item getDropItem() {
        return Items.DIAMOND_BLOCK;
    }

    /// Executes this golem's ranged attack. Fires at EVERY valid target in range at once.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            AttributeInstance rangeAttr = this.getAttribute(Attributes.FOLLOW_RANGE);
            double range = rangeAttr == null ? 10.0 : rangeAttr.getValue();
            List<Entity> entityList = this.level().getEntities(this, this.getBoundingBox().inflate(range * 1.5, range * 1.5, range * 1.5));
            for (Entity entity : entityList) {
                if (this.canAttack(entity)) {
                    EntityTurretArrow arrow = this.atMuzzle(new EntityTurretArrow(this.level(), this), entity);
                    double dx = entity.getX() - arrow.getX();
                    double dy = (entity.getBoundingBox().minY + entity.getBbHeight() * 0.5F) - arrow.getY();
                    double dz = entity.getZ() - arrow.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    arrow.shoot(dx, dy + dist * 0.15, dz, 1.6F, this.inaccuracyAt(dist));
                    arrow.setBaseDamage(this.getProjectileDamage());
                    this.targetHelper.setOwned(arrow);
                    this.upgrade.applyToArrow(arrow);
                    EnumUpgrade.MULTISHOT.applyToArrow(arrow);
                    this.prepareFiredArrow(arrow);
                    this.level().addFreshEntity(arrow);
                }
            }
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
