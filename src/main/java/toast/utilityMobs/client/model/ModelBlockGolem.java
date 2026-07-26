package toast.utilityMobs.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class ModelBlockGolem extends EntityModel<EntityUtilityGolem>
{
    public final ModelPart body;
    public final ModelPart legFrontLeft;
    public final ModelPart legFrontRight;
    public final ModelPart legBackLeft;
    public final ModelPart legBackRight;

    /// Set in setupAnim, consumed in renderToBuffer: the body lifts when the golem sits.
    private boolean sitting;

    public ModelBlockGolem(ModelPart root) {
        this.body = root.getChild("body");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
            PartPose.offset(0.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("leg_front_left",
            CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(8.0F, 19.0F, -6.0F));
        root.addOrReplaceChild("leg_front_right",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-8.0F, 19.0F, -6.0F));
        root.addOrReplaceChild("leg_back_left",
            CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(8.0F, 19.0F, 6.0F));
        root.addOrReplaceChild("leg_back_right",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-8.0F, 19.0F, 6.0F));
        // 1.12.2 built every part with the no-arg ModelRenderer(this), which defaults to texture
        // offset (0,0) on a 64x32 sheet.
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.sitting = golem.isSitting();
        if (this.sitting) {
            limbSwingAmount = 0.0F;
        }
        this.legFrontLeft.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legFrontRight.xRot = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackLeft.xRot = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackRight.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        poseStack.pushPose();
        // Sitting drops the body 0.25 blocks onto its legs. Model space is Y-down in both 1.12.2 and
        // 1.20.1 (both renderers scale -1 on Y first), so the sign carries over unchanged.
        poseStack.translate(0.0F, this.sitting ? 0.25F : 0.0F, 0.0F);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        poseStack.popPose();
        this.legFrontLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legFrontRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legBackLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legBackRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
