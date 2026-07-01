package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelTurret extends ModelBase
{
    public ModelRenderer head;
    public ModelRenderer leg;
    public ModelRenderer foot;

    public ModelTurret() {
        this.textureWidth = 64;
        this.textureHeight = 32;
        this.head = new ModelRenderer(this, 0, 8);
        this.head.addBox(-6.0F, -12.0F, -6.0F, 12, 12, 12);
        this.head.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.leg = new ModelRenderer(this, 56, 12);
        this.leg.addBox(-1.0F, 0.0F, -1.0F, 2, 18, 2);
        this.leg.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.foot = new ModelRenderer(this, 36, 0);
        this.foot.addBox(-6.0F, -6.0F, -2.0F, 12, 12, 2);
        this.foot.setRotationPoint(0.0F, 22.0F, 0.0F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        this.head.rotateAngleY = netHeadYaw / (180.0F / (float)Math.PI);
        this.head.rotateAngleX = headPitch / (180.0F / (float)Math.PI);
        // Rotate the base plate to follow the body yaw (as the 1.7.10 original did). Without this the
        // foot stays frozen while the body tracks the target, so the turret looks like it isn't aiming
        // until it fires. Restores the whole turret visibly turning toward its target.
        this.foot.rotateAngleY = ((net.minecraft.entity.EntityLiving) entityIn).renderYawOffset / (180.0F / (float)Math.PI);
        this.foot.rotateAngleX = 1.570796F;
        this.head.render(scale);
        this.leg.render(scale);
        this.foot.render(scale);
    }
}
