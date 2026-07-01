package toast.utilityMobs.block;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;

public class EntityChestGolem extends EntityContainerGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/chestgolem.png");

    /// The angle of the lid (between 0 and 1).
    public float prevLidAngle, lidAngle;

    public EntityChestGolem(World world) {
        super(world);
        this.setSize(0.875F, 0.875F);
        this.texture = EntityChestGolem.TEXTURE;
    }

    /// Called each tick this entity exists.
    @Override
    public void onUpdate() {
        this.prevLidAngle = this.lidAngle;
        if (this.isOpen()) {
            if (this.lidAngle < 1.0F) {
                if (this.lidAngle == 0.0F) {
                    this.playSound(UMSound.CHEST_OPEN, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
                }
                this.lidAngle = Math.min(1.0F, this.lidAngle + 0.1F);
            }
        }
        else if (this.lidAngle > 0.0F) {
            this.lidAngle = Math.max(0.0F, this.lidAngle - 0.1F);
            if (this.lidAngle < 0.5F && this.prevLidAngle >= 0.5F) {
                this.playSound(UMSound.CHEST_CLOSE, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
            }
        }
        super.onUpdate();
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }
}
