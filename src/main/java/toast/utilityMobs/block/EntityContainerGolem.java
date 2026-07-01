package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public abstract class EntityContainerGolem extends EntityBlockGolem implements IInventory
{
    // numUsingPlayers; The number of players using this chest golem.
    private static final DataParameter<Byte> USING = EntityDataManager.createKey(EntityContainerGolem.class, DataSerializers.BYTE);

    // The contents of this chest.
    private NonNullList<ItemStack> contents;

    public EntityContainerGolem(World world) {
        super(world);
        this.contents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
    }

    // Used to initialize data manager variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(USING, Byte.valueOf((byte)0));
    }

    // Functions for numUsingPlayers.
    public void incNumUsingPlayers() {
        this.dataManager.set(USING, Byte.valueOf((byte)(this.dataManager.get(USING).byteValue() + 1)));
    }
    public void decNumUsingPlayers() {
        this.dataManager.set(USING, Byte.valueOf((byte)(this.dataManager.get(USING).byteValue() - 1)));
    }
    public boolean isOpen() {
        return this.dataManager.get(USING).byteValue() > 0;
    }

    // Called when this block golem is told to get up.
    @Override
    public void setClosed() {
        this.dataManager.set(USING, Byte.valueOf((byte)0));
    }

    @Override
    public int getSizeInventory() {
        return 27;
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

    // Returns the name of the inventory (also the entity's name).
    @Override
    public String getName() {
        return this.hasCustomName() ? this.getCustomNameTag() : "Chest Golem";
    }

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
        this.incNumUsingPlayers();
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        this.decNumUsingPlayers();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        return true;
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

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
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

    // Opens this block golem's GUI.
    @Override
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            player.displayGUIChest(this);
        }
        return true;
    }

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
    }

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
    }

    // Steals the contents of the NBT given.
    public void takeContentsFromNBT(NBTTagCompound tag) {
        NBTTagList tagList = tag.getTagList("Items", new NBTTagCompound().getId());
        this.contents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound slotTag = tagList.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.contents.size()) {
                this.contents.set(slot, new ItemStack(slotTag));
            }
        }
        tag.setTag("Items", new NBTTagList());
        if (tag.hasKey("CustomName")) {
            this.setCustomNameTag(tag.getString("CustomName"));
        }
        tag.removeTag("CustomName");
    }
}
