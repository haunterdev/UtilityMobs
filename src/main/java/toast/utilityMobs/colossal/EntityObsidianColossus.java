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

public class EntityObsidianColossus extends EntityColossalGolem
{
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "colossal/obsidiancolossus.png");

    public EntityObsidianColossus(EntityType<? extends EntityObsidianColossus> type, Level level) {
        super(type, level);
        this.texture = EntityObsidianColossus.TEXTURE;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new EntityAIGolemWander(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // Returns the armor of this entity.
    @Override
    public int getArmorValue() {
        return 20;
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityColossalGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 500.0);
    }

    @Override
    protected Item getDropItem() {
        return Items.OBSIDIAN;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
        for (int i = this.random.nextInt(3) + 1; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
