package toast.utilityMobs.block;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.ai.EntityAIGolemFollow;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityBlockGolem extends EntityUtilityGolem
{
    /// When true (golems.block_collision config), block golems are solid and can be stood on / walked
    /// across - line them up to build a walkway of chests/furnaces. Mirrors EntityTurretGolem.collision.
    public static boolean collision = false;

    // Save the reference to the follow AI so it can be easily removed or altered.
    public EntityAIGolemFollow aiFollow = new EntityAIGolemFollow(this, 1.0, 10.0F, 5.0F);

    public EntityBlockGolem(World world) {
        super(world);
        this.setSize(0.9375F, 0.9375F);
        this.tasks.addTask(1, this.sitAI);
        this.tasks.addTask(2, this.aiFollow);
        this.tasks.addTask(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(4, new EntityAILookIdle(this));
    }

    /// Solid (standable) when the golems.block_collision config is on - lets players build block-golem
    /// walkways. Mirrors EntityTurretGolem.getCollisionBoundingBox; null (vanilla default) means pass-through.
    @Override
    public net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox() {
        return EntityBlockGolem.collision ? this.getEntityBoundingBox() : super.getCollisionBoundingBox();
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0);
    }

    @Override
    protected Item getDropItem() {
        return Item.getItemFromBlock(Blocks.CHEST);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            this.dropItem(this.getDropItem(), 1);
            if (this.rand.nextFloat() < dropChance / 4.0F) {
                this.dropItem(Items.SKULL, 1);
            }
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (this.canInteract(player)) {
            if (player.isSneaking()) {
                this.sitAI.sit = !this.isSitting();
                if (!this.sitAI.sit) {
                    this.setClosed();
                }
            }
            else if (this.tryHealFromHeld(player, hand, player.getHeldItem(hand))) {
                return true;
            }
            else if (this.openPrimaryGUI(player))
                return true;
        }
        return super.processInteract(player, hand);
    }

    /// Called when this block golem is told to get up.
    public void setClosed() {
        // To be overridden
    }

    /// The GUI opened by a plain (non-sneak) right-click. Defaults to this golem's storage GUI; smart
    /// container golems override this to open their configuration menu instead (EntityContainerGolem).
    public boolean openPrimaryGUI(EntityPlayer player) {
        return this.openGUI(player);
    }

    /// Opens this block golem's GUI.
    public boolean openGUI(EntityPlayer player) {
        return false;
    }

    @Override
    public boolean canInteract(EntityPlayer player) {
        if (player.isSneaking())
            return this.getOwnerName().isEmpty() || this.getOwnerName().equals(player.getName()) || this.targetHelper.playerHasPermission(player.getName(), TargetHelper.PERMISSION_TARGET | TargetHelper.PERMISSION_USE);
        return super.canInteract(player) && player.getDistanceSq(this) <= 64.0;
    }
}
