package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.SavedDataHelper;
import red.gaius.brightbronze.world.PlayableAreaData;

/**
 * MC 1.21.1 implementation of SavedDataHelper.
 * Uses SavedData.Factory with CompoundTag for serialization.
 */
public class SavedDataHelperImpl implements SavedDataHelper {
    
    private static final SavedData.Factory<PlayableAreaData> FACTORY = new SavedData.Factory<>(
        PlayableAreaData::new,
        (tag, provider) -> load(tag),
        PlayableAreaData.DATA_FIX_TYPES
    );
    
    private static PlayableAreaData load(CompoundTag tag) {
        return PlayableAreaData.CODEC.parse(NbtOps.INSTANCE, tag)
            .resultOrPartial(error -> BrightbronzeHorizons.LOGGER.error("Failed to load PlayableAreaData: {}", error))
            .orElseGet(PlayableAreaData::new);
    }
    
    @Override
    public PlayableAreaData getPlayableAreaData(DimensionDataStorage storage) {
        return storage.computeIfAbsent(FACTORY, PlayableAreaData.DATA_NAME_VALUE);
    }
}
