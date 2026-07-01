package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ContainerAnvilGolemSlot extends Slot
{
    /// The anvil golem container.
    public final ContainerAnvilGolem anvilContainer;
    /// The world object.
    public final World world;

    public ContainerAnvilGolemSlot(ContainerAnvilGolem container, IInventory inventory, int slotIndex, int xDisplay, int yDisplay) {
        super(inventory, slotIndex, xDisplay, yDisplay);
        this.anvilContainer = container;
        this.world = container.golem.world;
    }

    @Override
    public boolean isItemValid(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return (player.capabilities.isCreativeMode || player.experienceLevel >= this.anvilContainer.maximumCost) && this.anvilContainer.maximumCost > 0 && this.getHasStack();
    }

    @Override
    public ItemStack onTake(EntityPlayer player, ItemStack stack) {
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-this.anvilContainer.maximumCost);
        }

        this.anvilContainer.inputSlots.setInventorySlotContents(0, ItemStack.EMPTY);

        if (this.anvilContainer.stackSizeToBeUsedInRepair > 0) {
            ItemStack itemstack1 = this.anvilContainer.inputSlots.getStackInSlot(1);

            if (!itemstack1.isEmpty() && itemstack1.getCount() > this.anvilContainer.stackSizeToBeUsedInRepair) {
                itemstack1.shrink(this.anvilContainer.stackSizeToBeUsedInRepair);
                this.anvilContainer.inputSlots.setInventorySlotContents(1, itemstack1);
            }
            else {
                this.anvilContainer.inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
            }
        }
        else {
            this.anvilContainer.inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
        }

        this.anvilContainer.maximumCost = 0;

        if (!this.world.isRemote) {
            BlockPos pos = this.anvilContainer.golem.getPosition();
            if (!player.capabilities.isCreativeMode && player.getRNG().nextFloat() < 0.12F) {
                int damage = this.anvilContainer.golem.getDamage() + 1;
                if (damage > 2) {
                    this.world.playEvent(1020, pos, 0);
                    this.anvilContainer.golem.setDead();
                }
                else {
                    this.world.playEvent(1021, pos, 0);
                    this.anvilContainer.golem.setDamage(damage);
                }
            }
            else {
                this.world.playEvent(1021, pos, 0);
            }
        }
        return stack;
    }
}
