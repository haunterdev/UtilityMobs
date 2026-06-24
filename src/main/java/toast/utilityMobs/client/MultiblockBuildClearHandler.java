package toast.utilityMobs.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import vazkii.patchouli.client.handler.MultiblockVisualizationHandler;
import vazkii.patchouli.common.multiblock.Multiblock;

@SideOnly(Side.CLIENT)
public class MultiblockBuildClearHandler {

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote) {
            return;
        }
        if (!MultiblockVisualizationHandler.hasMultiblock || !MultiblockVisualizationHandler.isAnchored()) {
            return;
        }
        Entity entity = event.getEntity();
        // Only our own mobs count - and only living ones (skip the golem fishing hook / projectiles).
        if (!(entity instanceof EntityLiving) || !entity.getClass().getName().startsWith("toast.utilityMobs")) {
            return;
        }
        Multiblock mb = MultiblockVisualizationHandler.getMultiblock();
        if (mb == null) {
            return;
        }
        BlockPos start;
        try {
            start = MultiblockVisualizationHandler.getStartPos();
        } catch (Exception e) {
            return;
        }
        if (start == null) {
            return;
        }
        // A golem spawns inside its own footprint, so a radius covering the structure's extent (plus a
        // small margin) is a safe "this is the build that just finished" test without false clears from
        // unrelated mobs spawning elsewhere.
        double radius = mb.sizeX + mb.sizeY + mb.sizeZ + 2.0D;
        if (start.distanceSq(entity.posX, entity.posY, entity.posZ) <= radius * radius) {
            MultiblockVisualizationHandler.hasMultiblock = false;
        }
    }
}