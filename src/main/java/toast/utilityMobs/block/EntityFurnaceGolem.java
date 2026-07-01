package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;

public class EntityFurnaceGolem extends EntityContainerGolem implements ISidedInventory
{
    /// The textures for this class.
    public static final ResourceLocation[] TEXTURES = { new ResourceLocation(_UtilityMobs.TEXTURE + "block/furnacegolem.png"), new ResourceLocation(_UtilityMobs.TEXTURE + "block/furnacegolem_fire.png") };

    /// burningState; While this is 1, the furnace will be in its "on" state.
    private static final DataParameter<Byte> BURNING = EntityDataManager.createKey(EntityFurnaceGolem.class, DataSerializers.BYTE);

    /// The number of ticks that the furnace will keep burning.
    public int burnTime = 0;
    /// The number of ticks that a fresh copy of the currently-burning item would burn for.
    public int itemBurnTime = 0;
    /// The number of ticks that the current item has been cooking for.
    public int cookTime = 0;

    public EntityFurnaceGolem(World world) {
        super(world);
        this.texture = EntityFurnaceGolem.TEXTURES[0];
        this.isImmuneToFire = true;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 2);
    }

    /// Used to initialize dataWatcher variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BURNING, Byte.valueOf((byte)0));
    }

    /// Returns true if this mob is on fire. Used for rendering.
    @Override
    public boolean isBurning() {
        return this.getBurningState();
    }

    /// Gets/sets this lava monster's burningState variable. Used for rendering.
    public boolean getBurningState() {
        return this.dataManager.get(BURNING).byteValue() == 1;
    }
    public void setBurningState(boolean state) {
        this.dataManager.set(BURNING, Byte.valueOf(state ? (byte)1 : (byte)0));
    }

    /// Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        return slot == 2 ? false : slot == 1 ? TileEntityFurnace.isItemFuel(itemStack) : true;
    }

    /// Returns an array containing the indices of the slots that can be accessed by automation on the given side of this block.
    @Override /// ISidedInventory
    public int[] getSlotsForFace(net.minecraft.util.EnumFacing side) {
        return side == net.minecraft.util.EnumFacing.DOWN ? new int[] { 2, 1 } : side == net.minecraft.util.EnumFacing.UP ? new int[] { 0 } : new int[] { 1 };
    }

    /// Returns true if automation can insert the given item in the given slot from the given side.
    @Override /// ISidedInventory
    public boolean canInsertItem(int slot, ItemStack itemStack, net.minecraft.util.EnumFacing side) {
        return this.isItemValidForSlot(slot, itemStack);
    }

    /// Returns true if automation can extract the given item in the given slot from the given side.
    @Override /// ISidedInventory
    public boolean canExtractItem(int slot, ItemStack itemStack, net.minecraft.util.EnumFacing side) {
        return side != net.minecraft.util.EnumFacing.DOWN || slot != 1 || itemStack.getItem() == Items.BUCKET;
    }

    /// Returns the number of slots in the inventory.
    @Override
    public int getSizeInventory() {
        return 3;
    }

    /// Returns the name of the inventory.
    @Override
    public String getName() {
        return this.hasCustomName() ? this.getCustomNameTag() : "Furnace Golem";
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.FURNACE);
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            GuiHelper.displayGUIFurnace(player, this);
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
            this.texture = this.getBurningState() ? EntityFurnaceGolem.TEXTURES[1] : EntityFurnaceGolem.TEXTURES[0];
        }
    }

    /// Called each tick this entity is alive.
    @Override
    public void onLivingUpdate() {
        if (this.burnTime > 0) {
            this.burnTime--;
        }
        if (!this.world.isRemote) {
            if (this.burnTime == 0 && this.canSmelt()) {
                ItemStack fuelStack = this.getStackInSlot(1);
                this.itemBurnTime = this.burnTime = TileEntityFurnace.getItemBurnTime(fuelStack);
                if (this.burnTime > 0 && !fuelStack.isEmpty()) {
                    Item fuelItem = fuelStack.getItem();
                    fuelStack.shrink(1);
                    if (fuelStack.isEmpty()) {
                        this.setInventorySlotContents(1, fuelItem.getContainerItem(fuelStack));
                    }
                }
            }
            if (this.getBurningState() && this.canSmelt()) {
                this.cookTime++;
                if (this.cookTime == 200) {
                    this.cookTime = 0;
                    this.smeltItem();
                }
            }
            else {
                this.cookTime = 0;
            }
            boolean burnState = this.burnTime > 0;
            if (this.getBurningState() != burnState) {
                this.setBurningState(burnState);
            }
        }
        super.onLivingUpdate();
    }

    /// Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
    public boolean canSmelt() {
        if (this.getStackInSlot(0).isEmpty())
            return false;
        ItemStack itemStack = FurnaceRecipes.instance().getSmeltingResult(this.getStackInSlot(0));
        if (itemStack.isEmpty())
            return false;
        if (this.getStackInSlot(2).isEmpty())
            return true;
        if (!this.getStackInSlot(2).isItemEqual(itemStack))
            return false;
        int result = this.getStackInSlot(2).getCount() + itemStack.getCount();
        return result <= this.getInventoryStackLimit() && result <= itemStack.getMaxStackSize();
    }

    /// Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack.
    public void smeltItem() {
        if (this.canSmelt()) {
            ItemStack itemStack = FurnaceRecipes.instance().getSmeltingResult(this.getStackInSlot(0));
            if (this.getStackInSlot(2).isEmpty()) {
                this.setInventorySlotContents(2, itemStack.copy());
            }
            else if (this.getStackInSlot(2).isItemEqual(itemStack)) {
                this.getStackInSlot(2).grow(itemStack.getCount());
            }
            this.getStackInSlot(0).shrink(1);
            if (this.getStackInSlot(0).isEmpty()) {
                this.setInventorySlotContents(0, ItemStack.EMPTY);
            }
        }
    }

    /// Saves this entity to NBT.
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setShort("BurnTime", (short)this.burnTime);
        tag.setShort("CookTime", (short)this.cookTime);
    }

    /// Loads this entity from NBT.
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.burnTime = tag.getShort("BurnTime");
        this.cookTime = tag.getShort("CookTime");
        this.itemBurnTime = TileEntityFurnace.getItemBurnTime(this.getStackInSlot(1));
    }

    /// Loads this entity from NBT.
    @Override
    public void takeContentsFromNBT(NBTTagCompound tag) {
        super.takeContentsFromNBT(tag);
        this.burnTime = tag.getShort("BurnTime");
        this.cookTime = tag.getShort("CookTime");
        this.itemBurnTime = TileEntityFurnace.getItemBurnTime(this.getStackInSlot(1));
        tag.setShort("BurnTime", (short)0);
        tag.setShort("CookTime", (short)0);
    }
}
