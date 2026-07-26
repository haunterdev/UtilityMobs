package toast.utilityMobs;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A testing tool: one hit kills whatever it touches, however much health or armor that thing has.
 *
 * <p>Not part of the 1.12.2 mod. Creative-tab only, so golems, turrets and colossi can be cleared
 * quickly while testing. It is deliberately blunt: no attack-damage attribute to tune, no
 * enchantments, no durability. hurtEnemy zeroes the victim's invulnerability window first, so it also
 * works on something that was hit a moment ago, and it goes through {@code hurt} rather than
 * {@code kill} so death, drops and the usual events all still happen normally.
 */
public class ItemAdminSword extends Item {

    public ItemAdminSword() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.invulnerableTime = 0;
        // playerAttack requires a non-null Player, so fall back to a mob attack when something else
        // is somehow holding this.
        target.hurt(attacker instanceof net.minecraft.world.entity.player.Player player
            ? target.damageSources().playerAttack(player)
            : target.damageSources().mobAttack(attacker),
            Float.MAX_VALUE);
        return true;
    }

    /// Never takes damage, so it survives whatever it is used on.
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.utilitymobs.admin_sword.desc").withStyle(ChatFormatting.GRAY));
    }
}
