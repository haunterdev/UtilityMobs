package toast.utilityMobs.network;

import io.netty.buffer.ByteBuf;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageFetchTargetHelper implements IMessage {

    public MessageFetchTargetHelper() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        buf.readByte(); // Empty packets break things.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(0); // Empty packets break things.
    }

    public static class Handler implements IMessageHandler<MessageFetchTargetHelper, IMessage> {

        @Override
        public IMessage onMessage(MessageFetchTargetHelper message, MessageContext ctx) {
            // proxy.getPlayer() is the client player's name (null on a dedicated server). Going through
            // the proxy keeps this class free of client-only types so the server can still register and
            // encode the packet. See issue #10.
            String player = _UtilityMobs.proxy.getPlayer();
            if (player == null) {
                return null;
            }
            return new MessageTargetHelper(TargetHelper.getTargetHelper(player));
        }

    }
}
