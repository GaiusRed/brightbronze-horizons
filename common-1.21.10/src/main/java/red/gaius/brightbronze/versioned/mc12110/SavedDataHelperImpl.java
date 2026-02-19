package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import red.gaius.brightbronze.versioned.SavedDataHelper;
import red.gaius.brightbronze.world.PlayableAreaData;

/**
 * MC 1.21.10 implementation of SavedDataHelper.
 * Uses SavedDataType with Codec for serialization.
 */
public class SavedDataHelperImpl implements SavedDataHelper {
    
    private static final SavedDataType<PlayableAreaData> TYPE = new SavedDataType<>(
        PlayableAreaData.DATA_NAME_VALUE,
        PlayableAreaData::new,
        PlayableAreaData.CODEC,
        PlayableAreaData.DATA_FIX_TYPES
    );
    
    @Override
    public PlayableAreaData getPlayableAreaData(DimensionDataStorage storage) {
        return storage.computeIfAbsent(TYPE);
    }
}
