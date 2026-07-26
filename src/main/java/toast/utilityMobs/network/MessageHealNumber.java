package toast.utilityMobs.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/// Server -> client: pop a floating green +N over an entity that was just healed.
public class MessageHealNumber {

    private final int entityId;
    private final float amount;

    public MessageHealNumber(Entity entity, float amount) {
        this.entityId = entity.getId();
        this.amount = amount;
    }

    private MessageHealNumber(int entityId, float amount) {
        this.entityId = entityId;
        this.amount = amount;
    }

    /// Sends to everyone within 64 blocks, as 1.12.2's sendToAllAround did.
    public static void send(Entity entity, float amount) {
        if (entity.level().isClientSide || amount <= 0.0F)
            return;
        UMChannel.sendNear(new MessageHealNumber(entity, amount), entity);
    }

    public static void encode(MessageHealNumber message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeFloat(message.amount);
    }

    public static MessageHealNumber decode(FriendlyByteBuf buf) {
        return new MessageHealNumber(buf.readInt(), buf.readFloat());
    }

    public static void handle(MessageHealNumber message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> toast.utilityMobs.client.UMClientNetwork.healNumber(message.entityId, message.amount));
        ctx.get().setPacketHandled(true);
    }
}
