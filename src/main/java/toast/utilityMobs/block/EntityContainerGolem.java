package toast.utilityMobs.block;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class EntityContainerGolem extends EntityBlockGolem implements Container
{
    // numUsingPlayers; The number of players using this chest golem.
    private static final EntityDataAccessor<Byte> USING = SynchedEntityData.defineId(EntityContainerGolem.class, EntityDataSerializers.BYTE);

    // The contents of this chest.
    private NonNullList<ItemStack> contents;

    public EntityContainerGolem(EntityType<? extends EntityContainerGolem> type, Level level) {
        super(type, level);
        this.contents = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    // Used to initialize data manager variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(USING, Byte.valueOf((byte)0));
    }

    // Functions for numUsingPlayers.
    public void incNumUsingPlayers() {
        this.entityData.set(USING, Byte.valueOf((byte)(this.entityData.get(USING).byteValue() + 1)));
    }
    public void decNumUsingPlayers() {
        this.entityData.set(USING, Byte.valueOf((byte)(this.entityData.get(USING).byteValue() - 1)));
    }
    public boolean isOpen() {
        return this.entityData.get(USING).byteValue() > 0;
    }

    // Called when this block golem is told to get up.
    @Override
    public void setClosed() {
        this.entityData.set(USING, Byte.valueOf((byte)0));
    }

    @Override
    public int getContainerSize() {
        return 27;
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
    public void startOpen(Player player) {
        this.incNumUsingPlayers();
    }

    @Override
    public void stopOpen(Player player) {
        this.decNumUsingPlayers();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return true;
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

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
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

    // Opens this block golem's GUI.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            // ChestMenu drives startOpen/stopOpen (the sit-while-open behavior) automatically.
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.threeRows(id, inventory, this),
                this.getDisplayName()));
        }
        return true;
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
    }

    // Steals the contents of the NBT given.
    public void takeContentsFromNBT(CompoundTag tag) {
        ListTag tagList = tag.getList("Items", Tag.TAG_COMPOUND);
        this.contents = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag slotTag = tagList.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.contents.size()) {
                this.contents.set(slot, ItemStack.of(slotTag));
            }
        }
        tag.put("Items", new ListTag());
        if (tag.contains("CustomName")) {
            this.setCustomName(Component.literal(tag.getString("CustomName")));
        }
        tag.remove("CustomName");
    }
}
