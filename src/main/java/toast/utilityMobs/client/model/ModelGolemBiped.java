package toast.utilityMobs.client.model;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * The default humanoid golem model, on a legacy 64x32 texture sheet.
 *
 * <p>1.12.2 got this by passing {@code new ModelZombie(0.0F, true)} to RenderGolem - the boolean
 * only selects a 32px-tall texture, it adds no zombie geometry or animation. Every utility mob
 * texture in this mod is 64x32, so the whole point of that call was the sheet layout.
 *
 * <p>1.20.1's {@link HumanoidModel#createMesh} assumes a 64x64 sheet and gives the left arm and
 * left leg their own UV islands (32,48 and 16,48). On a 64x32 sheet those coordinates fall off the
 * texture, so both parts are rebuilt here as mirrored copies of their right-hand twins, which is
 * exactly what 1.12.2's ModelBiped did in its 32px branch.
 */
public final class ModelGolemBiped {
    private ModelGolemBiped() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static HumanoidModel<EntityUtilityGolem> create(ModelPart root) {
        return new ZombieArmed(root);
    }

    /**
     * The arms-out pose. 1.12.2 passed a real {@code ModelZombie}, whose setRotationAngles ended with
     * the zombie arm block: {@code flag = entityIn instanceof EntityZombie && isArmsRaised()}, always
     * false for a golem, giving {@code xRot = -PI / 2.25} on both arms plus the swing and bob terms.
     * 1.20.1 factors that exact code out as {@link AnimationUtils#animateZombieArms}, so calling it
     * with isAggressive=false reproduces the 1.12.2 pose line for line.
     */
    public static class ZombieArmed extends HumanoidModel<EntityUtilityGolem> {
        public ZombieArmed(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            // A held-item pose (bow, eating, etc.) already positions the arm; overwriting it would undo
            // the aim. 1.12.2's golems held no bow on this model, so this never diverges there.
            if (this.rightArmPose == HumanoidModel.ArmPose.EMPTY
                    && this.leftArmPose == HumanoidModel.ArmPose.EMPTY) {
                AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, false, this.attackTime, ageInTicks);
            }
        }
    }
}
