package toast.utilityMobs.golem;

import java.util.List;
import java.util.UUID;

import toast.utilityMobs.colossal.EntityColossalGolem;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Team;
import toast.utilityMobs.EffectHelper;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.ai.EntityAIGolemSit;

public abstract class EntityUtilityGolem extends AbstractGolem implements OwnableEntity
{
    /// owner; The username of this golem's owner.
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(EntityUtilityGolem.class, EntityDataSerializers.STRING);
    /// isSitting; If this is 1, this golem is sitting.
    private static final EntityDataAccessor<Byte> SITTING = SynchedEntityData.defineId(EntityUtilityGolem.class, EntityDataSerializers.BYTE);
    /// aggressive; If this is 1, this golem is hostile to all players.
    private static final EntityDataAccessor<Byte> AGGRESSIVE = SynchedEntityData.defineId(EntityUtilityGolem.class, EntityDataSerializers.BYTE);
    /// fishingRod; If this is not true, the held item is rendered as a stick.
    private static final EntityDataAccessor<Boolean> FISHING_ROD = SynchedEntityData.defineId(EntityUtilityGolem.class, EntityDataSerializers.BOOLEAN);

    /// The texture for this class.
    public ResourceLocation texture = null;
    /// This golem's target helper.
    public TargetHelper targetHelper = TargetHelper.getTargetHelper(null);
    /// This golem's sitting AI. Created on first access, never in a field initializer and never
    /// only in registerGoals:
    ///   - Mob's constructor calls registerGoals() ONLY when !level.isClientSide, but hurt() and
    ///     mobInteract() both run client-side, so a registerGoals-only assignment leaves the client
    ///     copy null (a turret arrow hitting a golem then crashed the client in hurt()).
    ///   - A field initializer runs AFTER the Mob super-ctor, so it would replace the very instance
    ///     EntityBlockGolem.registerGoals just handed to the goal selector.
    /// 1.12.2 used a plain field initializer and registered the goal from the constructor body,
    /// where that ordering hazard does not exist.
    private EntityAIGolemSit sitAI;
    /// Attack time counter.
    public int golemAttackTime = 0;
    /// Whether the golem sinks in water. Also doubles as another inWater flag.
    public byte sinks = -1;

    protected EntityUtilityGolem(EntityType<? extends EntityUtilityGolem> type, Level level) {
        super(type, level);
    }

    /// Always non-null, on both sides. Use this everywhere instead of touching the field.
    public EntityAIGolemSit sitAI() {
        if (this.sitAI == null) {
            this.sitAI = new EntityAIGolemSit(this);
        }
        return this.sitAI;
    }

    @Override
    protected void registerGoals() {
        this.sitAI();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    /// Base attributes shared by all utility golems. Leaf classes call this and override values.
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractGolem.createMobAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    /// Maps the legacy integer equipment slot index to the EquipmentSlot.
    /// 0=held (mainhand), 1=boots, 2=leggings, 3=chestplate, 4=helmet.
    public static EquipmentSlot equipSlot(int slot) {
        switch (slot) {
            case 1: return EquipmentSlot.FEET;
            case 2: return EquipmentSlot.LEGS;
            case 3: return EquipmentSlot.CHEST;
            case 4: return EquipmentSlot.HEAD;
            default: return EquipmentSlot.MAINHAND;
        }
    }

    /// Legacy equipment accessor shims (keep family code mechanical).
    public ItemStack getEquipmentInSlot(int slot) {
        return this.getItemBySlot(equipSlot(slot));
    }
    public void setCurrentItemOrArmor(int slot, ItemStack itemStack) {
        this.setItemSlot(equipSlot(slot), itemStack == null ? ItemStack.EMPTY : itemStack);
    }
    public void setEquipDropChance(int slot, float chance) {
        this.setDropChance(equipSlot(slot), chance);
    }

    /// Used to initialize data watcher variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, "");
        this.entityData.define(SITTING, Byte.valueOf((byte)0));
        this.entityData.define(AGGRESSIVE, Byte.valueOf((byte)0));
        this.entityData.define(FISHING_ROD, Boolean.valueOf(true));
    }

