package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.entity.Entity;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class ModelStackGolem extends ModelSnowMan {

    // Sets the model's various rotation angles.
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        EntityUtilityGolem golem = (EntityUtilityGolem) entityIn;
        if (golem.isSitting()) {
            this.rightHand.rotateAngleZ = 0.0F;
            this.leftHand.rotateAngleZ = 0.0F;
        }
    }
}
