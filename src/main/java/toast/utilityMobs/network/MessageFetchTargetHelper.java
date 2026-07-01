package toast.utilityMobs.network;

import io.netty.buffer.ByteBuf;
import toast.utilityMobs.TargetHelper;
import net.minecraftforge.fml.client.FMLClientHandler;
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
            return new MessageTargetHelper(TargetHelper.getTargetHelper(FMLClientHandler.instance().getClientPlayerEntity().getName()));
        }

    }
}
