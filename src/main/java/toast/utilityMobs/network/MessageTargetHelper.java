package toast.utilityMobs.network;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import toast.utilityMobs.TargetHelper;

/**
 * Both directions: one player's whitelist/blacklist/permission state.
 *
 * <p>1.12.2 loaded the helper straight out of {@code fromBytes}. Decoding in 1.20.1 happens on the
 * netty thread, so the payload is carried across as a byte array and applied in the main-thread
 * handler instead. The bytes themselves are still whatever TargetHelper.save writes.
 */
public class MessageTargetHelper {

    private final String owner;
    private final byte[] payload;

    public MessageTargetHelper(TargetHelper targetHelper) {
        this.owner = targetHelper.owner;
        FriendlyByteBuf scratch = new FriendlyByteBuf(Unpooled.buffer());
        targetHelper.save(scratch);
        this.payload = new byte[scratch.readableBytes()];
        scratch.readBytes(this.payload);
    }

    private MessageTargetHelper(String owner, byte[] payload) {
        this.owner = owner;
        this.payload = payload;
    }

    public static void encode(MessageTargetHelper message, FriendlyByteBuf buf) {
        buf.writeUtf(message.owner);
        buf.writeByteArray(message.payload);
    }

    public static MessageTargetHelper decode(FriendlyByteBuf buf) {
        return new MessageTargetHelper(buf.readUtf(), buf.readByteArray());
    }

    public static void handle(MessageTargetHelper message, Supplier<NetworkEvent.Context> ctx) {
        TargetHelper.getTargetHelper(message.owner).load(new FriendlyByteBuf(Unpooled.wrappedBuffer(message.payload)));
        ctx.get().setPacketHandled(true);
    }
}
