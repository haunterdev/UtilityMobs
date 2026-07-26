package toast.utilityMobs.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/**
 * An invisible, non-solid, full-bright block placed by the Jack o'Lantern Golem so it lights the area
 * around itself as it moves (like a real jack o'lantern, light level 15). The golem clears the previous
 * light when it walks; this block also schedules its own removal if no lantern golem is nearby, so stray
 * lights never linger (e.g. after a chunk unloads).
 *
 * <p>1.12.2 built this on Material.AIR and overrode a handful of shape and collision methods. 1.20.1
 * folds all of those into block properties, and AirBlock already supplies the invisible render shape
 * and the empty collision shape, so extending it is the direct translation.
 */
public class BlockGolemLight extends AirBlock
{
    /// How often the block re-checks whether its golem is still nearby.
    private static final int CHECK_INTERVAL = 20;

    public BlockGolemLight() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .replaceable()
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .air()
            .lightLevel(state -> 15)
            // 1.12.2's setBlockUnbreakable.
            .strength(-1.0F, 3600000.0F));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, BlockGolemLight.CHECK_INTERVAL);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Remove the light if no lantern golem is still standing here.
        AABB area = new AABB(pos).inflate(2.0);
        List<EntityLanternGolem> golems = level.getEntitiesOfClass(EntityLanternGolem.class, area);
        if (golems.isEmpty()) {
            level.removeBlock(pos, false);
        }
        else {
            level.scheduleTick(pos, this, BlockGolemLight.CHECK_INTERVAL);
        }
    }

    /** Helper used by the golem: this position is safe to overwrite with a light. */
    public static boolean isLightReplaceable(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
