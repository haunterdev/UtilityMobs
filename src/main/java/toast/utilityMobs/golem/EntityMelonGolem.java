package toast.utilityMobs.golem;

import java.util.List;

import net.minecraft.block.SoundType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIFollowEntity;

public class EntityMelonGolem extends EntityStackGolem
{
    // The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/melongolem.png");

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.WOOD;
    }


    public EntityMelonGolem(World world) {
        super(world);
        this.texture = EntityMelonGolem.TEXTURE;
        // Equip the pumpkin head here too so the guide book preview (which skips onInitialSpawn) shows it.
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        this.tasks.addTask(1, this.sitAI);
        this.sitAI.setMutexBits(7);
        this.tasks.addTask(2, new EntityAIFollowEntity(this, EntityGolem.class, 1.0, 4.0F, 32.0F));
        this.tasks.addTask(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(4, new EntityAILookIdle(this));
    }

    // Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
        data = super.onInitialSpawn(this.world.getDifficultyForLocation(this.getPosition()), data);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        return data;
    }

    // Called each tick while this entity is alive.
    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote && this.golemAttackTime <= 0) {
            this.golemAttackTime = 80;
            float healRange = 16.0F;
            List<EntityGolem> nearbyGolems = this.world.getEntitiesWithinAABB(EntityGolem.class, this.getEntityBoundingBox().grow(healRange, healRange, healRange));
            for (EntityGolem golem : nearbyGolems) {
                if (!(golem instanceof EntityMelonGolem) && golem.getHealth() < golem.getMaxHealth() && this.getDistanceSq(golem) <= healRange * healRange) {
                    if (golem instanceof EntityUtilityGolem && this.targetHelper.owner != ((EntityUtilityGolem) golem).targetHelper.owner && this.targetHelper.canDamagePlayer(((EntityUtilityGolem) golem).targetHelper.owner)) {
                        continue;
                    }
                    if (this.getEntitySenses().canSee(golem)) {
                        golem.heal(1.0F);
                        if (golem instanceof EntityUtilityGolem) {
                            toast.utilityMobs.network.MessageHealNumber.send(golem, 1.0F);
                        }
                        this.world.playEvent(2005, new BlockPos(MathHelper.floor(golem.posX), MathHelper.floor(golem.posY + golem.getEyeHeight()), MathHelper.floor(golem.posZ)), 0);
                    }
                }
            }
        }
        super.onLivingUpdate();
    }

    @Override
    protected Item getDropItem() {
        return Items.MELON;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.rand.nextInt(16); i-- > 0;) {
            this.dropItem(this.getDropItem(), 1);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (this.canInteract(player)) {
            ItemStack playerHeld = player.getHeldItemMainhand();
            if (player.isSneaking()) {
                this.sitAI.sit = !this.isSitting();
            }
            else if (!playerHeld.isEmpty() && playerHeld.getItem() == Items.MELON && this.getHealth() < this.getMaxHealth()) {
                if (!player.capabilities.isCreativeMode) {
                    playerHeld.shrink(1);
                }
                if (playerHeld.isEmpty()) {
                    player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                }
                this.healAndShowNumber(1.0F);
                return true;
            }
        }
        return super.processInteract(player, hand);
    }

    @Override
    public int getUsePermissions() {
        return TargetHelper.PERMISSION_TARGET;
    }
}
