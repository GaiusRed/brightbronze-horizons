package red.gaius.brightbronze;

import dev.architectury.event.events.common.LifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.gaius.brightbronze.config.BrightbronzeConfig;
import red.gaius.brightbronze.net.BrightbronzeNetworking;
import red.gaius.brightbronze.registry.ModArmorMaterials;
import red.gaius.brightbronze.registry.ModBlockEntities;
import red.gaius.brightbronze.registry.ModBlocks;
import red.gaius.brightbronze.registry.ModCreativeTabs;
import red.gaius.brightbronze.registry.ModItems;
import red.gaius.brightbronze.registry.ModWorldGen;
import red.gaius.brightbronze.world.StartingAreaManager;
import red.gaius.brightbronze.world.chunk.ChunkExpansionManager;

/**
 * Main mod class for Brightbronze Horizons.
 * A chunk-expansion mod where players expand their playable area by spawning
 * terrain chunks from biome-coherent source dimensions.
 */
public final class BrightbronzeHorizons {
    public static final String MOD_ID = "brightbronze_horizons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Common initialization called by both Fabric and NeoForge entrypoints.
     */
    public static void init() {
        LOGGER.info("Initializing Brightbronze Horizons...");

        // Register all mod content
        // Order: Blocks first (for BlockItems), then Items (which includes armor from ModArmorMaterials)
        ModBlocks.register();
        
        // Force class loading of ModArmorMaterials so armor items are added to ModItems.ITEMS
        // before we call ModItems.register()
        ModArmorMaterials.register();
        
        ModItems.register();
        ModBlockEntities.register();
        ModCreativeTabs.register();
        
        // Register world generation components (chunk generators)
        ModWorldGen.register();

        // Network packets (Phase 8: dedicated server config sync)
        BrightbronzeNetworking.init();

        // Phase 10/11: central server-side expansion manager (queue + bounded work)
        ChunkExpansionManager.init();
        
        // Register server lifecycle events
        registerServerEvents();

        LOGGER.info("Brightbronze Horizons initialized!");
    }
    
    /**
     * Registers server lifecycle event handlers.
     */
    private static void registerServerEvents() {
        // Initialize the starting area when the server is fully started
        // This ensures all dimensions and world data are available
        LifecycleEvent.SERVER_STARTED.register(server -> {
            BrightbronzeConfig.loadOrCreate(server);
            LOGGER.info("Server started, checking starting area initialization...");
            StartingAreaManager.checkAndInitialize(server);
        });
    }
}
