package toast.utilityMobs.client.model;

import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * The snowman-shaped golem model. 1.12.2 subclassed vanilla ModelSnowMan purely to drop the arm
 * swing while the golem is sitting.
 *
 * <p>This does NOT extend 1.20.1's SnowGolemModel, on purpose. CustomHeadLayer (which draws the block
 * in the golem's HEAD slot, as 1.12.2's LayerCustomHead did) needs a {@link HeadedModel}, and
 * SnowGolemModel's getHead() is its own method rather than an implementation of that interface. In a
 * dev run both are named getHead, so inheriting it compiled and worked; after reobf the interface
 * method is m_5585_ and SnowGolemModel's is a different SRG name, so the released jar threw
 * AbstractMethodError the first time a pumpkin-headed melon or snow golem was drawn, in the world or
 * in the guide book (1.20.1 issue #23). Declaring getHead() here, on a class whose only getHead is
 * the interface's, gives it the one name reobf can map.
 *
 * <p>The geometry is vanilla's own layer ({@link SnowGolemModel#createBodyLayer()}), and
 * {@link #setupAnim} is vanilla SnowGolemModel.setupAnim line for line, plus the sitting override.
 */
public class ModelStackGolem extends HierarchicalModel<EntityUtilityGolem> implements HeadedModel
{
    private final ModelPart root;
    private final ModelPart upperBody;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public ModelStackGolem(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.upperBody = root.getChild("upper_body");
    }

    public static LayerDefinition createBodyLayer() {
        return SnowGolemModel.createBodyLayer();
    }

    // Sets the model's various rotation angles.
    @Override
    public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);
        this.upperBody.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.25F;
        float f = Mth.sin(this.upperBody.yRot);
        float f1 = Mth.cos(this.upperBody.yRot);
        this.leftArm.yRot = this.upperBody.yRot;
        this.rightArm.yRot = this.upperBody.yRot + (float)Math.PI;
        this.leftArm.x = f1 * 5.0F;
        this.leftArm.z = -f * 5.0F;
        this.rightArm.x = -f1 * 5.0F;
        this.rightArm.z = f * 5.0F;
        // 1.12.2's ModelSnowMan set the arm roll every frame, so zeroing it while sitting undid itself on
        // standing. 1.20.1 bakes the roll into the layer's PartPose (left 1.0, right -1.0) and never sets
        // it again, so both states are written here or the arms stay down after the first sit.
        if (golem.isSitting()) {
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
        else {
            this.leftArm.zRot = 1.0F;
            this.rightArm.zRot = -1.0F;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }
}
