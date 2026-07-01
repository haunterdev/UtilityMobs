package toast.utilityMobs.turret;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityBrickTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/brickturret.png");

    {
        this.maxAttackTime = 35;
    }

    public EntityBrickTurret(World world) {
        super(world);
        this.texture = EntityBrickTurret.TEXTURE;
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.STONEBRICK);
    }

    @Override
    public double getProjectileDamage() { return 1.0; }
}
