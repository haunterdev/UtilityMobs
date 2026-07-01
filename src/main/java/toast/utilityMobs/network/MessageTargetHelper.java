package toast.utilityMobs.network;

import toast.utilityMobs.TargetHelper;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageTargetHelper implements IMessage {

    private String owner;

    public MessageTargetHelper() {
    }

    public MessageTargetHelper(TargetHelper targetHelper) {
        this.owner = targetHelper.owner;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.owner = ByteBufUtils.readUTF8String(buf);
        TargetHelper.getTargetHelper(this.owner).load(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.owner);
        TargetHelper.getTargetHelper(this.owner).save(buf);
    }

    public static class Handler implements IMessageHandler<MessageTargetHelper, IMessage> {

        @Override
        public IMessage onMessage(MessageTargetHelper message, MessageContext ctx) {
            // Already loaded in fromBytes()
            return null;
        }

    }
}
