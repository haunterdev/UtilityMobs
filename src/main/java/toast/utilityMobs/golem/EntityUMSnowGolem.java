package toast.utilityMobs.golem;

import net.minecraft.block.material.Material;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityUMSnowGolem extends EntityStackGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/snowman.png");

    public EntityUMSnowGolem(World world) {
        super(world);
        this.texture = EntityUMSnowGolem.TEXTURE;
        // Equip the pumpkin head here (not just in onInitialSpawn) so it shows in the guide book's
        // preview render, which never calls onInitialSpawn. Loaded golems re-read their real gear from NBT.
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        this.tasks.addTask(1, new EntityAIWeaponAttack(this, 1.0));
        this.tasks.addTask(2, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(4, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
        data = super.onInitialSpawn(this.world.getDifficultyForLocation(this.getPosition()), data);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        this.setCurrentItemOrArmor(0, new ItemStack(Items.SNOWBALL));
        this.setEquipDropChance(0, 0.0F);
        return data;
    }

    // Called each tick while this entity is alive.
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        int blockX = MathHelper.floor(this.posX);
        int blockY = MathHelper.floor(this.posY);
        int blockZ = MathHelper.floor(this.posZ);
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);

        if (this.isWet()) {
            this.attackEntityFrom(DamageSource.DROWN, 1.0F);
        }
        if (this.world.getBiome(pos).getTemperature(pos) > 1.0F) {
            this.attackEntityFrom(DamageSource.ON_FIRE, 1.0F);
        }

        // Server-side only, and only when mobGriefing is on. Running this client-side painted ghost snow
        // layers the server never placed (which lingered until a block update), so it looked like the
        // golem ignored the gamerule. Vanilla snowman places snow server-side under the same rule.
        boolean mobGriefing = !this.world.isRemote && this.world.getGameRules().getBoolean("mobGriefing");
        for (int l = 0; mobGriefing && l < 4; l++) {
            blockX = MathHelper.floor(this.posX + (l % 2 * 2 - 1) * 0.25F);
            blockY = MathHelper.floor(this.posY);
            blockZ = MathHelper.floor(this.posZ + (l / 2 % 2 * 2 - 1) * 0.25F);
            pos = new BlockPos(blockX, blockY, blockZ);
            if (this.world.getBlockState(pos).getMaterial() == Material.AIR && this.world.getBiome(pos).getTemperature(pos) < 0.8F && Blocks.SNOW_LAYER.canPlaceBlockAt(this.world, pos)) {
                this.world.setBlockState(pos, Blocks.SNOW_LAYER.getDefaultState());
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_SNOWMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SNOWMAN_DEATH;
    }

    @Override
    protected Item getDropItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.rand.nextInt(16); i-- > 0;) {
            this.dropItem(this.getDropItem(), 1);
        }
    }
}
