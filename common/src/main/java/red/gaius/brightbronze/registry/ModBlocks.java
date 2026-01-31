package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for all mod blocks.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.BLOCK);

    // ===== Brightbronze Material =====

    public static final RegistrySupplier<Block> BRIGHTBRONZE_BLOCK = BLOCKS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_block"),
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // ===== Chunk Spawners (to be added in Phase 3) =====

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod blocks");
    }
}
