package toast.utilityMobs.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/// Server -> client on login: asks the client to push its own target helper back to the server.
public class MessageFetchTargetHelper {

    public MessageFetchTargetHelper() {
    }

    public static void encode(MessageFetchTargetHelper message, FriendlyByteBuf buf) {
        buf.writeByte(0); // Empty packets break things.
    }

    public static MessageFetchTargetHelper decode(FriendlyByteBuf buf) {
        buf.readByte(); // Empty packets break things.
        return new MessageFetchTargetHelper();
    }

    public static void handle(MessageFetchTargetHelper message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> toast.utilityMobs.client.UMClientNetwork.replyWithTargetHelper());
        ctx.get().setPacketHandled(true);
    }
}
