package toast.utilityMobs.golem;

import java.util.EnumSet;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.common.ForgeHooks;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;
import toast.utilityMobs.network.GuiHelper;

public class EntitySteamGolem extends EntityLargeGolem implements Container, net.minecraft.world.MenuProvider
{
    /// Server side of opening this golem's screen. 1.12.2 routed this through IGuiHandler; 1.20.1 wants
    /// the entity itself to be the MenuProvider. getDisplayName is inherited from Entity.
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInv, Player player) {
        return new ContainerSteamGolem(containerId, playerInv, this);
    }

    /// The texture for this class.
    public static final ResourceLocation[] TEXTURES = {
        new ResourceLocation(_UtilityMobs.TEXTURE + "golem/steamgolem.png"),
        new ResourceLocation(_UtilityMobs.TEXTURE + "golem/steamgolem_fire.png")
    };

    /// The number of ticks that the furnace will keep burning.
    public int burnTime = 0;
    /// The number of ticks that a fresh copy of the currently-burning item would burn for.
    public int maxBurnTime = 0;

    /// burningState; While this is 1, the golem will be in its "on" state.
    private static final EntityDataAccessor<Byte> BURNING = SynchedEntityData.defineId(EntitySteamGolem.class, EntityDataSerializers.BYTE);

    // The contents of this furnace.
    private NonNullList<ItemStack> contents;

    // Registered entity size: 1.4 x 2.9 (set via EntityType.Builder.sized at registration).
    public EntitySteamGolem(EntityType<? extends EntitySteamGolem> type, Level level) {
        super(type, level);
        this.texture = EntitySteamGolem.TEXTURES[0];
        this.contents = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, this.sitAI());
        this.sitAI().setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        this.sitAI().sitAnywhere = true;
        this.goalSelector.addGoal(2, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 0.6));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityLargeGolem.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    /// Returns the armor of this entity.
    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 2);
    }

    // Used to initialize data manager variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BURNING, Byte.valueOf((byte)0));
    }

    /// Gets/sets this steam golem's burningState variable. Used for rendering.
    public boolean getBurningState() {
        return this.entityData.get(BURNING).byteValue() == 1;
    }
    public void setBurningState(boolean state) {
        this.entityData.set(BURNING, Byte.valueOf(state ? (byte)1 : (byte)0));
    }

    @Override
    protected Item getDropItem() {
        return Items.FURNACE;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (this.random.nextInt(2) == 0) {
            this.spawnAtLocation(this.getDropItem());
        }
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty()) {
                ItemStack split = stack.copy();
                while (stack.getCount() > 0) {
                    int splitSize = this.random.nextInt(21) + 10;
                    if (splitSize > stack.getCount()) {
                        splitSize = stack.getCount();
                    }
                    stack.shrink(splitSize);
                    split.setCount(splitSize);
                    this.spawnAtLocation(split.copy(), 0.0F);
                }
                this.contents.set(i, ItemStack.EMPTY);
            }
        }
    }

    // ---- Container implementation ----

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.contents.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.contents, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.contents, slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.contents.set(slot, itemStack);
        if (!itemStack.isEmpty() && itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.canInteract(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return AbstractFurnaceBlockEntity.isFuel(itemStack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.contents.size(); i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void setChanged() {
        // Do nothing
    }

    // ---- Interaction ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.canInteract(player) && !player.isShiftKeyDown()) {
            if (this.openGUI(player))
                return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /// Opens this golem's GUI.
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            GuiHelper.displayGUICustom(player, this);
        }
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }

    /// Called each tick this entity exists.
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.texture = this.getBurningState() ? EntitySteamGolem.TEXTURES[1] : EntitySteamGolem.TEXTURES[0];
            if (this.getBurningState()) {
                this.spawnFurnaceFX();
            }
        }
    }

    /// Emits the lit-furnace fire crackle + flame/smoke particles from the golem's furnace face while
    /// powered. Mirrors vanilla FurnaceBlock.animateTick exactly - one flame + one smoke per tick and a
    /// 10% chance of the crackle sound - so the rate and volume match a real lit furnace.
    /// Called only from the client branch of tick; every API used here is common-side (no-ops server).
    private void spawnFurnaceFX() {
        float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        // Chest/furnace face: half the body height up, pushed out to the front of the model.
        double faceX = this.getX() + forwardX * 0.55;
        double faceY = this.getY() + this.getBbHeight() * 0.5;
        double faceZ = this.getZ() + forwardZ * 0.55;
        // Spread sideways across the face (perpendicular to facing) and a little vertically.
        double side = (this.random.nextDouble() - 0.5) * 0.6;
        double px = faceX + forwardZ * side;
        double pz = faceZ - forwardX * side;
        double py = faceY + (this.random.nextDouble() - 0.3) * 0.4;
        if (this.random.nextDouble() < 0.1) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.FURNACE_FIRE_CRACKLE,
                SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        this.level().addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0, 0.0, 0.0);
        this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.0, 0.0);
    }

    /// Called each tick this entity is alive.
    @Override
    public void aiStep() {
        if (this.burnTime > 0) {
            this.burnTime--;
        }
        if (!this.level().isClientSide) {
            // Burn fuel from whichever slot holds it. We consume in place rather than shuffling fuel
            // into a fixed "burn" slot every tick - that old per-tick swap both made placed fuel jump
            // to the middle slot and mutated the open inventory mid-interaction, which desynced the GUI
            // and could visually dupe the stack. Now fuel stays exactly where the player put it.
            if (this.burnTime == 0) {
                for (int slot = 0; slot < this.contents.size(); slot++) {
                    ItemStack fuel = this.getItem(slot);
                    int burn = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                    if (!fuel.isEmpty() && burn > 0) {
                        this.maxBurnTime = this.burnTime = burn;
                        Item container = fuel.getItem().getCraftingRemainingItem();
                        fuel.shrink(1);
                        if (fuel.isEmpty()) {
                            this.setItem(slot, container == null ? ItemStack.EMPTY : new ItemStack(container));
                        }
                        break;
                    }
                }
            }
            boolean burnState = this.burnTime > 0;
            if (this.getBurningState() != burnState) {
                this.setBurningState(burnState);
            }
            this.sitAI().sit = !burnState;

            // Auto-close the GUI for any viewer who has walked too far away (mirrors vanilla horse/llama
            // inventories, which close past ~8 blocks). Without this the menu could stay open on a golem
            // that wandered off.
            for (Player viewer : this.level().players()) {
                if (viewer.containerMenu instanceof ContainerSteamGolem menu
                        && menu.getGolem() == this
                        && (!this.isAlive() || this.distanceToSqr(viewer) > 64.0)) {
                    viewer.closeContainer();
                }
            }
        }
        super.aiStep();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag tagList = new ListTag();
        for (int slot = 0; slot < this.contents.size(); slot++) {
            if (!this.contents.get(slot).isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte)slot);
                this.contents.get(slot).save(slotTag);
                tagList.add(slotTag);
            }
        }
        tag.put("Items", tagList);
        tag.putShort("BurnTime", (short)this.burnTime);
        tag.putShort("MaxBurnTime", (short)this.maxBurnTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag tagList = tag.getList("Items", Tag.TAG_COMPOUND);
        this.contents = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag slotTag = tagList.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.contents.size()) {
                this.contents.set(slot, ItemStack.of(slotTag));
            }
        }
        this.burnTime = tag.getShort("BurnTime");
        this.maxBurnTime = tag.getShort("MaxBurnTime");
    }

    // Returns an integer between 0 and the passed value representing how much burn time is left on the current fuel.
    public int getBurnTimeRemainingScaled(int max) {
        if (this.maxBurnTime == 0) {
            this.maxBurnTime = 200;
        }
        return this.burnTime * max / this.maxBurnTime;
    }
}
