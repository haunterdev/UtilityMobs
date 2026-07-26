package toast.utilityMobs.block;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.ai.EntityAIGolemFollow;
import toast.utilityMobs.golem.EntityUtilityGolem;

public class EntityBlockGolem extends EntityUtilityGolem
{
    /// When true (golems.block_collision config), block golems are solid and can be stood on / walked
    /// across - line them up to build a walkway of chests/furnaces. Mirrors EntityTurretGolem.collision.
    public static boolean collision = false;

    // Save the reference to the follow AI so it can be easily removed or altered.
    public EntityAIGolemFollow aiFollow;

    // Registered entity size: 0.9375 x 0.9375 (set via EntityType.Builder.sized at registration).
    public EntityBlockGolem(EntityType<? extends EntityBlockGolem> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.aiFollow = new EntityAIGolemFollow(this, 1.0, 10.0F, 5.0F);
        this.goalSelector.addGoal(1, this.sitAI());
        this.goalSelector.addGoal(2, this.aiFollow);
        this.goalSelector.addGoal(3, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    /// Solid (standable) when the golems.block_collision config is on - lets players build block-golem
    /// walkways. False (vanilla default) means pass-through.
    @Override
    public boolean canBeCollidedWith() {
        return EntityBlockGolem.collision && this.isAlive();
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityUtilityGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 10.0);
    }

    @Override
    protected Item getDropItem() {
        return Blocks.CHEST.asItem();
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        if (recentlyHit) {
            this.spawnAtLocation(this.getDropItem());
            if (this.random.nextFloat() < dropChance / 4.0F) {
                this.spawnAtLocation(Items.SKELETON_SKULL);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.canInteract(player)) {
            if (player.isShiftKeyDown()) {
                this.sitAI().sit = !this.isSitting();
                if (!this.sitAI().sit) {
                    this.setClosed();
                }
            }
            else if (this.tryHealFromHeld(player, hand, player.getItemInHand(hand))) {
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            else if (this.openPrimaryGUI(player))
                return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /// Called when this block golem is told to get up.
    public void setClosed() {
        // To be overridden
    }

    /// The GUI opened by a plain (non-sneak) right-click. Defaults to this golem's storage GUI; smart
    /// container golems override this to open their configuration menu instead (EntityContainerGolem).
    public boolean openPrimaryGUI(Player player) {
        return this.openGUI(player);
    }

    /// Opens this block golem's GUI.
    public boolean openGUI(Player player) {
        return false;
    }

    @Override
    public boolean canInteract(Player player) {
        if (player.isShiftKeyDown())
            return this.getOwnerName().isEmpty() || this.getOwnerName().equals(player.getScoreboardName()) || this.targetHelper.playerHasPermission(player.getScoreboardName(), TargetHelper.PERMISSION_TARGET | TargetHelper.PERMISSION_USE);
        return super.canInteract(player) && player.distanceToSqr(this) <= 64.0;
    }
}
