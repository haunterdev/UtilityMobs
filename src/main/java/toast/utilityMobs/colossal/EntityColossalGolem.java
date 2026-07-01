package toast.utilityMobs.colossal;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityColossalGolem extends EntityUtilityGolem
{
    public static final int ANIM_R_ARM_SWING = 1;
    public static final int ANIM_L_ARM_SWING = 2;
    public static boolean wanderWhileRidden = false;

    /// animId; The animation currently being played. 0 is no animation.
    private static final DataParameter<Byte> ANIM_ID = EntityDataManager.createKey(EntityColossalGolem.class, DataSerializers.BYTE);

    private int lastAnimId;
    private int animTick;

    public EntityColossalGolem(World world) {
        super(world);
        this.setSize(1.8F, 3.2F);
        this.stepHeight = 1.0F;

        // longMemory = true: keep chasing the target even when a path can't be found this tick, so an
        // aggro'd colossus (e.g. /umsummon ... hostile) relentlessly pursues a fleeing player instead of
        // giving up the moment the navigator clears.
        this.tasks.addTask(1, new EntityAIAttackMelee(this, 1.0, true));
        this.targetTasks.addTask(1, new EntityAIGolemTarget(this));
    }

    /// Used to initialize dataManager variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ANIM_ID, Byte.valueOf((byte)0));
    }

    /// Colossi have their own targeting toggles (colossals.attack_hostiles / colossals.attack_passives /
    /// colossals.attack_neutrals) separate from worker golems. Enemy-team colossi bypass this gate via
    /// EntityUtilityGolem.canAttack.
    @Override
    protected boolean passesTargetFilter(net.minecraft.entity.Entity target) {
        if (target instanceof net.minecraft.entity.EntityLivingBase && !(target instanceof net.minecraft.entity.player.EntityPlayer)) {
            if (toast.utilityMobs.TargetHelper.isNeutralMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_neutrals")) return false;
            } else if (target instanceof net.minecraft.entity.monster.IMob) {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_hostiles")) return false;
            } else {
                if (!toast.utilityMobs.Properties.getBoolean("colossals", "attack_passives")) return false;
            }
        }
        return true;
    }

    /// Gets/sets this colossus's animation variable. Used for rendering.
    public int getAnimId() {
        return this.dataManager.get(ANIM_ID).byteValue();
    }
    public void setAnimId(int id) {
        this.dataManager.set(ANIM_ID, Byte.valueOf((byte)id));
    }

    public int getAnimTick() {
        return this.animTick;
    }

    // Designates the first passenger as the one that controls movement.
    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    // Makes the golem solid (standable) when entity collision is enabled.
    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return _UtilityMobs.proxy.solidEntities() ? this.getEntityBoundingBox() : super.getCollisionBoundingBox();
    }

    // Returns if this entity is in water and will end up adding the waters velocity to the entity.
    @Override
    public boolean handleWaterMovement() {
        return false;
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.15);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10.0);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        int animId = this.getAnimId();
        if (this.lastAnimId != animId) {
            this.lastAnimId = animId;
            this.animTick = 0;
        }
        if (animId != 0) {
            this.animTick++;
            if (!this.world.isRemote && this.animTick > 16) {
                this.setAnimId(0);
            }
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.getAnimTick() == 2) {
            Vec3d lookVec = this.getLookVec();
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().offset(lookVec.x * this.width, 0.0, lookVec.z * this.width).grow(0.5, 2.0, 0.5));
            double reach = this.width * this.width * 4.0F + this.width;
            for (Entity entity : list) {
                if (entity != this.getControllingPassenger() && entity instanceof EntityLivingBase && this.canDamage(entity)) {
                    if (this.getDistanceSq(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ) <= reach) {
                        this.attackEntityAsMobFinish(entity);
                    }
                }
            }
        }
        this.setSprinting(this.motionX * this.motionX + this.motionZ * this.motionZ > 2.5E-007);
    }

    // Checks to see if the golem can damage the passed entity.
    private boolean canDamage(Entity entity) {
        if (!this.isBeingRidden() || entity instanceof IEntityOwnable || entity instanceof EntityPlayer)
            return this.targetHelper.isValidTarget(entity);
        return true;
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        if (!this.world.isRemote && this.getAnimId() == 0) {
            this.setAnimId(this.rand.nextBoolean() ? EntityColossalGolem.ANIM_L_ARM_SWING : EntityColossalGolem.ANIM_R_ARM_SWING);
        }
        return true;
    }

    // The actual method that causes damage.
    public boolean attackEntityAsMobFinish(Entity entity) {
        this.world.setEntityState(this, (byte)4);
        UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        double dX = entity.posX - this.posX;
        double dZ = entity.posZ - this.posZ;
        double dH = Math.sqrt(dX * dX + dZ * dZ);
        entity.motionX = dX / dH * 0.5 + this.motionX * 1.2;
        entity.motionY = 0.8;
        entity.motionZ = dZ / dH * 0.5 + this.motionZ * 1.2;
        if (entity instanceof EntityPlayerMP) {
            try {
                ((EntityPlayerMP) entity).connection.sendPacket(new SPacketEntityVelocity(entity));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return super.attackEntityAsMob(entity);
    }

    @Override
    public void handleStatusUpdate(byte b) {
        if (b == 4) {
            UMSound.playAt(this, UMSound.IRONGOLEM_THROW, 1.0F, 1.0F);
        }
        else {
            super.handleStatusUpdate(b);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRONGOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        UMSound.playAt(this, UMSound.IRONGOLEM_WALK, 1.0F, 1.0F);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        // Only the owner may mount a colossus.
        if (this.canInteract(player) && !player.isSneaking() && this.getOwnerName().equals(player.getName())) {
            if (!player.onGround) {
                if (!this.isBeingRidden()) {
                    player.startRiding(this);
                }
                else {
                    Entity rider = this.getControllingPassenger();
                    if (rider != null && !(rider instanceof EntityPlayer)) {
                        rider.dismountRidingEntity();
                        rider.setPosition(player.posX, player.posY, player.posZ);
                    }
                }
                return true;
            }
        }
        return super.processInteract(player, hand);
    }

    // Returns the Y offset from the entity's position for any entity riding this one.
    @Override
    public double getMountedYOffset() {
        return this.height;
    }

    // Moves the entity based on the specified heading.
    @Override
    public void travel(float strafe, float vertical, float forward) {
        Entity rider = this.getControllingPassenger();
        if (rider instanceof EntityPlayer && !this.targetHelper.isValidTarget(rider)) {
            float riderStrafe = ((EntityLivingBase)rider).moveStrafing;
            float riderForward = ((EntityLivingBase)rider).moveForward;
            boolean manualControl = Math.abs(riderStrafe) > 0.01F || Math.abs(riderForward) > 0.01F;
            if (manualControl || !EntityColossalGolem.wanderWhileRidden) {
                this.prevRotationYaw = this.rotationYaw = rider.rotationYaw;
                this.rotationPitch = rider.rotationPitch * 0.5F;
                this.setRotation(this.rotationYaw, this.rotationPitch);
                this.rotationYawHead = this.renderYawOffset = this.rotationYaw;
                strafe = riderStrafe * 0.15F;
                forward = riderForward * 0.3F;
                if (forward <= 0.0F) {
                    forward *= 0.25F;
                }
            }

            if (!this.world.isRemote) {
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                super.travel(strafe, vertical, forward);
            }

            this.prevLimbSwingAmount = this.limbSwingAmount;
            double vX = this.posX - this.prevPosX;
            double vZ = this.posZ - this.prevPosZ;
            float vH = MathHelper.sqrt(vX * vX + vZ * vZ) * 4.0F;
            if (vH > 1.0F) {
                vH = 1.0F;
            }

            this.limbSwingAmount += (vH - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;
        }
        else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            if (this.rand.nextFloat() < dropChance / 4.0F) {
                this.entityDropItem(new ItemStack(Items.SKULL, 1, 4), 0.0F);
            }
        }
    }
}
