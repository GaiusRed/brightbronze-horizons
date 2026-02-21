package red.gaius.brightbronze.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.world.BrightbronzeWorldMarker;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Mixin to enforce the VoidChunkGenerator on the overworld in Brightbronze worlds.
 * 
 * <p>This mixin intercepts world creation to replace the chunk generator if a worldgen
 * mod has overridden it.
 * 
 * <p>The problem: World presets define dimension configurations, but datapacks can
 * override dimensions directly via data/minecraft/dimension/overworld.json. Terralith
 * and similar worldgen mods do this, replacing our VoidChunkGenerator with NoiseBasedChunkGenerator.
 * 
 * <p>Our solution: Replace the chunk generator in the LEVEL_STEM registry before
 * levels are created, ensuring our VoidChunkGenerator is used regardless of what
 * datapacks have overridden.
 */
@Mixin(MinecraftServer.class)
public abstract class VoidWorldEnforcerMixin {

    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();

    /**
     * Called during server initialization to enforce the VoidChunkGenerator.
     */
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void brightbronze$beforeCreateLevels(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        
        // Mark this as a Brightbronze world if the flag was set during world creation
        BrightbronzeWorldMarker.ensureMarkerFile(server);
        
        // Check if this is a Brightbronze world
        boolean isBrightbronzeWorld = BrightbronzeWorldMarker.isCreatingBrightbronzeWorld() 
            || BrightbronzeWorldMarker.isBrightbronzeWorld(server);
        
        if (!isBrightbronzeWorld) {
            return;
        }
        
        try {
            RegistryAccess registries = registryAccess();
            Registry<LevelStem> dimensions = Versioned.registry().lookupRegistry(registries, Registries.LEVEL_STEM);
            
            // Check current overworld generator
            LevelStem overworldStem = dimensions.getOptional(LevelStem.OVERWORLD).orElse(null);
            if (overworldStem != null) {
                ChunkGenerator currentGenerator = overworldStem.generator();
                boolean isVoidGenerator = Versioned.worldGen().isVoidChunkGenerator(currentGenerator);
                
                // If this is a Brightbronze world and the generator was overridden, replace it
                if (!isVoidGenerator) {
                    BrightbronzeHorizons.LOGGER.info("Worldgen mod detected - enforcing VoidChunkGenerator for Brightbronze world");
                    
                    // Get the biome registry to create a new VoidChunkGenerator
                    Registry<Biome> biomeRegistry = Versioned.registry().lookupRegistry(registries, Registries.BIOME);
                    Holder<Biome> plainsBiome = Versioned.registry().getHolderOrThrow(biomeRegistry, Biomes.PLAINS);
                    
                    // Create a new VoidChunkGenerator with the plains biome holder
                    ChunkGenerator voidGenerator = Versioned.worldGen().createVoidChunkGenerator(plainsBiome);
                    
                    // Create a new LevelStem with our generator but the same dimension type
                    LevelStem newOverworldStem = new LevelStem(overworldStem.type(), voidGenerator);
                    
                    // Replace in the registry using reflection (the registry is frozen)
                    boolean replaced = replaceInRegistry(dimensions, LevelStem.OVERWORLD, newOverworldStem);
                    
                    if (replaced) {
                        BrightbronzeHorizons.LOGGER.info("Successfully enforced VoidChunkGenerator");
                    } else {
                        BrightbronzeHorizons.LOGGER.warn("Failed to enforce VoidChunkGenerator - void world may not work correctly");
                    }
                }
            }
            
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.error("Error enforcing VoidChunkGenerator: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Replaces a value in a frozen registry using reflection.
     */
    @SuppressWarnings("unchecked")
    private boolean replaceInRegistry(Registry<LevelStem> registry, ResourceKey<LevelStem> key, LevelStem newValue) {
        try {
            // Find the holder for the overworld using version-specific getHolderReference
            // This works on both 1.21.1 and 1.21.10 despite API differences
            Optional<Holder.Reference<LevelStem>> holderOpt = Versioned.registry().getHolderReference(registry, key);
            if (holderOpt.isEmpty()) {
                BrightbronzeHorizons.LOGGER.warn("No holder found for dimension: {}", key.location());
                return false;
            }
            
            Holder.Reference<LevelStem> refHolder = holderOpt.get();
            
            // Update the holder's value using reflection
            // The field is called "value" in Holder.Reference
            Field valueField = findField(refHolder.getClass(), "value");
            if (valueField != null) {
                valueField.setAccessible(true);
                valueField.set(refHolder, newValue);
                return true;
            }
            
            BrightbronzeHorizons.LOGGER.warn("Could not find value field in holder - registry may have changed");
            return false;
            
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.error("Reflection error during generator replacement: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Finds a field in the class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

}
