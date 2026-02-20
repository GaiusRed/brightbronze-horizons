package red.gaius.brightbronze.versioned;

import net.minecraft.world.level.Level;

/**
 * Provides version-specific level height helpers.
 * 
 * <p>This interface abstracts the getMinY/getMaxY methods which have different
 * names between Minecraft versions (getMinY/getMaxY in 1.21.10 vs 
 * getMinBuildHeight/getMaxBuildHeight in 1.21.1).
 */
public interface LevelHelper {
    
    /**
     * Gets the minimum Y coordinate (build height) for the level.
     * 
     * @param level The level
     * @return The minimum Y coordinate
     */
    int getMinY(Level level);
    
    /**
     * Gets the maximum Y coordinate (build height) for the level.
     * 
     * @param level The level
     * @return The maximum Y coordinate
     */
    int getMaxY(Level level);
}
