package red.gaius.brightbronze.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import red.gaius.brightbronze.BrightbronzeHorizons;

public record ConfigSyncPayload(String json) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
        CustomPacketPayload.codec(ConfigSyncPayload::write, ConfigSyncPayload::new);

    public static final Type<ConfigSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "config_sync"));

    private ConfigSyncPayload(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.json);
    }

    @Override
    public Type<ConfigSyncPayload> type() {
        return TYPE;
    }
}
