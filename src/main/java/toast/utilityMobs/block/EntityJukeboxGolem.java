package toast.utilityMobs.block;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;

public class EntityJukeboxGolem extends EntityBlockGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: jukebox.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.WOOD;
    }

    /// The textures for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/jukeboxgolem.png");

    /// record; The music disc currently playing.
    private static final EntityDataAccessor<String> RECORD = SynchedEntityData.defineId(EntityJukeboxGolem.class, EntityDataSerializers.STRING);

    public String lastRecord = "";

    // Registered entity size: 0.9375 x 0.9375 (set via EntityType.Builder.sized at registration).
    public EntityJukeboxGolem(EntityType<? extends EntityJukeboxGolem> type, Level level) {
        super(type, level);
        this.setEquipDropChance(0, 2.0F);
        this.texture = EntityJukeboxGolem.TEXTURE;
    }

    // Used to initialize data manager variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RECORD, "");
    }

    /// Get/set functions for the record name.
    public String getRecord() {
        return this.entityData.get(RECORD);
    }
    public void setRecord(RecordItem record) {
        if (record == null) {
            if (!this.getRecord().isEmpty()) {
                this.entityData.set(RECORD, "");
            }
        }
        else {
            String recordName = record.getSound().getLocation().toString();
            if (!this.getRecord().equals(recordName)) {
                this.entityData.set(RECORD, recordName);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return Items.JUKEBOX;
    }

    /// Called each tick this entity is alive.
    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide && !this.getRecord().equals(this.lastRecord)) {
            this.lastRecord = this.getRecord();
            final String record = this.lastRecord;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> toast.utilityMobs.client.UMClientNetwork.playRecordGolem(this, record));
        }
    }

    /// Opens this block golem's GUI: right-click swaps the disc in and out rather than showing a screen.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            ItemStack heldItem = this.getEquipmentInSlot(0);
            if (!heldItem.isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    float power = 0.7F;
                    double xOff = this.random.nextFloat() * power - power * 0.5;
                    double yOff = this.random.nextFloat() * power + (1.0F - power) * 0.2 + 0.6;
                    double zOff = this.random.nextFloat() * power - power * 0.5;
                    ItemStack dropItem = heldItem.copy();
                    ItemEntity itemEntity = new ItemEntity(this.level(), this.getX() + xOff, this.getY() + yOff, this.getZ() + zOff, dropItem);
                    itemEntity.setDefaultPickUpDelay();
                    this.level().addFreshEntity(itemEntity);
                }

                this.setCurrentItemOrArmor(0, ItemStack.EMPTY);
                this.setRecord(null);
            }
            else {
                ItemStack playerHeld = player.getMainHandItem();
                if (!playerHeld.isEmpty() && playerHeld.getItem() instanceof RecordItem) {
                    ItemStack disc = playerHeld.copy();
                    disc.setCount(1);
                    this.setCurrentItemOrArmor(0, disc);
                    this.setRecord((RecordItem)playerHeld.getItem());

                    if (!player.getAbilities().instabuild) {
                        playerHeld.shrink(1);
                    }
                    player.swing(InteractionHand.MAIN_HAND);
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
