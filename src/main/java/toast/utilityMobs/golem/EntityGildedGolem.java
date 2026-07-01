package toast.utilityMobs.golem;

import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import toast.utilityMobs.EffectHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.ai.EntityAIGolemTarget;
import toast.utilityMobs.ai.EntityAIWeaponAttack;

public class EntityGildedGolem extends EntityUtilityGolem
{
    /// The texture for this class.
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "golem/gildedgolem.png");

    @Override
    protected SoundType getGolemSoundType() {
        return SoundType.METAL;
    }


    public EntityGildedGolem(World world) {
        super(world);
        this.texture = EntityGildedGolem.TEXTURE;
        // Equip the fixed gear here too so the guide book preview (which skips onInitialSpawn) renders it;
        // the bare body texture is mostly empty without the golden armor on top.
        this.setCurrentItemOrArmor(0, new ItemStack(Items.GOLDEN_SWORD));
        this.setCurrentItemOrArmor(4, new ItemStack(Items.GOLDEN_HELMET));
        this.setCurrentItemOrArmor(3, new ItemStack(Items.GOLDEN_CHESTPLATE));
        this.setCurrentItemOrArmor(2, new ItemStack(Items.GOLDEN_LEGGINGS));
        this.setCurrentItemOrArmor(1, new ItemStack(Items.GOLDEN_BOOTS));
        for (int i = 5; i-- > 0;) {
            this.setEquipDropChance(i, 0.0F);
        }
        this.tasks.addTask(1, new EntityAIWeaponAttack(this, 1.0));
        this.tasks.addTask(2, new toast.utilityMobs.ai.EntityAIGolemWander(this, 1.0));
        this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(3, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIGolemTarget(this));
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
        data = super.onInitialSpawn(this.world.getDifficultyForLocation(this.getPosition()), data);
        this.setCurrentItemOrArmor(0, new ItemStack(Items.GOLDEN_SWORD));
        this.setCurrentItemOrArmor(4, new ItemStack(Items.GOLDEN_HELMET));
        this.setCurrentItemOrArmor(3, new ItemStack(Items.GOLDEN_CHESTPLATE));
        this.setCurrentItemOrArmor(2, new ItemStack(Items.GOLDEN_LEGGINGS));
        this.setCurrentItemOrArmor(1, new ItemStack(Items.GOLDEN_BOOTS));
        for (int i = 5; i-- > 0;) {
            EffectHelper.enchantItem(this.getEquipmentInSlot(i), Enchantments.THORNS, 1);
            this.setEquipDropChance(i, 0.0F);
        }
        return data;
    }

    @Override
    protected Item getDropItem() {
        return Items.GOLD_INGOT;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        for (int i = this.rand.nextInt(3) + 3; i-- > 0;) {
            this.dropItem(this.getDropItem(), 1);
        }
    }

    @Override
    public void hitEffects(Entity entity) {
        this.world.spawnEntity(new EntityXPOrb(this.world, entity.posX, entity.posY, entity.posZ, 1));
    }
}
