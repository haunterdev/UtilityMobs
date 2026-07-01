package toast.utilityMobs.golem;

import net.minecraft.block.SoundType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIFollowEntity;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityScarecrow extends EntityUtilityGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/scarecrow.png");

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.CLOTH;
    }


    public EntityScarecrow(World world) {
        super(world);
        this.texture = EntityScarecrow.TEXTURE;
        // Equip the pumpkin head here too so the guide book preview (which skips onInitialSpawn) shows it.
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        this.tasks.addTask(1, new EntityAIWeaponAttack(this, 1.0));
        this.tasks.addTask(2, new EntityAIFollowEntity(this, EntityPlayer.class, 1.0, 4.0F, 16.0F));
        this.tasks.addTask(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(4, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
        data = super.onInitialSpawn(this.world.getDifficultyForLocation(this.getPosition()), data);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        return data;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.WOOL);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        // Drop sticks rather than the specific (oak) fence, since a scarecrow can now be built from
        // any wooden fence via the fenceWood ore dict - sticks are the neutral common denominator.
        for (int i = this.rand.nextInt(3); i-- > 0;) {
            this.dropItem(Items.STICK, 2);
        }
        if (this.rand.nextInt(2) == 0) {
            this.dropItem(this.getDropItem(), 1);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!this.canInteract(player))
            return super.processInteract(player, hand);
        ItemStack playerHeld = player.getHeldItemMainhand();
        // Never absorb a spawn egg into the hand slot. Otherwise mass-spawning scarecrows in a crowd
        // (clicks landing on an existing scarecrow instead of the ground) makes them eat the egg and
        // drop it again on death - looks like an egg-dupe. Let the click fall through to vanilla so the
        // egg actually spawns its mob.
        if (!playerHeld.isEmpty() && playerHeld.getItem() instanceof ItemMonsterPlacer)
            return super.processInteract(player, hand);
        if (playerHeld.isEmpty() && this.getEquipmentInSlot(0).isEmpty())
            return super.processInteract(player, hand);
        if (playerHeld.isEmpty()) {
            this.setEquipment(0, ItemStack.EMPTY);
        }
        else if (player.isSneaking())
            return super.processInteract(player, hand);
        else {
            ItemStack heldItem = this.getEquipmentInSlot(0);
            if (!this.world.isRemote) {
                ItemStack split = playerHeld.copy();
                split.setCount(1);
                this.setEquipment(0, split);
            }
            if (!player.capabilities.isCreativeMode) {
                playerHeld.shrink(1);
            }
            if (playerHeld.isEmpty()) {
                player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
        return true;
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
