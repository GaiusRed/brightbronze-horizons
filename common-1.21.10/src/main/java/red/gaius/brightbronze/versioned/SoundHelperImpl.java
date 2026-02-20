package red.gaius.brightbronze.versioned;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Minecraft 1.21.10 implementation of SoundHelper.
 * Uses Entity as the first parameter (null for position-based sounds).
 */
public class SoundHelperImpl implements SoundHelper {

    @Override
    public void playSound(ServerLevel level, double x, double y, double z, 
                         SoundEvent sound, SoundSource source, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, source, volume, pitch);
    }
}
