package red.gaius.brightbronze.versioned;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Provides version-specific sound playing helpers.
 * 
 * <p>This interface abstracts the playSound method which has different
 * signatures between Minecraft versions (Entity in 1.21.10 vs Player in 1.21.1).
 */
public interface SoundHelper {
    
    /**
     * Plays a sound at the given position.
     * 
     * @param level The server level
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param sound The sound event to play
     * @param source The sound source category
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch modifier
     */
    void playSound(ServerLevel level, double x, double y, double z, 
                   SoundEvent sound, SoundSource source, float volume, float pitch);
}
