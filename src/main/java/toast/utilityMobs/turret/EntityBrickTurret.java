package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class EntityBrickTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/brickturret.png");

    public EntityBrickTurret(EntityType<? extends EntityBrickTurret> type, Level level) {
        super(type, level);
        this.maxAttackTime = 35;
        this.texture = EntityBrickTurret.TEXTURE;
    }

    @Override
    protected Item getDropItem() {
        return Items.STONE_BRICKS;
    }

    @Override
    public double getProjectileDamage() { return 1.0; }
}
