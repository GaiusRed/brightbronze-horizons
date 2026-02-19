package red.gaius.brightbronze.mixin.mc12110;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.BrightbronzeWorldMarker;
import red.gaius.brightbronze.versioned.Versioned;

/**
 * MC 1.21.10 version of CreateWorldScreenMixin.
 * 
 * <p>Modifies the default world preset to Brightbronze when creating a new world.
 */
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    /**
     * Gets the resource key for our custom world preset.
     * Uses the version abstraction for ResourceLocation creation.
     */
    private static ResourceKey<WorldPreset> getBrightbronzePreset() {
        return ResourceKey.create(
            Registries.WORLD_PRESET,
            Versioned.mc().createResourceLocation(BrightbronzeHorizons.MOD_ID, "brightbronze")
        );
    }

    /**
     * Modifies the default preset passed to openCreateWorldScreen to use Brightbronze instead of NORMAL.
     */
    @ModifyArg(
        method = "openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openCreateWorldScreen(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Ljava/util/function/Function;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContextMapper;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V"
        ),
        index = 4
    )
    private static ResourceKey<WorldPreset> brightbronze$useCustomDefaultPreset(ResourceKey<WorldPreset> original) {
        BrightbronzeHorizons.LOGGER.debug("Setting default world preset to Brightbronze");
        // Mark that we're creating a Brightbronze world
        BrightbronzeWorldMarker.markWorldCreation();
        return getBrightbronzePreset();
    }
}
