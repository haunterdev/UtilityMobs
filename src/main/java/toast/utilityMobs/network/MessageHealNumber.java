package toast.utilityMobs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import toast.utilityMobs._UtilityMobs;

public class MessageHealNumber implements IMessage {

    private int entityId;
    private float amount;

    public MessageHealNumber() {
    }

    public MessageHealNumber(Entity entity, float amount) {
        this.entityId = entity.getEntityId();
        this.amount = amount;
    }

    public static void send(Entity entity, float amount) {
        if (entity.world.isRemote || amount <= 0.0F) {
            return;
        }
        _UtilityMobs.CHANNEL.sendToAllAround(new MessageHealNumber(entity, amount),
                new net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint(
                        entity.dimension, entity.posX, entity.posY, entity.posZ, 64.0D));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.amount = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeFloat(this.amount);
    }

    public static class Handler implements IMessageHandler<MessageHealNumber, IMessage> {
        @Override
        public IMessage onMessage(final MessageHealNumber message, MessageContext ctx) {
            // Straight to the proxy: this class is also loaded on a dedicated server (so the server can
            // encode the packet), so it must not name a client-only type. See issue #10.
            _UtilityMobs.proxy.handleHealNumber(message.entityId, message.amount);
            return null;
        }
    }
}
