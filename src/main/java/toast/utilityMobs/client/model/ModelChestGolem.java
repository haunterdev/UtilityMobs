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
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityChestTrappedGolem;

/**
 * The chest golem model (chest, ender chest, trapped chest). A chest body on four stubby legs,
 * with a hinged lid that lifts when the golem's inventory is open. 64x64 sheet.
 *
 * <p>1.12.2 used named sub-boxes on the "top" part via setTextureOffset/addBox(name, ...); each of
 * those becomes one texOffs/addBox pair on the single "top" CubeListBuilder here. The 1.12.2 model
 * set the {@code mirror} field AFTER addBox on several parts, which is a no-op (ModelRenderer.addBox
 * captures mirror at add time), so no {@code .mirror()} is applied here - the UVs match 1.12.2.
 */
public class ModelChestGolem extends EntityModel<EntityChestGolem>
{
    public final ModelPart top;
    public final ModelPart bottom;
    public final ModelPart teethBottom;
    public final ModelPart legFrontLeft;
    public final ModelPart legFrontRight;
    public final ModelPart legBackLeft;
    public final ModelPart legBackRight;

    /// Set in setupAnim, consumed in renderToBuffer.
    private boolean sitting;
    /// True unless a sitting trapped-chest golem, which hides its legs.
    private boolean renderLegs = true;

    public ModelChestGolem(ModelPart root) {
        this.top = root.getChild("top");
        this.bottom = root.getChild("bottom");
        this.teethBottom = root.getChild("teeth_bottom");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // top: the chest lid (head + nose + upper teeth), hinged at the back.
        root.addOrReplaceChild("top",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F)
                .texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2.0F, 4.0F, 1.0F)
                .texOffs(0, 43).addBox(-6.0F, 0.0F, -13.0F, 12.0F, 1.0F, 12.0F),
            PartPose.offset(0.0F, 11.0F, 7.0F));
        root.addOrReplaceChild("bottom",
            CubeListBuilder.create().texOffs(0, 19).addBox(-7.0F, 0.0F, -14.0F, 14.0F, 10.0F, 14.0F),
            PartPose.offset(0.0F, 10.0F, 7.0F));
        // teeth_bottom: flipped upside down (zRot = PI) so its bottom face reads as the lower jaw.
        root.addOrReplaceChild("teeth_bottom",
            CubeListBuilder.create().texOffs(0, 43).addBox(-6.0F, 0.0F, -13.0F, 12.0F, 1.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 7.0F, 0.0F, 0.0F, (float)Math.PI));
        root.addOrReplaceChild("leg_front_left",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(7.0F, 19.0F, -5.0F));
        root.addOrReplaceChild("leg_front_right",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-7.0F, 19.0F, -5.0F));
        root.addOrReplaceChild("leg_back_left",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(7.0F, 19.0F, 5.0F));
        root.addOrReplaceChild("leg_back_right",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-7.0F, 19.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /// Lid angle. 1.12.2 fetched it in setLivingAnimations, before the per-tick setupAnim.
    @Override
    public void prepareMobModel(EntityChestGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
        float angle = 1.0F - golem.prevLidAngle - (golem.lidAngle - golem.prevLidAngle) * partialTick;
        angle = 1.0F - angle * angle * angle;
        this.top.xRot = -angle * (float)Math.PI / 2.0F;
    }

    @Override
    public void setupAnim(EntityChestGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.sitting = golem.isSitting();
        this.renderLegs = !(this.sitting && golem instanceof EntityChestTrappedGolem);
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
        // Sitting drops the chest body 0.25 blocks onto its legs. Model space is Y-down in both versions.
        poseStack.translate(0.0F, this.sitting ? 0.25F : 0.0F, 0.0F);
        this.top.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.bottom.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.teethBottom.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        poseStack.popPose();
        if (this.renderLegs) {
            this.legFrontLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
            this.legFrontRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
            this.legBackLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
            this.legBackRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
    }
}
