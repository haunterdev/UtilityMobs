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

/**
 * The anvil golem model: an anvil body walking on four stubby legs. 64x32 sheet.
 *
 * <p>1.12.2 assembled the body from four named sub-boxes on one ModelRenderer via setTextureOffset,
 * which is just a chained texOffs/addBox CubeListBuilder here.
 *
 * <p>Sitting sank the body by translating it 0.25 blocks along the model's (already y-flipped) axis.
 * That translate has no 1.20.1 equivalent inside render, so the body part's own y offset is moved by
 * the same amount instead: 0.25 blocks x 16 = 4 pixels, and +y is still down in model space.
 */
public class ModelAnvilGolem extends EntityModel<EntityUtilityGolem>
{
    /// The body's resting y offset, from 1.12.2's setRotationPoint(0.0F, 20.0F, 0.0F).
    private static final float BODY_Y = 20.0F;
    /// How far the body sinks while sitting, in pixels.
    private static final float SIT_SINK = 4.0F;

    public final ModelPart body;
    public final ModelPart legFrontLeft;
    public final ModelPart legFrontRight;
    public final ModelPart legBackLeft;
    public final ModelPart legBackRight;

    public ModelAnvilGolem(ModelPart root) {
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
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, -16.0F, -5.0F, 16.0F, 6.0F, 10.0F)
                .texOffs(40, 23).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 5.0F, 4.0F)
                .texOffs(32, 16).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 1.0F, 6.0F)
                .texOffs(0, 20).addBox(-6.0F, -4.0F, -4.0F, 12.0F, 4.0F, 8.0F),
            PartPose.offset(0.0F, ModelAnvilGolem.BODY_Y, 0.0F));

        root.addOrReplaceChild("leg_front_left",
            CubeListBuilder.create().texOffs(42, 0).mirror().addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(6.0F, 19.0F, -2.0F));
        root.addOrReplaceChild("leg_front_right",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-6.0F, 19.0F, -2.0F));
        root.addOrReplaceChild("leg_back_left",
            CubeListBuilder.create().texOffs(42, 0).mirror().addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(6.0F, 19.0F, 2.0F));
        root.addOrReplaceChild("leg_back_right",
            CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 6.0F, 4.0F),
            PartPose.offset(-6.0F, 19.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean sitting = golem.isSitting();
        if (sitting) {
            limbSwingAmount = 0.0F;
        }
        this.legFrontLeft.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legFrontRight.xRot = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackLeft.xRot = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackRight.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.body.y = ModelAnvilGolem.BODY_Y + (sitting ? ModelAnvilGolem.SIT_SINK : 0.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legFrontLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legFrontRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legBackLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legBackRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
