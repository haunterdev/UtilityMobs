package toast.utilityMobs.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import toast.utilityMobs.BuildHelper;

public class BlockEvent extends UtilityMobsEvent
{
    // The player involved with this event.
    public Player entityPlayer;
    // True if the player was holding a pumpkin.
    public boolean holdingGolemHead;
    // The location of this event.
    public int posX, posY, posZ;

    public BlockEvent(Player player, int x, int y, int z, Direction face) {
        this(player, BlockEvent.offset(x, y, z, face));
    }

    public BlockEvent(Player player, int x, int y, int z) {
        this(player, new BlockPos(x, y, z));
    }

    private BlockEvent(Player player, BlockPos pos) {
        this.entityPlayer = player;
        ItemStack held = player.getMainHandItem();
        // 1.12.2's Blocks.PUMPKIN was the carved, face-bearing block that builds golems; the uncarved
        // pumpkin did not exist until 1.13, so the two heads here are the carved one and the lantern.
        this.holdingGolemHead = !held.isEmpty()
            && (held.is(Blocks.CARVED_PUMPKIN.asItem()) || held.is(Blocks.JACK_O_LANTERN.asItem()));
        this.posX = pos.getX();
        this.posY = pos.getY();
        this.posZ = pos.getZ();
    }

    private static BlockPos offset(int x, int y, int z, Direction face) {
        BlockPos pos = new BlockPos(x, y, z);
        return face == null ? pos : pos.relative(face);
    }

    /// Actually triggers this event's effects.
    @Override
    public void execute() {
        BuildHelper.place(this.entityPlayer.level(), this.entityPlayer, this.holdingGolemHead, this.posX, this.posY, this.posZ);
    }
}
