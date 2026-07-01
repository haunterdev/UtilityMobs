package toast.utilityMobs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import toast.utilityMobs.turret.EntityTurretGolem;

/** Client -> server: which=0 toggle attack-hostile, which=1 toggle attack-passive, which=2 cycle targeting mode, which=3 toggle attack-neutral. */
public class MessageTurretToggle implements IMessage {

    public int entityId;
    public byte which;

    public MessageTurretToggle() {
    }

    public MessageTurretToggle(int entityId, int which) {
        this.entityId = entityId;
        this.which = (byte) which;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.which = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.which);
    }

    public static class Handler implements IMessageHandler<MessageTurretToggle, IMessage> {
        @Override
        public IMessage onMessage(final MessageTurretToggle message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Entity e = player.getServerWorld().getEntityByID(message.entityId);
                    if (e instanceof EntityTurretGolem) {
                        EntityTurretGolem turret = (EntityTurretGolem) e;
                        if (turret.canInteract(player)) {
                            if (message.which == 2) {
                                turret.cycleTargetMode();
                            } else {
                                turret.toggleTargetFlag(message.which);
                            }
                        }
                    }
                }
            });
            return null;
        }
    }
}
