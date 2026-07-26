package toast.utilityMobs;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    public void applyToArrow(AbstractArrow arrow) {
        if (this == EXPLOSIVE) {
            arrow.setBaseDamage(arrow.getBaseDamage() - 1.0);
        }
        else if (this == FIRE) {
            arrow.setBaseDamage(arrow.getBaseDamage() - 1.0);
            arrow.setSecondsOnFire(100);
        }
        else if (this == FIRE_EXPLOSIVE) {
            arrow.setBaseDamage(arrow.getBaseDamage() - 2.0);
            arrow.setSecondsOnFire(100);
        }
        else if (this == KILLER) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 1.5 + 1.0);
        }
        if (arrow.getBaseDamage() <= 0.0) {
            arrow.setBaseDamage(Double.MIN_VALUE);
        }
        this.applyTo(arrow);
    }

    // Applies this arrow effect to the entity.
    public void applyTo(Entity entity) {
        entity.getPersistentData().putBoolean("UM|" + this.upgradeName, true);
    }

    // Safely returns the arrow effect with the given ID.
    public boolean isApplied(Entity entity) {
        return entity.getPersistentData().getBoolean("UM|" + this.upgradeName);
    }

    // Spawns a burst of upgrade-themed particles at the given point (server-side; replicated to clients).
    // The particle correlates with the upgrade: ender pearl -> enderman teleport (PORTAL), the fiery
    // upgrades -> flame, everything else -> the upgrade item itself shattering (so a feather upgrade
    // poofs feathers, a slime ball spits slime, and so on).
    public void spawnEquipParticles(ServerLevel world, double x, double y, double z) {
        ParticleOptions type;
        if (this == SIGHT) {
            type = ParticleTypes.PORTAL;
        }
        else if (this == FIRE || this == FIRE_EXPLOSIVE) {
            type = ParticleTypes.FLAME;
        }
        else if (this.upgradeItem != null) {
            type = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(this.upgradeItem));
        }
        else {
            return;
        }
        for (int i = 0; i < 30; i++) {
            double dx = (world.random.nextDouble() - 0.5) * 0.6;
            double dy = world.random.nextDouble() * 0.6;
            double dz = (world.random.nextDouble() - 0.5) * 0.6;
            world.sendParticles(type, x, y, z, 1, dx, dy, dz, 0.05);
        }
    }
}
