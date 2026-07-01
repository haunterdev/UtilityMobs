package toast.utilityMobs.golem;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.EffectHelper;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.ai.EntityAIGolemSit;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.network.MessageHealNumber;

public abstract class EntityUtilityGolem extends EntityGolem implements IEntityOwnable
{
    /// owner; The username of this golem's owner.
    private static final DataParameter<String> OWNER = EntityDataManager.createKey(EntityUtilityGolem.class, DataSerializers.STRING);
    /// isSitting; If this is 1, this golem is sitting.
    private static final DataParameter<Byte> SITTING = EntityDataManager.createKey(EntityUtilityGolem.class, DataSerializers.BYTE);
    /// aggressive; If this is 1, this golem is hostile to all players.
    private static final DataParameter<Byte> AGGRESSIVE = EntityDataManager.createKey(EntityUtilityGolem.class, DataSerializers.BYTE);
    /// fishingRod; If this is not true, the held item is rendered as a stick.
    private static final DataParameter<Boolean> FISHING_ROD = EntityDataManager.createKey(EntityUtilityGolem.class, DataSerializers.BOOLEAN);

    /// The texture for this class.
    public ResourceLocation texture = null;
    /// This golem's target helper.
    public TargetHelper targetHelper = TargetHelper.getTargetHelper(null);
    /// This golem's sitting AI.
    public EntityAIGolemSit sitAI = new EntityAIGolemSit(this);
    /// Attack time counter.
    public int golemAttackTime = 0;
    /// Whether the golem sinks in water. Also doubles as another inWater flag.
    public byte sinks = -1;

