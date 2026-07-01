package toast.utilityMobs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import toast.utilityMobs.colossal.EntityColossalGolem;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageUseGolem implements IMessage {

    public MessageUseGolem() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        buf.readByte(); // Empty packets break things.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(0); // Empty packets break things.
    }

    public static class Handler implements IMessageHandler<MessageUseGolem, IMessage> {

        @Override
        public IMessage onMessage(MessageUseGolem message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (player.getRidingEntity() instanceof EntityColossalGolem) {
                        ((EntityColossalGolem) player.getRidingEntity()).attackEntityAsMob(null);
                    }
                }
            });
            return null;
        }

    }
}
