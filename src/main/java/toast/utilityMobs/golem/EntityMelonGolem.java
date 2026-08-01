package toast.utilityMobs.golem;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIFollowEntity;
import toast.utilityMobs.ai.EntityAIGolemWander;

public class EntityMelonGolem extends EntityStackGolem
{
    // The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/melongolem.png");

    // How near (blocks) a melon golem must be to heal another golem. Cached from golems.melon_heal_range.
    public static float healRange = 16.0F;

    // The follow AI, kept so its park distance can track healRange - otherwise a low heal range would leave
    // the melon golem parked (at the default follow distance) too far away to ever heal (issue: never
    // pathfinds close enough at small ranges).
    private EntityAIFollowEntity followAI;

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.WOOD;
    }

    // No footsteps, matching the vanilla snow golem it is modelled on (1.12.2 issue #11). The melon-wood
    // step sound was loud and constant for something this small.
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    public EntityMelonGolem(EntityType<? extends EntityMelonGolem> type, Level level) {
        super(type, level);
        this.texture = EntityMelonGolem.TEXTURE;
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
        this.goalSelector.addGoal(1, this.sitAI());
        // 1.12.2 setMutexBits(7) = MOVE | LOOK | JUMP.
        this.sitAI().setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        this.followAI = new EntityAIFollowEntity(this, AbstractGolem.class, 1.0, 4.0F, 32.0F);
        // Follow whoever actually needs healing, nearest first. Without this the melon golem takes the
        // first golem the entity list returns, which is why a hurt golem beside it could be ignored in
        // favour of a healthy one across the field. Differs from 1.12.2, which had no preference.
        this.followAI.setPreferred(candidate -> !(candidate instanceof EntityMelonGolem)
            && candidate.getHealth() < candidate.getMaxHealth());
        this.goalSelector.addGoal(2, this.followAI);
        this.goalSelector.addGoal(3, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    // Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityStackGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.equipPumpkin();
        return data;
    }

    // Called each tick while this entity is alive.
    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.golemAttackTime <= 0) {
            this.golemAttackTime = 80;
            float healRange = EntityMelonGolem.healRange;
            // Park no farther than we can heal from (default 4 keeps prior behavior at heal range >= 4); at
            // small heal ranges this lets the golem walk right up so it can actually reach and heal.
            if (this.followAI != null) {
                this.followAI.setRangeMin(Math.min(4.0F, healRange));
            }
            List<AbstractGolem> nearbyGolems = this.level().getEntitiesOfClass(AbstractGolem.class, this.getBoundingBox().inflate(healRange, healRange, healRange));
            for (AbstractGolem golem : nearbyGolems) {
                if (!(golem instanceof EntityMelonGolem) && golem.getHealth() < golem.getMaxHealth() && this.distanceToSqr(golem) <= healRange * healRange) {
                    if (golem instanceof EntityUtilityGolem && this.targetHelper.owner != ((EntityUtilityGolem)golem).targetHelper.owner && this.targetHelper.canDamagePlayer(((EntityUtilityGolem)golem).targetHelper.owner)) {
                        continue;
                    }
                    if (this.getSensing().hasLineOfSight(golem)) {
                        golem.heal(1.0F);
                        toast.utilityMobs.network.MessageHealNumber.send(golem, 1.0F);
                        this.level().levelEvent(2005, new BlockPos(Mth.floor(golem.getX()), Mth.floor(golem.getY() + golem.getEyeHeight()), Mth.floor(golem.getZ())), 0);
                    }
                }
            }
        }
        super.aiStep();
    }

    @Override
    protected Item getDropItem() {
        return Items.MELON_SLICE;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.random.nextInt(16); i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.canInteract(player)) {
            ItemStack playerHeld = player.getMainHandItem();
            if (player.isShiftKeyDown()) {
                this.sitAI().sit = !this.isSitting();
            }
            else if (!playerHeld.isEmpty() && playerHeld.getItem() == Items.MELON_SLICE && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    playerHeld.shrink(1);
                }
                if (playerHeld.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                this.healAndShowNumber(1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public int getUsePermissions() {
        return TargetHelper.PERMISSION_TARGET;
    }
}
