package toast.utilityMobs.colossal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import toast.utilityMobs._UtilityMobs;

public class EntityStoneColossus extends EntityColossalGolem
{
    public static final ResourceLocation TEXTURE = new ResourceLocation(_UtilityMobs.TEXTURE + "colossal/stonecolossus.png");

    public EntityStoneColossus(EntityType<? extends EntityStoneColossus> type, Level level) {
        super(type, level);
        this.texture = EntityStoneColossus.TEXTURE;
    }

    // Returns the armor of this entity.
    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 2);
    }

    @Override
    protected Item getDropItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting, float dropChance) {
        super.dropFewItems(recentlyHit, looting, dropChance);
        for (int i = this.random.nextInt(3) + 1; i-- > 0;) {
            this.spawnAtLocation(this.getDropItem());
        }
    }
}
