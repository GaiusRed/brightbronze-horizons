package red.gaius.brightbronze.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhAdminCommands;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.command.BbhDiskCommands;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.versioned.mc12110.McVersion12110;
import red.gaius.brightbronze.world.mob.MobSpawnTableReloadListener;
import red.gaius.brightbronze.world.rules.BiomeRuleReloadListener;

/**
 * NeoForge entrypoint for Brightbronze Horizons.
 */
@Mod(BrightbronzeHorizons.MOD_ID)
public final class BrightbronzeHorizonsNeoForge {
    public BrightbronzeHorizonsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // Initialize version-specific implementation (MC 1.21.10)
        Versioned.init(new McVersion12110());
        
        // Run common setup (registry, world gen, etc.)
        BrightbronzeHorizons.init();

        // Defer networking initialization to FMLCommonSetupEvent
        // This ensures Architectury is fully loaded before we use its NetworkManager
        modEventBus.addListener(this::onCommonSetup);

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

    /**
     * Called during FMLCommonSetupEvent - safe to use Architectury APIs here.
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Initialize networking on the main thread to ensure thread safety
        event.enqueueWork(BrightbronzeHorizons::initNetworking);
    }
}
