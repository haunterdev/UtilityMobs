package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import toast.utilityMobs.golem.EntityUtilityGolem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelBlockGolem extends ModelBase
{
    public ModelRenderer body;
    public ModelRenderer legFrontLeft;
    public ModelRenderer legFrontRight;
    public ModelRenderer legBackLeft;
    public ModelRenderer legBackRight;

    public ModelBlockGolem() {
        this.body = new ModelRenderer(this).addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16);
        this.body.setRotationPoint(0.0F, 20.0F, 0.0F);

        this.legFrontLeft = new ModelRenderer(this).addBox(-2.0F, -1.0F, -4.0F, 4, 6, 4);
        this.legFrontLeft.setRotationPoint(8.0F, 19.0F, -6.0F);
        this.legFrontLeft.mirror = true;

        this.legFrontRight = new ModelRenderer(this).addBox(-2.0F, -1.0F, -4.0F, 4, 6, 4);
        this.legFrontRight.setRotationPoint(-8.0F, 19.0F, -6.0F);

        this.legBackLeft = new ModelRenderer(this).addBox(-2.0F, -1.0F, 0.0F, 4, 6, 4);
        this.legBackLeft.setRotationPoint(8.0F, 19.0F, 6.0F);
        this.legBackLeft.mirror = true;

        this.legBackRight = new ModelRenderer(this).addBox(-2.0F, -1.0F, 0.0F, 4, 6, 4);
        this.legBackRight.setRotationPoint(-8.0F, 19.0F, 6.0F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        EntityUtilityGolem golem = (EntityUtilityGolem)entityIn;
        if (golem.isSitting()) {
            limbSwingAmount = 0.0F;
        }
        this.legFrontLeft.rotateAngleX = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legFrontRight.rotateAngleX = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackLeft.rotateAngleX = (float)Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        this.legBackRight.rotateAngleX = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, golem.isSitting() ? 0.25F : 0.0F, 0.0F);
        this.body.render(scale);
        GlStateManager.popMatrix();
        this.legFrontLeft.render(scale);
        this.legFrontRight.render(scale);
        this.legBackLeft.render(scale);
        this.legBackRight.render(scale);
    }
}
