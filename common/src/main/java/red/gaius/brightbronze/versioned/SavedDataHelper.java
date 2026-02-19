package red.gaius.brightbronze.versioned;

import net.minecraft.world.level.storage.DimensionDataStorage;
import red.gaius.brightbronze.world.PlayableAreaData;

/**
 * Version-specific helper for SavedData operations.
 * In MC 1.21.10, SavedData uses SavedDataType with Codec.
 * In MC 1.21.1, SavedData uses Factory pattern with NBT.
 */
public interface SavedDataHelper {
    /**
     * Get or create PlayableAreaData from dimension data storage.
     *
     * @param storage The dimension data storage
     * @return The PlayableAreaData instance
     */
    PlayableAreaData getPlayableAreaData(DimensionDataStorage storage);
}
