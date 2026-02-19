package red.gaius.brightbronze.mixin.mc1211;

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
 * MC 1.21.1 version of CreateWorldScreenMixin.
 * 
 * <p>In 1.21.1, the method signature is different - it's called "create" instead of "openFresh",
 * and has a different parameter list.
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
     * Modifies the default preset passed to create() to use Brightbronze instead of NORMAL.
     * 
     * In MC 1.21.1, the create method has signature:
     * create(Minecraft minecraft, Screen lastScreen)
     * which then calls openCreateWorldScreen internally.
     */
    @ModifyArg(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;create(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openCreateWorldScreen(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Ljava/util/function/Function;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContextMapper;Lnet/minecraft/resources/ResourceKey;)V"
        ),
        index = 4
    )
    private static ResourceKey<WorldPreset> brightbronze$useCustomDefaultPreset(ResourceKey<WorldPreset> original) {
        BrightbronzeHorizons.LOGGER.debug("Setting default world preset to Brightbronze (1.21.1)");
        BrightbronzeWorldMarker.markWorldCreation();
        return getBrightbronzePreset();
    }
}
