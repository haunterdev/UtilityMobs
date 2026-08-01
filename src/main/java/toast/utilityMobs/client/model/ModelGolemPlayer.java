package toast.utilityMobs.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * Player-shaped golem model, used by the bound soul (1.12.2 issue #15).
 *
 * <p>The point is the second (overlay) texture layer: body, arms, legs and hat all get a wear part, which
 * is what resource packs were asking for. It bakes from vanilla's own {@code ModelLayers.PLAYER}, so no
 * layer definition of this mod's needs registering, and that layer is 64x64. {@code boundsoul.png} is
 * therefore a 64x64 skin-layout texture rather than the legacy 64x32 sheet every other golem uses.
 *
 * <p>PlayerModel is generic over LivingEntity, not Player, so nothing here needs a real player.
 */
public class ModelGolemPlayer extends PlayerModel<EntityUtilityGolem>
{
    public ModelGolemPlayer(ModelPart root) {
        // slim = false: the classic 4px arms, so armour layers and held-item placement line up with
        // every other biped golem.
        super(root, false);
    }

    @Override
    public void prepareMobModel(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
        ModelGolemBiped.applyBowPose(this, golem);
        super.prepareMobModel(golem, limbSwing, limbSwingAmount, partialTick);
    }
}
