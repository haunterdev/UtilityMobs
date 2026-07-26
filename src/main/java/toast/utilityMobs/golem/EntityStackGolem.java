package toast.utilityMobs.golem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public class EntityStackGolem extends EntityUtilityGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: snow stack.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.SNOW;
    }

    public EntityStackGolem(EntityType<? extends EntityStackGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes();
    }
}
