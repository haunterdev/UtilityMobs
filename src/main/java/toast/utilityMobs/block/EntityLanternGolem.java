package toast.utilityMobs.block;

import net.minecraft.block.Block;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;

public class EntityLanternGolem extends EntityContainerGolem
{
    /// The textures for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/lanterngolem.png");

    public EntityLanternGolem(World world) {
        super(world);
        this.texture = EntityLanternGolem.TEXTURE;
        this.aiFollow.minDistance = 2.0F;
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.LIT_PUMPKIN));
        this.setEquipDropChance(4, 0.0F);
        return data;
    }

    /// Returns the number of slots in the inventory.
    @Override
    public int getSizeInventory() {
        return 9;
    }

    /// Returns the name of the inventory.
    @Override
    public String getName() {
        return this.hasCustomName() ? this.getCustomNameTag() : "Jack o'Lantern Golem";
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.PUMPKIN);
    }

    /// Opens this block golem's GUI.
    @Override
    public boolean openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            GuiHelper.displayGUICustom(player, this);
        }
        return true;
    }

    @Override
    public int getUsePermissions() {
        return super.getUsePermissions() | TargetHelper.PERMISSION_OPEN;
    }

    /// The block position currently lit by this golem's dynamic glow, or null.
    private BlockPos litPos;

    /// Called each tick this entity is alive.
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // Glow like a real jack o'lantern: keep an invisible full-bright block in the space we occupy,
        // moving it as we walk so the surrounding area stays lit (light level 15, ~15 block range).
        this.updateDynamicLight();

        // Also drop carried light blocks (torches/glowstone) in naturally dark spots so the area stays lit
        // after the golem leaves. Darkness is judged from SKY light only, so the golem's own glow (which is
        // BLOCK light) never fools the check.
        if (!this.world.isRemote && !this.sitAI.sit && --this.placeCooldown <= 0) {
            this.placeCooldown = 30;
            BlockPos pos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY), MathHelper.floor(this.posZ));
            int skyLight = this.world.getLightFor(EnumSkyBlock.SKY, pos) - this.world.getSkylightSubtracted();
            if (skyLight <= 7 && this.world.getBlockState(pos).getMaterial().isReplaceable()) {
                for (int i = this.getSizeInventory(); i-- > 0;) {
                    ItemStack itemStack = this.getStackInSlot(i);
                    if (!itemStack.isEmpty()) {
                        Block block = Block.getBlockFromItem(itemStack.getItem());
                        if (block != Blocks.AIR && block.getLightValue(block.getDefaultState()) > 7 && !block.isOpaqueCube(block.getDefaultState())) {
                            int data = itemStack.getItem().getMetadata(itemStack.getItemDamage());
                            this.world.setBlockState(pos, block.getStateFromMeta(data), 2);
                            itemStack.shrink(1);
                            if (itemStack.isEmpty()) {
                                this.setInventorySlotContents(i, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /// Ticks between light-block placement attempts.
    private int placeCooldown;

    /// Moves the invisible glow block to the golem's current position, clearing the old one.
    private void updateDynamicLight() {
        if (this.world.isRemote || _UtilityMobs.GOLEM_LIGHT == null)
            return;
        BlockPos pos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY + this.height * 0.5), MathHelper.floor(this.posZ));
        if (pos.equals(this.litPos))
            return;
        this.clearDynamicLight();
        if (BlockGolemLight.isLightReplaceable(this.world, pos)) {
            this.world.setBlockState(pos, _UtilityMobs.GOLEM_LIGHT.getDefaultState(), 2);
            this.litPos = pos;
        }
    }

    /// Removes our glow block if it is still ours.
    private void clearDynamicLight() {
        if (this.litPos != null) {
            if (this.world.getBlockState(this.litPos).getBlock() == _UtilityMobs.GOLEM_LIGHT) {
                this.world.setBlockToAir(this.litPos);
            }
            this.litPos = null;
        }
    }

    @Override
    public void setDead() {
        this.clearDynamicLight();
        super.setDead();
    }
}
