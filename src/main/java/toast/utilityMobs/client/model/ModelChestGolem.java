package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityChestTrappedGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelChestGolem extends ModelBase
{
    public ModelRenderer legFrontLeft;
    public ModelRenderer legFrontRight;
    public ModelRenderer legBackLeft;
    public ModelRenderer legBackRight;
    public ModelRenderer teethBottom;
    public ModelRenderer top;
    public ModelRenderer bottom;

    public ModelChestGolem() {
        this.textureHeight = 64;
        this.setTextureOffset("top.head", 0, 0);
        this.setTextureOffset("top.nose", 0, 0);
        this.setTextureOffset("top.teethTop", 0, 43);

        this.top = new ModelRenderer(this, "top")
        .addBox("head", -7.0F, -5.0F, -14.0F, 14, 5, 14)
        .addBox("nose", -1.0F, -2.0F, -15.0F, 2, 4, 1)
        .addBox("teethTop", -6.0F, 0.0F, -13.0F, 12, 1, 12);
        this.top.setRotationPoint(0.0F, 11.0F, 7.0F);

        this.bottom = new ModelRenderer(this, 0, 19).addBox(-7.0F, 0.0F, -14.0F, 14, 10, 14);
        this.bottom.setRotationPoint(0.0F, 10.0F, 7.0F);

        this.teethBottom = new ModelRenderer(this, 0, 43).addBox(-6.0F, 0.0F, -13.0F, 12, 1, 12);
        this.teethBottom.setRotationPoint(0.0F, 10.0F, 7.0F);
        this.teethBottom.rotateAngleZ = (float)Math.PI;
        this.teethBottom.mirror = true;

        this.legFrontLeft = new ModelRenderer(this, 42, 0).addBox(-2.0F, -1.0F, -4.0F, 4, 6, 4);
        this.legFrontLeft.setRotationPoint(7.0F, 19.0F, -5.0F);
        this.legFrontLeft.mirror = true;

        this.legFrontRight = new ModelRenderer(this, 42, 0).addBox(-2.0F, -1.0F, -4.0F, 4, 6, 4);
        this.legFrontRight.setRotationPoint(-7.0F, 19.0F, -5.0F);

        this.legBackLeft = new ModelRenderer(this, 42, 0).addBox(-2.0F, -1.0F, 0.0F, 4, 6, 4);
        this.legBackLeft.setRotationPoint(7.0F, 19.0F, 5.0F);
        this.legBackLeft.mirror = true;

        this.legBackRight = new ModelRenderer(this, 42, 0).addBox(-2.0F, -1.0F, 0.0F, 4, 6, 4);
        this.legBackRight.setRotationPoint(-7.0F, 19.0F, 5.0F);
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
        this.top.render(scale);
        this.bottom.render(scale);
        this.teethBottom.render(scale);
        GlStateManager.popMatrix();
        if (!golem.isSitting() || !(golem instanceof EntityChestTrappedGolem)) {
            this.legFrontLeft.render(scale);
            this.legFrontRight.render(scale);
            this.legBackLeft.render(scale);
            this.legBackRight.render(scale);
        }
    }

    // Used for easily adding entity-dependent animations.
    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        EntityChestGolem golem = (EntityChestGolem)entitylivingbaseIn;
        float angle = 1.0F - golem.prevLidAngle - (golem.lidAngle - golem.prevLidAngle) * partialTickTime;
        angle = 1.0F - angle * angle * angle;
        this.top.rotateAngleX = -angle * (float)Math.PI / 2.0F;
    }
}
