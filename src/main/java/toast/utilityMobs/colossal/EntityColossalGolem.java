package toast.utilityMobs.colossal;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityColossalGolem extends EntityUtilityGolem
{
    public static final int ANIM_R_ARM_SWING = 1;
    public static final int ANIM_L_ARM_SWING = 2;
    public static boolean wanderWhileRidden = false;

    /// animId; The animation currently being played. 0 is no animation.
    private static final EntityDataAccessor<Byte> ANIM_ID = SynchedEntityData.defineId(EntityColossalGolem.class, EntityDataSerializers.BYTE);

    private int lastAnimId;
    private int animTick;

    /// Registered entity size: 1.8 x 3.2 (set via EntityType.Builder.sized at registration).
    public EntityColossalGolem(EntityType<? extends EntityColossalGolem> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // longMemory = true (third arg): keep chasing the target even when a path can't be found this tick, so an
        // aggro'd colossus (e.g. /umsummon ... hostile) relentlessly pursues a fleeing player instead of
        // giving up the moment the navigator clears.
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Used to initialize dataManager variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_ID, Byte.valueOf((byte)0));
    }

    /// Colossi have their own targeting toggles (colossals.attack_hostiles / colossals.attack_passives /
    /// colossals.attack_neutrals) separate from worker golems. Enemy-team colossi bypass this gate via
    /// EntityUtilityGolem.canAttack.
    @Override
    protected boolean passesTargetFilter(Entity target) {
        if (target instanceof LivingEntity && !(target instanceof Player)) {
            if (toast.utilityMobs.TargetHelper.isNeutralMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_neutrals")) return false;
            } else if (toast.utilityMobs.TargetHelper.isHostileMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_hostiles")) return false;
            } else {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_passives")) return false;
            }
        }
        return true;
    }

    /// Base attributes. Leaf classes override MAX_HEALTH / MOVEMENT_SPEED / ATTACK_DAMAGE.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /// Gets/sets this colossus's animation variable. Used for rendering.
    public int getAnimId() {
        return this.entityData.get(ANIM_ID).byteValue();
    }
    public void setAnimId(int id) {
        this.entityData.set(ANIM_ID, Byte.valueOf((byte)id));
    }

    public int getAnimTick() {
        return this.animTick;
    }

    // Designates the first passenger as the one that controls movement.
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    /// Solid, i.e. standable and blocking. 1.12.2 did this with a getCollisionBoundingBox override
    /// gated on proxy.solidEntities(), which was only ever true on an integrated server because
    /// 1.12.2's entity solidity desynced in multiplayer. 1.20.1 supports canBeCollidedWith on both
    /// sides, so the gate is dropped: colossi are now solid on dedicated servers too, which 1.12.2
    /// never managed. Without this override a colossus has no collision at all and cannot be stood on.
    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    /// A colossus never reports itself in water, so the float goal and water physics leave it alone and it
    /// walks along the bottom (1.12.2 handleWaterMovement returned false unconditionally).
    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        int animId = this.getAnimId();
        if (this.lastAnimId != animId) {
            this.lastAnimId = animId;
            this.animTick = 0;
        }
        if (animId != 0) {
            this.animTick++;
            if (!this.level().isClientSide && this.animTick > 16) {
                this.setAnimId(0);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getAnimTick() == 2) {
            Vec3 lookVec = this.getLookAngle();
            AABB box = this.getBoundingBox().move(lookVec.x * this.getBbWidth(), 0.0, lookVec.z * this.getBbWidth()).inflate(0.5, 2.0, 0.5);
            List<Entity> list = this.level().getEntities(this, box);
            double reach = this.getBbWidth() * this.getBbWidth() * 4.0F + this.getBbWidth();
            for (Entity entity : list) {
                if (entity != this.getControllingPassenger() && entity instanceof LivingEntity && this.canDamage(entity)) {
                    if (this.distanceToSqr(entity.getX(), entity.getBoundingBox().minY, entity.getZ()) <= reach) {
                        this.attackEntityAsMobFinish(entity);
                    }
                }
            }
        }
        Vec3 motion = this.getDeltaMovement();
        this.setSprinting(motion.x * motion.x + motion.z * motion.z > 2.5E-007);
    }

    // Checks to see if the golem can damage the passed entity.
    private boolean canDamage(Entity entity) {
        if (!this.isVehicle() || entity instanceof OwnableEntity || entity instanceof Player)
            return this.targetHelper.isValidTarget(entity);
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!this.level().isClientSide && this.getAnimId() == 0) {
            this.setAnimId(this.random.nextBoolean() ? EntityColossalGolem.ANIM_L_ARM_SWING : EntityColossalGolem.ANIM_R_ARM_SWING);
        }
        return true;
    }

    // The actual method that causes damage.
    public boolean attackEntityAsMobFinish(Entity entity) {
        this.level().broadcastEntityEvent(this, (byte)4);
        UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        double dX = entity.getX() - this.getX();
        double dZ = entity.getZ() - this.getZ();
        double dH = Math.sqrt(dX * dX + dZ * dZ);
        Vec3 motion = this.getDeltaMovement();
        entity.setDeltaMovement(dX / dH * 0.5 + motion.x * 1.2, 0.8, dZ / dH * 0.5 + motion.z * 1.2);
        if (entity instanceof ServerPlayer serverPlayer) {
            try {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entity));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        }
        else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        UMSound.playAt(this, UMSound.IRONGOLEM_WALK, 1.0F, 1.0F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Only the owner may mount a colossus, and only in mid-air - jump, then right-click. The
        // air-only rule is what keeps mounting from stealing the ground-level heal/shear interactions.
        if (this.canInteract(player) && !player.isShiftKeyDown()
                && this.getOwnerName().equals(player.getScoreboardName())) {
            if (!player.onGround()) {
                if (!this.isVehicle()) {
                    player.startRiding(this);
                }
                else {
                    // Occupied by a golem passenger: bump it off and drop it where the player is.
                    Entity rider = this.getControllingPassenger();
                    if (rider != null && !(rider instanceof Player)) {
                        rider.stopRiding();
                        rider.setPos(player.getX(), player.getY(), player.getZ());
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Rider-driven movement.
     *
     * <p>1.12.2 did all of this in one travel(strafe, vertical, forward) override that read
     * moveStrafing/moveForward off the rider. 1.20.1 splits the same work across three hooks that
     * LivingEntity calls in order - getRiddenInput for the movement vector, tickRidden for everything
     * else per tick, getRiddenSpeed for the speed - and only enters them when the controlling passenger
     * is a player, which is exactly the branch the old override tested for first. The limb-swing
     * bookkeeping the old code did by hand is gone because travel() now calls calculateEntityAnimation.
     *
     * <p>Two gates carry over unchanged. A rider the colossus considers a valid target never gets
     * control, so you cannot ride a colossus that wants to kill you. And with
     * colossals.wander_while_ridden set, a rider giving no input lets the colossus keep wandering on its
     * own AI instead of standing still - it only takes the reins once the rider actually steers.
     */
    private boolean allowsRiderControl(Player rider) {
        return !this.targetHelper.isValidTarget(rider);
    }

    private boolean riderIsSteering(Player rider) {
        if (!this.allowsRiderControl(rider))
            return false;
        boolean manualControl = Math.abs(rider.xxa) > 0.01F || Math.abs(rider.zza) > 0.01F;
        return manualControl || !EntityColossalGolem.wanderWhileRidden;
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
        if (!this.riderIsSteering(rider))
            return travelVector;
        float strafe = rider.xxa * 0.15F;
        float forward = rider.zza * 0.3F;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected void tickRidden(Player rider, Vec3 travelVector) {
        super.tickRidden(rider, travelVector);
        if (!this.riderIsSteering(rider))
            return;
        this.setRot(rider.getYRot(), rider.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        // 1.12.2 called setAIMoveSpeed(MOVEMENT_SPEED) for any player rider it accepted, steering or
        // not; a rider it rejected fell through to plain travel() on whatever speed the AI had set.
        return this.allowsRiderControl(rider)
            ? (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED)
            : super.getRiddenSpeed(rider);
    }

    // Returns the Y offset from the entity's position for any entity riding this one.
    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            if (this.random.nextFloat() < dropChance / 4.0F) {
                // 1.12.2 dropped Items.SKULL meta 4 (the creeper head), the colossal "base" skull.
                this.spawnAtLocation(new ItemStack(Items.CREEPER_HEAD), 0.0F);
            }
        }
    }
}
