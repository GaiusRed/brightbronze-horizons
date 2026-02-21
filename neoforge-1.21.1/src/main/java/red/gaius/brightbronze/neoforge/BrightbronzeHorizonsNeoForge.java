package red.gaius.brightbronze.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhAdminCommands;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.command.BbhDiskCommands;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.versioned.mc1211.McVersion1211;
import red.gaius.brightbronze.world.mob1211.MobSpawnTableReloadListener;
import red.gaius.brightbronze.world.rules1211.BiomeRuleReloadListener;

/**
 * NeoForge entrypoint for Brightbronze Horizons (MC 1.21.1).
 */
@Mod(BrightbronzeHorizons.MOD_ID)
public final class BrightbronzeHorizonsNeoForge {
    public BrightbronzeHorizonsNeoForge() {
        // Initialize version-specific implementation (MC 1.21.1)
        Versioned.init(new McVersion1211());
        
        // Run common setup
        BrightbronzeHorizons.init();

        // Phase 7: datapack-driven mob spawn tables
        // Note: In 1.21.1, the event is AddReloadListenerEvent, not AddServerReloadListenersEvent
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
            event.addListener(new MobSpawnTableReloadListener())
        );

        // Phase 8/9: data-driven biome rules
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
            event.addListener(new BiomeRuleReloadListener())
        );

        // Debug commands (helps inspect dynamic source dimensions).
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
            BbhDebugCommands.register(event.getDispatcher())
        );

        // Phase 11: disk management and usage reporting.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
            BbhDiskCommands.register(event.getDispatcher())
        );

        // Phase 12: admin commands.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
            BbhAdminCommands.register(event.getDispatcher())
        );
    }
}
