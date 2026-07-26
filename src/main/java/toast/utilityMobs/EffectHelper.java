package toast.utilityMobs;

import java.util.Map;
import java.util.Random;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;

public abstract class EffectHelper
{
    // Clears the entity's AI tasks.
    public static void clearAI(Mob entity) {
        entity.goalSelector.removeAllGoals(goal -> true);
    }

    // Applies the potion's effect on the entity. If the potion is already active, its duration is increased up to the given duration and its amplifier is increased by the given amplifier + 1.
    public static void stackEffect(LivingEntity entity, MobEffect potion, int duration, int amplifier) {
        if (entity.hasEffect(potion)) {
            MobEffectInstance potionEffect = entity.getEffect(potion);
            entity.addEffect(new MobEffectInstance(potion, Math.max(duration, potionEffect.getDuration()), potionEffect.getAmplifier() + amplifier + 1));
        }
        else {
            entity.addEffect(new MobEffectInstance(potion, duration, amplifier));
        }
    }

    // Applies the potion's effect on the entity. If the potion is already active, its duration is increased up to the given duration and its amplifier is increased by the given amplifier + 1 up to the given amplifierMax.
    public static void stackEffect(LivingEntity entity, MobEffect potion, int duration, int amplifier, int amplifierMax) {
        if (amplifierMax < 0) {
            EffectHelper.stackEffect(entity, potion, duration, amplifier);
            return;
        }
        if (entity.hasEffect(potion)) {
            MobEffectInstance potionEffect = entity.getEffect(potion);
            entity.addEffect(new MobEffectInstance(potion, Math.max(duration, potionEffect.getDuration()), Math.min(amplifierMax, potionEffect.getAmplifier() + amplifier + 1)));
        }
        else if (amplifier >= 0) {
            entity.addEffect(new MobEffectInstance(potion, duration, Math.min(amplifier, amplifierMax)));
        }
    }

