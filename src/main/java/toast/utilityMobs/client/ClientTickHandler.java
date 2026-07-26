package toast.utilityMobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.network.MessageUseGolem;
import toast.utilityMobs.network.UMChannel;

/**
 * Turns holding the attack key while riding a colossus into a swing packet. This lived on 1.12.2's
 * ClientProxy.handleClientTick, which TickHandler called each client tick.
 *
 */
@Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientTickHandler {
    private ClientTickHandler() {}

    /// Ticks left before another swing packet may be sent.
    private static int packetCooldown;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (ClientTickHandler.packetCooldown > 0) {
            ClientTickHandler.packetCooldown--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.options.keyAttack.isDown()
                && player.getVehicle() instanceof EntityColossalGolem colossus
                && colossus.getAnimId() == 0) {
            ClientTickHandler.packetCooldown = 10;
            UMChannel.sendToServer(new MessageUseGolem());
        }
    }
}
