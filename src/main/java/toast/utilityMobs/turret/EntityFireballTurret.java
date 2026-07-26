package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntityFireballTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/fireballturret.png");

    public EntityFireballTurret(EntityType<? extends EntityFireballTurret> type, Level level) {
        super(type, level);
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
        };
        this.texture = EntityFireballTurret.TEXTURE;
    }

    @Override
    public Item getAmmoItem() { return Items.FIRE_CHARGE; }

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
        return Items.NETHERRACK;
    }

    /// Executes this ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            SmallFireball fireball = new SmallFireball(this.level(), this,
                target.getX() - this.getX(),
                target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (this.getY() + this.getBbHeight() / 2.0F),
                target.getZ() - this.getZ());
            this.targetHelper.setOwned(fireball);
            this.upgrade.applyTo(fireball);
            fireball.setPos(fireball.getX(), this.getY() + this.getBbHeight() - 0.5, fireball.getZ());
            this.atMuzzle(fireball, target);
            this.level().addFreshEntity(fireball);
        }
        UMSound.playAt(this, UMSound.GHAST_FIREBALL, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
