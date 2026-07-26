package toast.utilityMobs.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;

public class EntityChestEnderGolem extends EntityChestGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: ender chest is obsidian.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.STONE;
    }

    /// The texture for this class.
    @SuppressWarnings("hiding")
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "block/chestendergolem.png");

    public EntityChestEnderGolem(EntityType<? extends EntityChestEnderGolem> type, Level level) {
        super(type, level);
        this.texture = EntityChestEnderGolem.TEXTURE;
    }

    @Override
    public int getArmorValue() {
        return 20;
    }

    // Called each tick this entity is alive.
    @Override
    public void aiStep() {
        if (this.level().isClientSide && this.random.nextInt(4) == 0) {
            for (int i = 3; i-- > 0;) {
                int xOff = this.random.nextInt(2) * 2 - 1;
                int zOff = this.random.nextInt(2) * 2 - 1;
                double vX = this.random.nextFloat() * 1.0F * xOff;
                double vY = (this.random.nextFloat() - 0.5) * 0.125;
                double vZ = this.random.nextFloat() * 1.0F * zOff;
                this.level().addParticle(UMSound.PORTAL, this.getX() + 0.25 * xOff, this.getY() + this.random.nextFloat(), this.getZ() + 0.25 * zOff, vX, vY, vZ);
            }
        }
        super.aiStep();
    }

    @Override
    protected Item getDropItem() {
        return Items.OBSIDIAN;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
        for (int i = 7; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }

    /// Opens this block golem's GUI: the player's own ender chest inventory.
    @Override
    public boolean openGUI(Player player) {
        if (!this.level().isClientSide) {
            // 1.12.2 pointed the player's ender inventory at a TileEntityEnderChestProxy so the golem,
            // not a block, received the open/close callbacks that drive its lid. 1.20.1's
            // PlayerEnderChestContainer only accepts a real EnderChestBlockEntity, so the same effect
            // comes from wrapping the inventory and rerouting just those two callbacks.
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.threeRows(id, inventory, new EnderChestProxy(p.getEnderChestInventory(), this)),
                this.getDisplayName()));
        }
        return true;
    }
}
