package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class EntityStoneTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/stoneturret.png");

    public EntityStoneTurret(EntityType<? extends EntityStoneTurret> type, Level level) {
        super(type, level);
        this.texture = EntityStoneTurret.TEXTURE;
    }

    @Override
    protected Item getDropItem() {
        return Items.COBBLESTONE;
    }
}
