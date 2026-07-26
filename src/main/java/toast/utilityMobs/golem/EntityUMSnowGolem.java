package toast.utilityMobs.golem;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.ForgeEventFactory;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityUMSnowGolem extends EntityStackGolem
{
    /// The texture for this class. Reuses the vanilla snow golem skin.
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/snow_golem.png");

    public EntityUMSnowGolem(EntityType<? extends EntityUMSnowGolem> type, Level level) {
        super(type, level);
        this.texture = EntityUMSnowGolem.TEXTURE;
        // Equip the pumpkin head here (not just in finalizeSpawn) so it shows in the guide book's
        // preview render, which never calls finalizeSpawn. Loaded golems re-read their real gear from NBT.
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.CARVED_PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityStackGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.CARVED_PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        this.setCurrentItemOrArmor(0, new ItemStack(Items.SNOWBALL));
        this.setEquipDropChance(0, 0.0F);
        return data;
    }

    // Called each tick while this entity is alive.
    @Override
    public void aiStep() {
        super.aiStep();
        BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));

        if (this.isInWaterOrRain()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }
        if (this.level().getBiome(pos).value().getBaseTemperature() > 1.0F) {
            this.hurt(this.damageSources().onFire(), 1.0F);
        }

        // Server-side only, and only when mob griefing is allowed for THIS entity. Running this client-side
        // painted ghost snow layers the server never placed (which lingered until a block update), so it
        // looked like the golem ignored the gamerule. We ask Forge's per-entity mob-griefing event rather
        // than the raw gamerule, so mods that gate griefing per mob can stop the snow trail - the raw
        // gamerule ignored their per-entity override.
        boolean mobGriefing = !this.level().isClientSide && ForgeEventFactory.getMobGriefingEvent(this.level(), this);
        for (int l = 0; mobGriefing && l < 4; l++) {
            int blockX = Mth.floor(this.getX() + (l % 2 * 2 - 1) * 0.25F);
            int blockY = Mth.floor(this.getY());
            int blockZ = Mth.floor(this.getZ() + (l / 2 % 2 * 2 - 1) * 0.25F);
            BlockPos trail = new BlockPos(blockX, blockY, blockZ);
            if (this.level().getBlockState(trail).isAir()
                    && this.level().getBiome(trail).value().getBaseTemperature() < 0.8F
                    && Blocks.SNOW.defaultBlockState().canSurvive(this.level(), trail)) {
                this.level().setBlockAndUpdate(trail, Blocks.SNOW.defaultBlockState());
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SNOW_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SNOW_GOLEM_DEATH;
    }

    @Override
    protected Item getDropItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.random.nextInt(16); i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
