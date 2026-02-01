package red.gaius.brightbronze.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * Platform abstraction for locating the game config directory.
 */
public final class ConfigPaths {

    private ConfigPaths() {
    }

    @ExpectPlatform
    public static Path getConfigDir() {
        throw new AssertionError("Platform implementation not found");
    }
}
