package toast.utilityMobs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * An anvil opened against an anvil golem instead of a block.
 *
 * <p>Most of 1.12.2's version has been absorbed by vanilla. Its ContainerAnvilGolemSlot existed only
 * to re-implement the result slot's take behaviour and its private repair-material count, and its
 * updateRepairOutput override only recomputed that same private field. 1.20.1's AnvilMenu exposes
 * both (repairItemCountCost, getCost) and its own result slot already does the take logic, so all
 * that is left is redirecting the anvil's chance of chipping onto the golem.
 *
 * <p>The access is NULL on purpose: it makes the inherited take handler skip the block-damage branch
 * it would otherwise run against whatever block happens to sit at the golem's position, leaving the
 * golem damage below as the only thing that happens.
 */
public class ContainerAnvilGolem extends AnvilMenu
{
    /// The chance per use that the anvil chips, as in 1.12.2 and vanilla.
    private static final float BREAK_CHANCE = 0.12F;

    // The golem being crafted on.
    public final EntityAnvilGolem golem;

    public ContainerAnvilGolem(int containerId, Inventory inventory, EntityAnvilGolem anvil) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        this.golem = anvil;
        this.golem.startOpen(inventory.player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // ItemCombinerMenu.removed hands the input slots back through access.execute(...), and this menu
        // deliberately uses ContainerLevelAccess.NULL (see the class note), whose execute is a no-op. So
        // that callback never ran and anything left in the two input slots was destroyed on close.
        // Returning them here is what 1.12.2 got from ContainerRepair.onContainerClosed -> clearContainer.
        this.clearContainer(player, this.inputSlots);
        this.golem.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem.stillValid(player);
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        if (this.golem.level().isClientSide)
            return;
        BlockPos pos = this.golem.blockPosition();
        if (!player.getAbilities().instabuild && player.getRandom().nextFloat() < ContainerAnvilGolem.BREAK_CHANCE) {
            int damage = this.golem.getDamage() + 1;
            if (damage > 2) {
                // 1029 destroyed, 1030 used. 1.12.2 called the same two events by their old ids 1020/1021.
                this.golem.level().levelEvent(1029, pos, 0);
                this.golem.discard();
            }
            else {
                this.golem.level().levelEvent(1030, pos, 0);
                this.golem.setDamage(damage);
            }
        }
        else {
            this.golem.level().levelEvent(1030, pos, 0);
        }
    }
}
