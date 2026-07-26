package toast.utilityMobs.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * The thin-limbed golem model used by the scarecrow, on a legacy 64x32 texture sheet.
 *
 * <p>1.12.2 built this by subclassing {@code ModelZombie(0.0F, true)} - the boolean only picks the
 * 32px-tall sheet, see {@link ModelGolemBiped} - and then replacing all four limbs with 2x12x2 boxes,
 * i.e. skeleton arms and legs. Those replacement boxes are byte-for-byte the same as vanilla's own
 * {@code SkeletonModel.createBodyLayer}, so the mesh below matches vanilla exactly, and the same 64x32
 * sheet size applies as it does for the biped golem.
 *
 * <p>The arm pose differs from vanilla's skeleton: 1.12.2 raised the bow arm whenever the golem held
 * a bow, with no "is aggressive" gate, so that condition is kept here.
 */
public class ModelSkeletonGolem extends HumanoidModel<EntityUtilityGolem>
{
    public ModelSkeletonGolem(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
            PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
            PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
            PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
            PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /// The arms-out pose, inherited in 1.12.2 by subclassing ModelZombie. See ModelGolemBiped.ZombieArmed
    /// for why animateZombieArms with isAggressive=false is the exact equivalent. Skipped while a bow is
    /// posed, so the raised bow arm documented above still wins.
    @Override
    public void setupAnim(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (this.rightArmPose == HumanoidModel.ArmPose.EMPTY
                && this.leftArmPose == HumanoidModel.ArmPose.EMPTY) {
            net.minecraft.client.model.AnimationUtils.animateZombieArms(
                this.leftArm, this.rightArm, false, this.attackTime, ageInTicks);
        }
    }

    /// 1.20.1's prepareMobModel is the direct replacement for 1.12.2's setLivingAnimations.
    @Override
    public void prepareMobModel(EntityUtilityGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
        ItemStack held = golem.getItemInHand(InteractionHand.MAIN_HAND);
        this.rightArmPose = held.getItem() instanceof BowItem
            ? HumanoidModel.ArmPose.BOW_AND_ARROW
            : HumanoidModel.ArmPose.EMPTY;
        super.prepareMobModel(golem, limbSwing, limbSwingAmount, partialTick);
    }
}
