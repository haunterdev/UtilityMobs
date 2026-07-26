package toast.utilityMobs.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import toast.utilityMobs.block.ContainerAnvilGolem;
import toast.utilityMobs.block.ContainerFurnaceGolem;
import toast.utilityMobs.block.ContainerWorkbenchGolem;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityContainerGolem;
import toast.utilityMobs.block.EntityFurnaceGolem;

/**
 * Opens a golem's screen. 1.12.2's IGuiHandler is gone in 1.20.1: menus are opened directly with a
 * MenuProvider, and the client rebuilds them from a registered MenuType.
 *
 * <p>The anvil, furnace and crafting golems still reuse the vanilla screens, exactly as 1.12.2 did by
 * sending a window of type "minecraft:anvil" / "minecraft:furnace" / "minecraft:crafting_table". A
 * subclass of a vanilla menu keeps the vanilla MenuType baked into its constructor, so the client
 * builds the plain vanilla menu and shows the vanilla screen without any registration of ours.
 *
 * <p>Golems with a screen of their own go through displayGUICustom, which writes the entity id into
 * the opening packet so the client can find the entity its menu belongs to.
 */
public final class GuiHelper {
    private GuiHelper() {}

    /// Opens an anvil GUI using a container golem instead of a block position.
    public static void displayGUIAnvil(Player player, EntityAnvilGolem golem) {
        if (player instanceof ServerPlayer) {
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ContainerAnvilGolem(id, inventory, golem),
                Component.literal("Repairing")));
        }
    }

    /// Opens a furnace GUI using a furnace golem instead of a furnace tile entity.
    public static void displayGUIFurnace(Player player, EntityFurnaceGolem golem) {
        if (player instanceof ServerPlayer) {
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ContainerFurnaceGolem(id, inventory, golem),
                golem.getDisplayName()));
        }
    }

    /// Opens a workbench GUI using a container golem instead of a block position.
    public static void displayGUIWorkbench(Player player, EntityContainerGolem golem) {
        if (player instanceof ServerPlayer) {
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ContainerWorkbenchGolem(id, inventory, golem),
                Component.literal("Crafting")));
        }
    }

    /// Opens a custom GUI based on the entity passed. The entity id rides along in the packet so the
    /// client-side menu factory can look the golem back up.
    public static void displayGUICustom(Player player, Entity golem) {
        if (player instanceof ServerPlayer serverPlayer && golem instanceof net.minecraft.world.MenuProvider provider) {
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeInt(golem.getId()));
        }
    }
}
