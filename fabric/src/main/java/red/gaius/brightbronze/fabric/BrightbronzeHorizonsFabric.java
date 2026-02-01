package red.gaius.brightbronze.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhDebugCommands;

/**
 * Fabric entrypoint for Brightbronze Horizons.
 */
public final class BrightbronzeHorizonsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run common setup
        BrightbronzeHorizons.init();

        // Debug commands (helps inspect dynamic source dimensions).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BbhDebugCommands.register(dispatcher)
        );
    }
}
