package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerLanternGolem extends Container
{
    private final EntityLanternGolem golem;

    public ContainerLanternGolem(InventoryPlayer inventory, EntityLanternGolem lanternGolem) {
        this.golem = lanternGolem;
        this.golem.openInventory(inventory.player);
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlotToContainer(new Slot(lanternGolem, j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
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
            if (slotIndex >= 9) {
                if (!this.mergeItemStack(itemStackInSlot, 0, 9, false))
                    return ItemStack.EMPTY;
            }
            else {
                if (!this.mergeItemStack(itemStackInSlot, 9, 45, false))
                    return ItemStack.EMPTY;
            }

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
