package red.gaius.brightbronze.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for mod creative tabs.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> BRIGHTBRONZE_TAB = CREATIVE_TABS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_horizons"),
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + BrightbronzeHorizons.MOD_ID + ".brightbronze_horizons"),
                    () -> new ItemStack(ModItems.BRIGHTBRONZE_INGOT.get())));

    private ModCreativeTabs() {
    }

    public static void register() {
        CREATIVE_TABS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod creative tabs");
    }

    /**
     * Populates the creative tab with all mod items.
     * Called after items are registered.
     */
    public static void populateTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        // Brightbronze Materials
        output.accept(ModItems.BRIGHTBRONZE_AMALGAM.get());
        output.accept(ModItems.BRIGHTBRONZE_INGOT.get());
        output.accept(ModItems.BRIGHTBRONZE_NUGGET.get());
        output.accept(ModItems.BRIGHTBRONZE_BLOCK_ITEM.get());

        // Brightbronze Tools
        output.accept(ModItems.BRIGHTBRONZE_SWORD.get());
        output.accept(ModItems.BRIGHTBRONZE_PICKAXE.get());
        output.accept(ModItems.BRIGHTBRONZE_AXE.get());
        output.accept(ModItems.BRIGHTBRONZE_SHOVEL.get());
        output.accept(ModItems.BRIGHTBRONZE_HOE.get());

        // Brightbronze Armor (to be added after ModArmorMaterials)
        output.accept(ModArmorMaterials.BRIGHTBRONZE_HELMET.get());
        output.accept(ModArmorMaterials.BRIGHTBRONZE_CHESTPLATE.get());
        output.accept(ModArmorMaterials.BRIGHTBRONZE_LEGGINGS.get());
        output.accept(ModArmorMaterials.BRIGHTBRONZE_BOOTS.get());

        // Chunk Spawners
        output.accept(ModItems.COAL_CHUNK_SPAWNER_ITEM.get());
        output.accept(ModItems.COPPER_CHUNK_SPAWNER_ITEM.get());
        output.accept(ModItems.IRON_CHUNK_SPAWNER_ITEM.get());
        output.accept(ModItems.GOLD_CHUNK_SPAWNER_ITEM.get());
        output.accept(ModItems.EMERALD_CHUNK_SPAWNER_ITEM.get());
        output.accept(ModItems.DIAMOND_CHUNK_SPAWNER_ITEM.get());
    }
}
