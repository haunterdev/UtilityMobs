package toast.utilityMobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

public enum EnumUpgrade
{
    DEFAULT("default", null),
    MULTISHOT("multishot", null),
    KILLER("killer", Items.DIAMOND, 0x4AEDD9),
    FIRE("fire", Items.IRON_INGOT, 0xE8870E),
    FEATHER("feather", Items.FEATHER, 0xF0F0F0),
    SLOW("slow", Items.SLIME_BALL, 0x5A6C81),
    EGG("egg", Items.EGG, 0xE8E0C8),
    SIGHT("sight", Items.ENDER_PEARL, 0x1FB89E),
    EXPLOSIVE("explosive", Items.GUNPOWDER, 0x5A5A5A),
    POISON("poison", Items.SPIDER_EYE, 0x4E9B2D),
    FIRE_EXPLOSIVE("fire_explosive", Items.FIRE_CHARGE, 0xF0A030);


    // Returns the appropriate upgrade out of the array of allowed upgrades.
    public static EnumUpgrade getUpgrade(EnumUpgrade[] upgrades, ItemStack upgradeStack) {
        if (upgradeStack != null && !upgradeStack.isEmpty()) {
            Item upgradeItem = upgradeStack.getItem();
            for (EnumUpgrade upgrade : upgrades) {
                if (upgrade.upgradeItem == upgradeItem)
                    return upgrade;
            }
        }
        return EnumUpgrade.DEFAULT;
    }

    // Returns the appropriate upgrade to be applied from the item stack.
    public static EnumUpgrade getUpgrade(ItemStack upgradeStack) {
        return EnumUpgrade.getUpgrade(EnumUpgrade.values(), upgradeStack);
    }

    public final String upgradeName;
    public final Item upgradeItem;
    public final int gradientColor;

    private EnumUpgrade(String id, Item upgrade) {
        this(id, upgrade, 0x000000);
    }

    private EnumUpgrade(String id, Item upgrade, int gradientColor) {
        this.upgradeName = id;
        this.upgradeItem = upgrade;
        this.gradientColor = gradientColor;
    }

    // Applies this arrow effect to the arrow and initializes arrow stats.
    // NOTE: deliberately if/else, not switch(this) - an enum switch makes the compiler emit a synthetic
    // EnumUpgrade$1 switch-map class that fails to load under the 1.12.2 LaunchClassLoader/coremod
    // transformers (NoClassDefFoundError -> NPE), crashing turret ranged attacks.
    public void applyToArrow(EntityArrow arrow) {
        if (this == EXPLOSIVE) {
            arrow.setDamage(arrow.getDamage() - 1.0);
        }
        else if (this == FIRE) {
            arrow.setDamage(arrow.getDamage() - 1.0);
            arrow.setFire(100);
        }
        else if (this == FIRE_EXPLOSIVE) {
            arrow.setDamage(arrow.getDamage() - 2.0);
            arrow.setFire(100);
        }
        else if (this == KILLER) {
            arrow.setDamage(arrow.getDamage() * 1.5 + 1.0);
        }
        if (arrow.getDamage() <= 0.0) {
            arrow.setDamage(Double.MIN_VALUE);
        }
        this.applyTo(arrow);
    }

    // Applies this arrow effect to the entity.
    public void applyTo(Entity entity) {
        entity.getEntityData().setBoolean("UM|" + this.upgradeName, true);
    }

    // Safely returns the arrow effect with the given ID.
    public boolean isApplied(Entity entity) {
        return entity.getEntityData().getBoolean("UM|" + this.upgradeName);
    }

    // Spawns a burst of upgrade-themed particles at the given point (server-side; replicated to clients).
    // The particle correlates with the upgrade: ender pearl -> enderman teleport (PORTAL), the fiery
    // upgrades -> flame, everything else -> the upgrade item itself shattering (so a feather upgrade
    // poofs feathers, a slime ball spits slime, and so on).
    // NOTE: deliberately if/else, not switch(this) - see applyToArrow for why an enum switch crashes here.
    public void spawnEquipParticles(WorldServer world, double x, double y, double z) {
        EnumParticleTypes type;
        int[] args;
        if (this == SIGHT) {
            type = EnumParticleTypes.PORTAL;
            args = new int[0];
        }
        else if (this == FIRE || this == FIRE_EXPLOSIVE) {
            type = EnumParticleTypes.FLAME;
            args = new int[0];
        }
        else if (this.upgradeItem != null) {
            type = EnumParticleTypes.ITEM_CRACK;
            args = new int[] { Item.getIdFromItem(this.upgradeItem) };
        }
        else {
            return;
        }
        for (int i = 0; i < 30; i++) {
            double dx = (world.rand.nextDouble() - 0.5) * 0.6;
            double dy = world.rand.nextDouble() * 0.6;
            double dz = (world.rand.nextDouble() - 0.5) * 0.6;
            world.spawnParticle(type, x, y, z, 1, dx, dy, dz, 0.05, args);
        }
    }
}
