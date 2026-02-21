package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.world.level.Level;
import red.gaius.brightbronze.versioned.LevelHelper;

/**
 * MC 1.21.1 implementation of LevelHelper.
 * 
 * <p>In 1.21.1, Level uses getMinBuildHeight() and getMaxBuildHeight()
 * from the LevelHeightAccessor interface.
 */
public class LevelHelperImpl implements LevelHelper {
    
    @Override
    public int getMinY(Level level) {
        return level.getMinBuildHeight();
    }
    
    @Override
    public int getMaxY(Level level) {
        return level.getMaxBuildHeight();
    }
}
