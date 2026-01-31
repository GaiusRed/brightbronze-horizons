package red.gaius.brightbronze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.gaius.brightbronze.registry.ModArmorMaterials;
import red.gaius.brightbronze.registry.ModBlocks;
import red.gaius.brightbronze.registry.ModCreativeTabs;
import red.gaius.brightbronze.registry.ModItems;

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
        ModCreativeTabs.register();

        LOGGER.info("Brightbronze Horizons initialized!");
    }
}
