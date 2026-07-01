package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.client.model.ModelChestGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;

@SideOnly(Side.CLIENT)
public class RenderChestGolem extends RenderLiving<EntityUtilityGolem>
{
    public RenderChestGolem(RenderManager renderManager) {
        super(renderManager, new ModelChestGolem(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
