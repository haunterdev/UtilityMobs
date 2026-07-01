package toast.utilityMobs.turret;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import toast.utilityMobs.EnumUpgrade;

public class ContainerTurretGolem extends Container {
    private final EntityTurretGolem turret;

    /** Live view over the turret's equipment slot 0 (the upgrade). */
    private static class UpgradeInventory extends InventoryBasic {
        private final EntityTurretGolem turret;
        UpgradeInventory(EntityTurretGolem turret) {
            super("upgrade", false, 1);
            this.turret = turret;
        }
        @Override public ItemStack getStackInSlot(int index) { return this.turret.getEquipmentInSlot(0); }
        @Override public ItemStack decrStackSize(int index, int count) {
            ItemStack cur = this.turret.getEquipmentInSlot(0);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = cur.splitStack(count);
            this.turret.setCurrentItemOrArmor(0, cur.isEmpty() ? ItemStack.EMPTY : cur);
            this.markDirty();
            return split;
        }
        @Override public ItemStack removeStackFromSlot(int index) {
            ItemStack cur = this.turret.getEquipmentInSlot(0);
            this.turret.setCurrentItemOrArmor(0, ItemStack.EMPTY);
            return cur;
        }
        @Override public void setInventorySlotContents(int index, ItemStack stack) {
            if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
            this.turret.setCurrentItemOrArmor(0, stack);
            this.markDirty();
        }
        @Override public int getInventoryStackLimit() { return 1; }
        @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
            return EnumUpgrade.getUpgrade(this.turret.upgrades, stack) != EnumUpgrade.DEFAULT;
        }
        @Override public boolean isUsableByPlayer(EntityPlayer player) { return this.turret.canInteract(player); }
    }

    public ContainerTurretGolem(InventoryPlayer playerInv, EntityTurretGolem turret) {
        this.turret = turret;
        final IInventory upgradeInv = new UpgradeInventory(turret);
        // Upgrade slot (left column)
        this.addSlotToContainer(new Slot(upgradeInv, 0, 24, 24) {
            @Override public boolean isItemValid(ItemStack stack) { return upgradeInv.isItemValidForSlot(0, stack); }
            @Override public int getSlotStackLimit() { return 1; }
        });
        // Ammo grid (3x3), placed to the LEFT of the main GUI. Only present when require_ammo is on.
        if (turret.requiresAmmo()) {
            final IInventory ammoInv = turret.getAmmoInventory();
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    final int idx = col + row * 3;
                    this.addSlotToContainer(new Slot(ammoInv, idx, -64 + col * 18, 24 + row * 18) {
                        @Override public boolean isItemValid(ItemStack stack) {
                            return !stack.isEmpty() && stack.getItem() == turret.getAmmoItem();
                        }
                    });
                }
            }
        }
        // Player inventory
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 114 + i * 18));
        for (int i = 0; i < 9; ++i)
            this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 172));
    }

    public EntityTurretGolem getTurret() { return this.turret; }

    @Override
    public boolean canInteractWith(EntityPlayer player) { return this.turret.canInteract(player); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            int playerStart = this.turret.requiresAmmo() ? 10 : 1;
            int size = this.inventorySlots.size();
            if (index == 0) {
                // upgrade slot -> player inventory
                if (!this.mergeItemStack(stack, playerStart, size, true)) return ItemStack.EMPTY;
            } else if (this.turret.requiresAmmo() && index >= 1 && index < playerStart) {
                // ammo slot -> player inventory
                if (!this.mergeItemStack(stack, playerStart, size, true)) return ItemStack.EMPTY;
            } else {
                // player inventory -> turret slots
                if (this.turret.requiresAmmo() && !stack.isEmpty() && stack.getItem() == this.turret.getAmmoItem()) {
                    if (!this.mergeItemStack(stack, 1, playerStart, false)) return ItemStack.EMPTY;
                } else if (this.getSlot(0).isItemValid(stack) && !this.getSlot(0).getHasStack()) {
                    ItemStack one = stack.copy();
                    one.setCount(1);
                    this.getSlot(0).putStack(one);
                    stack.shrink(1);
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
