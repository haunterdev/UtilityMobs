package toast.utilityMobs.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class ModelTurret extends EntityModel<EntityUtilityGolem>
{
    public final ModelPart head;
    public final ModelPart leg;
    public final ModelPart foot;

    public ModelTurret(ModelPart root) {
        this.head = root.getChild("head");
        this.leg = root.getChild("leg");
        this.foot = root.getChild("foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 8).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F),
            PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("leg",
            CubeListBuilder.create().texOffs(56, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
            PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("foot",
            CubeListBuilder.create().texOffs(36, 0).addBox(-6.0F, -6.0F, -2.0F, 12.0F, 12.0F, 2.0F),
            PartPose.offset(0.0F, 22.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EntityUtilityGolem entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw / (180.0F / (float)Math.PI);
        this.head.xRot = headPitch / (180.0F / (float)Math.PI);
        // Rotate the base plate to follow the body yaw (as the 1.7.10 original did). Without this the
        // foot stays frozen while the body tracks the target, so the turret looks like it isn't aiming
        // until it fires. Restores the whole turret visibly turning toward its target.
        this.foot.yRot = entity.yBodyRot / (180.0F / (float)Math.PI);
        this.foot.xRot = 1.570796F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.leg.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.foot.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
