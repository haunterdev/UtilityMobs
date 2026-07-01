package toast.utilityMobs.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;

public class EntityAnvilGolem extends EntityContainerGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURES[] = { new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_0.png"), new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_1.png"), new ResourceLocation(_UtilityMobs.TEXTURE + "block/anvilgolem_2.png") };

    /// damage; The amount of damage this anvil golem has taken from use.
    private static final DataParameter<Integer> DAMAGE = EntityDataManager.createKey(EntityAnvilGolem.class, DataSerializers.VARINT);

    public EntityAnvilGolem(World world) {
        super(world);
        this.texture = EntityAnvilGolem.TEXTURES[0];
        this.isImmuneToFire = true;
    }

    /// Returns the texture for this mob.
    @Override
    public ResourceLocation getTexture() {
        return EntityAnvilGolem.TEXTURES[this.getDamage()];
    }

    /// Used to initialize data watcher variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(DAMAGE, Integer.valueOf(0));
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 16);
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.ANVIL);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            this.entityDropItem(new ItemStack(this.getDropItem(), 1, this.getDamage()), 0.0F);
            if (this.rand.nextFloat() < dropChance / 4.0F) {
                this.dropItem(Items.SKULL, 1);
            }
        }
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            GuiHelper.displayGUIAnvil(player, this);
        }
        return true;
    }

    /// Gets/sets the usage damage.
    public int getDamage() {
        return this.dataManager.get(DAMAGE).intValue();
    }
    public void setDamage(int damage) {
        this.dataManager.set(DAMAGE, Integer.valueOf(damage));
    }

    /// Saves this entity to NBT.
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setByte("Damage", (byte)this.getDamage());
    }

    /// Loads this entity from NBT.
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.setDamage(tag.getByte("Damage"));
    }
}
