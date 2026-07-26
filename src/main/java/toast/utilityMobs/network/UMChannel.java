package toast.utilityMobs.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import toast.utilityMobs._UtilityMobs;

/**
 * The mod's packet channel, replacing 1.12.2's {@code SimpleNetworkWrapper "UM|Info"}.
 *
 * <p>Discriminators are pinned to the values 1.12.2 used, since the message set and its meaning are
 * unchanged: 0 use-golem, 1 target-helper, 2 fetch-target-helper, 3 explosion, 4 turret-toggle,
 * 5 heal-number. 1.12.2 registered the two directions of message 1 as separate handler entries under
 * one id; 1.20.1 registers a message once and takes a nullable direction, so message 1 registers with
 * no direction and travels both ways.
 */
public final class UMChannel {
    private UMChannel() {}

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(_UtilityMobs.MODID, "info"))
        .networkProtocolVersion(() -> UMChannel.PROTOCOL_VERSION)
        .clientAcceptedVersions(UMChannel.PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(UMChannel.PROTOCOL_VERSION::equals)
        .simpleChannel();

    private static boolean registered = false;

    /// Registers every message. Called once from the mod constructor.
    public static void register() {
        if (UMChannel.registered)
            return;
        UMChannel.registered = true;

        UMChannel.CHANNEL.messageBuilder(MessageUseGolem.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MessageUseGolem::encode).decoder(MessageUseGolem::decode).consumerMainThread(MessageUseGolem::handle).add();
        UMChannel.CHANNEL.messageBuilder(MessageTargetHelper.class, 1)
            .encoder(MessageTargetHelper::encode).decoder(MessageTargetHelper::decode).consumerMainThread(MessageTargetHelper::handle).add();
        UMChannel.CHANNEL.messageBuilder(MessageFetchTargetHelper.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MessageFetchTargetHelper::encode).decoder(MessageFetchTargetHelper::decode).consumerMainThread(MessageFetchTargetHelper::handle).add();
        UMChannel.CHANNEL.messageBuilder(MessageExplosion.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MessageExplosion::encode).decoder(MessageExplosion::decode).consumerMainThread(MessageExplosion::handle).add();
        UMChannel.CHANNEL.messageBuilder(MessageTurretToggle.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MessageTurretToggle::encode).decoder(MessageTurretToggle::decode).consumerMainThread(MessageTurretToggle::handle).add();
        UMChannel.CHANNEL.messageBuilder(MessageHealNumber.class, 5, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MessageHealNumber::encode).decoder(MessageHealNumber::decode).consumerMainThread(MessageHealNumber::handle).add();
    }

    public static void sendToServer(Object message) {
        UMChannel.CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        UMChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /// 1.12.2's sendToDimension.
    public static void sendToDimension(Object message, ResourceKey<Level> dimension) {
        UMChannel.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }

    /// 1.12.2's sendToAllAround, with the same 64 block radius every caller used.
    public static void sendNear(Object message, Entity around) {
        UMChannel.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
            around.getX(), around.getY(), around.getZ(), 64.0, around.level().dimension())), message);
    }
}
