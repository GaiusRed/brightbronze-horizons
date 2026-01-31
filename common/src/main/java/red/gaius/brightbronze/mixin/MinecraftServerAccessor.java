package red.gaius.brightbronze.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Mixin accessor for MinecraftServer to access private fields needed for dynamic dimension creation.
 * 
 * This allows us to:
 * - Access the levels map to register new dimensions
 * - Access the storageSource for world data
 * - Access the executor for async operations
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    
    /**
     * Gets the map of all loaded server levels.
     * We need this to register newly created dynamic dimensions.
     */
    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> getLevels();
    
    /**
     * Gets the level storage access for world data.
     * Required when creating a new ServerLevel.
     */
    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess getStorageSource();
    
    /**
     * Gets the executor used for async operations.
     * Required when creating a new ServerLevel.
     */
    @Accessor("executor")
    Executor getExecutor();
}
