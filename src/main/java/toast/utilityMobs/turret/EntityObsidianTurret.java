package toast.utilityMobs.turret;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class EntityObsidianTurret extends EntityTurretGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation("utilitymobs:textures/models/turret/obsidianturret.png");

    public EntityObsidianTurret(EntityType<? extends EntityObsidianTurret> type, Level level) {
        super(type, level);
        this.texture = EntityObsidianTurret.TEXTURE;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTurretGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0);
    }

    @Override
    public int getArmorValue() {
        return 20;
    }
}
