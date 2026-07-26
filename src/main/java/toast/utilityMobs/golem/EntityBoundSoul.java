package toast.utilityMobs.golem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIFollowEntity;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityBoundSoul extends EntityUtilityGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/boundsoul.png");

    public EntityBoundSoul(EntityType<? extends EntityBoundSoul> type, Level level) {
        super(type, level);
        this.texture = EntityBoundSoul.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new EntityAIFollowEntity(this, Player.class, 1.0, 10.0F, 16.0F));
        this.goalSelector.addGoal(3, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.SAND;
    }

    @Override
    protected Item getDropItem() {
        return Items.SOUL_SAND;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.random.nextInt(3); i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.canInteract(player))
            return super.mobInteract(player, hand);
        ItemStack playerHeld = player.getMainHandItem();
        if (playerHeld.isEmpty()) {
            if (!this.level().isClientSide) {
                if (!this.setEquipment(ItemStack.EMPTY))
                    return super.mobInteract(player, hand);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player.isShiftKeyDown())
            return super.mobInteract(player, hand);
        FoodProperties food = playerHeld.getFoodProperties(this);
        if (food != null) {
            this.healAndShowNumber(food.getNutrition());
        }
        else if (!this.level().isClientSide) {
            ItemStack split = playerHeld.copy();
            split.setCount(1);
            this.setEquipment(split);
        }
        if (!player.getAbilities().instabuild) {
            playerHeld.shrink(1);
        }
        if (playerHeld.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean isWeaponDamageOnly() {
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }
}
