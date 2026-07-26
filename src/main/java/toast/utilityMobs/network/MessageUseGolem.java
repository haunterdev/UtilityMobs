package toast.utilityMobs.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import toast.utilityMobs.colossal.EntityColossalGolem;

/// Client -> server: the rider of a colossal golem asked it to swing.
public class MessageUseGolem {

    public MessageUseGolem() {
    }

    public static void encode(MessageUseGolem message, FriendlyByteBuf buf) {
        buf.writeByte(0); // Empty packets break things.
    }

    public static MessageUseGolem decode(FriendlyByteBuf buf) {
        buf.readByte(); // Empty packets break things.
        return new MessageUseGolem();
    }

    public static void handle(MessageUseGolem message, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getVehicle() instanceof EntityColossalGolem golem) {
            golem.doHurtTarget(null);
        }
        ctx.get().setPacketHandled(true);
    }
}
