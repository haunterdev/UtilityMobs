package toast.utilityMobs.network;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/// Server -> client: the particle half of a golem explosion, which the server never plays itself.
public class MessageExplosion {

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

    private MessageExplosion() {
    }

    // Built from the data the EffectHelper already holds (Explosion exposes no public size/coord
    // getters, only getToBlow()).
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

    public static void encode(MessageExplosion message, FriendlyByteBuf buf) {
        buf.writeByte(message.type.getId());
        buf.writeFloat(message.size);
        buf.writeFloat(message.posX);
        buf.writeFloat(message.posY);
        buf.writeFloat(message.posZ);

        if (message.type == ExplosionType.NORMAL) {
            int count = message.affectedBlocks.length;
            buf.writeInt(count);
            for (int i = 0; i < count; i++) {
                for (int d = 0; d < 3; d++) {
                    buf.writeByte(message.affectedBlocks[i][d]);
                }
            }
        }
    }

    public static MessageExplosion decode(FriendlyByteBuf buf) {
        MessageExplosion message = new MessageExplosion();
        message.type = ExplosionType.getType(buf.readByte());
        message.size = buf.readFloat();
        message.posX = buf.readFloat();
        message.posY = buf.readFloat();
        message.posZ = buf.readFloat();

        if (message.type == ExplosionType.NORMAL) {
            int count = buf.readInt();
            message.affectedBlocks = new byte[count][];
            for (int i = 0; i < count; i++) {
                message.affectedBlocks[i] = new byte[] {
                        buf.readByte(), buf.readByte(), buf.readByte()
                };
            }
        }
        return message;
    }

    public static void handle(MessageExplosion message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> toast.utilityMobs.client.UMClientNetwork.explosion(message));
        ctx.get().setPacketHandled(true);
    }
}
