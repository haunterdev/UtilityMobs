package toast.utilityMobs.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;

public class EntityWorkbenchGolem extends EntityContainerGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: crafting table.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.WOOD;
    }

    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/workbenchgolem.png");

    public EntityWorkbenchGolem(EntityType<? extends EntityWorkbenchGolem> type, Level level) {
        super(type, level);
        this.texture = EntityWorkbenchGolem.TEXTURE;
    }

    @Override
    protected Item getDropItem() {
        return Items.CRAFTING_TABLE;
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            toast.utilityMobs.network.GuiHelper.displayGUIWorkbench(player, this);
        }
        return true;
    }
}
