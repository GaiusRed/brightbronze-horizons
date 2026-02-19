package red.gaius.brightbronze.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to track whether a world is a Brightbronze world.
 * 
 * <p>This is needed because worldgen mods can override our dimension settings via datapacks.
 * We need to detect if the world was created with the Brightbronze preset so we can:
 * <ul>
 *   <li>Warn the user if the void generator was overridden</li>
 *   <li>Potentially enforce the void generator in future versions</li>
 *   <li>Enable/disable mod features based on world type</li>
 * </ul>
 * 
 * <p>Detection strategy:
 * <ol>
 *   <li>Check for marker file (brightbronze_world.marker) in world folder</li>
 *   <li>Check if overworld uses VoidChunkGenerator</li>
 *   <li>Check PlayableAreaData for initialization state</li>
 * </ol>
 */
public class BrightbronzeWorldMarker {
    
    private static final String MARKER_FILE_NAME = "brightbronze_world.marker";
    
    // Thread-local tracking for world creation flow
    private static volatile boolean creatingBrightbronzeWorld = false;
    
    private BrightbronzeWorldMarker() {
        // Utility class
    }
    
    /**
     * Called by CreateWorldScreenMixin when a Brightbronze world is being created.
     * Sets a flag that will be used to write the marker file when the world is saved.
     */
    public static void markWorldCreation() {
        creatingBrightbronzeWorld = true;
        BrightbronzeHorizons.LOGGER.debug("World creation marked as Brightbronze");
    }
    
    /**
     * Returns whether we are currently creating a Brightbronze world.
     * This is used by other mixins to check if they should enforce void generation.
     * 
     * @return true if a Brightbronze world is being created
     */
    public static boolean isCreatingBrightbronzeWorld() {
        return creatingBrightbronzeWorld;
    }
    
    /**
     * Checks if a world is a Brightbronze world.
     * 
     * <p>Uses multiple detection strategies:
     * <ol>
     *   <li>Marker file in world folder</li>
     *   <li>VoidChunkGenerator on overworld (if accessible)</li>
     *   <li>PlayableAreaData indicating initialization</li>
     * </ol>
     * 
     * @param server The Minecraft server
     * @return true if this is a Brightbronze world
     */
    public static boolean isBrightbronzeWorld(MinecraftServer server) {
        // Strategy 1: Check for marker file
        Path worldPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path markerPath = worldPath.resolve(MARKER_FILE_NAME);
        
        if (Files.exists(markerPath)) {
            return true;
        }
        
        // Strategy 2: Check if we're in the middle of creating a Brightbronze world
        if (creatingBrightbronzeWorld) {
            // Write the marker file for future sessions
            writeMarkerFile(server);
            creatingBrightbronzeWorld = false;
            return true;
        }
        
        // Strategy 3: Check if PlayableAreaData exists and is initialized
        // This indicates a world that has been played with the mod
        try {
            PlayableAreaData data = PlayableAreaData.get(server);
            if (data != null && data.isInitialized()) {
                // Write marker file if missing
                writeMarkerFile(server);
                return true;
            }
        } catch (Exception e) {
            // PlayableAreaData not available yet, continue checking
        }
        
        // Strategy 4: Check if the overworld uses VoidChunkGenerator
        // This is the most reliable check for new worlds
        try {
            var overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                ChunkGenerator generator = overworld.getChunkSource().getGenerator();
                if (Versioned.worldGen().isVoidChunkGenerator(generator)) {
                    writeMarkerFile(server);
                    return true;
                }
            }
        } catch (Exception e) {
            // Overworld not available yet, continue checking
        }
        
        return false;
    }
    
    /**
     * Writes the marker file to the world folder.
     */
    private static void writeMarkerFile(MinecraftServer server) {
        try {
            Path worldPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            Path markerPath = worldPath.resolve(MARKER_FILE_NAME);
            
            if (!Files.exists(markerPath)) {
                Files.writeString(markerPath, "Brightbronze Horizons World Marker\nThis file indicates that this world uses the Brightbronze preset.\nDo not delete this file.\n");
                BrightbronzeHorizons.LOGGER.debug("Created Brightbronze world marker file");
            }
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed to write Brightbronze marker file: {}", e.getMessage());
        }
    }
    
    /**
     * Called when a world is created to ensure the marker file is written.
     */
    public static void ensureMarkerFile(MinecraftServer server) {
        if (isBrightbronzeWorld(server)) {
            writeMarkerFile(server);
        }
    }
}
