package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
    Player-shaped golem model. Used by the bound soul (issue #15) so it gets the modern player geometry and,
    more usefully for resource packs, the second (overlay) texture layer on the body, arms and legs. That
    means boundsoul.png is a 64x64 skin-layout texture, not the legacy 64x32 one.

    Also aims a bow, exactly like ModelGolemBiped.
*/
@SideOnly(Side.CLIENT)
public class ModelGolemPlayer extends ModelPlayer
{
    public ModelGolemPlayer() {
        this(0.0F);
    }

    public ModelGolemPlayer(float modelSize) {
        // smallArms = false: the classic 4px arms, so armour layers and held-item placement line up with
        // every other biped golem.
        super(modelSize, false);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        ModelGolemBiped.applyBowPose(this, entitylivingbaseIn);
        super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
    }
}
