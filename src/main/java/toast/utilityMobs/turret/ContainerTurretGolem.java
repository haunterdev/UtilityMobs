package toast.utilityMobs.turret;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.setup.ModMenus;

public class ContainerTurretGolem extends AbstractContainerMenu {
    private final EntityTurretGolem turret;
    /// Whether the ammo grid is present. Read once at construction so the client and server agree on
    /// the slot layout even if the config is toggled while a screen is open.
    private final boolean withAmmo;

    /** Live view over the turret's equipment slot 0 (the upgrade). */
    private static class UpgradeInventory extends SimpleContainer {
        private final EntityTurretGolem turret;
        UpgradeInventory(EntityTurretGolem turret) {
            super(1);
            this.turret = turret;
        }
        @Override public ItemStack getItem(int index) { return this.turret.getEquipmentInSlot(0); }
        @Override public ItemStack removeItem(int index, int count) {
            ItemStack cur = this.turret.getEquipmentInSlot(0);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = cur.split(count);
            this.turret.setCurrentItemOrArmor(0, cur.isEmpty() ? ItemStack.EMPTY : cur);
            this.setChanged();
            return split;
        }
        @Override public ItemStack removeItemNoUpdate(int index) {
            ItemStack cur = this.turret.getEquipmentInSlot(0);
            this.turret.setCurrentItemOrArmor(0, ItemStack.EMPTY);
            return cur;
        }
        @Override public void setItem(int index, ItemStack stack) {
            if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
            this.turret.setCurrentItemOrArmor(0, stack);
            this.setChanged();
        }
        @Override public int getMaxStackSize() { return 1; }
        @Override public boolean canPlaceItem(int index, ItemStack stack) {
            return EnumUpgrade.getUpgrade(this.turret.upgrades, stack) != EnumUpgrade.DEFAULT;
        }
        @Override public boolean stillValid(Player player) { return this.turret.canInteract(player); }
    }

    public ContainerTurretGolem(int containerId, Inventory playerInv, EntityTurretGolem turret) {
        super(ModMenus.TURRET_GOLEM.get(), containerId);
        this.turret = turret;
        this.withAmmo = turret != null && turret.requiresAmmo();
        final Container upgradeInv = turret == null ? new SimpleContainer(1) : new UpgradeInventory(turret);
        // Upgrade slot (left column)
        this.addSlot(new Slot(upgradeInv, 0, 24, 24) {
            @Override public boolean mayPlace(ItemStack stack) { return upgradeInv.canPlaceItem(0, stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        // Ammo grid (3x3), placed to the LEFT of the main GUI. Only present when require_ammo is on.
        if (this.withAmmo) {
            final Container ammoInv = turret.getAmmoInventory();
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    final int idx = col + row * 3;
                    this.addSlot(new Slot(ammoInv, idx, -64 + col * 18, 24 + row * 18) {
                        @Override public boolean mayPlace(ItemStack stack) {
                            return !stack.isEmpty() && stack.getItem() == turret.getAmmoItem();
                        }
                    });
                }
            }
        }
        // Player inventory
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 114 + i * 18));
        for (int i = 0; i < 9; ++i)
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 172));
    }

    public EntityTurretGolem getTurret() { return this.turret; }

    public boolean hasAmmoGrid() { return this.withAmmo; }

    @Override
    public boolean stillValid(Player player) { return this.turret != null && this.turret.canInteract(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = this.withAmmo ? 10 : 1;
            int size = this.slots.size();
            if (index == 0) {
                // upgrade slot -> player inventory
                if (!this.moveItemStackTo(stack, playerStart, size, true)) return ItemStack.EMPTY;
            } else if (this.withAmmo && index >= 1 && index < playerStart) {
                // ammo slot -> player inventory
                if (!this.moveItemStackTo(stack, playerStart, size, true)) return ItemStack.EMPTY;
            } else {
                // player inventory -> turret slots
                if (this.withAmmo && !stack.isEmpty() && stack.getItem() == this.turret.getAmmoItem()) {
                    if (!this.moveItemStackTo(stack, 1, playerStart, false)) return ItemStack.EMPTY;
                } else if (this.getSlot(0).mayPlace(stack) && !this.getSlot(0).hasItem()) {
                    ItemStack one = stack.copy();
                    one.setCount(1);
                    this.getSlot(0).set(one);
                    stack.shrink(1);
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
