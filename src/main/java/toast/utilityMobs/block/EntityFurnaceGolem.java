package toast.utilityMobs.block;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;

public class EntityFurnaceGolem extends EntityContainerGolem implements WorldlyContainer
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: furnace.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.STONE;
    }

    /// The textures for this class.
    public static final ResourceLocation[] TEXTURES = {
        new ResourceLocation(_UtilityMobs.TEXTURE + "block/furnacegolem.png"),
        new ResourceLocation(_UtilityMobs.TEXTURE + "block/furnacegolem_fire.png")
    };

    /// How long one smelt takes. Vanilla's cooking total time, which 1.12.2 hardcoded as 200.
    private static final int COOK_TIME_TOTAL = 200;

    /// burningState; While this is 1, the furnace will be in its "on" state.
    private static final EntityDataAccessor<Byte> BURNING = SynchedEntityData.defineId(EntityFurnaceGolem.class, EntityDataSerializers.BYTE);

    /// The number of ticks that the furnace will keep burning.
    public int burnTime = 0;
    /// The number of ticks that a fresh copy of the currently-burning item would burn for.
    public int itemBurnTime = 0;
    /// The number of ticks that the current item has been cooking for.
    public int cookTime = 0;

    /// The four values vanilla's FurnaceMenu syncs to the screen: lit time, lit duration, cooking
    /// progress, cooking total. 1.12.2 sent the same numbers through Container.detectAndSendChanges.
    private final ContainerData furnaceData = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return EntityFurnaceGolem.this.burnTime;
                case 1: return EntityFurnaceGolem.this.itemBurnTime;
                case 2: return EntityFurnaceGolem.this.cookTime;
                case 3: return EntityFurnaceGolem.COOK_TIME_TOTAL;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: EntityFurnaceGolem.this.burnTime = value; break;
                case 1: EntityFurnaceGolem.this.itemBurnTime = value; break;
                case 2: EntityFurnaceGolem.this.cookTime = value; break;
                default: break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    // Registered entity size: 0.9375 x 0.9375 (set via EntityType.Builder.sized at registration).
    public EntityFurnaceGolem(EntityType<? extends EntityFurnaceGolem> type, Level level) {
        super(type, level);
        this.texture = EntityFurnaceGolem.TEXTURES[0];
    }

    /// 1.12.2 set isImmuneToFire in the constructor.
    @Override
    public boolean fireImmune() {
        return true;
    }

    public ContainerData getFurnaceData() {
        return this.furnaceData;
    }

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

    // Not ported: 1.12.2's `isBurning() { return getBurningState(); }`. In 1.12.2 that drove the texture
    // swap, but it also fed the renderer's fire overlay, so a smelting furnace golem was wrapped in
    // flames as though it were burning to death. The texture swap is done directly in tick(), and
    // spawnFurnaceFX below emits the lit-furnace particles the steam golem already uses, which is what
    // the effect was meant to convey. Differs from 1.12.2, deliberate.

    /// Gets/sets this furnace golem's burningState variable. Used for rendering.
    public boolean getBurningState() {
        return this.entityData.get(BURNING).byteValue() == 1;
    }
    public void setBurningState(boolean state) {
        this.entityData.set(BURNING, Byte.valueOf(state ? (byte)1 : (byte)0));
    }

    /// Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == 2 ? false : slot == 1 ? AbstractFurnaceBlockEntity.isFuel(itemStack) : true;
    }

    /// Returns an array containing the indices of the slots that can be accessed by automation on the given side of this block.
    @Override /// WorldlyContainer
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[] { 2, 1 } : side == Direction.UP ? new int[] { 0 } : new int[] { 1 };
    }

    /// Returns true if automation can insert the given item in the given slot from the given side.
    @Override /// WorldlyContainer
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction side) {
        return this.canPlaceItem(slot, itemStack);
    }

    /// Returns true if automation can extract the given item in the given slot from the given side.
    @Override /// WorldlyContainer
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction side) {
        return side != Direction.DOWN || slot != 1 || itemStack.getItem() == Items.BUCKET;
    }

    /// Returns the number of slots in the inventory.
    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    protected Item getDropItem() {
        return Items.FURNACE;
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            GuiHelper.displayGUIFurnace(player, this);
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
            this.texture = this.getBurningState() ? EntityFurnaceGolem.TEXTURES[1] : EntityFurnaceGolem.TEXTURES[0];
            if (this.getBurningState()) {
                this.spawnFurnaceFX();
            }
        }
    }

    /// Emits the lit-furnace crackle plus flame/smoke from the golem's furnace face while smelting.
    /// Same rates as vanilla FurnaceBlock.animateTick and as EntitySteamGolem.spawnFurnaceFX: one flame
    /// and one smoke per tick, 10% chance of the crackle. Client-only, called from tick's client branch.
    private void spawnFurnaceFX() {
        float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
        double forwardX = -net.minecraft.util.Mth.sin(yaw);
        double forwardZ = net.minecraft.util.Mth.cos(yaw);
        double faceX = this.getX() + forwardX * 0.55;
        double faceY = this.getY() + this.getBbHeight() * 0.5;
        double faceZ = this.getZ() + forwardZ * 0.55;
        double side = (this.random.nextDouble() - 0.5) * 0.6;
        double px = faceX + forwardZ * side;
        double pz = faceZ - forwardX * side;
        double py = faceY + (this.random.nextDouble() - 0.3) * 0.4;
        if (this.random.nextDouble() < 0.1) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, px, py, pz, 0.0, 0.0, 0.0);
        this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, px, py, pz, 0.0, 0.0, 0.0);
    }

    /// Called each tick this entity is alive.
    @Override
    public void aiStep() {
        if (this.burnTime > 0) {
            this.burnTime--;
        }
        if (!this.level().isClientSide) {
            if (this.burnTime == 0 && this.canSmelt()) {
                ItemStack fuelStack = this.getItem(1);
                this.itemBurnTime = this.burnTime = EntityFurnaceGolem.burnDuration(fuelStack);
                if (this.burnTime > 0 && !fuelStack.isEmpty()) {
                    Item fuelItem = fuelStack.getItem();
                    fuelStack.shrink(1);
                    if (fuelStack.isEmpty()) {
                        this.setItem(1, fuelItem.getCraftingRemainingItem() == null
                            ? ItemStack.EMPTY
                            : new ItemStack(fuelItem.getCraftingRemainingItem()));
                    }
                }
            }
            if (this.getBurningState() && this.canSmelt()) {
                this.cookTime++;
                if (this.cookTime == EntityFurnaceGolem.COOK_TIME_TOTAL) {
                    this.cookTime = 0;
                    this.smeltItem();
                }
            }
            else {
                this.cookTime = 0;
            }
            boolean burnState = this.burnTime > 0;
            if (this.getBurningState() != burnState) {
                this.setBurningState(burnState);
            }
        }
        super.aiStep();
    }

    /// How long the given stack burns for. 1.12.2 asked TileEntityFurnace directly; the Forge hook is
    /// the 1.20.1 equivalent and is what honours other mods' fuels.
    private static int burnDuration(ItemStack fuelStack) {
        return ForgeHooks.getBurnTime(fuelStack, RecipeType.SMELTING);
    }

    /// The smelting result of whatever is in the input slot, or an empty stack.
    private ItemStack smeltingResult() {
        if (this.level() == null)
            return ItemStack.EMPTY;
        return this.level().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, this, this.level())
            .map((SmeltingRecipe recipe) -> recipe.getResultItem(this.level().registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    /// Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
    public boolean canSmelt() {
        if (this.getItem(0).isEmpty())
            return false;
        ItemStack itemStack = this.smeltingResult();
        if (itemStack.isEmpty())
            return false;
        if (this.getItem(2).isEmpty())
            return true;
        if (!ItemStack.isSameItem(this.getItem(2), itemStack))
            return false;
        int result = this.getItem(2).getCount() + itemStack.getCount();
        return result <= this.getMaxStackSize() && result <= itemStack.getMaxStackSize();
    }

    /// Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack.
    public void smeltItem() {
        if (this.canSmelt()) {
            ItemStack itemStack = this.smeltingResult();
            if (this.getItem(2).isEmpty()) {
                this.setItem(2, itemStack.copy());
            }
            else if (ItemStack.isSameItem(this.getItem(2), itemStack)) {
                this.getItem(2).grow(itemStack.getCount());
            }
            this.getItem(0).shrink(1);
            if (this.getItem(0).isEmpty()) {
                this.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort("BurnTime", (short)this.burnTime);
        tag.putShort("CookTime", (short)this.cookTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.burnTime = tag.getShort("BurnTime");
        this.cookTime = tag.getShort("CookTime");
        this.itemBurnTime = EntityFurnaceGolem.burnDuration(this.getItem(1));
    }

    @Override
    public void takeContentsFromNBT(CompoundTag tag) {
        super.takeContentsFromNBT(tag);
        this.burnTime = tag.getShort("BurnTime");
        this.cookTime = tag.getShort("CookTime");
        this.itemBurnTime = EntityFurnaceGolem.burnDuration(this.getItem(1));
        tag.putShort("BurnTime", (short)0);
        tag.putShort("CookTime", (short)0);
    }
}
