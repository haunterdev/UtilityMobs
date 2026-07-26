package toast.utilityMobs;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the mod's three commands. 1.12.2 registered each CommandBase in the server-starting event;
 * Brigadier builders go on RegisterCommandsEvent instead, but the command names, arguments and
 * permission level (2) are unchanged.
 */
@Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UMCommands {
    private UMCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(CommandUMSummon.build());
        event.getDispatcher().register(CommandUMTargetList.whitelist());
        event.getDispatcher().register(CommandUMTargetList.blacklist());
    }
}
