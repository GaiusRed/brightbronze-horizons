package red.gaius.brightbronze.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhDebugCommands;

/**
 * NeoForge entrypoint for Brightbronze Horizons.
 */
@Mod(BrightbronzeHorizons.MOD_ID)
public final class BrightbronzeHorizonsNeoForge {
    public BrightbronzeHorizonsNeoForge() {
        // Run common setup
        BrightbronzeHorizons.init();

        // Debug commands (helps inspect dynamic source dimensions).
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
            BbhDebugCommands.register(event.getDispatcher())
        );
    }
}
