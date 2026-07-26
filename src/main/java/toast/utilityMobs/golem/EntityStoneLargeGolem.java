package toast.utilityMobs.golem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityStoneLargeGolem extends EntityLargeGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/stonelargegolem.png");

    public EntityStoneLargeGolem(EntityType<? extends EntityStoneLargeGolem> type, Level level) {
        super(type, level);
        this.texture = EntityStoneLargeGolem.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityLargeGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    /// Returns the armor of this entity.
    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 2);
    }

    @Override
    protected Item getDropItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (this.random.nextInt(2) == 0) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
