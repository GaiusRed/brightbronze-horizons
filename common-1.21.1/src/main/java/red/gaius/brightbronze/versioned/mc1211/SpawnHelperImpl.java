package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import red.gaius.brightbronze.versioned.SpawnHelper;
import org.jetbrains.annotations.Nullable;

/**
 * MC 1.21.1 implementation of SpawnHelper.
 * Uses ServerLevelData.getXSpawn()/getYSpawn()/getZSpawn()
 */
public class SpawnHelperImpl implements SpawnHelper {
    
    @Override
    @Nullable
    public BlockPos getSpawnPosition(ServerLevel level) {
        var levelData = level.getLevelData();
        if (levelData instanceof ServerLevelData serverLevelData) {
            return new BlockPos(
                serverLevelData.getXSpawn(),
                serverLevelData.getYSpawn(),
                serverLevelData.getZSpawn()
            );
        }
        return null;
    }
    
    @Override
    public void setSpawnPosition(ServerLevel level, BlockPos pos, float angle) {
        level.setDefaultSpawnPos(pos, angle);
    }
}
