package toast.utilityMobs.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;

public class EntityChestGolem extends EntityContainerGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: chest.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.WOOD;
    }

    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/chestgolem.png");

    /// The angle of the lid (between 0 and 1).
    public float prevLidAngle, lidAngle;

    /// Registered entity size: 0.875 x 0.875 (set via EntityType.Builder.sized at registration).
    public EntityChestGolem(EntityType<? extends EntityChestGolem> type, Level level) {
        super(type, level);
        this.texture = EntityChestGolem.TEXTURE;
    }

    /// Called each tick this entity exists.
    @Override
    public void tick() {
        this.prevLidAngle = this.lidAngle;
        if (this.isOpen()) {
            if (this.lidAngle < 1.0F) {
                if (this.lidAngle == 0.0F) {
                    this.playSound(UMSound.CHEST_OPEN, 0.5F, this.level().random.nextFloat() * 0.1F + 0.9F);
                }
                this.lidAngle = Math.min(1.0F, this.lidAngle + 0.1F);
            }
        }
        else if (this.lidAngle > 0.0F) {
            this.lidAngle = Math.max(0.0F, this.lidAngle - 0.1F);
            if (this.lidAngle < 0.5F && this.prevLidAngle >= 0.5F) {
                this.playSound(UMSound.CHEST_CLOSE, 0.5F, this.level().random.nextFloat() * 0.1F + 0.9F);
            }
        }
        super.tick();
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }
}
