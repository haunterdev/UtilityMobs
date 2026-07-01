package toast.utilityMobs.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.block.ContainerAnvilGolem;
import toast.utilityMobs.block.ContainerFurnaceGolem;
import toast.utilityMobs.block.ContainerLanternGolem;
import toast.utilityMobs.block.ContainerWorkbenchGolem;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityContainerGolem;
import toast.utilityMobs.block.EntityFurnaceGolem;
import toast.utilityMobs.block.EntityLanternGolem;
import toast.utilityMobs.client.GuiGenericInventory;
import toast.utilityMobs.client.GuiSteamGolem;
import toast.utilityMobs.golem.ContainerSteamGolem;
import toast.utilityMobs.golem.EntitySteamGolem;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHelper implements IGuiHandler
{
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        Entity entity = world.getEntityByID(ID);
        if (entity instanceof EntitySteamGolem)
            return new ContainerSteamGolem(player.inventory, (EntitySteamGolem) entity);
        if (entity instanceof EntityLanternGolem)
            return new ContainerLanternGolem(player.inventory, (EntityLanternGolem) entity);
        if (entity instanceof toast.utilityMobs.turret.EntityTurretGolem)
            return new toast.utilityMobs.turret.ContainerTurretGolem(player.inventory, (toast.utilityMobs.turret.EntityTurretGolem) entity);
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        Entity entity = world.getEntityByID(ID);
        if (entity instanceof EntitySteamGolem)
            return new GuiSteamGolem(player.inventory, (EntitySteamGolem) entity);
        if (entity instanceof EntityLanternGolem)
            return new GuiGenericInventory(new ContainerLanternGolem(player.inventory, (EntityLanternGolem) entity), (IInventory) entity, GuiGenericInventory.TEXTURE_DISPENSER);
        if (entity instanceof toast.utilityMobs.turret.EntityTurretGolem)
            return new toast.utilityMobs.client.GuiTurretGolem(player.inventory, (toast.utilityMobs.turret.EntityTurretGolem) entity);
        return null;
    }

    /// Opens an anvil GUI using a container golem instead of a block position.
    public static void displayGUIAnvil(EntityPlayer player, EntityAnvilGolem golem) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP)player;
            playerMP.getNextWindowId();
            playerMP.connection.sendPacket(new SPacketOpenWindow(playerMP.currentWindowId, "minecraft:anvil", new TextComponentString("Repairing")));
            player.openContainer = new ContainerAnvilGolem(player.inventory, golem, player);
            player.openContainer.windowId = playerMP.currentWindowId;
            player.openContainer.addListener(playerMP);
        }
    }

    /// Opens a furnace GUI using a furnace golem instead of a furnace tile entity.
    public static void displayGUIFurnace(EntityPlayer player, EntityFurnaceGolem golem) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP)player;
            playerMP.getNextWindowId();
            playerMP.connection.sendPacket(new SPacketOpenWindow(playerMP.currentWindowId, "minecraft:furnace", golem.getDisplayName(), golem.getSizeInventory()));
            player.openContainer = new ContainerFurnaceGolem(player.inventory, golem);
            player.openContainer.windowId = playerMP.currentWindowId;
            player.openContainer.addListener(playerMP);
        }
    }

    /// Opens a workbench GUI using a container golem instead of a block position.
    public static void displayGUIWorkbench(EntityPlayer player, EntityContainerGolem golem) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP)player;
            playerMP.getNextWindowId();
            playerMP.connection.sendPacket(new SPacketOpenWindow(playerMP.currentWindowId, "minecraft:crafting_table", new TextComponentString("Crafting")));
            player.openContainer = new ContainerWorkbenchGolem(player.inventory, golem);
            player.openContainer.windowId = playerMP.currentWindowId;
            player.openContainer.addListener(playerMP);
        }
    }

    // Opens a custom GUI based on the entity passed.
    public static void displayGUICustom(EntityPlayer player, Entity golem) {
        if (player instanceof EntityPlayerMP) {
            player.openGui(_UtilityMobs.instance, golem.getEntityId(), player.world, 0, 0, 0);
        }
    }
}
