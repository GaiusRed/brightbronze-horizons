package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import red.gaius.brightbronze.versioned.SpawnHelper;
import org.jetbrains.annotations.Nullable;

/**
 * MC 1.21.1 implementation of SpawnHelper.
 * Uses ServerLevelData spawn methods directly.
 */
public class SpawnHelperImpl implements SpawnHelper {
    
    @Override
    @Nullable
    public BlockPos getSpawnPosition(ServerLevel level) {
        var levelData = level.getLevelData();
        if (levelData instanceof ServerLevelData serverLevelData) {
            // In 1.21.1, ServerLevelData doesn't have separate getXSpawn etc.
            // Use the level's spawn position directly
            return level.getSharedSpawnPos();
        }
        return null;
    }
    
    @Override
    public void setSpawnPosition(ServerLevel level, BlockPos pos, float angle) {
        // In 1.21.1, ServerLevelData has setSpawn(BlockPos, float)
        var levelData = level.getLevelData();
        if (levelData instanceof ServerLevelData serverLevelData) {
            serverLevelData.setSpawn(pos, angle);
        }
    }
}
