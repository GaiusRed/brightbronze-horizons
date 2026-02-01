package red.gaius.brightbronze.net;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.server.level.ServerPlayer;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.config.BrightbronzeConfig;

public final class BrightbronzeNetworking {

    private BrightbronzeNetworking() {
    }

    public static void init() {
        // Client-side: register receiver for S2C sync payload.
        // (On Fabric, receiver registration also registers the payload type; avoid double-registering.)
        EnvExecutor.runInEnv(EnvType.CLIENT, () -> BrightbronzeNetworking::registerClientReceivers);

        // Server-side: declare payload types we may send, and send config to players when they join.
        EnvExecutor.runInEnv(EnvType.SERVER, () -> () -> {
            NetworkManager.registerS2CPayloadType(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
            PlayerEvent.PLAYER_JOIN.register(BrightbronzeNetworking::sendConfigToPlayer);
        });
    }

    private static void registerClientReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC,
            (payload, context) -> context.queue(() -> BrightbronzeConfig.applyNetworkJson(payload.json()))
        );
    }

    private static void sendConfigToPlayer(ServerPlayer player) {
        if (!NetworkManager.canPlayerReceive(player, ConfigSyncPayload.TYPE)) {
            return;
        }
        String json = BrightbronzeConfig.toNetworkJson();
        if (json == null || json.isBlank()) {
            return;
        }
        NetworkManager.sendToPlayer(player, new ConfigSyncPayload(json));
        BrightbronzeHorizons.LOGGER.debug("Sent config sync to {}", player.getName().getString());
    }
}
