package toast.utilityMobs.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.GuiHelper;
import toast.utilityMobs.setup.ModBlocks;

public class EntityLanternGolem extends EntityContainerGolem implements net.minecraft.world.MenuProvider
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: jack o'lantern.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.WOOD;
    }

    /// Server side of opening this golem's screen. 1.12.2 routed this through IGuiHandler; 1.20.1 wants
    /// the entity itself to be the MenuProvider. getDisplayName is inherited from Entity.
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInv, Player player) {
        return new ContainerLanternGolem(containerId, playerInv, this);
    }

    /// The textures for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/lanterngolem.png");

    /// Ticks between light-block placement attempts.
    private int placeCooldown;
    /// The block position currently lit by this golem's dynamic glow, or null.
    private BlockPos litPos;

    // Registered entity size: 0.9375 x 0.9375 (set via EntityType.Builder.sized at registration).
    public EntityLanternGolem(EntityType<? extends EntityLanternGolem> type, Level level) {
        super(type, level);
        this.texture = EntityLanternGolem.TEXTURE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // aiFollow is assigned in the super call, which only runs server-side.
        this.aiFollow.minDistance = 2.0F;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.setCurrentItemOrArmor(4, new ItemStack(Blocks.JACK_O_LANTERN));
        this.setEquipDropChance(4, 0.0F);
        return data;
    }

    /// Returns the number of slots in the inventory.
    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    protected Item getDropItem() {
        return Items.PUMPKIN;
    }

    /// Opens this block golem's GUI.
    @Override
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

    /// Called each tick this entity is alive.
    @Override
    public void aiStep() {
        super.aiStep();

        // Glow like a real jack o'lantern: keep an invisible full-bright block in the space we occupy,
        // moving it as we walk so the surrounding area stays lit (light level 15, ~15 block range).
        this.updateDynamicLight();

        // Also drop carried light blocks (torches/glowstone) in naturally dark spots so the area stays lit
        // after the golem leaves. Darkness is judged from SKY light only, so the golem's own glow (which is
        // BLOCK light) never fools the check.
        if (!this.level().isClientSide && !this.sitAI().sit && --this.placeCooldown <= 0) {
            this.placeCooldown = 30;
            BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
            int skyLight = this.level().getBrightness(LightLayer.SKY, pos) - this.level().getSkyDarken();
            if (skyLight <= 7 && this.level().getBlockState(pos).canBeReplaced()) {
                for (int i = this.getContainerSize(); i-- > 0;) {
                    ItemStack itemStack = this.getItem(i);
                    if (!itemStack.isEmpty() && itemStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                        BlockState placed = blockItem.getBlock().defaultBlockState();
                        if (placed.getLightEmission() > 7 && !placed.canOcclude()) {
                            this.level().setBlock(pos, placed, 2);
                            itemStack.shrink(1);
                            if (itemStack.isEmpty()) {
                                this.setItem(i, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /// Moves the invisible glow block to the golem's current position, clearing the old one.
    private void updateDynamicLight() {
        if (this.level().isClientSide)
            return;
        BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY() + this.getBbHeight() * 0.5), Mth.floor(this.getZ()));
        if (pos.equals(this.litPos))
            return;
        this.clearDynamicLight();
        if (BlockGolemLight.isLightReplaceable(this.level(), pos)) {
            this.level().setBlock(pos, ModBlocks.GOLEM_LIGHT.get().defaultBlockState(), 2);
            this.litPos = pos;
        }
    }

    /// Removes our glow block if it is still ours.
    private void clearDynamicLight() {
        if (this.litPos != null) {
            if (this.level().getBlockState(this.litPos).is(ModBlocks.GOLEM_LIGHT.get())) {
                this.level().removeBlock(this.litPos, false);
            }
            this.litPos = null;
        }
    }

    /// 1.12.2 cleared the light in setDead.
    @Override
    public void remove(RemovalReason reason) {
        this.clearDynamicLight();
        super.remove(reason);
    }
}
