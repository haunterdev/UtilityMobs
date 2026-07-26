package toast.utilityMobs.golem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIGolemWander;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityObsidianGolem extends EntityLargeGolem
{
    /// Differs from 1.12.2, which left every golem outside the six that already override this
    /// on the default iron-golem hurt/death sounds. Flavoured by build material: obsidian.
    @Override
    protected net.minecraft.world.level.block.SoundType getGolemSoundType() {
        return net.minecraft.world.level.block.SoundType.STONE;
    }

    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/obsidiangolem.png");

    public EntityObsidianGolem(EntityType<? extends EntityObsidianGolem> type, Level level) {
        super(type, level);
        this.texture = EntityObsidianGolem.TEXTURE;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityAIWeaponAttack(this, 1.0));
        this.goalSelector.addGoal(2, new EntityAIGolemWander(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EntityAIGolemTarget(this));
    }

    /// Initializes this entity's attributes.
    public static AttributeSupplier.Builder createAttributes() {
        return EntityLargeGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public int getArmorValue() {
        return 20;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        // Do nothing
    }
}
