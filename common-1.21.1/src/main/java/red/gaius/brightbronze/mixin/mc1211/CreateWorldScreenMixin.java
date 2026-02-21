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

import java.util.Optional;

/**
 * MC 1.21.1 version of CreateWorldScreenMixin.
 * 
 * <p>In 1.21.1, openFresh(Minecraft, Screen) creates a new CreateWorldScreen directly
 * and passes Optional.of(WorldPresets.NORMAL) to the constructor. We intercept the
 * Optional.of() call to replace NORMAL with our preset.
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
     * Modifies the preset passed to Optional.of() in openFresh to use Brightbronze instead of NORMAL.
     * 
     * In MC 1.21.1, openFresh calls:
     *   new CreateWorldScreen(minecraft, screen, context, Optional.of(WorldPresets.NORMAL), OptionalLong.empty())
     * 
     * We intercept the Optional.of() call to replace NORMAL with our preset.
     */
    @ModifyArg(
        method = "openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;of(Ljava/lang/Object;)Ljava/util/Optional;"
        ),
        index = 0
    )
    private static Object brightbronze$useCustomDefaultPreset(Object original) {
        if (original instanceof ResourceKey<?> key) {
            BrightbronzeHorizons.LOGGER.info("Setting default world preset to Brightbronze (1.21.1)");
            BrightbronzeWorldMarker.markWorldCreation();
            return getBrightbronzePreset();
        }
        return original;
    }
}
