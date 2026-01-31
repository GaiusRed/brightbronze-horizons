package red.gaius.brightbronze.fabric;

import net.fabricmc.api.ModInitializer;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Fabric entrypoint for Brightbronze Horizons.
 */
public final class BrightbronzeHorizonsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run common setup
        BrightbronzeHorizons.init();
    }
}
