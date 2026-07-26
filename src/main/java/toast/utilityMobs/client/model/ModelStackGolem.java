package toast.utilityMobs.client.model;

import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * The snowman-shaped golem model. 1.12.2 subclassed vanilla ModelSnowMan purely to drop the arm
 * swing while the golem is sitting.
 *
 * <p>1.20.1's SnowGolemModel keeps its arm parts private, so they are fetched by name from the baked
 * root instead of being inherited fields. The names are taken from the vanilla class itself, not
 * guessed.
 *
 * <p>{@link HeadedModel} is declared here only so the model satisfies CustomHeadLayer's bound; the
 * required getHead() is already inherited from SnowGolemModel. That layer is what draws the block in
 * the golem's HEAD slot, which is how 1.12.2 rendered it too.
 */
public class ModelStackGolem extends SnowGolemModel<EntityUtilityGolem> implements HeadedModel
{
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public ModelStackGolem(ModelPart root) {
        super(root);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    // Sets the model's various rotation angles.
    @Override
    public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (golem.isSitting()) {
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
    }
}
