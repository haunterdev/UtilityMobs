package toast.utilityMobs.turret;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import toast.utilityMobs.EnumUpgrade;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAITurretAttack;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityTurretGolem extends EntityUtilityGolem
{
    /// All applicable upgrades.
    public static final EnumUpgrade[] upgradesAll = {
        EnumUpgrade.KILLER, EnumUpgrade.FIRE, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
    };
    /// The UUID for the sight upgrade's modifier.
    private static final UUID sightBoostUUID = UUID.fromString("70A27B59-9566-4402-BC1F-2EE2A276D836");
    /// The modifier applied by the sight upgrade. Operation 1 (multiply base) with amount 1.0 DOUBLES the
    /// turret's follow range (base + base*1.0): a 10-range turret becomes 20, a 20-range turret becomes 40.
    private static final AttributeModifier sightBoost = new AttributeModifier(EntityTurretGolem.sightBoostUUID, "Ender pearl upgrade", 1.0, 1).setSaved(false);
    /// When true (turrets.collision config), turrets are solid and can be stood on / walked across.
    public static boolean collision = false;

    /// Possible upgrades for this turret type.
    public EnumUpgrade[] upgrades = {
            EnumUpgrade.KILLER, EnumUpgrade.FIRE, EnumUpgrade.FEATHER, EnumUpgrade.SLOW, EnumUpgrade.EGG, EnumUpgrade.SIGHT, EnumUpgrade.EXPLOSIVE, EnumUpgrade.POISON, EnumUpgrade.FIRE_EXPLOSIVE
    };
    /// This turret's targeting AI.
    public EntityAIGolemTarget targetAI = new EntityAIGolemTarget(this);
    /// This turret's current upgrade.
    public EnumUpgrade upgrade;
    /// Attack time counter.
    public int maxAttackTime = 60;
    /// 9-slot ammo inventory (always present; only required to fire when the require_ammo config is on).
    private final InventoryBasic ammoInventory = new InventoryBasic("ammo", false, 9);
    /// Lazily-built capability wrapper exposing the ammo inventory to hoppers/pipes.
    private net.minecraftforge.items.IItemHandler ammoHandler;
    /// World-space anchor the turret is pinned to when not feather-equipped. Double.NaN sentinel = not yet captured.
    private double anchorX = Double.NaN;
    private double anchorZ = Double.NaN;
    /// Target category flags: bit0 = attack hostile (IMob), bit1 = attack passive. Default both on.
    private static final DataParameter<Byte> TARGET_FLAGS = EntityDataManager.createKey(EntityTurretGolem.class, DataSerializers.BYTE);
    /// Targeting mode: 0=CLOSE (nearest), 1=FAR (farthest), 2=STRONG (max health), 3=WEAK (min health).
    private static final DataParameter<Byte> TARGET_MODE = EntityDataManager.createKey(EntityTurretGolem.class, DataSerializers.BYTE);

    public EntityTurretGolem(World world) {
        super(world);
        this.setEquipDropChance(0, 2.0F);
        this.sinks = 1;
        this.tasks.addTask(1, new EntityAITurretAttack(this));
        this.tasks.addTask(2, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, this.targetAI);
        this.updateTurretStats();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TARGET_FLAGS, Byte.valueOf((byte)3));
        this.dataManager.register(TARGET_MODE, Byte.valueOf((byte)0));
    }

    /// Target-category toggles (synced via dataManager; set server-side via MessageTurretToggle).
    public boolean attacksHostile() { return (this.dataManager.get(TARGET_FLAGS).byteValue() & 1) != 0; }
    public boolean attacksPassive() { return (this.dataManager.get(TARGET_FLAGS).byteValue() & 2) != 0; }
    public boolean attacksNeutral() { return (this.dataManager.get(TARGET_FLAGS).byteValue() & 4) != 0; }
    public void toggleTargetFlag(int which) {
        byte f = this.dataManager.get(TARGET_FLAGS).byteValue();
        int bit = (which == 0) ? 1 : (which == 1) ? 2 : 4;
        f ^= bit;
        this.dataManager.set(TARGET_FLAGS, Byte.valueOf(f));
    }

    /// Targeting-mode accessors (synced; set server-side via MessageTurretToggle which==2).
    public int getTargetMode() { return this.dataManager.get(TARGET_MODE).byteValue() & 0xFF; }
    public void cycleTargetMode() {
        int next = (this.getTargetMode() + 1) % 4;
        this.dataManager.set(TARGET_MODE, Byte.valueOf((byte)next));
    }

    /// Turrets ignore the global golems.* config and use their per-entity GUI toggles instead.
    @Override
    protected boolean passesTargetFilter(Entity target) {
        if (target instanceof EntityLivingBase && !(target instanceof EntityPlayer)) {
            if (toast.utilityMobs.TargetHelper.isNeutralMob(target)) {
                if (!this.attacksNeutral()) return false;
            } else if (target instanceof IMob) {
                if (!this.attacksHostile()) return false;
            } else {
                if (!this.attacksPassive()) return false;
            }
        }
        return true;
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(10.0);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            return;
        }
        if (this.isMobile()) {
            // Mobile (feather) turrets move freely; keep the anchor following them so it re-pins on removal.
            this.anchorX = this.posX;
            this.anchorZ = this.posZ;
            return;
        }
        if (Double.isNaN(this.anchorX)) {
            this.anchorX = this.posX;
            this.anchorZ = this.posZ;
        }
        if (this.posX != this.anchorX || this.posZ != this.anchorZ) {
            this.motionX = 0.0;
            this.motionZ = 0.0;
            this.setPosition(this.anchorX, this.posY, this.anchorZ);
        }
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!held.isEmpty() && held.getItem() == Items.FEATHER) {
            super.move(type, x, y, z);
        }
        else {
            super.move(type, 0.0, y, 0.0);
        }
    }

    // Returns the Y Offset of this entity when riding another.
    @Override
    public double getYOffset() {
        return super.getYOffset() - this.height / 4.0;
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.DISPENSER);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (this.rand.nextFloat() < toast.utilityMobs.Properties.getDouble("turrets", "drop_chance")) {
            this.dropItem(this.getDropItem(), 1);
        }
        for (int i = 0; i < this.ammoInventory.getSizeInventory(); i++) {
            ItemStack s = this.ammoInventory.getStackInSlot(i);
            if (!s.isEmpty()) this.entityDropItem(s.copy(), 0.0F);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!this.canInteract(player))
            return super.processInteract(player, hand);
        ItemStack held = player.getHeldItem(hand);
        // Quick-apply: right-clicking with a valid upgrade item installs it directly, swapping out
        // whatever upgrade is already applied (the old one is returned to the player).
        if (!player.isSneaking() && !held.isEmpty() && EnumUpgrade.getUpgrade(this.upgrades, held) != EnumUpgrade.DEFAULT) {
            if (!this.world.isRemote) {
                ItemStack current = this.getEquipmentInSlot(0);
                ItemStack install = held.copy();
                install.setCount(1);
                this.setCurrentItemOrArmor(0, install);
                if (!current.isEmpty() && !player.addItemStackToInventory(current)) {
                    this.entityDropItem(current, 0.0F);
                }
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        if (!player.isSneaking() && this.tryHealFromHeld(player, hand, held)) {
            return true;
        }
        if (!this.world.isRemote) {
            toast.utilityMobs.network.GuiHelper.displayGUICustom(player, this);
        }
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setByte("UMTargetFlags", this.dataManager.get(TARGET_FLAGS).byteValue());
        tag.setByte("UMTargetMode", this.dataManager.get(TARGET_MODE).byteValue());
        net.minecraft.nbt.NBTTagList ammoList = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < this.ammoInventory.getSizeInventory(); i++) {
            ItemStack s = this.ammoInventory.getStackInSlot(i);
            if (!s.isEmpty()) {
                net.minecraft.nbt.NBTTagCompound slotTag = new net.minecraft.nbt.NBTTagCompound();
                slotTag.setByte("Slot", (byte)i);
                s.writeToNBT(slotTag);
                ammoList.appendTag(slotTag);
            }
        }
        tag.setTag("UMAmmo", ammoList);
        if (!Double.isNaN(this.anchorX)) {
            tag.setDouble("UMAnchorX", this.anchorX);
            tag.setDouble("UMAnchorZ", this.anchorZ);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("UMTargetFlags")) {
            this.dataManager.set(TARGET_FLAGS, Byte.valueOf(tag.getByte("UMTargetFlags")));
        }
        if (tag.hasKey("UMTargetMode")) {
            this.dataManager.set(TARGET_MODE, Byte.valueOf(tag.getByte("UMTargetMode")));
        }
        for (int i = 0; i < this.ammoInventory.getSizeInventory(); i++) {
            this.ammoInventory.setInventorySlotContents(i, ItemStack.EMPTY);
        }
        if (tag.hasKey("UMAmmo")) {
            net.minecraft.nbt.NBTTagList ammoList = tag.getTagList("UMAmmo", 10);
            for (int i = 0; i < ammoList.tagCount(); i++) {
                net.minecraft.nbt.NBTTagCompound slotTag = ammoList.getCompoundTagAt(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot >= 0 && slot < this.ammoInventory.getSizeInventory()) {
                    this.ammoInventory.setInventorySlotContents(slot, new ItemStack(slotTag));
                }
            }
        }
        if (tag.hasKey("UMAnchorX")) {
            this.anchorX = tag.getDouble("UMAnchorX");
            this.anchorZ = tag.getDouble("UMAnchorZ");
        }
        this.updateTurretStats();
    }

    /// Sets the equipped item at the given index to the given item stack. The upgrade slot (index 0)
    /// is the single choke point for BOTH install paths (right-click quick-apply and the GUI slot), so
    /// the equip particle/sound fires here whenever the upgrade actually changes to a new, non-default
    /// one. Vanilla NBT load restores equipment via setItemStackToSlot (not this method), so reloads
    /// don't replay the effect; removing the upgrade leaves a DEFAULT upgrade, which is also skipped.
    @Override
    public void setCurrentItemOrArmor(int index, ItemStack itemStack) {
        EnumUpgrade prev = this.upgrade;
        super.setCurrentItemOrArmor(index, itemStack);
        if (index == 0) {
            this.updateTurretStats();
            if (!this.world.isRemote && this.upgrade != EnumUpgrade.DEFAULT && this.upgrade != prev) {
                this.playUpgradeEquipFx(this.upgrade);
            }
        }
    }

    /// Server-side burst of upgrade-themed particles + a click sound when an upgrade is installed.
    private void playUpgradeEquipFx(EnumUpgrade up) {
        if (!(this.world instanceof net.minecraft.world.WorldServer)) {
            return;
        }
        double y = this.posY + this.height * 0.6;
        up.spawnEquipParticles((net.minecraft.world.WorldServer)this.world, this.posX, y, this.posZ);
        this.world.playSound(null, this.posX, y, this.posZ, net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                net.minecraft.util.SoundCategory.BLOCKS, 0.7F, 1.0F);
    }

    /// Updates this turret's range and effect based on its held Items.
    public void updateTurretStats() {
        IAttributeInstance range = this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        range.removeModifier(EntityTurretGolem.sightBoost);
        this.upgrade = EnumUpgrade.getUpgrade(this.upgrades, this.getEquipmentInSlot(0));
        if (this.upgrade == EnumUpgrade.SIGHT) {
            range.applyModifier(EntityTurretGolem.sightBoost);
        }
    }

    /// True when this turret is allowed to move freely (feather upgrade in the main hand).
    private boolean isMobile() {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        return !held.isEmpty() && held.getItem() == Items.FEATHER;
    }

    /// Ammo model accessors. getAmmoItem/getAmmoPerShot are overridable per turret type (defaults retunable).
    public InventoryBasic getAmmoInventory() { return this.ammoInventory; }
    public net.minecraft.item.Item getAmmoItem() { return net.minecraft.init.Items.ARROW; }
    /// Ammo drawn per attack == projectiles fired that attack, so a shotgun/volley (6 shots) burns 6 ammo
    /// and a single-shot turret burns 1. Overridable if a type ever decouples the two.
    public int getAmmoPerShot() { return this.getProjectileCount(); }
    public boolean requiresAmmo() { return toast.utilityMobs.Properties.getBoolean("turrets", "require_ammo"); }

    /// True if the ammo inventory holds at least one matching ammo item.
    public boolean hasAmmo() {
        net.minecraft.item.Item want = this.getAmmoItem();
        for (int i = 0; i < this.ammoInventory.getSizeInventory(); i++) {
            ItemStack s = this.ammoInventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == want) return true;
        }
        return false;
    }

    /// Removes up to `count` matching ammo items, dispenser-style (random matching slot each removal).
    public void consumeAmmo(int count) {
        net.minecraft.item.Item want = this.getAmmoItem();
        for (int n = 0; n < count; n++) {
            java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
            for (int i = 0; i < this.ammoInventory.getSizeInventory(); i++) {
                ItemStack s = this.ammoInventory.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() == want) slots.add(Integer.valueOf(i));
            }
            if (slots.isEmpty()) return;
            int slot = slots.get(this.rand.nextInt(slots.size())).intValue();
            this.ammoInventory.decrStackSize(slot, 1);
        }
    }

    /// Marks a fired arrow as pickupable when the ammo economy is active; otherwise leaves it non-pickupable.
    protected void prepareFiredArrow(net.minecraft.entity.projectile.EntityArrow arrow) {
        if (this.requiresAmmo()) {
            arrow.pickupStatus = net.minecraft.entity.projectile.EntityArrow.PickupStatus.ALLOWED;
        }
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (this.ammoHandler == null) this.ammoHandler = new InvWrapper(this.ammoInventory);
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.ammoHandler);
        }
        return super.getCapability(capability, facing);
    }

    /// Arrow/snowball spread model (vanilla projectile "inaccuracy"; higher = wider = less accurate).
    /// Centralised here so every turret's spread is one overridable curve and the GUI can display an
    /// accuracy stat from the same numbers. The effective spread at a horizontal distance d is
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
    /// Mirrors EntityColossalGolem; null (vanilla default) means non-solid.
    @Override
    public net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox() {
        return EntityTurretGolem.collision ? this.getEntityBoundingBox() : super.getCollisionBoundingBox();
    }

    /// Stats model accessors (single source of truth, consumed by TurretStats + GUI).
    public double getProjectileDamage() { return 2.0; }   // EntityArrow default damage
    public int getProjectileCount() { return 1; }
    public double getProjectileVelocity() { return 1.6; }
    public int getMaxAttackTime() { return this.maxAttackTime; }
    public double getBaseRange() {
        return this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getBaseValue();
    }
    public boolean isArrowBased() { return true; }
    public double getDisplayDamageOverride() { return 0.0; }
    public java.util.List<String> getBaseEffectKeys() { return java.util.Collections.emptyList(); }

    /// Executes this golem's ranged attack.
    @Override
    public void doRangedAttack(EntityLivingBase target) {
        if (!this.world.isRemote) {
            for (int i = this.getProjectileCount(); i-- > 0;) {
                EntityTippedArrow arrow = new EntityTurretArrow(this.world, this);
                double dx = target.posX - this.posX;
                // Aim at the target's CENTER of mass, not its lower third. From the turret's high barrel
                // a lower-third aim point dives steeply at point-blank range and the shot passes under or
                // past a mob hugging the base; centre mass keeps the shot in the hitbox at any range.
                double dy = (target.getEntityBoundingBox().minY + target.height * 0.5F) - arrow.posY;
                double dz = target.posZ - this.posZ;
                double dist = (double)MathHelper.sqrt(dx * dx + dz * dz);
                // Flatter arc + spread that scales with distance: near-zero jitter point-blank (so hugging
                // mobs are hit reliably), the usual spread far out.
                arrow.shoot(dx, dy + dist * 0.15, dz, (float)this.getProjectileVelocity(), this.inaccuracyAt(dist));
                arrow.setDamage(this.getProjectileDamage());
                this.targetHelper.setOwned(arrow);
                this.upgrade.applyToArrow(arrow);
                this.prepareFiredArrow(arrow);
                this.world.spawnEntity(arrow);
            }
        }
        this.playSound(UMSound.BOW, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
    }
}
