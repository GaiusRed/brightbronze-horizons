package red.gaius.brightbronze.neoforge;

import net.neoforged.fml.common.Mod;

import red.gaius.brightbronze.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
