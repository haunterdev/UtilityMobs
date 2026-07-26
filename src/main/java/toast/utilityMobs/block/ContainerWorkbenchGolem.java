package toast.utilityMobs.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * A crafting table opened against a workbench golem instead of a block.
 *
 * <p>1.12.2 sent the client a window of type "minecraft:crafting_table", so the client showed the
 * vanilla screen while the server ran this subclass. That still works in 1.20.1 for free: CraftingMenu
 * hardcodes MenuType.CRAFTING, so a subclass reports the vanilla type and the client builds a plain
 * CraftingMenu. No MenuType of our own is needed.
 *
 * <p>stillValid must be overridden because the inherited check asks whether a crafting table block is
 * still at the access position, and there is no block here.
 */
public class ContainerWorkbenchGolem extends CraftingMenu
{
    /// The golem being crafted on.
    public final EntityContainerGolem golem;

    public ContainerWorkbenchGolem(int containerId, Inventory inventory, EntityContainerGolem workbench) {
        super(containerId, inventory, ContainerLevelAccess.create(workbench.level(), workbench.blockPosition()));
        this.golem = workbench;
        this.golem.startOpen(inventory.player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.golem.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem.stillValid(player);
    }
}
