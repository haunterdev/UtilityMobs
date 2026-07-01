package toast.utilityMobs.client.renderer;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.golem.EntityUtilityGolem;

@SideOnly(Side.CLIENT)
public class RenderGolem extends RenderBiped<EntityUtilityGolem>
{
    public RenderGolem(RenderManager renderManager) {
        this(renderManager, new ModelBiped());
    }

    public RenderGolem(RenderManager renderManager, ModelBiped model) {
        super(renderManager, model, 0.5F);
        // 1.12.2 RenderBiped only adds the held-item layer; armor must be added here.
        // Faithful to 1.7.10 RenderGolem.func_82421_b(): zombie/skeleton models use zombie armor models.
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                if (RenderGolem.this.getMainModel() instanceof ModelZombie || RenderGolem.this.getMainModel() instanceof ModelSkeleton) {
                    this.modelLeggings = new ModelZombie(0.5F, false);
                    this.modelArmor = new ModelZombie(1.0F, false);
                }
                else {
                    this.modelLeggings = new ModelBiped(0.5F);
                    this.modelArmor = new ModelBiped(1.0F);
                }
            }
        });
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUtilityGolem entity) {
        return entity.getTexture();
    }
}
