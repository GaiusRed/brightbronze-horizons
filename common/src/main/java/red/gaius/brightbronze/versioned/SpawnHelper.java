package red.gaius.brightbronze.versioned;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Provides version-specific spawn position access.
 * 
 * <p>In 1.21.10: Uses LevelData.getRespawnData() → RespawnData.pos()
 * <p>In 1.21.1: Uses ServerLevelData.getXSpawn()/getYSpawn()/getZSpawn()
 */
public interface SpawnHelper {
    
    /**
     * Gets the spawn position for the given level.
     * 
     * @param level The server level to get spawn position from
     * @return The spawn position, or null if not available
     */
    @Nullable
    BlockPos getSpawnPosition(ServerLevel level);
    
    /**
     * Sets the spawn position for the given level.
     * 
     * @param level The server level to set spawn position for
     * @param pos The new spawn position
     * @param angle The spawn angle
     */
    void setSpawnPosition(ServerLevel level, BlockPos pos, float angle);
}
