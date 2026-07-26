package toast.utilityMobs.turret;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import toast.utilityMobs.setup.ModEntities;

/**
    A tipped-arrow fired by a turret. Identical to {@link Arrow} in every way except that
    it puffs into a small cloud of particles when it is removed, instead of popping out of existence.
    Covers the two cases where turret arrows otherwise vanish without feedback: timing out after
    sticking in the ground (the 1200-tick in-ground despawn), and the server destroying the entity
    once it leaves the tracker. The poof is client-only and fires exactly once (guarded by isRemoved).
 */
public class EntityTurretArrow extends Arrow
{
    public EntityTurretArrow(EntityType<? extends EntityTurretArrow> type, Level level) {
        super(type, level);
    }

    /// Mirrors AbstractArrow(EntityType, LivingEntity, Level): Arrow itself offers no shooter ctor
    /// that takes a custom entity type, so the eye-height spawn and owner assignment are done here.
    public EntityTurretArrow(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_ARROW.get(), level);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.setOwner(shooter);
        if (shooter instanceof Player) {
            this.pickup = Arrow.Pickup.ALLOWED;
        }
    }

    public EntityTurretArrow(Level level, double x, double y, double z) {
        super(ModEntities.TURRET_ARROW.get(), level);
        this.setPos(x, y, z);
    }

    /// onClientRemoval, not remove: 1.12.2 hooked setDead, which every despawn path went through. In
    /// 1.20.1 remove(RemovalReason) is only one of the ways an entity goes away, and it is not the one
    /// the client uses - ClientLevel.removeEntity calls the final setRemoved and then this hook, never
    /// remove() - so hooking remove() meant the poof never played for the ordinary despawn. There is no
    /// isRemoved() guard because this fires exactly once, after removal, so the flag is already set.
    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        for (int i = 0; i < 5; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.35;
            double oy = this.random.nextDouble() * 0.2;
            double oz = (this.random.nextDouble() - 0.5) * 0.35;
            this.level().addParticle(ParticleTypes.CRIT, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0.0, 0.04, 0.0);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
