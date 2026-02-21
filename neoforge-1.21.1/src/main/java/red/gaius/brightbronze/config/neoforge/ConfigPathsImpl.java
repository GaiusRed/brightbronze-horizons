package red.gaius.brightbronze.config.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ConfigPathsImpl {

    private ConfigPathsImpl() {
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
