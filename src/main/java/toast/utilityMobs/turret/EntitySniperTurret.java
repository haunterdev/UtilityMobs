package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.UMSound;

public class EntitySniperTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/sniperturret.png");

    public EntitySniperTurret(EntityType<? extends EntitySniperTurret> type, Level level) {
        super(type, level);
        this.maxAttackTime = 70;
        this.texture = EntitySniperTurret.TEXTURE;
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTurretGolem.createAttributes()
            .add(Attributes.FOLLOW_RANGE, 20.0);
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
        return Items.LAPIS_BLOCK;
    }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            float power = 1.6F;
            float distance = this.distanceTo(target);
            if (20.0F < distance) {
                power += (distance - 20.0F) * 3.0F / 100.0F;
            }
            EntityTurretArrow arrow = this.atMuzzle(new EntityTurretArrow(this.level(), this), target);
            double dx = target.getX() - arrow.getX();
            double dy = (target.getBoundingBox().minY + target.getBbHeight() * 0.5F) - arrow.getY();
            double dz = target.getZ() - arrow.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + dist * 0.15, dz, power, this.inaccuracyAt(dist));
            arrow.setBaseDamage(this.getProjectileDamage());
            this.targetHelper.setOwned(arrow);
            this.upgrade.applyToArrow(arrow);
            this.prepareFiredArrow(arrow);
            this.level().addFreshEntity(arrow);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
