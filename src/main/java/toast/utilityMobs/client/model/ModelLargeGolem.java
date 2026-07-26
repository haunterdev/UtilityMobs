package toast.utilityMobs.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import toast.utilityMobs.golem.EntityLargeGolem;

public class ModelLargeGolem extends EntityModel<EntityLargeGolem>
{
    /// 1.12.2 passed this as the constructor's second argument, defaulting to -7.0F.
    private static final float Y_OFFSET = -7.0F;

    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart armRight;
    public final ModelPart armLeft;
    public final ModelPart legLeft;
    public final ModelPart legRight;

    /// Set in setupAnim, consumed in renderToBuffer.
    private boolean sitting;

    public ModelLargeGolem(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.armRight = root.getChild("arm_right");
        this.armLeft = root.getChild("arm_left");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F, none)
                .texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F, none),
            PartPose.offset(0.0F, Y_OFFSET, -2.0F));

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F, none)
                .texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
            PartPose.offset(0.0F, Y_OFFSET, 0.0F));

        root.addOrReplaceChild("arm_right",
            CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F, none),
            PartPose.offset(0.0F, -7.0F, 0.0F));

        root.addOrReplaceChild("arm_left",
            CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F, none),
            PartPose.offset(0.0F, -7.0F, 0.0F));

        root.addOrReplaceChild("leg_left",
            CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F, none),
            PartPose.offset(-4.0F, 18.0F + Y_OFFSET, 0.0F));

        root.addOrReplaceChild("leg_right",
            CubeListBuilder.create().mirror().texOffs(60, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F, none),
            PartPose.offset(5.0F, 18.0F + Y_OFFSET, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    /// 1.12.2 setLivingAnimations: arm pose from hit / animation timers, else the walk swing.
    @Override
    public void prepareMobModel(EntityLargeGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
        int hit = golem.getHitTime();
        if (hit > 0) {
            this.armRight.xRot = -2.0F + 1.5F * this.adjust(hit - partialTick, 10.0F);
            this.armLeft.xRot = -2.0F + 1.5F * this.adjust(hit - partialTick, 10.0F);
        }
        else {
            int anim = golem.getAnimationTime();
            if (anim > 0) {
                this.armRight.xRot = -0.8F + 0.025F * this.adjust(anim, 70.0F);
                this.armLeft.xRot = 0.0F;
            }
            else {
                this.armRight.xRot = (-0.2F + 1.5F * this.adjust(limbSwing, 13.0F)) * limbSwingAmount;
                this.armLeft.xRot = (-0.2F - 1.5F * this.adjust(limbSwing, 13.0F)) * limbSwingAmount;
            }
        }
    }

    @Override
    public void setupAnim(EntityLargeGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.sitting = golem.isSitting();
        if (this.sitting) {
            limbSwingAmount = 0.0F;
        }
        this.head.yRot = netHeadYaw / (180.0F / (float)Math.PI);
        this.head.xRot = headPitch / (180.0F / (float)Math.PI);
        this.legLeft.xRot = -1.5F * this.adjust(limbSwing, 13.0F) * limbSwingAmount;
        this.legRight.xRot = 1.5F * this.adjust(limbSwing, 13.0F) * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        poseStack.pushPose();
        // Sitting drops the upper body onto the legs. Model space is Y-down, sign as in 1.12.2.
        poseStack.translate(0.0F, this.sitting ? 0.25F : 0.0F, 0.0F);
        this.head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.armRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.armLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        poseStack.popPose();
        this.legLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    private float adjust(float f, float f1) {
        return (Math.abs(f % f1 - f1 * 0.5F) - f1 * 0.25F) / (f1 * 0.25F);
    }
}
