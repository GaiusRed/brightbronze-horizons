package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import red.gaius.brightbronze.versioned.SpawnHelper;
import org.jetbrains.annotations.Nullable;

/**
 * MC 1.21.10 implementation of SpawnHelper.
 * Uses LevelData.getRespawnData() → RespawnData.pos()
 */
public class SpawnHelperImpl implements SpawnHelper {
    
    @Override
    @Nullable
    public BlockPos getSpawnPosition(ServerLevel level) {
        LevelData.RespawnData respawnData = level.getLevelData().getRespawnData();
        if (respawnData != null) {
            return respawnData.pos();
        }
        return null;
    }
    
    @Override
    public void setSpawnPosition(ServerLevel level, BlockPos pos, float angle) {
        // In 1.21.10, use RespawnData to set spawn
        LevelData.RespawnData newRespawn = LevelData.RespawnData.of(
            Level.OVERWORLD,
            pos,
            angle,
            0.0f  // pitch
        );
        ServerLevelData levelData = (ServerLevelData) level.getLevelData();
        levelData.setSpawn(newRespawn);
    }
}
