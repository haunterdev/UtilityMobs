package toast.utilityMobs.golem;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerSteamGolem extends Container
{
    private final EntitySteamGolem golem;
    public int lastBurnTime = 0;
    public int lastItemBurnTime = 0;

    public ContainerSteamGolem(InventoryPlayer inventory, EntitySteamGolem steamGolem) {
        this.golem = steamGolem;
        this.golem.openInventory(inventory.player);
        int i;
        for (i = 0; i < 3; i++) {
            // Fuel-only slots: reject anything that isn't furnace fuel on manual placement (mergeItemStack
            // / hoppers already respect this via isItemValid). Keeps non-fuel out of the golem entirely.
            this.addSlotToContainer(new Slot(steamGolem, i, 62 + i * 18, 44) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return TileEntityFurnace.isItemFuel(stack);
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    /// Callback for when the crafting gui is closed.
    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.golem.closeInventory(player);
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 1, this.golem.burnTime);
        listener.sendWindowProperty(this, 2, this.golem.maxBurnTime);
    }

    /**
     * Looks for changes made in the container, sends them to every listener.
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < this.listeners.size(); i++) {
            IContainerListener listener = this.listeners.get(i);
            if (this.lastBurnTime != this.golem.burnTime) {
                listener.sendWindowProperty(this, 1, this.golem.burnTime);
            }
            if (this.lastItemBurnTime != this.golem.maxBurnTime) {
                listener.sendWindowProperty(this, 2, this.golem.maxBurnTime);
            }
        }
        this.lastBurnTime = this.golem.burnTime;
        this.lastItemBurnTime = this.golem.maxBurnTime;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 1) {
            this.golem.burnTime = value;
        }
        if (id == 2) {
            this.golem.maxBurnTime = value;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.golem.isUsableByPlayer(player);
    }

    /// Called when a player shift-clicks on a slot.
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemStackInSlot = slot.getStack();
            itemStack = itemStackInSlot.copy();
            if (slotIndex >= 3) {
                if (TileEntityFurnace.isItemFuel(itemStackInSlot)) {
                    // Merge from player inventory to golem inventory.
                    if (!this.mergeItemStack(itemStackInSlot, 0, 3, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex >= 3 && slotIndex < 30) {
                    // Merge from main inventory to hotbar.
                    if (!this.mergeItemStack(itemStackInSlot, 30, 39, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex >= 30 && slotIndex < 39) {
                    // Merge from hotbar to main inventory.
                    if (!this.mergeItemStack(itemStackInSlot, 3, 30, false))
                        return ItemStack.EMPTY;
                }
            }
            else {
                // Merge from golem inventory to player inventory.
                if (!this.mergeItemStack(itemStackInSlot, 3, 39, false))
                    return ItemStack.EMPTY;
            }

            if (itemStackInSlot.getCount() == 0) {
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
