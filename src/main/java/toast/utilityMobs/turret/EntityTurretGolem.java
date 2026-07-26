package toast.utilityMobs.turret;

import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAITurretAttack;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityTurretGolem extends EntityUtilityGolem implements net.minecraft.world.MenuProvider
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: dispenser.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.STONE;
    }

    /// Server side of opening this turret's screen. 1.12.2 routed this through IGuiHandler; 1.20.1 wants
    /// the entity itself to be the MenuProvider. getDisplayName is inherited from Entity.
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInv, Player player) {
        return new ContainerTurretGolem(containerId, playerInv, this);
    }

    /// All applicable upgrades.
    public static final EnumUpgrade[] upgradesAll = {
        EnumUpgrade.KILLER, EnumUpgrade.FIRE, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
    };
    /// The UUID for the sight upgrade's modifier.
    private static final UUID sightBoostUUID = UUID.fromString("70A27B59-9566-4402-BC1F-2EE2A276D836");
    /// The modifier applied by the sight upgrade. MULTIPLY_BASE with amount 1.0 DOUBLES the turret's
    /// follow range (base + base*1.0). Applied transiently so it is never saved to NBT.
    private static final AttributeModifier sightBoost = new AttributeModifier(EntityTurretGolem.sightBoostUUID, "Ender pearl upgrade", 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
    /// When true (turrets.collision config), turrets are solid and can be stood on / walked across.
    public static boolean collision = false;

    /// Possible upgrades for this turret type.
    public EnumUpgrade[] upgrades = {
            EnumUpgrade.KILLER, EnumUpgrade.FIRE, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
    };
    /// This turret's targeting AI. Assigned in registerGoals (runs during the super-ctor).
    public EntityAIGolemTarget targetAI;
    /// This turret's current upgrade.
    public EnumUpgrade upgrade;
    /// Attack time counter.
    public int maxAttackTime = 60;
    /// 9-slot ammo inventory (always present; only required to fire when the require_ammo config is on).
    private final SimpleContainer ammoInventory = new SimpleContainer(9);
    /// Lazily-built capability wrapper exposing the ammo inventory to hoppers/pipes.
    private LazyOptional<net.minecraftforge.items.IItemHandler> ammoHandler = LazyOptional.empty();
    /// World-space anchor the turret is pinned to when not feather-equipped. Double.NaN sentinel = not yet captured.
    private double anchorX = Double.NaN;
    private double anchorZ = Double.NaN;
    /// Target category flags: bit0 = attack hostile, bit1 = attack passive, bit2 = attack neutral.
    private static final EntityDataAccessor<Byte> TARGET_FLAGS = SynchedEntityData.defineId(EntityTurretGolem.class, EntityDataSerializers.BYTE);
    /// Targeting mode: 0=CLOSE (nearest), 1=FAR (farthest), 2=STRONG (max health), 3=WEAK (min health).
    private static final EntityDataAccessor<Byte> TARGET_MODE = SynchedEntityData.defineId(EntityTurretGolem.class, EntityDataSerializers.BYTE);
    /// The turret's effective follow range, mirrored to the client for the range visualiser.
    /// Attributes.FOLLOW_RANGE is declared without setSyncable(true) in vanilla, so the client's copy
    /// never sees the sight upgrade's modifier and the sphere stayed at the base radius. The upgrade
    /// itself always worked: targeting runs server-side off the real attribute.
    private static final EntityDataAccessor<Float> EFFECTIVE_RANGE = SynchedEntityData.defineId(EntityTurretGolem.class, EntityDataSerializers.FLOAT);

    public EntityTurretGolem(EntityType<? extends EntityTurretGolem> type, Level level) {
        super(type, level);
        this.setEquipDropChance(0, 2.0F);
        this.sinks = 1;
        this.updateTurretStats();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetAI = new EntityAIGolemTarget(this);
        this.goalSelector.addGoal(1, new EntityAITurretAttack(this));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, this.targetAI);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_FLAGS, Byte.valueOf((byte)3));
        this.entityData.define(TARGET_MODE, Byte.valueOf((byte)0));
        this.entityData.define(EFFECTIVE_RANGE, Float.valueOf(32.0F));
    }

    /// The follow range as the server sees it, upgrade modifiers included. Client code (the range
    /// visualiser, the GUI stats panel) must read this rather than the attribute.
    public double getEffectiveRange() {
        return this.entityData.get(EFFECTIVE_RANGE).floatValue();
    }

    /// Target-category toggles (synced via entityData; set server-side via MessageTurretToggle).
    public boolean attacksHostile() { return (this.entityData.get(TARGET_FLAGS).byteValue() & 1) != 0; }
    public boolean attacksPassive() { return (this.entityData.get(TARGET_FLAGS).byteValue() & 2) != 0; }
    public boolean attacksNeutral() { return (this.entityData.get(TARGET_FLAGS).byteValue() & 4) != 0; }
    public void toggleTargetFlag(int which) {
        byte f = this.entityData.get(TARGET_FLAGS).byteValue();
        int bit = (which == 0) ? 1 : (which == 1) ? 2 : 4;
        f ^= bit;
        this.entityData.set(TARGET_FLAGS, Byte.valueOf(f));
    }

    /// Targeting-mode accessors (synced; set server-side via MessageTurretToggle which==2).
    public int getTargetMode() { return this.entityData.get(TARGET_MODE).byteValue() & 0xFF; }
    public void cycleTargetMode() {
        int next = (this.getTargetMode() + 1) % 4;
        this.entityData.set(TARGET_MODE, Byte.valueOf((byte)next));
    }

    /// Turrets ignore the global golems.* config and use their per-entity GUI toggles instead.
    @Override
    protected boolean passesTargetFilter(Entity target) {
        if (target instanceof LivingEntity && !(target instanceof Player)) {
            if (TargetHelper.isNeutralMob(target)) {
                if (!this.attacksNeutral()) return false;
            } else if (TargetHelper.isHostileMob(target)) {
                if (!this.attacksHostile()) return false;
            } else {
                if (!this.attacksPassive()) return false;
            }
        }
        return true;
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.FOLLOW_RANGE, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.isMobile()) {
            // Mobile (feather) turrets move freely; keep the anchor following them so it re-pins on removal.
            this.anchorX = this.getX();
            this.anchorZ = this.getZ();
            return;
        }
        if (Double.isNaN(this.anchorX)) {
            this.anchorX = this.getX();
            this.anchorZ = this.getZ();
        }
        if (this.getX() != this.anchorX || this.getZ() != this.anchorZ) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(0.0, motion.y, 0.0);
            this.setPos(this.anchorX, this.getY(), this.anchorZ);
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        ItemStack held = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.isEmpty() && held.getItem() == Items.FEATHER) {
            super.move(type, movement);
        }
        else {
            super.move(type, new Vec3(0.0, movement.y, 0.0));
        }
    }

    // Returns the Y Offset of this entity when riding another.
    @Override
    public double getMyRidingOffset() {
        return super.getMyRidingOffset() - this.getBbHeight() / 4.0;
    }

    @Override
    protected Item getDropItem() {
        return Blocks.DISPENSER.asItem();
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (this.random.nextFloat() < toast.utilityMobs.Properties.getDouble("turrets", "drop_chance")) {
            this.spawnAtLocation(this.getDropItem());
        }
        for (int i = 0; i < this.ammoInventory.getContainerSize(); i++) {
            ItemStack s = this.ammoInventory.getItem(i);
            if (!s.isEmpty()) this.spawnAtLocation(s.copy(), 0.0F);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.canInteract(player))
            return super.mobInteract(player, hand);
        ItemStack held = player.getItemInHand(hand);
        // Quick-apply: right-clicking with a valid upgrade item installs it directly, swapping out
        // whatever upgrade is already applied (the old one is returned to the player).
        if (!player.isShiftKeyDown() && !held.isEmpty() && EnumUpgrade.getUpgrade(this.upgrades, held) != EnumUpgrade.DEFAULT) {
            if (!this.level().isClientSide) {
                ItemStack current = this.getEquipmentInSlot(0);
                ItemStack install = held.copy();
                install.setCount(1);
                this.setCurrentItemOrArmor(0, install);
                if (!current.isEmpty() && !player.getInventory().add(current)) {
                    this.spawnAtLocation(current, 0.0F);
                }
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!player.isShiftKeyDown() && this.tryHealFromHeld(player, hand, held)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            toast.utilityMobs.network.GuiHelper.displayGUICustom(player, this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("UMTargetFlags", this.entityData.get(TARGET_FLAGS).byteValue());
        tag.putByte("UMTargetMode", this.entityData.get(TARGET_MODE).byteValue());
        ListTag ammoList = new ListTag();
        for (int i = 0; i < this.ammoInventory.getContainerSize(); i++) {
            ItemStack s = this.ammoInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte)i);
                s.save(slotTag);
                ammoList.add(slotTag);
            }
        }
        tag.put("UMAmmo", ammoList);
        if (!Double.isNaN(this.anchorX)) {
            tag.putDouble("UMAnchorX", this.anchorX);
            tag.putDouble("UMAnchorZ", this.anchorZ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("UMTargetFlags")) {
            this.entityData.set(TARGET_FLAGS, Byte.valueOf(tag.getByte("UMTargetFlags")));
        }
        if (tag.contains("UMTargetMode")) {
            this.entityData.set(TARGET_MODE, Byte.valueOf(tag.getByte("UMTargetMode")));
        }
        for (int i = 0; i < this.ammoInventory.getContainerSize(); i++) {
            this.ammoInventory.setItem(i, ItemStack.EMPTY);
        }
        if (tag.contains("UMAmmo")) {
            ListTag ammoList = tag.getList("UMAmmo", Tag.TAG_COMPOUND);
            for (int i = 0; i < ammoList.size(); i++) {
                CompoundTag slotTag = ammoList.getCompound(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot >= 0 && slot < this.ammoInventory.getContainerSize()) {
                    this.ammoInventory.setItem(slot, ItemStack.of(slotTag));
                }
            }
        }
        if (tag.contains("UMAnchorX")) {
            this.anchorX = tag.getDouble("UMAnchorX");
            this.anchorZ = tag.getDouble("UMAnchorZ");
        }
        this.updateTurretStats();
    }

    /// Sets the equipped item at the given index to the given item stack. The upgrade slot (index 0)
    /// is the single choke point for BOTH install paths (right-click quick-apply and the GUI slot), so
    /// the equip particle/sound fires here whenever the upgrade actually changes to a new, non-default
    /// one. Vanilla NBT load restores equipment via setItemSlot (not this method), so reloads
    /// don't replay the effect; removing the upgrade leaves a DEFAULT upgrade, which is also skipped.
    @Override
    public void setCurrentItemOrArmor(int index, ItemStack itemStack) {
        EnumUpgrade prev = this.upgrade;
        super.setCurrentItemOrArmor(index, itemStack);
        if (index == 0) {
            this.updateTurretStats();
            if (!this.level().isClientSide && this.upgrade != EnumUpgrade.DEFAULT && this.upgrade != prev) {
                this.playUpgradeEquipFx(this.upgrade);
            }
        }
    }

    /// Server-side burst of upgrade-themed particles + a click sound when an upgrade is installed.
    private void playUpgradeEquipFx(EnumUpgrade up) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double y = this.getY() + this.getBbHeight() * 0.6;
        up.spawnEquipParticles(serverLevel, this.getX(), y, this.getZ());
        this.level().playSound(null, this.getX(), y, this.getZ(), SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    /// Updates this turret's range and effect based on its held Items.
    public void updateTurretStats() {
        AttributeInstance range = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) {
            range.removeModifier(EntityTurretGolem.sightBoost);
        }
        this.upgrade = EnumUpgrade.getUpgrade(this.upgrades, this.getEquipmentInSlot(0));
        if (this.upgrade == EnumUpgrade.SIGHT && range != null) {
            range.addTransientModifier(EntityTurretGolem.sightBoost);
        }
        // Push the result to the client, which cannot derive it: see EFFECTIVE_RANGE.
        if (range != null && !this.level().isClientSide) {
            this.entityData.set(EFFECTIVE_RANGE, Float.valueOf((float)range.getValue()));
        }
    }

    /// True when this turret is allowed to move freely (feather upgrade in the main hand).
    private boolean isMobile() {
        ItemStack held = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return !held.isEmpty() && held.getItem() == Items.FEATHER;
    }

    /// Ammo model accessors. getAmmoItem/getAmmoPerShot are overridable per turret type.
    public SimpleContainer getAmmoInventory() { return this.ammoInventory; }
    public Item getAmmoItem() { return Items.ARROW; }
    /// Ammo drawn per attack == projectiles fired that attack, so a shotgun/volley (6 shots) burns 6 ammo
    /// and a single-shot turret burns 1. Overridable if a type ever decouples the two.
    public int getAmmoPerShot() { return this.getProjectileCount(); }
    public boolean requiresAmmo() { return toast.utilityMobs.Properties.getBoolean("turrets", "require_ammo"); }

    /// True if the ammo inventory holds at least one matching ammo item.
    public boolean hasAmmo() {
        Item want = this.getAmmoItem();
        for (int i = 0; i < this.ammoInventory.getContainerSize(); i++) {
            ItemStack s = this.ammoInventory.getItem(i);
            if (!s.isEmpty() && s.getItem() == want) return true;
        }
        return false;
    }

    /// Removes up to `count` matching ammo items, dispenser-style (random matching slot each removal).
    public void consumeAmmo(int count) {
        Item want = this.getAmmoItem();
        for (int n = 0; n < count; n++) {
            java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
            for (int i = 0; i < this.ammoInventory.getContainerSize(); i++) {
                ItemStack s = this.ammoInventory.getItem(i);
                if (!s.isEmpty() && s.getItem() == want) slots.add(Integer.valueOf(i));
            }
            if (slots.isEmpty()) return;
            int slot = slots.get(this.random.nextInt(slots.size())).intValue();
            this.ammoInventory.removeItem(slot, 1);
        }
    }

    /// Marks a fired arrow as pickupable when the ammo economy is active; otherwise leaves it non-pickupable.
    protected void prepareFiredArrow(AbstractArrow arrow) {
        if (this.requiresAmmo()) {
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        }
    }

    /// Horizontal distance from the turret's centre out to the mouth of the barrel. The head cube is
    /// 12 model units across, i.e. 0.375 blocks of half-width, so this clears it with room to spare.
    protected static final double MUZZLE_OFFSET = 0.75;

    /// Moves a freshly built projectile out to the barrel mouth, along the horizontal line to the
    /// target. Differs from 1.12.2, kept deliberately: the original spawned every projectile at the
    /// shooter's eye point, which sits inside the head cube, so shots appeared to squeeze out of the
    /// middle of the model instead of leaving the barrel, and read as coming from behind the turret.
    /// Callers measure their aim vector from the returned projectile, not from the turret centre.
    protected <T extends Entity> T atMuzzle(T projectile, Entity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz > 1.0E-4) {
            projectile.setPos(
                projectile.getX() + dx / horiz * EntityTurretGolem.MUZZLE_OFFSET,
                projectile.getY(),
                projectile.getZ() + dz / horiz * EntityTurretGolem.MUZZLE_OFFSET);
        }
        return projectile;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (!this.ammoHandler.isPresent()) {
                this.ammoHandler = LazyOptional.of(() -> new InvWrapper(this.ammoInventory));
            }
            return this.ammoHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.ammoHandler.invalidate();
    }

    /// Arrow/snowball spread model (vanilla projectile "inaccuracy"; higher = wider = less accurate).
    /// The effective spread at a horizontal distance d is
    ///   min(maxInaccuracy, baseInaccuracy + d * inaccuracyFalloff)
    /// Defaults (0 / 1.5 / 12) reproduce the old base/gatling/fire/snow behaviour: pinpoint at point
    /// blank (so hugging mobs are hit), widening with range up to a cap.
    public float getBaseInaccuracy() { return 0.0F; }
    public float getInaccuracyFalloff() { return 1.5F; }
    public float getMaxInaccuracy() { return 12.0F; }
    /// Effective spread at the given horizontal distance.
    public float inaccuracyAt(double dist) {
        return (float)Math.min(this.getMaxInaccuracy(), this.getBaseInaccuracy() + dist * this.getInaccuracyFalloff());
    }

    /// Solid (standable) when the turrets.collision config is on - lets players build turret walkways.
    @Override
    public boolean canBeCollidedWith() {
        return EntityTurretGolem.collision && this.isAlive();
    }

    /// Stats model accessors (single source of truth, consumed by TurretStats + GUI).
    public double getProjectileDamage() { return 2.0; }   // vanilla arrow default damage
    public int getProjectileCount() { return 1; }
    public double getProjectileVelocity() { return 1.6; }
    public int getMaxAttackTime() { return this.maxAttackTime; }
    public double getBaseRange() {
        AttributeInstance range = this.getAttribute(Attributes.FOLLOW_RANGE);
        return range == null ? 10.0 : range.getBaseValue();
    }
    public boolean isArrowBased() { return true; }
    public double getDisplayDamageOverride() { return 0.0; }
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.emptyList(); }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(LivingEntity target) {
        if (!this.level().isClientSide) {
            for (int i = this.getProjectileCount(); i-- > 0;) {
                EntityTurretArrow arrow = this.atMuzzle(new EntityTurretArrow(this.level(), this), target);
                double dx = target.getX() - arrow.getX();
                // Aim at the target's CENTER of mass, not its lower third. From the turret's high barrel
                // a lower-third aim point dives steeply at point-blank range and the shot passes under or
                // past a mob hugging the base; centre mass keeps the shot in the hitbox at any range.
                double dy = (target.getBoundingBox().minY + target.getBbHeight() * 0.5F) - arrow.getY();
                double dz = target.getZ() - arrow.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                // Flatter arc + spread that scales with distance: near-zero jitter point-blank (so hugging
                // mobs are hit reliably), the usual spread far out.
                arrow.shoot(dx, dy + dist * 0.15, dz, (float)this.getProjectileVelocity(), this.inaccuracyAt(dist));
                arrow.setBaseDamage(this.getProjectileDamage());
                this.targetHelper.setOwned(arrow);
                this.upgrade.applyToArrow(arrow);
                this.prepareFiredArrow(arrow);
                this.level().addFreshEntity(arrow);
            }
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }
}
