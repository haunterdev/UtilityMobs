package toast.utilityMobs.golem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityUMIronGolem extends EntityLargeGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: iron blocks.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.METAL;
    }

    // The texture for this class. Reuses the vanilla iron golem skin.
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/iron_golem/iron_golem.png");

    public EntityUMIronGolem(EntityType<? extends EntityUMIronGolem> type, Level level) {
        super(type, level);
        this.texture = EntityUMIronGolem.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new EntityAIGolemWander(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    // Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityLargeGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.ATTACK_DAMAGE, 17.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected Item getDropItem() {
        return Items.IRON_INGOT;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.random.nextInt(3); i-- > 0;) {
            this.spawnAtLocation(Items.POPPY);
        }
        for (int i = this.random.nextInt(3) + 3; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
