package red.gaius.brightbronze.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhAdminCommands;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.command.BbhDiskCommands;
import red.gaius.brightbronze.world.mob.MobSpawnTableReloadListener;
import red.gaius.brightbronze.world.rules.BiomeRuleReloadListener;

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

        // Phase 8/9: data-driven biome rules
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) ->
            event.addListener(
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "biome_rules"),
                new BiomeRuleReloadListener()
            )
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
