package toast.utilityMobs.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;

public class EntityAnvilGolem extends EntityContainerGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: anvil.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.ANVIL;
    }

    /// The texture for this class, one per damage step.
    public static final ResourceLocation TEXTURES[] = {
        new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_0.png"),
        new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_1.png"),
        new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_2.png")
    };

    /// The block dropped per damage step. 1.12.2 dropped Blocks.ANVIL with the damage as its metadata;
    /// 1.20.1 splits the three anvil stages into three blocks.
    private static final Item[] DROPS = { Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL };

    /// damage; The amount of damage this anvil golem has taken from use.
    private static final EntityDataAccessor<Integer> DAMAGE = SynchedEntityData.defineId(EntityAnvilGolem.class, EntityDataSerializers.INT);

    // Registered entity size: 0.9375 x 0.9375 (set via EntityType.Builder.sized at registration).
    public EntityAnvilGolem(EntityType<? extends EntityAnvilGolem> type, Level level) {
        super(type, level);
        this.texture = EntityAnvilGolem.TEXTURES[0];
    }

    /// 1.12.2 set isImmuneToFire in the constructor.
    @Override
    public boolean fireImmune() {
        return true;
    }

    /// Returns the texture for this mob.
    @Override
    public ResourceLocation getTexture() {
        return EntityAnvilGolem.TEXTURES[this.getDamage()];
    }

    // Used to initialize data manager variables.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DAMAGE, Integer.valueOf(0));
    }

    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 16);
    }

    @Override
    protected Item getDropItem() {
        return Items.ANVIL;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            this.spawnAtLocation(new ItemStack(EntityAnvilGolem.DROPS[this.getDamage()]), 0.0F);
            if (this.random.nextFloat() < dropChance / 4.0F) {
                this.spawnAtLocation(Items.SKELETON_SKULL);
            }
        }
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            GuiHelper.displayGUIAnvil(player, this);
        }
        return true;
    }

    /// Gets/sets the usage damage.
    public int getDamage() {
        return this.entityData.get(DAMAGE).intValue();
    }
    public void setDamage(int damage) {
        this.entityData.set(DAMAGE, Integer.valueOf(damage));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Damage", (byte)this.getDamage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setDamage(tag.getByte("Damage"));
    }
}
