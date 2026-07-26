package toast.utilityMobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.ForgeRegistries;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.network.MessageExplosion;
import toast.utilityMobs.network.MessageTargetHelper;
import toast.utilityMobs.network.UMChannel;

/**
 * The client half of the packet handlers. 1.12.2 put this logic inside each message's Handler and
 * guarded it with @SideOnly; 1.20.1 needs the client-only types kept out of the server's class
 * loader entirely, so the bodies live here and the messages reach them through DistExecutor.
 */
public final class UMClientNetwork {
    private UMClientNetwork() {}

    /// The logged-in player's name, or null if there is none yet.
    public static String localPlayerName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : mc.player.getGameProfile().getName();
    }

    /// Replies to MessageFetchTargetHelper with the local player's own helper.
    public static void replyWithTargetHelper() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        UMChannel.sendToServer(new MessageTargetHelper(
            TargetHelper.getTargetHelper(mc.player.getGameProfile().getName())));
    }

    /// Starts a jukebox golem's record. 1.12.2 kept this on ClientProxy.
    public static void playRecordGolem(EntityJukeboxGolem golem, String record) {
        if ("".equals(record))
            return;
        try {
            Minecraft client = Minecraft.getInstance();
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(record));
            if (sound == null) {
                sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("record." + record));
            }
            if (sound == null) {
                return;
            }
            RecordItem itemRecord = RecordItem.getBySound(sound);
            if (itemRecord != null) {
                client.gui.setNowPlaying(itemRecord.getDisplayName());
            }
            client.getSoundManager().play(new MovingSoundRecord(golem, record, sound));
        }
        catch (Exception ex) {
            _UtilityMobs.console("[WARNING] Unable to play record \"", record, "\" in record golem!");
            ex.printStackTrace();
        }
    }

    public static void healNumber(int entityId, float amount) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        Entity entity = level.getEntity(entityId);
        if (entity != null) {
            HealTextRenderer.add(entity, amount);
        }
    }

    public static void explosion(MessageExplosion message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        if (message.type == MessageExplosion.ExplosionType.NORMAL && message.size >= 2.0F) {
            level.addParticle(UMSound.HUGE_EXPLOSION, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
        }
        else {
            level.addParticle(UMSound.LARGE_EXPLODE, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
        }

        if (message.type == MessageExplosion.ExplosionType.NORMAL && message.affectedBlocks != null) {
            int count = message.affectedBlocks.length;
            double[] relPos;
            double fxPosX, fxPosY, fxPosZ;
            for (int i = 0; i < count; i++) {
                relPos = new double[3];
                for (int d = 0; d < 3; d++) {
                    relPos[d] = message.affectedBlocks[i][d] + level.random.nextFloat();
                }
                fxPosX = relPos[0] + message.posX;
                fxPosY = relPos[1] + message.posY;
                fxPosZ = relPos[2] + message.posZ;
                double velo = Math.sqrt(relPos[0] * relPos[0] + relPos[1] * relPos[1] + relPos[2] * relPos[2]);
                double mult = 0.5 / (velo / message.size + 0.1) * (level.random.nextFloat() * level.random.nextFloat() + 0.3F) / velo;
                for (int d = 0; d < 3; d++) {
                    relPos[d] *= mult;
                }
                level.addParticle(UMSound.EXPLODE, (fxPosX + message.posX) / 2.0, (fxPosY + message.posY) / 2.0, (fxPosZ + message.posZ) / 2.0, relPos[0], relPos[1], relPos[2]);
                level.addParticle(UMSound.SMOKE, fxPosX, fxPosY, fxPosZ, relPos[0], relPos[1], relPos[2]);
            }
        }
    }
}
