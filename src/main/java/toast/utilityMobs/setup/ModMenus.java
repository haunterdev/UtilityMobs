package toast.utilityMobs.setup;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegistryObject;
import toast.utilityMobs.block.ContainerLanternGolem;
import toast.utilityMobs.block.EntityLanternGolem;
import toast.utilityMobs.golem.ContainerSteamGolem;
import toast.utilityMobs.golem.EntitySteamGolem;
import toast.utilityMobs.turret.ContainerTurretGolem;
import toast.utilityMobs.turret.EntityTurretGolem;

/**
 * Menus that need a type of their own, i.e. the ones with a screen this mod draws itself. The golems
 * that reuse a vanilla screen (anvil, furnace, crafting) do not appear here: their menus subclass a
 * vanilla menu and so inherit the vanilla MenuType.
 *
 * <p>Each factory reads the golem's entity id out of the opening packet, which is what
 * {@link toast.utilityMobs.network.GuiHelper#displayGUICustom} writes. 1.12.2 passed the same id
 * through IGuiHandler's ID parameter.
 */
public final class ModMenus {
    private ModMenus() {}

    public static final RegistryObject<MenuType<ContainerLanternGolem>> LANTERN_GOLEM =
        ModRegistries.MENUS.register("lantern_golem", () -> IForgeMenuType.create(
            (IContainerFactory<ContainerLanternGolem>)(id, inventory, buf) -> {
                EntityLanternGolem golem = ModMenus.golem(inventory, buf.readInt(), EntityLanternGolem.class);
                return new ContainerLanternGolem(id, inventory, golem);
            }));

    public static final RegistryObject<MenuType<ContainerTurretGolem>> TURRET_GOLEM =
        ModRegistries.MENUS.register("turret_golem", () -> IForgeMenuType.create(
            (IContainerFactory<ContainerTurretGolem>)(id, inventory, buf) -> {
                EntityTurretGolem turret = ModMenus.golem(inventory, buf.readInt(), EntityTurretGolem.class);
                return new ContainerTurretGolem(id, inventory, turret);
            }));

    public static final RegistryObject<MenuType<ContainerSteamGolem>> STEAM_GOLEM =
        ModRegistries.MENUS.register("steam_golem", () -> IForgeMenuType.create(
            (IContainerFactory<ContainerSteamGolem>)(id, inventory, buf) -> {
                EntitySteamGolem golem = ModMenus.golem(inventory, buf.readInt(), EntitySteamGolem.class);
                return new ContainerSteamGolem(id, inventory, golem);
            }));

    /// Looks the golem back up on the client. Returns null only if the entity has already gone away,
    /// which the menu constructors tolerate because the screen closes on the next validity check.
    public static <T extends Entity> T golem(Inventory inventory, int entityId, Class<T> type) {
        Entity entity = inventory.player.level().getEntity(entityId);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }
}
