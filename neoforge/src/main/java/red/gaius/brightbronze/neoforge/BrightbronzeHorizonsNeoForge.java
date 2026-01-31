package red.gaius.brightbronze.neoforge;

import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * NeoForge entrypoint for Brightbronze Horizons.
 */
@Mod(BrightbronzeHorizons.MOD_ID)
public final class BrightbronzeHorizonsNeoForge {
    public BrightbronzeHorizonsNeoForge() {
        // Run common setup
        BrightbronzeHorizons.init();
    }
}