    public EntityUtilityGolem(World world) {
        super(world);
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, false));
    }

    /// Maps the legacy integer equipment slot index to the 1.12.2 EntityEquipmentSlot.
    /// 0=held (mainhand), 1=boots, 2=leggings, 3=chestplate, 4=helmet.
    public static EntityEquipmentSlot equipSlot(int slot) {
        switch (slot) {
            case 1: return EntityEquipmentSlot.FEET;
            case 2: return EntityEquipmentSlot.LEGS;
            case 3: return EntityEquipmentSlot.CHEST;
            case 4: return EntityEquipmentSlot.HEAD;
            default: return EntityEquipmentSlot.MAINHAND;
        }
    }

    /// Legacy equipment accessor shims (keep family code mechanical).
    public ItemStack getEquipmentInSlot(int slot) {
        return this.getItemStackFromSlot(equipSlot(slot));
    }
    public void setCurrentItemOrArmor(int slot, ItemStack itemStack) {
        this.setItemStackToSlot(equipSlot(slot), itemStack == null ? ItemStack.EMPTY : itemStack);
    }
    public void setEquipDropChance(int slot, float chance) {
        this.setDropChance(equipSlot(slot), chance);
    }

    /// Used to initialize data watcher variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(OWNER, "");
        this.dataManager.register(SITTING, Byte.valueOf((byte)0));
        this.dataManager.register(AGGRESSIVE, Byte.valueOf((byte)0));
        this.dataManager.register(FISHING_ROD, Boolean.valueOf(true));
    }

    /// Get/set functions for fishing rod. fishing rod == true, stick == false.
    public void setFishingRod(boolean rod) {
        this.dataManager.set(FISHING_ROD, Boolean.valueOf(rod));
    }
    public boolean getFishingRod() {
        return this.dataManager.get(FISHING_ROD).booleanValue();
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(1.0);
    }

    /// The block-material sound set that flavors this golem's hurt/death/step sounds.
    /// Return null (default) to keep the vanilla iron-golem sounds.
    protected SoundType getGolemSoundType() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        SoundType type = this.getGolemSoundType();
        return type == null ? SoundEvents.ENTITY_IRONGOLEM_HURT : type.getHitSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundType type = this.getGolemSoundType();
        return type == null ? SoundEvents.ENTITY_IRONGOLEM_DEATH : type.getBreakSound();
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.Block block) {
        SoundType type = this.getGolemSoundType();
        if (type == null) {
            super.playStepSound(pos, block);
            return;
        }
        this.playSound(type.getStepSound(), type.getVolume() * 0.15F, type.getPitch());
    }

    /// Returns the texture for this mob.
    public ResourceLocation getTexture() {
        return this.texture;
    }

    @Override
    public void onUpdate() {
        String owner = this.getOwnerName();
        if (this.targetHelper.destroyed() || !owner.equals(this.targetHelper.owner)) {
            this.targetHelper = TargetHelper.getTargetHelper(owner);
        }
        this.golemAttackTime = Math.max(this.golemAttackTime - 1, 0);
        super.onUpdate();
        // EntityGolem (our parent) never advances the vanilla arm-swing the way EntityMob does, so
        // swingProgressInt stays frozen and the biped-model golems never visibly swing. Tick it here
        // ourselves so swingArm() from handleStatusUpdate(4) animates. MUST run AFTER super.onUpdate():
        // onEntityUpdate (inside super) sets prevSwingProgress = swingProgress at its top, so advancing
        // the swing before super would leave prev == current every tick -> no interpolation -> choppy
        // swing. Ticking after super mirrors EntityMob's cadence and keeps the swing smooth.
        this.updateArmSwingProgress();
    }

    /// The melee AI broadcasts entity-status 4 on each hit. Large golems override this to drive their
    /// hitTime arm-raise; the biped-model golems (armor/gilded/bound-soul/stone/scarecrow) turn it into a
    /// client-side vanilla arm swing here. Routing through the status channel (not the bare SPacketAnimation
    /// swingArm) is what actually animates these models in-game.
    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.swingArm(EnumHand.MAIN_HAND);
        }
        else {
            super.handleStatusUpdate(id);
        }
    }

    /// Returns the held item; client renders a stick when the fishing rod flag is off.
    @Override
    public ItemStack getHeldItemMainhand() {
        if (this.world.isRemote && !this.getFishingRod())
            return new ItemStack(Items.STICK);
        return super.getHeldItemMainhand();
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        ItemStack weapon = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        float attackDamage = (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        if (this.isWeaponDamageOnly() && !weapon.isEmpty()) {
            // Weapon golems deal only the equipped weapon's own damage, not their innate base on top of it.
            attackDamage -= (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue();
            attackDamage = Math.max(0.0F, attackDamage);
        }
        int knockback = 0;
        if (entity instanceof EntityLivingBase) {
            attackDamage += EnchantmentHelper.getModifierForCreature(weapon, ((EntityLivingBase)entity).getCreatureAttribute());
            knockback += EnchantmentHelper.getKnockbackModifier(this);
        }

        boolean hit = entity.attackEntityFrom(DamageSource.causeMobDamage(this), attackDamage);
        if (hit) {
            this.hitEffects(entity);

            if (knockback > 0) {
                entity.addVelocity(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * knockback * 0.5F, 0.1, MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * knockback * 0.5F);
                this.motionX *= 0.6;
                this.motionZ *= 0.6;
            }

            int fireAspect = EnchantmentHelper.getFireAspectModifier(this);
            if (!weapon.isEmpty()) {
                if (EffectHelper.isFireWeapon(weapon)) {
                    fireAspect += 2;
                }
                else if (EffectHelper.isLavaWeapon(weapon)) {
                    if (!entity.isImmuneToFire()) {
                        entity.attackEntityFrom(DamageSource.LAVA, 4.0F);
                        fireAspect += 4;
                    }
                }
                else if (weapon.getItem() == Item.getItemFromBlock(Blocks.TNT)) {
                    EffectHelper.explode(this, 3.0F);
                    this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    this.setDead();
                }
            }
            if (fireAspect > 0) {
                entity.setFire(fireAspect << 2);
            }

            if (entity instanceof EntityLivingBase) {
                EnchantmentHelper.applyThornEnchantments((EntityLivingBase) entity, this); // Triggers hit entity's enchants.
            }
            EnchantmentHelper.applyArthropodEnchantments(this, entity); // Triggers attacker's enchants.
        }
        return hit;
    }

    public void hitEffects(Entity entity) {
        // To be overridden
    }

    /// If true, an equipped weapon fully replaces this golem's innate base damage instead of stacking on top of it.
    /// Weaponless, the golem still deals its base damage so it remains functional.
    protected boolean isWeaponDamageOnly() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return true;
    }

    @Nullable
    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.PUMPKIN);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        this.dropFewItems(recentlyHit, looting, 0.0F);
    }

    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            this.dropItem(this.getDropItem(), 1);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!this.canInteract(player))
            return super.processInteract(player, hand);
        ItemStack playerHeld = player.getHeldItem(hand);
        if (!player.isSneaking() && this.tryHealFromHeld(player, hand, playerHeld)) {
            return true;
        }
        else if (player.isSneaking() && !playerHeld.isEmpty() && playerHeld.getItem() instanceof ItemShears) {
            if (!this.world.isRemote) {
                float health = this.getHealth();
                float maxHealth = this.getMaxHealth();
                this.dropFewItems(true, 0, health * health / maxHealth / maxHealth);
                this.dropEquipment(true, 0);
                this.spawnExplosionParticle();
            }
            this.playSound(UMSound.STEP_STONE, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
            player.swingArm(hand);
            this.setDead();
        }
        else if (!(this instanceof EntityLargeGolem) && !(this instanceof EntityColossalGolem)) {
            double mountRange = 7.0;
            List<EntityColossalGolem> list = this.world.getEntitiesWithinAABB(EntityColossalGolem.class, new AxisAlignedBB(this.posX - mountRange, this.posY - mountRange, this.posZ - mountRange, this.posX + mountRange, this.posY + mountRange, this.posZ + mountRange));
            if (list != null) {
                for (EntityColossalGolem golem : list) {
                    if (!golem.isBeingRidden() && golem.getLeashed() && golem.getLeashHolder() == player) {
                        this.startRiding(golem);
                        break;
                    }
                }
            }
        }
        return super.processInteract(player, hand);
    }

    protected boolean tryHealFromHeld(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (held.isEmpty() || this.getHealth() >= this.getMaxHealth()) {
            return false;
        }
        Item repairItem = this.getRepairItem();
        if (repairItem == null || held.getItem() != repairItem) {
            return false;
        }
        if (!this.world.isRemote) {
            float healed = this.healAndShowNumber(this.getRepairAmount());
            if (healed > 0.0F && !player.capabilities.isCreativeMode) {
                held.shrink(1);
                if (held.isEmpty()) {
                    player.setHeldItem(hand, ItemStack.EMPTY);
                }
            }
        }
        player.swingArm(hand);
        return true;
    }

    protected Item getRepairItem() {
        return this.getDropItem();
    }

    protected float getRepairAmount() {
        return Math.min(25.0F, Math.max(1.0F, this.getMaxHealth() * 0.25F));
    }

    protected float healAndShowNumber(float amount) {
        float before = this.getHealth();
        this.heal(amount);
        float healed = this.getHealth() - before;
        if (healed > 0.0F && !this.world.isRemote) {
            MessageHealNumber.send(this, healed);
            UMSound.playAt(this, UMSound.STEP_STONE, 0.6F, 1.4F + this.rand.nextFloat() * 0.2F);
        }
        return healed;
    }

    public boolean canInteract(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        if (player.isSneaking() && !held.isEmpty() && held.getItem() instanceof ItemShears)
            return this.targetHelper.playerHasPermission(player.getName(), this.getUsePermissions() | TargetHelper.PERMISSION_OPEN);
        return this.isEntityAlive() && this.targetHelper.playerHasPermission(player.getName(), this.getUsePermissions());
    }

    public int getUsePermissions() {
        return TargetHelper.PERMISSION_TARGET | TargetHelper.PERMISSION_USE;
    }

    public boolean setEquipment(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            for (int slot = 0; slot < 5; slot++) {
                if (!this.getEquipmentInSlot(slot).isEmpty())
                    return this.setEquipment(slot, ItemStack.EMPTY);
            }
            return false;
        }
        int slot = 0;
        if (itemStack.getItem() instanceof ItemArmor) {
            slot = armorIntSlot(((ItemArmor)itemStack.getItem()).armorType);
        }
        return this.setEquipment(slot, itemStack);
    }
    public boolean setEquipment(int slot, ItemStack itemStack) {
        if (itemStack == null) {
            itemStack = ItemStack.EMPTY;
        }
        if (!this.world.isRemote && !this.getEquipmentInSlot(slot).isEmpty()) {
            this.entityDropItem(this.getEquipmentInSlot(slot), 0.0F);
        }
        this.setCurrentItemOrArmor(slot, itemStack);
        this.setEquipDropChance(slot, 2.0F);
        return true;
    }

    /// Inverse of equipSlot for armor: EntityEquipmentSlot -> legacy int slot index.
    private static int armorIntSlot(EntityEquipmentSlot armorSlot) {
        switch (armorSlot) {
            case FEET: return 1;
            case LEGS: return 2;
            case CHEST: return 3;
            case HEAD: return 4;
            default: return 0;
        }
    }

    /// Executes this golem's ranged attack.
    public void doRangedAttack(EntityLivingBase target) {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (held.isEmpty())
            return;
        else if (held.getItem() instanceof ItemBow) {
            EntityTippedArrow arrow = new EntityTippedArrow(this.world, this);
            double motionX = target.posX - this.posX;
            double motionY = target.getEntityBoundingBox().minY + target.height / 3.0F - arrow.posY;
            double motionZ = target.posZ - this.posZ;
            double dist = (double)MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
            arrow.shoot(motionX, motionY + dist * 0.2, motionZ, 1.6F, 12.0F);
            this.targetHelper.setOwned(arrow);
            EnumUpgrade.DEFAULT.applyToArrow(arrow);
            this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
            int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, held);
            int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, held);
            if (power > 0) {
                arrow.setDamage(arrow.getDamage() + power * 0.5 + 0.5);
            }
            if (punch > 0) {
                arrow.setKnockbackStrength(punch);
            }
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, held) > 0) {
                arrow.setFire(100);
            }
            this.world.spawnEntity(arrow);
        }
        else {
            EntitySnowball snowball = new EntitySnowball(this.world, this);
            this.targetHelper.setOwned(snowball);
            EnumUpgrade.DEFAULT.applyTo(snowball);
            double motionX = target.posX - this.posX;
            double motionY = target.posY + target.getEyeHeight() - 1.1 - snowball.posY;
            double motionZ = target.posZ - this.posZ;
            float velocity = (float)Math.sqrt(motionX * motionX + motionZ * motionZ) * 0.2F;
            snowball.shoot(motionX, motionY + velocity, motionZ, 1.6F, 12.0F);
            this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
            this.world.spawnEntity(snowball);
        }
    }

    /// Called when the entity is attacked.
    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (this.isEntityInvulnerable(damageSource))
            return false;
        this.sitAI.sit = false;
        return super.attackEntityFrom(damageSource, damage);
    }

    /// Returns the owner entity.
    @Nullable
    @Override
    public EntityPlayer getOwner() {
        return this.world.getPlayerEntityByName(this.getOwnerName());
    }

    @Nullable
    @Override
    public UUID getOwnerId() {
        EntityPlayer owner = this.getOwner();
        return owner == null ? null : owner.getUniqueID();
    }

    /// Get/set functions for the owner name.
    public String getOwnerName() {
        return this.dataManager.get(OWNER);
    }
    public void setOwner(String username) {
        if (!this.getOwnerName().equals(username)) {
            this.dataManager.set(OWNER, username == null ? "" : username);
            this.targetHelper = TargetHelper.getTargetHelper(username);
        }
    }

    /// Owner-name prefix that marks a golem as belonging to a battle TEAM rather than a player.
    /// Two team golems with different owners are enemies; same owner = allies. Set via the /umsummon
    /// team arg or by right-clicking the golem with a dye.
    public static final String TEAM_PREFIX = "team_";
    /// True if this golem belongs to a battle team (owner starts with TEAM_PREFIX).
    public boolean isOnTeam() {
        String owner = this.getOwnerName();
        return owner != null && owner.startsWith(EntityUtilityGolem.TEAM_PREFIX);
    }
    /// True if the given entity is a golem on a DIFFERENT team than this one (an enemy combatant).
    public boolean isEnemyTeam(Entity entity) {
        if (!this.isOnTeam() || !(entity instanceof EntityUtilityGolem) || !((EntityUtilityGolem)entity).isOnTeam())
            return false;
        return !this.getOwnerName().equals(((EntityUtilityGolem)entity).getOwnerName());
    }

    @Override
    public Team getTeam() {
        EntityLivingBase owner = this.getOwner();
        if (owner != null)
            return owner.getTeam();
        return super.getTeam();
    }

    @Override
    public boolean isOnSameTeam(Entity entity) {
        EntityLivingBase owner = this.getOwner();
        if (entity == owner)
            return true;
        if (owner != null)
            return owner.isOnSameTeam(entity);
        else if (entity instanceof EntityUtilityGolem)
            return ((IEntityOwnable)entity).getOwner() == null;
        return super.isOnSameTeam(entity);
    }

    /// Gets/sets the isSitting variable.
    public boolean isSitting() {
        return this.dataManager.get(SITTING).byteValue() == 1;
    }
    public void setSitting(boolean sitting) {
        this.dataManager.set(SITTING, Byte.valueOf(sitting ? (byte)1 : (byte)0));
    }

    /// Gets/sets the aggressive variable. Aggressive golems target all non-creative/spectator players.
    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE).byteValue() == 1;
    }
    public void setAggressive(boolean aggressive) {
        this.dataManager.set(AGGRESSIVE, Byte.valueOf(aggressive ? (byte)1 : (byte)0));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean handleWaterMovement() {
        if (this.sinks < 0)
            return super.handleWaterMovement();
        if (this.world.handleMaterialAcceleration(this.getEntityBoundingBox().expand(0.0, -0.4, 0.0).contract(0.001, 0.001, 0.001), Material.WATER, this)) {
            if (this.sinks < 1) {
                float speed = (float)Math.sqrt(this.motionX * this.motionX * 0.2 + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.2) * 0.2F;
                if (speed > 1.0F) {
                    speed = 1.0F;
                }
                this.playSound(UMSound.SPLASH, speed, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                float y = (float)Math.floor(this.getEntityBoundingBox().minY);
                float x, z;
                for (float i = this.width * 20.0F + 1.0F; i-- > 0.0F;) {
                    x = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    z = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.world.spawnParticle(UMSound.BUBBLE, this.posX + x, y + 1.0F, this.posZ + z, this.motionX, this.motionY - this.rand.nextFloat() * 0.2F, this.motionZ);
                    x = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    z = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.world.spawnParticle(UMSound.WATER_SPLASH, this.posX + x, y + 1.0F, this.posZ + z, this.motionX, this.motionY, this.motionZ);
                }
            }
            this.fallDistance = 0.0F;
            this.sinks = 1;
            this.extinguish();
        }
        else {
            this.sinks = 0;
        }
        return false;
    }

    /// Saves this entity to NBT.
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("Sitting", this.isSitting());
        tag.setBoolean("Aggressive", this.isAggressive());
        tag.setString("Owner", this.getOwnerName());
    }

    /// Loads this entity from NBT.
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.sitAI.sit = tag.getBoolean("Sitting");
        this.setSitting(this.sitAI.sit);
        this.setAggressive(tag.getBoolean("Aggressive"));
        String name = null;
        if (tag.hasKey("Owner")) {
            name = tag.getString("Owner");
        }
        else if (tag.hasKey("owner")) {
            name = tag.getString("owner");
        }
        if ("".equals(name)) {
            name = null;
        }
        this.setOwner(name);
    }

    /// Category gate for what this golem may target. Base implementation reads the golems.* config toggles.
    /// Overridden by turrets to use their per-entity GUI flags instead.
    protected boolean passesTargetFilter(Entity target) {
        if (target instanceof EntityLivingBase && !(target instanceof EntityPlayer)) {
            if (toast.utilityMobs.TargetHelper.isNeutralMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_neutrals")) return false;
            } else if (target instanceof net.minecraft.entity.monster.IMob) {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_hostiles")) return false;
            } else {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_passives")) return false;
            }
        }
        return true;
    }

    /// Returns true if this golem can attack the target (includes the line-of-sight raytrace).
    public boolean canAttack(Entity target) {
        return this.canAttackNoSight(target) && this.getEntitySenses().canSee(target);
    }

    /// Cheap half of canAttack: owner/team/whitelist/range filters with NO line-of-sight raytrace.
    /// Targeting scans run this against every candidate in the box (cheap, no raytrace); the expensive
    /// canSee() is then applied only to the few best candidates by the targeting AI. See EntityAIGolemTarget.
    public boolean canAttackNoSight(Entity target) {
        // Never target creative/spectator players (capabilities.disableDamage covers both).
        if (target instanceof EntityPlayer && ((EntityPlayer)target).capabilities.disableDamage) {
            return false;
        }
        // Enemy-team golems are always valid combat targets - bypass the hostile/passive config gate so
        // dyed/`/umsummon` team golems fight regardless of golems.attack_passives. isValidTarget (owner +
        // whitelist) and range checks below still apply.
        if (!this.isEnemyTeam(target) && !this.passesTargetFilter(target)) {
            return false;
        }
        double range = this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        boolean validTarget;
        if (this.isAggressive() && target instanceof EntityPlayer) {
            // Aggressive golems target any non-creative/spectator player (already guarded above).
            validTarget = true;
        }
        else {
            validTarget = this.targetHelper.isValidTarget(target);
        }
        return target != this && validTarget && range * range >= this.getDistanceSq(target.posX, target.getEntityBoundingBox().minY, target.posZ);
    }

    /// Max entities this golem pushes per tick, cached from golems.collision_push_cap.
    /// 0 = unlimited (scan still profiled), -1 = pure vanilla collision (no override at all).
    public static int collisionPushCap = 8;

    /// Crowd threshold from golems.collision_disable_density. When a golem already has at least this many
    /// entities crowding it, collision is skipped entirely (scan AND push) because shoving is pointless
    /// and ruinously expensive in a dense pile. 0 = never disable.
    public static int collisionDisableDensity = 0;

    /// How often (in ticks) a golem in a known-dense pile re-scans to detect that the crowd has dispersed.
    private static final int DENSITY_RESAMPLE_TICKS = 20;
    /// Neighbor count from this golem's last collision scan, used to short-circuit the dense case.
    private int lastCollisionNeighbors = 0;

    /// Run the collision scan only every N ticks (staggered by entity id), from golems.collision_interval.
    /// The scan (getEntitiesInAABBexcluding) is the dominant cost at scale; running it every Nth tick
    /// instead of every tick cuts it ~N-fold. Pushing a few ticks apart is visually indistinguishable.
    /// 1 = every tick (vanilla cadence).
    public static int collisionInterval = 1;

    /// Activation radius (blocks) from golems.active_range. A golem with NO player within this radius
    /// skips its expensive broadphase scans (targeting + collision) - the structural win for very large
    /// armies, where most golems are nowhere near a player and need not pay the O(n^2) scan cost.
    /// 0 or less = always active (no gating).
    public static int activeRange = 64;

    /// True if this golem should run its expensive per-tick scans (a player is within activeRange).
    /// getClosestPlayer/isAnyPlayerWithinRangeAt only iterates the (small) player list, so this is cheap.
    public boolean isPerfActive() {
        int r = EntityUtilityGolem.activeRange;
        if (r <= 0)
            return true;
        return this.world.isAnyPlayerWithinRangeAt(this.posX, this.posY, this.posZ, r);
    }

    /// Bounds the per-tick collision cost, the dominant load of a packed golem army. Two effects:
    ///   - push cap: vanilla pushes EVERY colliding entity (O(n^2) across a clump); we cap the pushes.
    ///   - density disable: when crowded past collision_disable_density, skip the whole method. The scan
    ///     itself is O(neighbors), so across a clump it is also O(n^2); skipping it most ticks (re-sampling
    ///     every DENSITY_RESAMPLE_TICKS so dispersal is noticed) removes that cost too. This is the "no
    ///     point bouncing when it's slowing the game to a crawl" case.
    @Override
    protected void collideWithNearbyEntities() {
        if (EntityUtilityGolem.collisionPushCap < 0) {
            super.collideWithNearbyEntities();
            return;
        }
        // Mounted golems (e.g. riding a colossus) move with their mount - collision shoving is pointless
        // and was the dominant cost when mounting a colossus into a packed army. Skip it outright.
        if (this.getRidingEntity() != null) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_riding", 1);
            return;
        }
        // Settled golems (no attack target AND no active path) don't need collision resolution - they
        // just stand. Worker/block golems teleport to reposition rather than shove, so a standing army
        // costs nothing here. The player still parts the crowd (the PLAYER's own collision pushes golems
        // aside), so it stays player-friendly. Collision resumes automatically once a golem acquires a
        // target or starts pathing (i.e. when it is actually moving and pushing matters). This is the
        // structural win for a large army gathered around the player.
        if (this.getAttackTarget() == null && this.getNavigator().noPath()) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_idle", 1);
            return;
        }
        // Base scan throttle: most golems collide every Nth tick (staggered) instead of every tick.
        int interval = EntityUtilityGolem.collisionInterval;
        if (interval > 1 && ((this.ticksExisted + this.getEntityId()) % interval) != 0) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_throttle", 1);
            return;
        }
        // Activation gate: no player nearby -> skip the broadphase scan entirely.
        if (!this.isPerfActive()) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_inactive", 1);
            return;
        }
        int disable = EntityUtilityGolem.collisionDisableDensity;
        // Known-dense: skip scan+push entirely, except on the periodic re-sample tick.
        if (disable > 0 && this.lastCollisionNeighbors >= disable && (this.ticksExisted % EntityUtilityGolem.DENSITY_RESAMPLE_TICKS) != 0) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_dense", 1);
            return;
        }
        long t0 = toast.utilityMobs.UMProfiler.start();
        List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2, 0.0, 0.2), net.minecraft.util.EntitySelectors.<Entity>getTeamCollisionPredicate(this));
        this.lastCollisionNeighbors = list.size();
        // Crowded past the threshold: scanned only to refresh the count, but do not push at all.
        if (disable > 0 && list.size() >= disable) {
            toast.utilityMobs.UMProfiler.count("collision_disabled_pushes", list.size());
            toast.utilityMobs.UMProfiler.count("collision_calls", 1);
            toast.utilityMobs.UMProfiler.end("collision", t0);
            return;
        }
        int cap = EntityUtilityGolem.collisionPushCap;
        int limit = (cap == 0 || cap >= list.size()) ? list.size() : cap;
        for (int j = 0; j < limit; j++) {
            this.collideWithEntity(list.get(j));
        }
        toast.utilityMobs.UMProfiler.count("collision_pushed", limit);
        toast.utilityMobs.UMProfiler.count("collision_skipped", list.size() - limit);
        toast.utilityMobs.UMProfiler.count("collision_calls", 1);
        toast.utilityMobs.UMProfiler.end("collision", t0);
    }
}
