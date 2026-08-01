package toast.utilityMobs;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.setup.ModEntities;

/**
 * The fishing-rod hook a golem casts at its target to yank it closer. Hand-rolled projectile physics,
 * kept as-is from 1.12.2 rather than rebased onto vanilla's FishingHook (which is player-bound).
 */
public class EntityGolemFishHook extends Entity implements IEntityAdditionalSpawnData
{
    private BlockPos tilePos = new BlockPos(-1, -1, -1);
    private Block inTile;
    private boolean inGround = false;
    public int shake = 0;
    public EntityUtilityGolem angler = null;
    private int ticksInGround = 0;
    private int ticksInAir = 0;

    /// Registered entity size: 0.25 x 0.25 (set via EntityType.Builder.sized at registration).
    public EntityGolemFishHook(EntityType<? extends EntityGolemFishHook> type, Level level) {
        super(type, level);
    }

    public EntityGolemFishHook(Level level, EntityUtilityGolem golem, Entity target) {
        super(ModEntities.GOLEM_FISH_HOOK.get(), level);
        this.angler = golem;
        this.moveTo(golem.getX(), golem.getY() + golem.getEyeHeight(), golem.getZ(), golem.getYRot(), golem.getXRot());
        double x = this.getX() - Mth.cos(this.getYRot() / 180.0F * (float)Math.PI) * 0.16F;
        double y = this.getY() - 0.1;
        double z = this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float)Math.PI) * 0.16F;
        this.setPos(x, y, z);
        double mX = (target.getX() - golem.getX()) * 0.7;
        double mY = (target.getY() + target.getEyeHeight() - 0.7 - this.getY()) * 0.7;
        double mZ = (target.getZ() - golem.getZ()) * 0.7;
        this.setDeltaMovement(mX, mY, mZ);
        double vH = Math.sqrt(mX * mX + mZ * mZ);
        if (vH >= 1E-7) {
            this.setYRot((float)(Math.atan2(mZ, mX) * 180.0 / Math.PI) - 90.0F);
            this.setXRot((float)(-Math.atan2(mY, vH) * 180.0 / Math.PI));
            double dX = mX / vH;
            double dZ = mZ / vH;
            this.moveTo(golem.getX() + dX, this.getY(), golem.getZ() + dZ, this.getYRot(), this.getXRot());
            this.calculateVelocity(mX, mY + vH * 0.2, mZ, 1.0F, 14 - (this.level().getDifficulty().getId() << 2));
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    // The angler is set by the server-side constructor only. The client builds its copy through the
    // EntityType constructor, so without this it stayed null there - and the null check at the top of tick()
    // then discarded the hook on its very first client tick, which is why a golem's cast had no visible
    // bobber AND no line (RenderGolemFishHook draws the line only when angler != null).
    // getAddEntityPacket already goes through NetworkHooks, which is what carries this payload.
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.angler == null ? -1 : this.angler.getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        int anglerId = additionalData.readInt();
        Entity entity = anglerId < 0 ? null : this.level().getEntity(anglerId);
        this.angler = entity instanceof EntityUtilityGolem golem ? golem : null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        double d1 = this.getBoundingBox().getSize() * 256.0;
        return d < d1 * d1;
    }

    public void calculateVelocity(double vX, double vY, double vZ, float v, float variance) {
        double vi = Math.sqrt(vX * vX + vY * vY + vZ * vZ);
        vX /= vi;
        vY /= vi;
        vZ /= vi;
        vX += this.random.nextGaussian() * 0.0075 * variance;
        vY += this.random.nextGaussian() * 0.0075 * variance;
        vZ += this.random.nextGaussian() * 0.0075 * variance;
        vX *= v;
        vY *= v;
        vZ *= v;
        this.setDeltaMovement(vX, vY, vZ);
        double vH = Math.sqrt(vX * vX + vZ * vZ);
        this.setYRot((float)(Math.atan2(vX, vZ) * 180.0 / Math.PI));
        this.yRotO = this.getYRot();
        this.setXRot((float)(Math.atan2(vY, vH) * 180.0 / Math.PI));
        this.xRotO = this.getXRot();
        this.ticksInGround = 0;
    }

    @Override
    public void lerpMotion(double vX, double vY, double vZ) {
        this.setDeltaMovement(vX, vY, vZ);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double vH = Math.sqrt(vX * vX + vZ * vZ);
            this.setYRot((float)(Math.atan2(vX, vZ) * 180.0 / Math.PI));
            this.yRotO = this.getYRot();
            this.setXRot((float)(Math.atan2(vY, vH) * 180.0 / Math.PI));
            this.xRotO = this.getXRot();
        }
    }

    @Override
    public void tick() {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        super.tick();
        // Server-side only. The client mirrors the removal through the despawn packet; letting it decide for
        // itself meant one dropped/late spawn-data read removed the hook client-side while the server still
        // had it in flight.
        if (!this.level().isClientSide && (this.angler == null || !this.angler.isAlive() || this.distanceToSqr(this.angler) > 1024.0)) {
            this.discard();
            return;
        }
        if (this.shake > 0) {
            this.shake--;
        }
        if (this.inGround) {
            Block inBlock = this.level().getBlockState(this.tilePos).getBlock();
            if (inBlock == this.inTile) {
                this.ticksInGround++;
                if (this.ticksInGround == 1200) {
                    this.discard();
                }
                return;
            }
            this.inGround = false;
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(
                motion.x * this.random.nextFloat() * 0.2F,
                motion.y * this.random.nextFloat() * 0.2F,
                motion.z * this.random.nextFloat() * 0.2F);
            this.ticksInGround = 0;
            this.ticksInAir = 0;
        }
        else {
            this.ticksInAir++;
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 posVec = new Vec3(this.getX(), this.getY(), this.getZ());
        Vec3 motionVec = posVec.add(motion);
        HitResult object = this.level().clip(new ClipContext(posVec, motionVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (object.getType() != HitResult.Type.MISS) {
            motionVec = object.getLocation();
        }
        else {
            object = null;
        }

        if (!this.level().isClientSide) {
            Entity entityHit = null;
            List<Entity> entitiesInPath = this.level().getEntities(this, this.getBoundingBox().expandTowards(motion).inflate(1.0, 1.0, 1.0));
            double d = Double.POSITIVE_INFINITY;
            for (int i = 0; i < entitiesInPath.size(); i++) {
                Entity entityInPath = entitiesInPath.get(i);
                if (entityInPath.canBeCollidedWith() && !entityInPath.is(this.angler)) {
                    AABB aabb = entityInPath.getBoundingBox().inflate(0.3, 0.3, 0.3);
                    Optional<Vec3> object1 = aabb.clip(posVec, motionVec);
                    if (object1.isPresent()) {
                        double d1 = posVec.distanceTo(object1.get());
                        if (d1 < d) {
                            entityHit = entityInPath;
                            d = d1;
                        }
                    }
                }
            }
            if (entityHit != null) {
                object = new EntityHitResult(entityHit);
            }
        }
        if (object != null) {
            this.onImpact(object);
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        float var16 = (float)Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float)(Math.atan2(motion.x, motion.z) * 180.0 / Math.PI));
        for (this.setXRot((float)(Math.atan2(motion.y, var16) * 180.0 / Math.PI)); this.getXRot() - this.xRotO < -180.0F; this.xRotO -= 360.0F) {
            // Do nothing
        }
        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        this.setXRot(this.xRotO + (this.getXRot() - this.xRotO) * 0.2F);
        this.setYRot(this.yRotO + (this.getYRot() - this.yRotO) * 0.2F);
        if (this.isInWater()) {
            this.discard();
        }
        motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.99, motion.y * 0.99 - this.getGravityVelocity(), motion.z * 0.99);
        this.setPos(this.getX(), this.getY(), this.getZ());
    }

    public void onImpact(HitResult object) {
        if (object instanceof EntityHitResult hit && this.angler != null) {
            Entity entityHit = hit.getEntity();
            // Reeling something in makes the bobber-retrieve sound, matching a player's fishing rod and
            // SpecialMobs' fishing zombie. There was no sound at all on a hit before (1.12.2 issue #1.10).
            this.playSound(net.minecraft.sounds.SoundEvents.FISHING_BOBBER_RETRIEVE, 0.5F,
                0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
            double vX = this.angler.getX() - this.getX();
            double vY = this.angler.getY() - this.getY();
            double vZ = this.angler.getZ() - this.getZ();
            double v = Math.sqrt(vX * vX + vY * vY + vZ * vZ);
            double mult = 0.31;
            entityHit.setDeltaMovement(vX * mult, vY * mult + Math.sqrt(v) * 0.1, vZ * mult);
            entityHit.setOnGround(false);
            if (entityHit instanceof ServerPlayer serverPlayer) {
                // Players ignore server-side motion changes unless told explicitly.
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entityHit));
            }
        }
        this.discard();
    }

    protected float getGravityVelocity() {
        return 0.03F;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.angler != null) {
            this.angler.setFishingRod(true);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("xTile", this.tilePos.getX());
        tag.putInt("yTile", this.tilePos.getY());
        tag.putInt("zTile", this.tilePos.getZ());
        // 1.12.2 stored the block as a numeric id byte. Numeric block ids are gone in 1.20.1, so the
        // registry name is stored instead.
        tag.putString("inTile", BuiltInRegistries.BLOCK.getKey(this.inTile == null ? Blocks.AIR : this.inTile).toString());
        tag.putByte("shake", (byte)this.shake);
        tag.putBoolean("inGround", this.inGround);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.tilePos = new BlockPos(tag.getInt("xTile"), tag.getInt("yTile"), tag.getInt("zTile"));
        this.inTile = BuiltInRegistries.BLOCK.get(new ResourceLocation(tag.getString("inTile")));
        this.shake = tag.getByte("shake") & 0xff;
        this.inGround = tag.getBoolean("inGround");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
