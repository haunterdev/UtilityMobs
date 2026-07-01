package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.client.model.ModelStackGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

@SideOnly(Side.CLIENT)
public class RenderStackGolem extends RenderLiving<EntityUtilityGolem>
{
    public RenderStackGolem(RenderManager renderManager) {
        super(renderManager, new ModelStackGolem(), 0.5F);
        // The block in the HEAD equipment slot is rendered on the head bone (1.12.2 layer system
        // replaces the old renderEquippedItems / setRenderPassModel approach).
        this.addLayer(new LayerCustomHead(((ModelStackGolem)this.getMainModel()).head));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
