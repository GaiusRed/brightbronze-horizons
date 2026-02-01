package red.gaius.brightbronze.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.world.mob.MobSpawnTableReloadListener;

/**
 * NeoForge entrypoint for Brightbronze Horizons.
 */
@Mod(BrightbronzeHorizons.MOD_ID)
public final class BrightbronzeHorizonsNeoForge {
    public BrightbronzeHorizonsNeoForge() {
        // Run common setup
        BrightbronzeHorizons.init();

        // Phase 7: datapack-driven mob spawn tables
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) ->
            event.addListener(
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "mob_spawns"),
                new MobSpawnTableReloadListener()
            )
        );

        // Debug commands (helps inspect dynamic source dimensions).
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
            BbhDebugCommands.register(event.getDispatcher())
        );
    }
}
