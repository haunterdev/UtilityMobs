package toast.utilityMobs.golem;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import toast.utilityMobs.setup.ModMenus;

/**
 * The steam golem's three fuel slots.
 *
 * <p>1.12.2 synced the burn timer by hand: addListener and detectAndSendChanges pushed window
 * properties 1 and 2, and updateProgressBar wrote them back on the client. 1.20.1 has ContainerData
 * for exactly that, so the two values ride the same channel with none of the bookkeeping.
 */
public class ContainerSteamGolem extends AbstractContainerMenu
{
    /// The golem's own slots, before the player inventory begins.
    private static final int GOLEM_SLOTS = 3;

    private final EntitySteamGolem golem;
    private final ContainerData burnData;

    /// Server-side constructor: the data slots read straight off the golem.
    public ContainerSteamGolem(int containerId, Inventory inventory, EntitySteamGolem steamGolem) {
        super(ModMenus.STEAM_GOLEM.get(), containerId);
        this.golem = steamGolem;
        this.burnData = steamGolem == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? steamGolem.burnTime : steamGolem.maxBurnTime;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    steamGolem.burnTime = value;
                }
                else {
                    steamGolem.maxBurnTime = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        Container container = steamGolem == null ? new SimpleContainer(ContainerSteamGolem.GOLEM_SLOTS) : steamGolem;
        int i;
        for (i = 0; i < ContainerSteamGolem.GOLEM_SLOTS; i++) {
            // Fuel-only slots: reject anything that isn't furnace fuel on manual placement (moveItemStackTo
            // / hoppers already respect this via mayPlace). Keeps non-fuel out of the golem entirely.
            final Container fuelContainer = container;
            this.addSlot(new Slot(fuelContainer, i, 62 + i * 18, 44) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AbstractFurnaceBlockEntity.isFuel(stack);
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }
        this.addDataSlots(this.burnData);
    }

    /// The golem this container is viewing. Used by the golem's tick to auto-close distant viewers.
    public EntitySteamGolem getGolem() {
        return this.golem;
    }

    /// Burn timer, as synced. The screen reads these rather than the golem so it works on the client.
    public int getBurnTime() {
        return this.burnData.get(0);
    }

    public int getMaxBurnTime() {
        return this.burnData.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem != null && this.golem.stillValid(player);
    }

    /// Called when a player shift-clicks on a slot.
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStackInSlot = slot.getItem();
            itemStack = itemStackInSlot.copy();
            if (slotIndex >= ContainerSteamGolem.GOLEM_SLOTS) {
                if (AbstractFurnaceBlockEntity.isFuel(itemStackInSlot)) {
                    // Merge from player inventory to golem inventory.
                    if (!this.moveItemStackTo(itemStackInSlot, 0, 3, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex < 30) {
                    // Merge from main inventory to hotbar.
                    if (!this.moveItemStackTo(itemStackInSlot, 30, 39, false))
                        return ItemStack.EMPTY;
                }
                else if (slotIndex < 39) {
                    // Merge from hotbar to main inventory.
                    if (!this.moveItemStackTo(itemStackInSlot, 3, 30, false))
                        return ItemStack.EMPTY;
                }
            }
            else {
                // Merge from golem inventory to player inventory.
                if (!this.moveItemStackTo(itemStackInSlot, 3, 39, false))
                    return ItemStack.EMPTY;
            }

            if (itemStackInSlot.getCount() == 0) {
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
