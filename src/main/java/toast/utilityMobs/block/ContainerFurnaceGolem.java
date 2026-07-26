package toast.utilityMobs.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;

/**
 * A furnace opened against a furnace golem instead of a block entity.
 *
 * <p>1.12.2 had to rebuild the whole furnace container by hand, because vanilla's demanded a real
 * TileEntityFurnace. 1.20.1's FurnaceMenu takes any Container plus any ContainerData, and the golem
 * supplies both, so all that is left of the original is telling the golem when a player opens and
 * closes it - which is what drives its open state.
 *
 * <p>The four synced values keep the meaning the vanilla screen expects, since the client shows the
 * vanilla furnace screen: 0 burn time, 1 burn total, 2 cook time, 3 cook total. See
 * {@link EntityFurnaceGolem#getFurnaceData()}.
 */
public class ContainerFurnaceGolem extends FurnaceMenu
{
    /// The golem being smelted in.
    public final EntityFurnaceGolem golem;

    public ContainerFurnaceGolem(int containerId, Inventory inventory, EntityFurnaceGolem furnace) {
        super(containerId, inventory, furnace, furnace.getFurnaceData());
        this.golem = furnace;
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