    /// Get/set functions for fishing rod. fishing rod == true, stick == false.
    public void setFishingRod(boolean rod) {
        this.entityData.set(FISHING_ROD, Boolean.valueOf(rod));
    }
    public boolean getFishingRod() {
        return this.entityData.get(FISHING_ROD).booleanValue();
    }

    /// The block-material sound set that flavors this golem's hurt/death/step sounds.
    /// Return null (default) to keep the vanilla iron-golem sounds.
    protected SoundType getGolemSoundType() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        SoundType type = this.getGolemSoundType();
        // Use the block BREAK sound (not the quiet dig "hit" sound, which read as a footstep).
        // playHurtSound below plays it lighter/higher so a hit sounds like the block chipping
        // while death (getDeathSound) is the full break.
        return type == null ? SoundEvents.IRON_GOLEM_HURT : type.getBreakSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundType type = this.getGolemSoundType();
        return type == null ? SoundEvents.IRON_GOLEM_DEATH : type.getBreakSound();
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        SoundType type = this.getGolemSoundType();
        if (type == null) {
            super.playHurtSound(source);
            return;
        }
        // Block-typed golem: a hit chips the block. Play the BREAK sound at reduced volume and a slightly
        // higher pitch so it clearly reads as "block breaking" yet stays distinct from the full-volume
        // death break.
        this.playSound(type.getBreakSound(), type.getVolume() * 0.7F, type.getPitch() * 1.2F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundType type = this.getGolemSoundType();
        if (type == null) {
            super.playStepSound(pos, state);
            return;
        }
        this.playSound(type.getStepSound(), type.getVolume() * 0.15F, type.getPitch());
    }

    /// Returns the texture for this mob.
    public ResourceLocation getTexture() {
        return this.texture;
    }

    @Override
    public void tick() {
        String owner = this.getOwnerName();
        if (this.targetHelper.destroyed() || !owner.equals(this.targetHelper.owner)) {
            this.targetHelper = TargetHelper.getTargetHelper(owner);
        }
        this.golemAttackTime = Math.max(this.golemAttackTime - 1, 0);
        super.tick();
    }

    @Override
    public void aiStep() {
        // AbstractGolem (like 1.12.2's EntityGolem) never advances the vanilla arm-swing timer -
        // only Monster and Player do - so biped-model golems would never visibly swing. Mirror
        // Monster.aiStep's cadence exactly: advance the swing first, then super.
        this.updateSwingTime();
        super.aiStep();
    }