    // Causes the itemStack to glow as if it is enchanted. (Modern glint trick: an
    // "Enchantments" list holding one empty compound - counts as enchanted, applies nothing.)
    public static void setItemGlowing(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("Enchantments", Tag.TAG_LIST)) {
            ListTag ench = new ListTag();
            ench.add(new CompoundTag());
            tag.put("Enchantments", ench);
        }
    }

    // Sets the item stack's name.
    public static void setItemName(ItemStack itemStack, int rarityColor, String name) {
        EffectHelper.setItemName(itemStack, "§" + Integer.toHexString(rarityColor) + name);
    }
    public static void setItemName(ItemStack itemStack, String name) {
        if (itemStack != null && !itemStack.isEmpty()) {
            itemStack.setHoverName(Component.literal(name));
        }
    }

    // Removes all info text from an item stack.
    public static void clearItemText(ItemStack itemStack) {
        if (itemStack.getTag() != null && itemStack.getTag().contains("display")) {
            itemStack.getTag().getCompound("display").remove("Lore");
        }
    }

    // Sets or adds item stack info text.
    public static void setItemText(ItemStack itemStack, int rarityColor, String... text) {
        EffectHelper.clearItemText(itemStack);
        EffectHelper.addItemText(itemStack, rarityColor, text);
    }
    public static void setItemText(ItemStack itemStack, String... text) {
        EffectHelper.clearItemText(itemStack);
        EffectHelper.addItemText(itemStack, text);
    }
    public static void addItemText(ItemStack itemStack, int rarityColor, String... text) {
        String color = "§" + Integer.toHexString(rarityColor);
        for (int i = text.length; i-- > 0;) {
            text[i] = color + text[i];
        }
        EffectHelper.addItemText(itemStack, text);
    }
    public static void addItemText(ItemStack itemStack, String... text) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("display", Tag.TAG_COMPOUND)) {
            tag.put("display", new CompoundTag());
        }
        CompoundTag displayTag = tag.getCompound("display");
        if (!displayTag.contains("Lore", Tag.TAG_LIST)) {
            displayTag.put("Lore", new ListTag());
        }
        ListTag lore = displayTag.getList("Lore", Tag.TAG_STRING);
        for (String line : text) {
            // Lore lines are serialized text components in 1.20.1.
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        }
    }

    // Applies the enchantment to the itemStack at the given level or changes an existing enchantment's level.
    public static void overrideEnchantment(ItemStack itemStack, Enchantment enchantment, int level) {
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(itemStack);
        enchants.put(enchantment, level);
        EnchantmentHelper.setEnchantments(enchants, itemStack);
    }

    // Applies the enchantment to the itemStack at the given level. Called by all other enchantItem methods to do the actual enchanting.
    public static void enchantItem(ItemStack itemStack, Enchantment enchantment, int level) {
        itemStack.enchant(enchantment, level);
    }

    // Applies the enchantment with the given (registry-numeric) enchantment id and level.
    public static void enchantItem(ItemStack itemStack, int enchantmentID, int level) {
        Enchantment ench = BuiltInRegistries.ENCHANTMENT.byId(enchantmentID);
        if (ench != null) {
            EffectHelper.enchantItem(itemStack, ench, level);
        }
    }

    // Randomly enchants the itemStack based on the level (identical to using an enchantment table).
    public static boolean enchantItem(ItemStack itemStack, int level) {
        return EffectHelper.enchantItem(_UtilityMobs.random, itemStack, level);
    }
    public static boolean enchantItem(Random random, ItemStack itemStack, int level) {
        return EffectHelper.enchantItem(RandomSource.create(random.nextLong()), itemStack, level);
    }
    public static boolean enchantItem(RandomSource random, ItemStack itemStack, int level) {
        if (level <= 0 || itemStack == null || itemStack.isEmpty() || !itemStack.isEnchantable())
            return false;
        EnchantmentHelper.enchantItem(random, itemStack, level, false);
        return true;
    }

    // Dyes the given itemStack. Only works on leather armor, returns true if it works.
    public static boolean dye(ItemStack itemStack, String colorName) {
        String norm = colorName.toLowerCase().replace("_", "");
        for (DyeColor color : DyeColor.values()) {
            if (norm.equals(color.getName().replace("_", "")))
                return EffectHelper.dye(itemStack, (byte)color.getId());
        }
        _UtilityMobs.debugException("Tried to dye with an invalid dye name (" + colorName + ")! Valid dye names: black, red, green, brown, blue, purple, cyan, silver, gray, pink, lime, yellow, lightBlue, magenta, orange, white.");
        return false;
    }
    public static boolean dye(ItemStack itemStack, byte colorIndex) {
        if (colorIndex < 0 || colorIndex >= 16) {
            _UtilityMobs.debugException("Tried to dye with an invalid dye index (" + colorIndex + ")!");
            return false;
        }
        float[] rgb = Sheep.getColorArray(DyeColor.byId(colorIndex));
        return EffectHelper.dye(itemStack, (int)(rgb[0] * 255.0F), (int)(rgb[1] * 255.0F), (int)(rgb[2] * 255.0F));
    }
    public static boolean dye(ItemStack itemStack, int red, int green, int blue) {
        if (red > 255 || green > 255 || blue > 255 || red < 0 || green < 0 || blue < 0) {
            _UtilityMobs.debugException("Tried to dye with an invalid RGB value (" + red + ", " + green + ", " + blue + ")!");
            return false;
        }
        return EffectHelper.dye(itemStack, (red << 16) + (green << 8) + blue);
    }
    public static boolean dye(ItemStack itemStack, int color) {
        if (color < 0 || color > 0xffffff) {
            _UtilityMobs.debugException("Tried to dye with an invalid color value (" + color + ")!");
            return false;
        }
        if (itemStack.getItem() instanceof DyeableLeatherItem dyeable) {
            dyeable.setColor(itemStack, color);
            return true;
        }
        return false;
    }

    // Returns true if the itemstack is a lava or fire weapon.
    public static boolean isFireWeapon(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return false;
        return itemStack.getItem() == Items.FLINT_AND_STEEL || itemStack.getItem() == Blocks.FIRE.asItem();
    }
    public static boolean isLavaWeapon(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return false;
        return itemStack.getItem() == Items.LAVA_BUCKET || itemStack.getItem() == Blocks.LAVA.asItem();
    }

    // Creates an instance of an explosion at the exploder with the given power.
    public static Explosion explosion(Entity exploder, float power) {
        boolean mobGriefing = exploder.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return EffectHelper.explosion(exploder, power, false, mobGriefing);
    }

    // Creates an explosion at the exploder with explicit flame/terrain flags.
    private static Explosion explosion(Entity exploder, float power, boolean flaming, boolean smoking) {
        return new Explosion(exploder.level(), exploder, exploder.getX(), exploder.getY(), exploder.getZ(), power, flaming,
                smoking ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.KEEP);
    }

    /// Causes a standard explosion at the exploder with the given power.
    public static Explosion explode(Entity exploder, float power) {
        boolean mobGriefing = exploder.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return EffectHelper.explode(exploder, EffectHelper.explosion(exploder, power, false, mobGriefing), mobGriefing, power);
    }

    /// Triggers an explosion that damages entities, and blocks if smoking is true.
    private static Explosion explode(Entity exploder, Explosion explosion, boolean smoking, float power) {
        explosion.explode();
        explosion.finalizeExplosion(true);
        if (!exploder.level().isClientSide) {
            toast.utilityMobs.network.UMChannel.sendToDimension(
                new toast.utilityMobs.network.MessageExplosion(power, exploder.getX(), exploder.getY(), exploder.getZ(), smoking, explosion.getToBlow()),
                exploder.level().dimension());
        }
        return explosion;
    }

    /// Causes a fiery explosion at the exploder with the given power.
    public static Explosion explodeFire(Entity exploder, float power) {
        boolean mobGriefing = exploder.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return EffectHelper.explode(exploder, EffectHelper.explosion(exploder, power, true, mobGriefing), mobGriefing, power);
    }

    /// Causes an explosion that does not destroy blocks at the exploder with the given power.
    public static Explosion explodeSafe(Entity exploder, float power) {
        return EffectHelper.explode(exploder, EffectHelper.explosion(exploder, power, false, false), false, power);
    }

    /// Causes a fiery explosion that does not destroy blocks at the exploder with the given power.
    public static Explosion explodeFireSafe(Entity exploder, float power) {
        return EffectHelper.explode(exploder, EffectHelper.explosion(exploder, power, true, false), false, power);
    }
}
