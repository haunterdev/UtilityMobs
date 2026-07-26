package toast.utilityMobs.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import toast.utilityMobs.turret.EntityTurretGolem;

/** Client -> server: which=0 toggle attack-hostile, which=1 toggle attack-passive, which=2 cycle targeting mode, which=3 toggle attack-neutral. */
public class MessageTurretToggle {

    public int entityId;
    public byte which;

    public MessageTurretToggle(int entityId, int which) {
        this.entityId = entityId;
        this.which = (byte)which;
    }

    public static void encode(MessageTurretToggle message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeByte(message.which);
    }

    public static MessageTurretToggle decode(FriendlyByteBuf buf) {
        return new MessageTurretToggle(buf.readInt(), buf.readByte());
    }

    public static void handle(MessageTurretToggle message, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            Entity entity = player.serverLevel().getEntity(message.entityId);
            if (entity instanceof EntityTurretGolem turret && turret.canInteract(player)) {
                if (message.which == 2) {
                    turret.cycleTargetMode();
                }
                else {
                    turret.toggleTargetFlag(message.which);
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
