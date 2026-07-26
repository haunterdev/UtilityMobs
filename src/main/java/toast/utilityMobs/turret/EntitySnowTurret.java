package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.UMSound;

public class EntitySnowTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/snowturret.png");

    public EntitySnowTurret(EntityType<? extends EntitySnowTurret> type, Level level) {
        super(type, level);
        this.maxAttackTime = 20;
        this.upgrades = new EnumUpgrade[] {
                EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.SIGHT, EnumUpgrade.POISON
        };
        this.texture = EntitySnowTurret.TEXTURE;
    }

    @Override
    public Item getAmmoItem() { return Items.SNOWBALL; }

    @Override
    public boolean isArrowBased() { return false; }
    @Override
    public double getDisplayDamageOverride() { return 0.0; }
    @Override
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.singletonList("utilitymobs.effect.knockback"); }

    @Override
    protected Item getDropItem() {
        // 1.12.2 Blocks.SNOW is the full snow block (Blocks.SNOW_LAYER was the layer).
        return Items.SNOW_BLOCK;
    }

    /// Executes this ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            Snowball snowball = this.atMuzzle(new Snowball(this.level(), this), target);
            this.targetHelper.setOwned(snowball);
            this.upgrade.applyTo(snowball);
            double dX = target.getX() - snowball.getX();
            // Aim at centre mass so point-blank / hugging mobs are hit (see EntityTurretGolem).
            double dY = (target.getBoundingBox().minY + target.getBbHeight() * 0.5F) - snowball.getY();
            double dZ = target.getZ() - snowball.getZ();
            double horizontal = Math.sqrt(dX * dX + dZ * dZ);
            snowball.shoot(dX, dY + horizontal * 0.15, dZ, 1.6F, this.inaccuracyAt(horizontal));
            this.level().addFreshEntity(snowball);
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
