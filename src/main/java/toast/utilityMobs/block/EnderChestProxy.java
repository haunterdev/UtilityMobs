package toast.utilityMobs.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.20.1 stand-in for 1.12.2's TileEntityEnderChestProxy.
 *
 * <p>1.12.2 could hand the player's ender inventory a fake TileEntityEnderChest whose openChest and
 * closeChest called into the golem, which is how an ender chest golem opened its lid. 1.20.1's
 * PlayerEnderChestContainer takes a real EnderChestBlockEntity or nothing, so instead the inventory is
 * wrapped: every storage call passes straight through to the player's real ender inventory, and only
 * the open and close callbacks are redirected to the golem.
 */
public class EnderChestProxy implements Container
{
    private final Container enderInventory;
    private final EntityContainerGolem golem;

    public EnderChestProxy(Container enderInventory, EntityContainerGolem golem) {
        this.enderInventory = enderInventory;
        this.golem = golem;
    }

    @Override
    public int getContainerSize() {
        return this.enderInventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.enderInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.enderInventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.enderInventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.enderInventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.enderInventory.setItem(slot, itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return this.enderInventory.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        this.enderInventory.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return this.enderInventory.canPlaceItem(slot, itemStack);
    }

    @Override
    public void clearContent() {
        this.enderInventory.clearContent();
    }

    /// The golem, not the player's ender inventory, is what should look open.
    @Override
    public void startOpen(Player player) {
        this.golem.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        this.golem.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem.stillValid(player);
    }
}
