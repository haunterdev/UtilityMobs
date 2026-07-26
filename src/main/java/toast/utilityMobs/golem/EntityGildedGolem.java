package toast.utilityMobs.golem;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import toast.utilityMobs.EffectHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityGildedGolem extends EntityUtilityGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/gildedgolem.png");

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.METAL;
    }

    public EntityGildedGolem(EntityType<? extends EntityGildedGolem> type, Level level) {
        super(type, level);
        this.texture = EntityGildedGolem.TEXTURE;
        // Equip the fixed gear here too so the guide book preview (which skips finalizeSpawn) renders it;
        // the bare body texture is mostly empty without the golden armor on top.
        this.equipFixedGear();
    }

    private void equipFixedGear() {
        this.setCurrentItemOrArmor(0, new ItemStack(Items.GOLDEN_SWORD));
        this.setCurrentItemOrArmor(4, new ItemStack(Items.GOLDEN_HELMET));
        this.setCurrentItemOrArmor(3, new ItemStack(Items.GOLDEN_CHESTPLATE));
        this.setCurrentItemOrArmor(2, new ItemStack(Items.GOLDEN_LEGGINGS));
        this.setCurrentItemOrArmor(1, new ItemStack(Items.GOLDEN_BOOTS));
        for (int i = 5; i-- > 0;) {
            this.setEquipDropChance(i, 0.0F);
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.equipFixedGear();
        for (int i = 5; i-- > 0;) {
            EffectHelper.enchantItem(this.getEquipmentInSlot(i), Enchantments.THORNS, 1);
        }
        return data;
    }

    @Override
    protected Item getDropItem() {
        return Items.GOLD_INGOT;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.random.nextInt(3) + 3; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }

    @Override
    public void hitEffects(Entity entity) {
        this.level().addFreshEntity(new ExperienceOrb(this.level(), entity.getX(), entity.getY(), entity.getZ(), 1));
    }
}
