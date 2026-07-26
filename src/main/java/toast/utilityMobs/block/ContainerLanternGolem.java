package toast.utilityMobs.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import toast.utilityMobs.setup.ModMenus;

/// The lantern golem's 3x3 light store.
public class ContainerLanternGolem extends AbstractContainerMenu
{
    /// Slots belonging to the golem rather than the player.
    private static final int GOLEM_SLOTS = 9;

    private final EntityLanternGolem golem;
    private final Container container;

    public ContainerLanternGolem(int containerId, Inventory inventory, EntityLanternGolem lanternGolem) {
        super(ModMenus.LANTERN_GOLEM.get(), containerId);
        this.golem = lanternGolem;
        // The golem is the inventory. If it has already despawned client-side, back onto a throwaway so
        // the slots still have something to point at until stillValid closes the screen.
        this.container = lanternGolem == null ? new SimpleContainer(ContainerLanternGolem.GOLEM_SLOTS) : lanternGolem;
        if (this.golem != null) {
            this.golem.startOpen(inventory.player);
        }
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.container, j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.golem != null) {
            this.golem.stopOpen(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem != null && this.golem.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStackInSlot = slot.getItem();
            itemStack = itemStackInSlot.copy();
            if (slotIndex >= ContainerLanternGolem.GOLEM_SLOTS) {
                if (!this.moveItemStackTo(itemStackInSlot, 0, 9, false))
                    return ItemStack.EMPTY;
            }
            else {
                if (!this.moveItemStackTo(itemStackInSlot, 9, 45, false))
                    return ItemStack.EMPTY;
            }

            if (itemStackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            else {
                slot.setChanged();
            }

            if (itemStackInSlot.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, itemStackInSlot);
        }
        return itemStack;
    }
}
