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
import toast.utilityMobs.colossal.EntityColossalGolem;

/**
 * The colossal golem model: a huge two-legged, two-armed golem. 128x128 sheet.
 *
 * <p>The 1.12.2 model was a flat set of parts (no hierarchy), animated by resetting the four arm
 * parts' rotation points every frame and then layering a swing animation on top. That is preserved
 * here: setupAnim resets the arm part positions and rotations before applying the walk sway and the
 * arm-swing attack pose, since a baked ModelPart keeps its mutated state between frames.
 */
public class ModelColossalGolem extends EntityModel<EntityColossalGolem>
{
    private static final float TO_RADS = (float)Math.PI / 180.0F;

    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart lower;
    public final ModelPart armRight;
    public final ModelPart forearmRight;
    public final ModelPart armLeft;
    public final ModelPart forearmLeft;
    public final ModelPart legRight;
    public final ModelPart lowerLegRight;
    public final ModelPart legLeft;
    public final ModelPart lowerLegLeft;

    public ModelColossalGolem(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.lower = root.getChild("lower");
        this.armRight = root.getChild("arm_right");
        this.forearmRight = root.getChild("forearm_right");
        this.armLeft = root.getChild("arm_left");
        this.forearmLeft = root.getChild("forearm_left");
        this.legRight = root.getChild("leg_right");
        this.lowerLegRight = root.getChild("lower_leg_right");
        this.legLeft = root.getChild("leg_left");
        this.lowerLegLeft = root.getChild("lower_leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -8.0F, -10.5F, 10.0F, 12.0F, 10.0F)
                .texOffs(30, 0).addBox(-1.5F, 0.0F, -13.5F, 3.0F, 5.0F, 3.0F),
            PartPose.offset(0.0F, -9.0F, -5.0F));

        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 22).addBox(-12.0F, -4.0F, -8.0F, 24.0F, 18.0F, 16.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));
        root.addOrReplaceChild("lower",
            CubeListBuilder.create().texOffs(4, 56).addBox(-6.0F, 8.0F, 3.0F, 12.0F, 11.0F, 10.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));

        root.addOrReplaceChild("arm_right",
            CubeListBuilder.create().texOffs(56, 56).addBox(-16.0F, -4.5F, -3.0F, 4.0F, 18.0F, 6.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));
        root.addOrReplaceChild("forearm_right",
            CubeListBuilder.create().texOffs(52, 80).addBox(-17.0F, 10.0F, -1.0F, 6.0F, 18.0F, 8.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));

        root.addOrReplaceChild("arm_left",
            CubeListBuilder.create().texOffs(84, 56).addBox(12.0F, -4.5F, -3.0F, 4.0F, 18.0F, 6.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));
        root.addOrReplaceChild("forearm_left",
            CubeListBuilder.create().texOffs(80, 80).addBox(11.0F, 10.0F, -1.0F, 6.0F, 18.0F, 8.0F),
            PartPose.offset(0.0F, -9.0F, -2.0F));

        root.addOrReplaceChild("leg_right",
            CubeListBuilder.create().texOffs(2, 77).addBox(-3.5F, -5.0F, -3.0F, 6.0F, 12.0F, 5.0F),
            PartPose.offset(-5.0F, 9.0F, 8.0F));
        root.addOrReplaceChild("lower_leg_right",
            CubeListBuilder.create().texOffs(0, 94).addBox(-4.0F, 2.0F, -7.0F, 7.0F, 13.0F, 6.0F),
            PartPose.offset(-5.0F, 9.0F, 8.0F));

        root.addOrReplaceChild("leg_left",
            CubeListBuilder.create().texOffs(28, 77).addBox(-2.5F, -5.0F, -3.0F, 6.0F, 12.0F, 5.0F),
            PartPose.offset(5.0F, 9.0F, 8.0F));
        root.addOrReplaceChild("lower_leg_left",
            CubeListBuilder.create().texOffs(26, 94).addBox(-3.0F, 2.0F, -7.0F, 7.0F, 13.0F, 6.0F),
            PartPose.offset(5.0F, 9.0F, 8.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(EntityColossalGolem golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.animate(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.lower.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.armRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.forearmRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.armLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.forearmLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.lowerLegRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.legLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.lowerLegLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    private float adjust(float f, float f1) {
        return (Math.abs(f % f1 - f1 * 0.5F) - f1 * 0.25F) / (f1 * 0.25F);
    }

    private void setAngles(EntityColossalGolem golem, float time, float moveSpeed, float rotationFloat, float rotationYaw, float rotationPitch) {
        this.head.yRot = this.rad(rotationYaw);
        this.head.xRot = this.rad(rotationPitch);

        this.body.xRot = this.rad(48.0F);
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.lower.xRot = this.rad(12.0F);

        this.setPos(this.armRight, 0.0F, -9.0F, -2.0F);
        this.armRight.xRot = this.rad(12.0F) - 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.armRight.yRot = 0.0F;
        this.armRight.zRot = this.rad(6.0F);
        this.setPos(this.forearmRight, 0.0F, -9.0F, -2.0F);
        this.forearmRight.xRot = this.rad(-5.0F) - 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.forearmRight.yRot = 0.0F;
        this.forearmRight.zRot = this.rad(6.0F);

        this.setPos(this.armLeft, 0.0F, -9.0F, -2.0F);
        this.armLeft.xRot = this.rad(12.0F) + 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.armLeft.yRot = 0.0F;
        this.armLeft.zRot = this.rad(-6.0F);
        this.setPos(this.forearmLeft, 0.0F, -9.0F, -2.0F);
        this.forearmLeft.xRot = this.rad(-5.0F) + 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.forearmLeft.yRot = 0.0F;
        this.forearmLeft.zRot = this.rad(-6.0F);

        this.legLeft.xRot = this.rad(-43.0F) - 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.lowerLegLeft.xRot = -1.5F * this.adjust(time, 13.0F) * moveSpeed;

        this.legRight.xRot = this.rad(-43.0F) + 1.5F * this.adjust(time, 13.0F) * moveSpeed;
        this.lowerLegRight.xRot = 1.5F * this.adjust(time, 13.0F) * moveSpeed;
    }

    public void animate(EntityColossalGolem golem, float time, float moveSpeed, float rotationFloat, float rotationYaw, float rotationPitch) {
        if (golem.isSitting()) {
            moveSpeed = 0.0F;
        }
        this.setAngles(golem, time, moveSpeed, rotationFloat, rotationYaw, rotationPitch);

        int anim = golem.getAnimId();
        if (anim == 0)
            return;

        int tick = golem.getAnimTick();
        float progress = 0.0F;
        if (tick <= 6) {
            progress = tick / 6.0F;
        }
        else if (tick <= 16) {
            progress = 1.0F - (tick - 6) / 10.0F;
        }

        if (anim == EntityColossalGolem.ANIM_R_ARM_SWING) {
            this.rotate(this.body, progress, 0.0F, -16.0F, -16.0F);

            this.rotate(this.armRight, progress, -140.0F, -16.0F, -16.0F);
            this.rotate(this.forearmRight, progress, -140.0F, -20.0F, -16.0F);

            this.rotate(this.armLeft, progress, 0.0F, -16.0F, -29.0F);
            this.rotate(this.forearmLeft, progress, 0.0F, -16.0F, -16.0F);
            this.move(this.forearmLeft, progress, 0.0F, -5.0F, 0.0F);
        }
        else if (anim == EntityColossalGolem.ANIM_L_ARM_SWING) {
            this.rotate(this.body, progress, 0.0F, 16.0F, 16.0F);

            this.rotate(this.armLeft, progress, -140.0F, 16.0F, -16.0F);
            this.rotate(this.forearmLeft, progress, -140.0F, 20.0F, -16.0F);

            this.rotate(this.armRight, progress, 0.0F, 16.0F, 29.0F);
            this.rotate(this.forearmRight, progress, 0.0F, 16.0F, 16.0F);
            this.move(this.forearmRight, progress, 0.0F, -5.0F, 0.0F);
        }
    }

    private void rotate(ModelPart box, float progress, float x, float y, float z) {
        if (x != 0.0F) {
            box.xRot += this.rad(x) * progress;
        }
        if (y != 0.0F) {
            box.yRot += this.rad(y) * progress;
        }
        if (z != 0.0F) {
            box.zRot += this.rad(z) * progress;
        }
    }

    private void move(ModelPart box, float progress, float x, float y, float z) {
        if (x != box.x) {
            box.x += x * progress;
        }
        if (y != box.y) {
            box.y += y * progress;
        }
        if (z != box.z) {
            box.z += z * progress;
        }
    }

    private void setPos(ModelPart box, float x, float y, float z) {
        box.x = x;
        box.y = y;
        box.z = z;
    }

    private float rad(float deg) {
        return deg * ModelColossalGolem.TO_RADS;
    }
}
