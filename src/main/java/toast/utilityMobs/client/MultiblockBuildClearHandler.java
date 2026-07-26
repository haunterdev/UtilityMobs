package toast.utilityMobs.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.client.handler.MultiblockVisualizationHandler;

/**
 * Auto-clears Patchouli's projected multiblock "ghost" once the structure is actually built.
 *
 * <p>Patchouli already self-clears a completed projection, but its completion test is
 * "every block of the pattern is present in the world". Our golems never satisfy that: the
 * instant the final block (pumpkin / skull / fence) lands, the pattern is consumed and replaced
 * by a single entity, so Patchouli sees the blocks vanish rather than complete and the ghost
 * lingers forever. We detect the build the only way that's reliable for us - the golem entity
 * spawning at the anchored site - and dismiss the projection ourselves.
 */
public class MultiblockBuildClearHandler {

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            return;
        }
        if (!MultiblockVisualizationHandler.hasMultiblock || !MultiblockVisualizationHandler.isAnchored()) {
            return;
        }
        Entity entity = event.getEntity();
        // Only our own mobs count - and only living ones (skip the golem fishing hook / projectiles).
        if (!(entity instanceof Mob) || !entity.getClass().getName().startsWith("toast.utilityMobs")) {
            return;
        }
        IMultiblock mb = MultiblockVisualizationHandler.getMultiblock();
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
        Vec3i size = mb.getSize();
        double radius = size.getX() + size.getY() + size.getZ() + 2.0D;
        if (start.distToCenterSqr(entity.getX(), entity.getY(), entity.getZ()) <= radius * radius) {
            MultiblockVisualizationHandler.hasMultiblock = false;
        }
    }
}
