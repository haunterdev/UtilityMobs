package toast.utilityMobs.client.renderer;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.client.model.ModelTurret;
import toast.utilityMobs.golem.EntityUtilityGolem;

@SideOnly(Side.CLIENT)
public class RenderTurret extends RenderLiving<EntityUtilityGolem>
{
    public RenderTurret(RenderManager renderManager) {
        super(renderManager, new ModelTurret(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
