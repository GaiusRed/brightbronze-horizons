package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.world.level.Level;
import red.gaius.brightbronze.versioned.LevelHelper;

/**
 * MC 1.21.10 implementation of LevelHelper.
 * 
 * <p>In 1.21.10, Level has getMinY() and getMaxY() methods directly.
 */
public class LevelHelperImpl implements LevelHelper {
    
    @Override
    public int getMinY(Level level) {
        return level.getMinY();
    }
    
    @Override
    public int getMaxY(Level level) {
        return level.getMaxY();
    }
}
