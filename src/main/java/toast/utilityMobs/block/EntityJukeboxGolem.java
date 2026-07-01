package toast.utilityMobs.block;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;

public class EntityJukeboxGolem extends EntityBlockGolem
{
    /// The textures for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/jukeboxgolem.png");

    /// record; The music disc currently playing.
    private static final DataParameter<String> RECORD = EntityDataManager.createKey(EntityJukeboxGolem.class, DataSerializers.STRING);

    public String lastRecord = "";

    public EntityJukeboxGolem(World world) {
        super(world);
        this.setEquipDropChance(0, 2.0F);
        this.texture = EntityJukeboxGolem.TEXTURE;
    }

    // Used to initialize data watcher variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(RECORD, "");
    }

    /// Get/set functions for the record name.
    public String getRecord() {
        return this.dataManager.get(RECORD);
    }
    public void setRecord(ItemRecord record) {
        if (record == null) {
            if (!this.getRecord().isEmpty()) {
                this.dataManager.set(RECORD, "");
            }
        }
        else {
            String recordName = record.getSound().getSoundName().toString();
            if (!this.getRecord().equals(recordName)) {
                this.dataManager.set(RECORD, recordName);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.JUKEBOX);
    }

    /// Called each tick this entity is alive.
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.world.isRemote && !this.getRecord().equals(this.lastRecord)) {
            this.lastRecord = this.getRecord();
            _UtilityMobs.proxy.playRecordGolem(this, this.lastRecord);
        }
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            ItemStack heldItem = this.getEquipmentInSlot(0);
            if (!heldItem.isEmpty()) {
                if (!player.capabilities.isCreativeMode) {
                    float power = 0.7F;
                    double xOff = this.rand.nextFloat() * power - power * 0.5;
                    double yOff = this.rand.nextFloat() * power + (1.0F - power) * 0.2 + 0.6;
                    double zOff = this.rand.nextFloat() * power - power * 0.5;
                    ItemStack dropItem = heldItem.copy();
                    EntityItem entityItem = new EntityItem(this.world, this.posX + xOff, this.posY + yOff, this.posZ + zOff, dropItem);
                    entityItem.setDefaultPickupDelay();
                    this.world.spawnEntity(entityItem);
                }

                this.setCurrentItemOrArmor(0, ItemStack.EMPTY);
                this.setRecord(null);
            }
            else {
                ItemStack playerHeld = player.getHeldItemMainhand();
                if (!playerHeld.isEmpty() && playerHeld.getItem() instanceof ItemRecord) {
                    this.setCurrentItemOrArmor(0, playerHeld.copy());
                    this.getEquipmentInSlot(0).setCount(1);
                    this.setRecord((ItemRecord) playerHeld.getItem());

                    if (!player.capabilities.isCreativeMode) {
                        playerHeld.shrink(1);
                    }
                    player.swingArm(EnumHand.MAIN_HAND);
                }
            }
        }
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }
}
