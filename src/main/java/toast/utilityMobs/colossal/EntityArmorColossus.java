package toast.utilityMobs.colossal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemWander;

public class EntityArmorColossus extends EntityColossalGolem
{
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "colossal/armorcolossus.png");

    public EntityArmorColossus(EntityType<? extends EntityArmorColossus> type, Level level) {
        super(type, level);
        this.texture = EntityArmorColossus.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new EntityAIGolemWander(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityColossalGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 18.0);
    }

    @Override
    protected Item getDropItem() {
        return Items.IRON_INGOT;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
        for (int i = this.random.nextInt(25) + 3; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
