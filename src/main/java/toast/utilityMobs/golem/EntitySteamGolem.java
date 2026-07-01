package toast.utilityMobs.golem;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;
import toast.utilityMobs.network.GuiHelper;

public class EntitySteamGolem extends EntityLargeGolem implements IInventory
{
    /// The texture for this class.
    public static final ResourceLocation[] TEXTURES = { new ResourceLocation(_UtilityMobs.TEXTURE + "golem/steamgolem.png"), new ResourceLocation(_UtilityMobs.TEXTURE + "golem/steamgolem_fire.png") };

    /// The number of ticks that the furnace will keep burning.
    public int burnTime = 0;
    /// The number of ticks that a fresh copy of the currently-burning item would burn for.
    public int maxBurnTime = 0;

    /// burningState; While this is 1, the golem will be in its "on" state.
    private static final DataParameter<Byte> BURNING = EntityDataManager.createKey(EntitySteamGolem.class, DataSerializers.BYTE);

    // The contents of this furnace.
    private NonNullList<ItemStack> contents;

    public EntitySteamGolem(World world) {
        super(world);
        this.texture = EntitySteamGolem.TEXTURES[0];
        this.contents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        this.tasks.addTask(1, this.sitAI);
        this.sitAI.setMutexBits(7);
        this.sitAI.sitAnywhere = true;
        this.tasks.addTask(2, new EntityAIWeaponAttack(this, 1.0));
        this.tasks.addTask(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 0.6));
        this.targetTasks.addTask(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0);
    }

    /// Returns the armor of this entity.
    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 2);
    }

    /// Used to initialize data manager variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BURNING, Byte.valueOf((byte)0));
    }

    /// Gets/sets this steam golem's burningState variable. Used for rendering.
    public boolean getBurningState() {
        return this.dataManager.get(BURNING).byteValue() == 1;
    }
    public void setBurningState(boolean state) {
        this.dataManager.set(BURNING, Byte.valueOf(state ? (byte)1 : (byte)0));
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.FURNACE);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (this.rand.nextInt(2) == 0) {
            this.dropItem(this.getDropItem(), 1);
        }
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty()) {
                ItemStack split = stack.copy();
                while (stack.getCount() > 0) {
                    int splitSize = this.rand.nextInt(21) + 10;
                    if (splitSize > stack.getCount()) {
                        splitSize = stack.getCount();
                    }
                    stack.shrink(splitSize);
                    split.setCount(splitSize);
                    this.entityDropItem(split.copy(), 0.0F);
                }
                this.contents.set(i, ItemStack.EMPTY);
            }
        }
    }

    // ---- IInventory implementation ----

    @Override
    public int getSizeInventory() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.contents.get(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return ItemStackHelper.getAndSplit(this.contents, slot, amount);
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        return ItemStackHelper.getAndRemove(this.contents, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack itemStack) {
        this.contents.set(slot, itemStack);
        if (!itemStack.isEmpty() && itemStack.getCount() > this.getInventoryStackLimit()) {
            itemStack.setCount(this.getInventoryStackLimit());
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getCustomNameTag() : "Steam Golem";
    }

    // NOTE: do NOT override hasCustomName() to always-true. It is shared with Entity, and forcing
    // it true makes the hover label render an empty getCustomNameTag() (a small black sliver above
    // the head). Inherit Entity's real value; getName() above still supplies the GUI/inventory title.

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.canInteract(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        // Do nothing
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        // Do nothing
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        return TileEntityFurnace.isItemFuel(itemStack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
        // No fields
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.contents.size(); i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void markDirty() {
        // Do nothing
    }

    // ---- Interaction ----

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (this.canInteract(player) && !player.isSneaking()) {
            if (this.openGUI(player))
                return true;
        }
        return super.processInteract(player, hand);
    }

    /// Opens this golem's GUI.
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            GuiHelper.displayGUICustom(player, this);
        }
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }

    /// Called each tick this entity exists.
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            this.texture = this.getBurningState() ? EntitySteamGolem.TEXTURES[1] : EntitySteamGolem.TEXTURES[0];
        }
    }

    /// Called each tick this entity is alive.
    @Override
    public void onLivingUpdate() {
        if (this.burnTime > 0) {
            this.burnTime--;
        }
        if (!this.world.isRemote) {
            // Burn fuel from whichever slot holds it. We consume in place rather than shuffling fuel
            // into a fixed "burn" slot every tick - that old per-tick swap both made placed fuel jump
            // to the middle slot and mutated the open inventory mid-interaction, which desynced the GUI
            // and could visually dupe the stack. Now fuel stays exactly where the player put it.
            if (this.burnTime == 0) {
                for (int slot = 0; slot < this.contents.size(); slot++) {
                    ItemStack fuel = this.getStackInSlot(slot);
                    int burn = TileEntityFurnace.getItemBurnTime(fuel);
                    if (!fuel.isEmpty() && burn > 0) {
                        this.maxBurnTime = this.burnTime = burn;
                        ItemStack container = fuel.getItem().getContainerItem(fuel);
                        fuel.shrink(1);
                        if (fuel.isEmpty()) {
                            this.setInventorySlotContents(slot, container);
                        }
                        break;
                    }
                }
            }
            boolean burnState = this.burnTime > 0;
            if (this.getBurningState() != burnState) {
                this.setBurningState(burnState);
            }
            this.sitAI.sit = !burnState;
        }
        super.onLivingUpdate();
    }

    /// Saves this entity to NBT.
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagList tagList = new NBTTagList();
        for (int slot = 0; slot < this.contents.size(); slot++) {
            if (!this.contents.get(slot).isEmpty()) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte("Slot", (byte)slot);
                this.contents.get(slot).writeToNBT(slotTag);
                tagList.appendTag(slotTag);
            }
        }
        tag.setTag("Items", tagList);
        tag.setShort("BurnTime", (short)this.burnTime);
        tag.setShort("MaxBurnTime", (short)this.maxBurnTime);
    }

    /// Loads this entity from NBT.
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NBTTagList tagList = tag.getTagList("Items", new NBTTagCompound().getId());
        this.contents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound slotTag = tagList.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.contents.size()) {
                this.contents.set(slot, new ItemStack(slotTag));
            }
        }
        this.burnTime = tag.getShort("BurnTime");
        this.maxBurnTime = tag.getShort("MaxBurnTime");
    }

    // Returns an integer between 0 and the passed value representing how much burn time is left on the current fuel.
    @SideOnly(Side.CLIENT)
    public int getBurnTimeRemainingScaled(int max) {
        if (this.maxBurnTime == 0) {
            this.maxBurnTime = 200;
        }
        return this.burnTime * max / this.maxBurnTime;
    }
}
