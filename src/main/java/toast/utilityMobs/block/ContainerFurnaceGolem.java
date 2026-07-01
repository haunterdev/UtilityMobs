package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerFurnaceGolem extends Container
{
    private final EntityFurnaceGolem golem;
    public int lastCookTime = 0;
    public int lastBurnTime = 0;
    public int lastItemBurnTime = 0;

    public ContainerFurnaceGolem(InventoryPlayer inventory, EntityFurnaceGolem furnace) {
        this.golem = furnace;
        this.golem.openInventory(inventory.player);
        this.addSlotToContainer(new Slot(furnace, 0, 56, 17));
        this.addSlotToContainer(new Slot(furnace, 1, 56, 53));
        this.addSlotToContainer(new SlotFurnaceOutput(inventory.player, furnace, 2, 116, 35));
        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.golem.closeInventory(player);
    }

    // The client opens the VANILLA GuiFurnace (the GUI was opened with the "minecraft:furnace" window id),
    // so the window-property ids MUST match what GuiFurnace reads from a vanilla furnace's fields:
    //   0 = burn time (flame), 1 = current item burn time (flame total),
    //   2 = cook time (arrow),  3 = total cook time (arrow total, always 200).
    // The old code sent cook/burn/itemBurn as ids 0/1/2 and never sent id 3, so the arrow divided by a
    // zero total-cook-time and never moved.
    private static final int TOTAL_COOK_TIME = 200;

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, this.golem.burnTime);
        listener.sendWindowProperty(this, 1, this.golem.itemBurnTime);
        listener.sendWindowProperty(this, 2, this.golem.cookTime);
        listener.sendWindowProperty(this, 3, TOTAL_COOK_TIME);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < this.listeners.size(); i++) {
            IContainerListener listener = this.listeners.get(i);
            if (this.lastBurnTime != this.golem.burnTime) {
                listener.sendWindowProperty(this, 0, this.golem.burnTime);
            }
            if (this.lastItemBurnTime != this.golem.itemBurnTime) {
                listener.sendWindowProperty(this, 1, this.golem.itemBurnTime);
            }
            if (this.lastCookTime != this.golem.cookTime) {
                listener.sendWindowProperty(this, 2, this.golem.cookTime);
            }
        }
        this.lastCookTime = this.golem.cookTime;
        this.lastBurnTime = this.golem.burnTime;
        this.lastItemBurnTime = this.golem.itemBurnTime;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) {
            this.golem.burnTime = value;
        }
        if (id == 1) {
            this.golem.itemBurnTime = value;
        }
        if (id == 2) {
            this.golem.cookTime = value;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.golem.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemStackInSlot = slot.getStack();
            itemStack = itemStackInSlot.copy();
            if (slotIndex == 2) {
                if (!this.mergeItemStack(itemStackInSlot, 3, 39, true))
                    return ItemStack.EMPTY;
                slot.onSlotChange(itemStackInSlot, itemStack);
            }
            else if (slotIndex != 1 && slotIndex != 0) {
                if (!FurnaceRecipes.instance().getSmeltingResult(itemStackInSlot).isEmpty()) {
                    if (!this.mergeItemStack(itemStackInSlot, 0, 1, false))
                        return ItemStack.EMPTY;
                }
                else if (TileEntityFurnace.isItemFuel(itemStackInSlot)) {
                    if (!this.mergeItemStack(itemStackInSlot, 1, 2, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex >= 3 && slotIndex < 30) {
                    if (!this.mergeItemStack(itemStackInSlot, 30, 39, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex >= 30 && slotIndex < 39 && !this.mergeItemStack(itemStackInSlot, 3, 30, false))
                    return ItemStack.EMPTY;
            }
            else if (!this.mergeItemStack(itemStackInSlot, 3, 39, false))
                return ItemStack.EMPTY;

            if (itemStackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            }
            else {
                slot.onSlotChanged();
            }

            if (itemStackInSlot.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, itemStackInSlot);
        }
        return itemStack;
    }
}
