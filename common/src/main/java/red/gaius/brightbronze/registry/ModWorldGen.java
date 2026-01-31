package red.gaius.brightbronze.registry;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.gen.SingleBiomeChunkGenerator;
import red.gaius.brightbronze.world.gen.VoidChunkGenerator;

/**
 * Registry for world generation components like chunk generators.
 * 
 * <p>This class handles registration of custom chunk generator codecs
 * required for source dimension creation and persistence.
 */
public final class ModWorldGen {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = 
            DeferredRegister.create(BrightbronzeHorizons.MOD_ID, Registries.CHUNK_GENERATOR);

    public static final RegistrySupplier<MapCodec<? extends ChunkGenerator>> SINGLE_BIOME_GENERATOR =
            CHUNK_GENERATORS.register("single_biome", () -> SingleBiomeChunkGenerator.CODEC);

    public static final RegistrySupplier<MapCodec<? extends ChunkGenerator>> VOID_GENERATOR =
            CHUNK_GENERATORS.register("void", () -> VoidChunkGenerator.CODEC);

    private ModWorldGen() {
    }

    /**
     * Registers all world generation components.
     * Must be called during mod initialization.
     */
    public static void register() {
        CHUNK_GENERATORS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered world generation components");
    }
}
