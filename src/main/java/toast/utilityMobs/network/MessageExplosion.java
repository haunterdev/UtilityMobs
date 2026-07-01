package toast.utilityMobs.network;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import toast.utilityMobs.UMSound;

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
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Handler.this.handle(message);
                }
            });
            return null;
        }

        private void handle(MessageExplosion message) {
            World world = FMLClientHandler.instance().getWorldClient();
            if (world == null)
                return;
            if (message.type == ExplosionType.NORMAL && message.size >= 2.0F) {
                world.spawnParticle(UMSound.HUGE_EXPLOSION, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
            }
            else {
                world.spawnParticle(UMSound.LARGE_EXPLODE, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
            }

            if (message.type == ExplosionType.NORMAL && message.affectedBlocks != null) {
                int count = message.affectedBlocks.length;
                double[] relPos;
                double fxPosX, fxPosY, fxPosZ;
                for (int i = 0; i < count; i++) {
                    relPos = new double[3];
                    for (int d = 0; d < 3; d++) {
                        relPos[d] = message.affectedBlocks[i][d] + world.rand.nextFloat();
                    }
                    fxPosX = relPos[0] + message.posX;
                    fxPosY = relPos[1] + message.posY;
                    fxPosZ = relPos[2] + message.posZ;
                    double velo = Math.sqrt(relPos[0] * relPos[0] + relPos[1] * relPos[1] + relPos[2] * relPos[2]);
                    double mult = 0.5 / (velo / message.size + 0.1) * (world.rand.nextFloat() * world.rand.nextFloat() + 0.3F) / velo;
                    for (int d = 0; d < 3; d++) {
                        relPos[d] *= mult;
                    }
                    world.spawnParticle(UMSound.EXPLODE, (fxPosX + message.posX) / 2.0, (fxPosY + message.posY) / 2.0, (fxPosZ + message.posZ) / 2.0, relPos[0], relPos[1], relPos[2]);
                    world.spawnParticle(UMSound.SMOKE, fxPosX, fxPosY, fxPosZ, relPos[0], relPos[1], relPos[2]);
                }
            }
        }
    }
}
