package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityEnderChest;

public class TileEntityEnderChestProxy extends TileEntityEnderChest
{
    EntityContainerGolem golem;

    public TileEntityEnderChestProxy(EntityContainerGolem chest) {
        this.golem = chest;
    }

    // Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count ticks and creates a new spawn inside its implementation.
    @Override
    public void update() {
        // Do nothing
    }

    // Called when a GUI using this inventory is opened.
    @Override
    public void openChest() {
        this.golem.openInventory(null);
    }

    // Called when a GUI using this inventory is closed.
    @Override
    public void closeChest() {
        this.golem.closeInventory(null);
    }

    // Do not make give this method the name canInteractWith because it clashes with Container.
    @Override
    public boolean canBeUsed(EntityPlayer player) {
        return this.golem.isUsableByPlayer(player);
    }
}
