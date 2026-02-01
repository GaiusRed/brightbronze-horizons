package red.gaius.brightbronze.config.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ConfigPathsImpl {

    private ConfigPathsImpl() {
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
