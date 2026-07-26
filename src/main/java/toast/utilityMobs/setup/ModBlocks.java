package toast.utilityMobs.setup;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.block.BlockGolemLight;

/**
 * Blocks. There is exactly one, and it has no item form on purpose: 1.12.2 registered GOLEM_LIGHT as a
 * bare Block with no ItemBlock so it could never be obtained or placed by a player.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final RegistryObject<Block> GOLEM_LIGHT =
        ModRegistries.BLOCKS.register("golem_light", BlockGolemLight::new);
}
