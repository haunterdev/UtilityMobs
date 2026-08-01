package toast.utilityMobs.network;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import toast.utilityMobs._UtilityMobs;

public class MessageExplosion implements IMessage {

    public static enum ExplosionType {
        SAFE(0),
        NORMAL(1);

        private static final ExplosionType[] allTypes = new ExplosionType[ExplosionType.values().length];

        private final byte TYPE_ID;

        ExplosionType(int id) {
            this.TYPE_ID = (byte)id;
        }

        // Returns this type's id.
        public byte getId() {
            return this.TYPE_ID;
        }

        // Returns the explosion type with the given id.
        public static ExplosionType getType(byte id) {
            return ExplosionType.allTypes[id % ExplosionType.allTypes.length];
        }

        static {
            // Assign all enum types to an ordered array.
            ExplosionType[] types = ExplosionType.values();
            int length = types.length;

            for (int i = 0; i < length; i++) {
                ExplosionType type = types[i];
                ExplosionType.allTypes[type.getId()] = type;
            }
        }
    }

    // This explosion's type.
    public ExplosionType type;
    // The explosion radius.
    public float size;
    // The explosion coords.
    public float posX, posY, posZ;

    // Array of affected block relative coords. Only used for NORMAL type explosions.
    public byte[][] affectedBlocks;

    public MessageExplosion() {
    }

    // Built from the data the EffectHelper already holds (1.12.2 Explosion exposes no
    // public size/coord getters, only getAffectedBlockPositions()).
    public MessageExplosion(float size, double x, double y, double z, boolean smoking, List<BlockPos> affectedBlockPositions) {
        this.type = smoking ? ExplosionType.NORMAL : ExplosionType.SAFE;
        this.size = size;
        this.posX = (float)x;
        this.posY = (float)y;
        this.posZ = (float)z;

        if (this.type == ExplosionType.NORMAL && affectedBlockPositions != null) {
            int count = affectedBlockPositions.size();
            this.affectedBlocks = new byte[count][];
            int blockX = (int)Math.floor(this.posX);
            int blockY = (int)Math.floor(this.posY);
            int blockZ = (int)Math.floor(this.posZ);
            for (int i = 0; i < count; i++) {
                BlockPos pos = affectedBlockPositions.get(i);
                this.affectedBlocks[i] = new byte[] {
                        (byte)(pos.getX() - blockX), (byte)(pos.getY() - blockY), (byte)(pos.getZ() - blockZ)
                };
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.type = ExplosionType.getType(buf.readByte());
        this.size = buf.readFloat();
        this.posX = buf.readFloat();
        this.posY = buf.readFloat();
        this.posZ = buf.readFloat();

        if (this.type == ExplosionType.NORMAL) {
            int count = buf.readInt();
            this.affectedBlocks = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.affectedBlocks[i] = new byte[] {
                        buf.readByte(), buf.readByte(), buf.readByte()
                };
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.type.getId());
        buf.writeFloat(this.size);
        buf.writeFloat(this.posX);
        buf.writeFloat(this.posY);
        buf.writeFloat(this.posZ);

        if (this.type == ExplosionType.NORMAL) {
            int count = this.affectedBlocks.length;
            buf.writeInt(count);
            for (int i = 0; i < count; i++) {
                for (int d = 0; d < 3; d++) {
                    buf.writeByte(this.affectedBlocks[i][d]);
                }
            }
        }
    }

    public static class Handler implements IMessageHandler<MessageExplosion, IMessage> {

        @Override
        public IMessage onMessage(final MessageExplosion message, MessageContext ctx) {
            // Straight to the proxy: this class is also loaded on a dedicated server (so the server can
            // encode the packet), so it must not name a client-only type. See issue #10.
            _UtilityMobs.proxy.handleExplosionFx(message);
            return null;
        }
    }
}
