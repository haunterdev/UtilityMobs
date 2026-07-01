package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerWorkbench;

public class ContainerWorkbenchGolem extends ContainerWorkbench
{
    /// The golem being crafted on.
    public final EntityContainerGolem golem;

    public ContainerWorkbenchGolem(InventoryPlayer inventory, EntityContainerGolem workbench) {
        super(inventory, workbench.world, workbench.getPosition());
        this.golem = workbench;
        this.golem.openInventory(inventory.player);
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
}
