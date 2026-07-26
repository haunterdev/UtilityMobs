package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityGhastTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/ghastturret.png");

    /// 1.12.2 EntityLargeFireball defaulted explosionPower to 1; 1.20.1 requires it as a ctor argument.
    private static final int EXPLOSION_POWER = 1;

    public EntityGhastTurret(EntityType<? extends EntityGhastTurret> type, Level level) {
        super(type, level);
        this.maxAttackTime = 100;
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.POISON
        };
        this.texture = EntityGhastTurret.TEXTURE;
    }

    @Override
    public Item getAmmoItem() { return Items.FIRE_CHARGE; }

    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 8);
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
        return Items.NETHER_BRICKS;
    }

    /// Executes this ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            LargeFireball fireball = new LargeFireball(this.level(), this,
                target.getX() - this.getX(),
                target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (this.getY() + this.getBbHeight() / 2.0F),
                target.getZ() - this.getZ(),
                EXPLOSION_POWER);
            this.targetHelper.setOwned(fireball);
            this.upgrade.applyTo(fireball);
            fireball.setPos(fireball.getX(), this.getY() + this.getBbHeight() - 0.5, fireball.getZ());
            this.atMuzzle(fireball, target);
            this.level().addFreshEntity(fireball);
        }
        UMSound.playAt(this, UMSound.GHAST_FIREBALL, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
