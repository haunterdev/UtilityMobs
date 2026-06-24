package toast.utilityMobs.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockGolemLight extends Block
{
    public BlockGolemLight() {
        super(Material.AIR);
        this.setLightLevel(1.0F); // 1.0 == light value 15
        this.setTickRandomly(false);
        this.setBlockUnbreakable();
        this.setTranslationKey("utilitymobs.golem_light");
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.SOLID;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            world.scheduleUpdate(pos, this, 20);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, java.util.Random rand) {
        if (world.isRemote)
            return;
        // Remove the light if no lantern golem is still standing here.
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(2.0);
        List<EntityLanternGolem> golems = world.getEntitiesWithinAABB(EntityLanternGolem.class, area);
        if (golems.isEmpty()) {
            world.setBlockToAir(pos);
        }
        else {
            world.scheduleUpdate(pos, this, 20);
        }
    }

    // Drop nothing if somehow broken.
    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isReplaceable(world, pos);
    }

    public static boolean isLightReplaceable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block == net.minecraft.init.Blocks.AIR || block.isReplaceable(world, pos);
    }
}