package toast.utilityMobs.golem;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIFollowEntity;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityScarecrow extends EntityUtilityGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/scarecrow.png");

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.WOOL;
    }

    public EntityScarecrow(EntityType<? extends EntityScarecrow> type, Level level) {
        super(type, level);
        this.texture = EntityScarecrow.TEXTURE;
        // Equip the pumpkin head here too so the guide book preview (which skips finalizeSpawn) shows it.
        this.equipPumpkin();
    }

    private void equipPumpkin() {
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.CARVED_PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new EntityAIFollowEntity(this, Player.class, 1.0, 4.0F, 16.0F));
        this.goalSelector.addGoal(3, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.equipPumpkin();
        return data;
    }

    /// 1.12.2 overrode canTriggerWalking() to false so the scarecrow moves silently. That hook has no
    /// 1.20.1 equivalent (verified against the recompiled Entity class), and its observable effect here
    /// was suppressing step sounds, so the step sound is silenced directly instead.
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Do nothing
    }

    @Override
    protected Item getDropItem() {
        return Items.WHITE_WOOL;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        // Drop sticks rather than the specific (oak) fence, since a scarecrow can now be built from
        // any wooden fence via the fence tag - sticks are the neutral common denominator.
        for (int i = this.random.nextInt(3); i-- > 0;) {
            this.spawnAtLocation(new ItemStack(Items.STICK, 2));
        }
        if (this.random.nextInt(2) == 0) {
            this.spawnAtLocation(this.getDropItem());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.canInteract(player))
            return super.mobInteract(player, hand);
        ItemStack playerHeld = player.getMainHandItem();
        // Never absorb a spawn egg into the hand slot. Otherwise mass-spawning scarecrows in a crowd
        // (clicks landing on an existing scarecrow instead of the ground) makes them eat the egg and
        // drop it again on death - looks like an egg-dupe. Let the click fall through to vanilla so the
        // egg actually spawns its mob.
        if (!playerHeld.isEmpty() && playerHeld.getItem() instanceof SpawnEggItem)
            return super.mobInteract(player, hand);
        if (playerHeld.isEmpty() && this.getEquipmentInSlot(0).isEmpty())
            return super.mobInteract(player, hand);
        if (playerHeld.isEmpty()) {
            this.setEquipment(0, ItemStack.EMPTY);
        }
        else if (player.isShiftKeyDown())
            return super.mobInteract(player, hand);
        else {
            if (!this.level().isClientSide) {
                ItemStack split = playerHeld.copy();
                split.setCount(1);
                this.setEquipment(0, split);
            }
            if (!player.getAbilities().instabuild) {
                playerHeld.shrink(1);
            }
            if (playerHeld.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
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
