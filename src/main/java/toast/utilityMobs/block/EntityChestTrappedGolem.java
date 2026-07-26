package toast.utilityMobs.block;

import java.util.EnumSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import toast.utilityMobs._UtilityMobs;

public class EntityChestTrappedGolem extends EntityChestGolem
{
    /// The texture for this class.
    @SuppressWarnings("hiding")
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/chesttrappedgolem.png");

    public EntityChestTrappedGolem(EntityType<? extends EntityChestTrappedGolem> type, Level level) {
        super(type, level);
        this.texture = EntityChestTrappedGolem.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 1.12.2 setMutexBits(7) = MOVE | LOOK | JUMP.
        this.sitAI().setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        this.sitAI().sitAnywhere = true;
    }

    @Override
    protected Item getDropItem() {
        return Items.TRAPPED_CHEST;
    }

    /// Locks this entity's yaw to a cardinal direction.
    public void snapRotationYaw() {
        float yaw = this.getYRot() % 360.0F;
        if (yaw < -135.0F || yaw >= 135.0F) {
            yaw = 180.0F;
        }
        else if (yaw < -45.0F) {
            yaw = -90.0F;
        }
        else if (yaw < 45.0F) {
            yaw = 0.0F;
        }
        else {
            yaw = 90.0F;
        }
        this.setYRot(yaw);
    }

    /// Sets the isSitting variable.
    @Override
    public void setSitting(boolean sitting) {
        super.setSitting(sitting);
        if (sitting) {
            this.setPos(Math.floor(this.getX()) + 0.5, Math.ceil(this.getY()), Math.floor(this.getZ()) + 0.5);
            this.snapRotationYaw();
            this.setXRot(0.0F);
        }
    }

    /// Moves this entity. A sitting trapped chest golem is anchored in place.
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (!this.isSitting()) {
            super.move(type, movement);
        }
        else {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, 0.0, motion.z);
        }
    }
}
