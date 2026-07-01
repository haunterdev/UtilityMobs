package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class ContainerAnvilGolem extends ContainerRepair
{
    // The golem being crafted on.
    public final EntityAnvilGolem golem;
    // The two slots where you put the items in that you want to merge and/or rename.
    public IInventory inputSlots;
    // Determined by damage of input item and stackSize of repair materials.
    public int stackSizeToBeUsedInRepair;

    public ContainerAnvilGolem(InventoryPlayer inventory, EntityAnvilGolem anvil, EntityPlayer player) {
        super(inventory, anvil.world, new BlockPos(-1, -1, -1), player);
        this.golem = anvil;
        this.golem.openInventory(player);
        this.inputSlots = this.inventorySlots.get(0).inventory;
        Slot oldSlot = this.inventorySlots.get(2);
        Slot newSlot = new ContainerAnvilGolemSlot(this, oldSlot.inventory, oldSlot.getSlotIndex(), oldSlot.xPos, oldSlot.yPos);
        newSlot.slotNumber = oldSlot.slotNumber;
        this.inventorySlots.set(2, newSlot);
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

    // Called when the Anvil Input Slot changes; recomputes the result and the repair material count.
    @Override
    public void updateRepairOutput() {
        super.updateRepairOutput();
        // Update the needlessly private field, stackSizeToBeUsedInRepair.
        ItemStack itemStack = this.inputSlots.getStackInSlot(0);
        if (!itemStack.isEmpty()) {
            ItemStack itemStackTmp = itemStack.copy();
            ItemStack itemStackToBeUsed = this.inputSlots.getStackInSlot(1);
            this.stackSizeToBeUsedInRepair = 0;
            if (!itemStackToBeUsed.isEmpty()) {
                if (itemStackTmp.isItemStackDamageable() && itemStackTmp.getItem().getIsRepairable(itemStack, itemStackToBeUsed)) {
                    int itemDamage = Math.min(itemStackTmp.getItemDamage(), itemStackTmp.getMaxDamage() / 4);
                    if (itemDamage <= 0)
                        return;
                    int stackSizeToBeUsed;
                    for (stackSizeToBeUsed = 0; itemDamage > 0 && stackSizeToBeUsed < itemStackToBeUsed.getCount(); stackSizeToBeUsed++) {
                        itemStackTmp.setItemDamage(itemStackTmp.getItemDamage() - itemDamage);
                        itemDamage = Math.min(itemStackTmp.getItemDamage(), itemStackTmp.getMaxDamage() / 4);
                    }
                    this.stackSizeToBeUsedInRepair = stackSizeToBeUsed;
                }
            }
        }
    }
}