    /// The melee AI broadcasts entity-status 4 on each hit. Large golems override this to drive their
    /// hitTime arm-raise; the biped-model golems (armor/gilded/bound-soul/stone/scarecrow) turn it into a
    /// client-side vanilla arm swing here. Routing through the status channel is what actually animates
    /// these models in-game.
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.swing(InteractionHand.MAIN_HAND);
        }
        else {
            super.handleEntityEvent(id);
        }
    }

    /// Returns the held item; client renders a stick when the fishing rod flag is off.
    @Override
    public ItemStack getMainHandItem() {
        if (this.level().isClientSide && !this.getFishingRod())
            return new ItemStack(Items.STICK);
        return super.getMainHandItem();
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        ItemStack weapon = this.getItemBySlot(EquipmentSlot.MAINHAND);
        float attackDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (this.isWeaponDamageOnly() && !weapon.isEmpty()) {
            // Weapon golems deal only the equipped weapon's own damage, not their innate base on top of it.
            attackDamage -= (float)this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            attackDamage = Math.max(0.0F, attackDamage);
        }
        int knockback = 0;
        if (entity instanceof LivingEntity living) {
            attackDamage += EnchantmentHelper.getDamageBonus(weapon, living.getMobType());
            knockback += EnchantmentHelper.getKnockbackBonus(this);
        }

        boolean hit = entity.hurt(this.damageSources().mobAttack(this), attackDamage);
        if (hit) {
            this.hitEffects(entity);

            if (knockback > 0) {
                entity.push(-Mth.sin(this.getYRot() * (float)Math.PI / 180.0F) * knockback * 0.5F, 0.1, Mth.cos(this.getYRot() * (float)Math.PI / 180.0F) * knockback * 0.5F);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }

            int fireAspect = EnchantmentHelper.getFireAspect(this);
            if (!weapon.isEmpty()) {
                if (EffectHelper.isFireWeapon(weapon)) {
                    fireAspect += 2;
                }
                else if (EffectHelper.isLavaWeapon(weapon)) {
                    if (!entity.fireImmune()) {
                        entity.hurt(this.damageSources().lava(), 4.0F);
                        fireAspect += 4;
                    }
                }
                else if (weapon.getItem() == Blocks.TNT.asItem()) {
                    EffectHelper.explode(this, 3.0F);
                    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    this.discard();
                }
            }
            if (fireAspect > 0) {
                entity.setSecondsOnFire(fireAspect << 2);
            }

            if (entity instanceof LivingEntity living) {
                EnchantmentHelper.doPostHurtEffects(living, this); // Triggers hit entity's enchants (thorns).
            }
            EnchantmentHelper.doPostDamageEffects(this, entity); // Triggers attacker's enchants.
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

    @Nullable
    protected Item getDropItem() {
        return Blocks.PUMPKIN.asItem();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.dropFewItems(recentlyHit, looting);
    }

    protected void dropFewItems(boolean recentlyHit, int looting) {
        this.dropFewItems(recentlyHit, looting, 0.0F);
    }

    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            Item drop = this.getDropItem();
            if (drop != null) {
                this.spawnAtLocation(drop);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.canInteract(player))
            return super.mobInteract(player, hand);
        ItemStack playerHeld = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && this.tryHealFromHeld(player, hand, playerHeld)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        else if (player.isShiftKeyDown() && !playerHeld.isEmpty() && playerHeld.getItem() instanceof ShearsItem) {
            if (!this.level().isClientSide) {
                float health = this.getHealth();
                float maxHealth = this.getMaxHealth();
                this.dropFewItems(true, 0, health * health / maxHealth / maxHealth);
                this.dropEquipment();
                this.spawnAnim();
            }
            this.playSound(UMSound.STEP_STONE, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
            player.swing(hand);
            this.discard();
        }
        else if (!(this instanceof EntityLargeGolem)) {
            // Walk a small golem onto a colossus the player is leading: leash a colossus, then
            // right-click a nearby small golem to seat it as the colossus's passenger.
            double mountRange = 7.0;
            List<EntityColossalGolem> list = this.level().getEntitiesOfClass(EntityColossalGolem.class, this.getBoundingBox().inflate(mountRange));
            for (EntityColossalGolem golem : list) {
                if (!golem.isVehicle() && golem.isLeashed() && golem.getLeashHolder() == player) {
                    this.startRiding(golem);
                    break;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    protected boolean tryHealFromHeld(Player player, InteractionHand hand, ItemStack held) {
        if (held.isEmpty() || this.getHealth() >= this.getMaxHealth()) {
            return false;
        }
        Item repairItem = this.getRepairItem();
        if (repairItem == null || held.getItem() != repairItem) {
            return false;
        }
        if (!this.level().isClientSide) {
            float healed = this.healAndShowNumber(this.getRepairAmount());
            if (healed > 0.0F && !player.getAbilities().instabuild) {
                held.shrink(1);
                if (held.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
        }
        player.swing(hand);
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
        if (healed > 0.0F && !this.level().isClientSide) {
            toast.utilityMobs.network.MessageHealNumber.send(this, healed);
            UMSound.playAt(this, UMSound.STEP_STONE, 0.6F, 1.4F + this.random.nextFloat() * 0.2F);
        }
        return healed;
    }

    public boolean canInteract(Player player) {
        ItemStack held = player.getMainHandItem();
        if (player.isShiftKeyDown() && !held.isEmpty() && held.getItem() instanceof ShearsItem)
            return this.targetHelper.playerHasPermission(player.getScoreboardName(), this.getUsePermissions() | TargetHelper.PERMISSION_OPEN);
        return this.isAlive() && this.targetHelper.playerHasPermission(player.getScoreboardName(), this.getUsePermissions());
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
        if (itemStack.getItem() instanceof ArmorItem armor) {
            slot = armorIntSlot(armor.getEquipmentSlot());
        }
        return this.setEquipment(slot, itemStack);
    }
    public boolean setEquipment(int slot, ItemStack itemStack) {
        if (itemStack == null) {
            itemStack = ItemStack.EMPTY;
        }
        if (!this.level().isClientSide && !this.getEquipmentInSlot(slot).isEmpty()) {
            this.spawnAtLocation(this.getEquipmentInSlot(slot), 0.0F);
        }
        this.setCurrentItemOrArmor(slot, itemStack);
        this.setEquipDropChance(slot, 2.0F);
        return true;
    }

    /// Inverse of equipSlot for armor: EquipmentSlot -> legacy int slot index.
    private static int armorIntSlot(EquipmentSlot armorSlot) {
        switch (armorSlot) {
            case FEET: return 1;
            case LEGS: return 2;
            case CHEST: return 3;
            case HEAD: return 4;
            default: return 0;
        }
    }

    /// Executes this golem's ranged attack.
    public void doRangedAttack(LivingEntity target) {
        ItemStack held = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty())
            return;
        else if (held.getItem() instanceof BowItem) {
            Arrow arrow = new Arrow(this.level(), this);
            double motionX = target.getX() - this.getX();
            double motionY = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - arrow.getY();
            double motionZ = target.getZ() - this.getZ();
            double dist = Math.sqrt(motionX * motionX + motionZ * motionZ);
            arrow.shoot(motionX, motionY + dist * 0.2, motionZ, 1.6F, 12.0F);
            this.targetHelper.setOwned(arrow);
            EnumUpgrade.DEFAULT.applyToArrow(arrow);
            this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, held);
            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, held);
            if (power > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5 + 0.5);
            }
            if (punch > 0) {
                arrow.setKnockback(punch);
            }
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, held) > 0) {
                arrow.setSecondsOnFire(100);
            }
            this.level().addFreshEntity(arrow);
        }
        else {
            Snowball snowball = new Snowball(this.level(), this);
            this.targetHelper.setOwned(snowball);
            EnumUpgrade.DEFAULT.applyTo(snowball);
            double motionX = target.getX() - this.getX();
            double motionY = target.getY() + target.getEyeHeight() - 1.1 - snowball.getY();
            double motionZ = target.getZ() - this.getZ();
            float velocity = (float)Math.sqrt(motionX * motionX + motionZ * motionZ) * 0.2F;
            snowball.shoot(motionX, motionY + velocity, motionZ, 1.6F, 12.0F);
            this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(snowball);
        }
    }

    /// Called when the entity is attacked.
    @Override
    public boolean hurt(DamageSource damageSource, float damage) {
        if (this.isInvulnerableTo(damageSource))
            return false;
        this.sitAI().sit = false;
        return super.hurt(damageSource, damage);
    }

    /// Returns the owner entity. Owners are tracked by NAME (a 1.7.10-era mod trait kept through
    /// both ports - it is what makes team golems and offline-owner golems work), so resolve by
    /// scanning the level's player list rather than by UUID.
    @Nullable
    @Override
    public Player getOwner() {
        String name = this.getOwnerName();
        if (name == null || name.isEmpty())
            return null;
        for (Player player : this.level().players()) {
            if (name.equals(player.getScoreboardName()))
                return player;
        }
        return null;
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        Player owner = this.getOwner();
        return owner == null ? null : owner.getUUID();
    }

    /// Get/set functions for the owner name.
    public String getOwnerName() {
        return this.entityData.get(OWNER);
    }
    public void setOwner(String username) {
        if (!this.getOwnerName().equals(username)) {
            this.entityData.set(OWNER, username == null ? "" : username);
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
        if (!this.isOnTeam() || !(entity instanceof EntityUtilityGolem other) || !other.isOnTeam())
            return false;
        return !this.getOwnerName().equals(other.getOwnerName());
    }

    @Override
    public Team getTeam() {
        LivingEntity owner = this.getOwner();
        if (owner != null)
            return owner.getTeam();
        return super.getTeam();
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        LivingEntity owner = this.getOwner();
        if (entity == owner)
            return true;
        if (owner != null)
            return owner.isAlliedTo(entity);
        else if (entity instanceof EntityUtilityGolem other)
            return other.getOwner() == null;
        return super.isAlliedTo(entity);
    }

    /// Gets/sets the isSitting variable.
    public boolean isSitting() {
        return this.entityData.get(SITTING).byteValue() == 1;
    }
    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, Byte.valueOf(sitting ? (byte)1 : (byte)0));
    }

    /// Gets/sets the aggressive variable. Aggressive golems target all non-creative/spectator players.
    public boolean isAggressive() {
        return this.entityData.get(AGGRESSIVE).byteValue() == 1;
    }
    public void setAggressive(boolean aggressive) {
        this.entityData.set(AGGRESSIVE, Byte.valueOf(aggressive ? (byte)1 : (byte)0));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /// Sinking golems are never pushed by water currents.
    @Override
    public boolean isPushedByFluid() {
        return this.sinks < 0 && super.isPushedByFluid();
    }

    /// 1.12.2 handleWaterMovement port: a golem with sinks >= 0 never reports "in water" (so the
    /// float goal and water physics leave it alone and it walks along the bottom), but still plays
    /// the surface splash once on entry and stays fire-extinguished/fall-safe while submerged.
    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() {
        if (this.sinks < 0)
            return super.updateInWaterStateAndDoFluidPushing();
        if (this.level().containsAnyLiquid(this.getBoundingBox().expandTowards(0.0, -0.4, 0.0).deflate(0.001))) {
            if (this.sinks < 1) {
                this.doWaterSplashEffect();
            }
            this.resetFallDistance();
            this.sinks = 1;
            this.clearFire();
        }
        else {
            this.sinks = 0;
        }
        return false;
    }

    /// Saves this entity to NBT.
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Sitting", this.isSitting());
        tag.putBoolean("Aggressive", this.isAggressive());
        tag.putString("Owner", this.getOwnerName());
    }

    /// Loads this entity from NBT.
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.sitAI().sit = tag.getBoolean("Sitting");
        this.setSitting(this.sitAI().sit);
        this.setAggressive(tag.getBoolean("Aggressive"));
        String name = null;
        if (tag.contains("Owner")) {
            name = tag.getString("Owner");
        }
        else if (tag.contains("owner")) {
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
        if (target instanceof LivingEntity && !(target instanceof Player)) {
            if (toast.utilityMobs.TargetHelper.isNeutralMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_neutrals")) return false;
            } else if (toast.utilityMobs.TargetHelper.isHostileMob(target)) {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_hostiles")) return false;
            } else {
                if (!toast.utilityMobs.Properties.getBoolean("golems", "attack_passives")) return false;
            }
        }
        return true;
    }

    /// Returns true if this golem can attack the target (includes the line-of-sight raytrace).
    public boolean canAttack(Entity target) {
        return this.canAttackNoSight(target) && this.getSensing().hasLineOfSight(target);
    }

    /// Cheap half of canAttack: owner/team/whitelist/range filters with NO line-of-sight raytrace.
    /// Targeting scans run this against every candidate in the box (cheap, no raytrace); the expensive
    /// hasLineOfSight() is then applied only to the few best candidates by the targeting AI.
    public boolean canAttackNoSight(Entity target) {
        // Never target creative/spectator players (abilities.invulnerable covers both).
        if (target instanceof Player player && player.getAbilities().invulnerable) {
            return false;
        }
        // Enemy-team golems and globally-whitelisted entities (general.attack_whitelist) bypass the
        // hostile/passive/neutral category gate so they are attacked even with the attack_* toggle off.
        if (!this.isEnemyTeam(target) && !toast.utilityMobs.TargetHelper.isGloballyWhitelisted(target) && !this.passesTargetFilter(target)) {
            return false;
        }
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        boolean validTarget;
        if (this.isAggressive() && target instanceof Player) {
            // Aggressive golems target any non-creative/spectator player (already guarded above).
            validTarget = true;
        }
        else {
            validTarget = this.targetHelper.isValidTarget(target);
        }
        return target != this && validTarget && range * range >= this.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
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
    /// 1 = every tick (vanilla cadence).
    public static int collisionInterval = 1;

    /// Activation radius (blocks) from golems.active_range. 0 or less = always active (no gating).
    public static int activeRange = 64;

    /// True if this golem should run its expensive per-tick scans (a player is within activeRange).
    public boolean isPerfActive() {
        int r = EntityUtilityGolem.activeRange;
        if (r <= 0)
            return true;
        return this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), r);
    }

    /// Bounds the per-tick collision cost, the dominant load of a packed golem army. Two effects:
    ///   - push cap: vanilla pushes EVERY colliding entity (O(n^2) across a clump); we cap the pushes.
    ///   - density disable: when crowded past collision_disable_density, skip the whole method, re-sampling
    ///     every DENSITY_RESAMPLE_TICKS so dispersal is noticed.
    /// (1.12.2 collideWithNearbyEntities -> 1.20.1 pushEntities. The capped path intentionally skips
    /// vanilla's cramming damage - a deliberately packed golem army must not crush itself.)
    @Override
    protected void pushEntities() {
        if (EntityUtilityGolem.collisionPushCap < 0) {
            super.pushEntities();
            return;
        }
        // Mounted golems (e.g. riding a colossus) move with their mount - collision shoving is pointless
        // and was the dominant cost when mounting a colossus into a packed army. Skip it outright.
        if (this.getVehicle() != null) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_riding", 1);
            return;
        }
        // Settled golems (no attack target AND no active path) don't need collision resolution - they
        // just stand. The player's own collision still parts the crowd. Collision resumes automatically
        // once a golem acquires a target or starts pathing.
        if (this.getTarget() == null && this.getNavigation().isDone()) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_idle", 1);
            return;
        }
        // Base scan throttle: most golems collide every Nth tick (staggered) instead of every tick.
        int interval = EntityUtilityGolem.collisionInterval;
        if (interval > 1 && ((this.tickCount + this.getId()) % interval) != 0) {
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
        if (disable > 0 && this.lastCollisionNeighbors >= disable && (this.tickCount % EntityUtilityGolem.DENSITY_RESAMPLE_TICKS) != 0) {
            toast.utilityMobs.UMProfiler.count("collision_skipped_dense", 1);
            return;
        }
        long t0 = toast.utilityMobs.UMProfiler.start();
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
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
            this.doPush(list.get(j));
        }
        toast.utilityMobs.UMProfiler.count("collision_pushed", limit);
        toast.utilityMobs.UMProfiler.count("collision_skipped", list.size() - limit);
        toast.utilityMobs.UMProfiler.count("collision_calls", 1);
        toast.utilityMobs.UMProfiler.end("collision", t0);
    }
}
