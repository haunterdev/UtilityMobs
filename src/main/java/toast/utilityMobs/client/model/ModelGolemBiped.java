package toast.utilityMobs.client.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
    Plain biped golem model that knows how to aim a bow.

    RenderBiped never sets rightArmPose, so a golem rendered with a bare ModelBiped held its bow like any
    other item and never played the 1.9+ aiming animation (issue #17). isHandActive() is already synced by
    EntityLivingBase, and EntityAIWeaponAttack now draws the bow for 20 ticks before releasing, so reading
    it here is all that is needed.
*/
@SideOnly(Side.CLIENT)
public class ModelGolemBiped extends ModelBiped
{
    public ModelGolemBiped() {
        super();
    }

    public ModelGolemBiped(float modelSize) {
        super(modelSize);
    }

    /** Sets the bow-aiming arm pose from the entity's held item and draw state. Shared with ModelGolemPlayer. */
    static void applyBowPose(ModelBiped model, EntityLivingBase entity) {
        model.rightArmPose = ModelBiped.ArmPose.EMPTY;
        model.leftArmPose = ModelBiped.ArmPose.EMPTY;
        ItemStack held = entity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!held.isEmpty() && held.getItem() instanceof ItemBow && entity.isHandActive()) {
            if (entity.getPrimaryHand() == EnumHandSide.RIGHT) {
                model.rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            }
            else {
                model.leftArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            }
        }
        else if (!held.isEmpty()) {
            model.rightArmPose = ModelBiped.ArmPose.ITEM;
        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        ModelGolemBiped.applyBowPose(this, entitylivingbaseIn);
        super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
    }
}
