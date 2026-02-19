package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;

/**
 * Registry for all mod items.
 * 
 * <p>Uses the version abstraction layer to handle differences between MC versions.
 * Tool and armor items have their materials defined here but actual item creation
 * is delegated to the versioned factories.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.ITEM);

    // ===== Helper method to create ResourceKey =====
    
    private static ResourceKey<Item> key(String name) {
        return Versioned.items().key(BrightbronzeHorizons.MOD_ID, name);
    }

    // ===== Brightbronze Materials =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AMALGAM = ITEMS.register(
            "brightbronze_amalgam",
            () -> Versioned.items().simpleItem(key("brightbronze_amalgam"), CreativeModeTabs.INGREDIENTS));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_INGOT = ITEMS.register(
            "brightbronze_ingot",
            () -> Versioned.items().simpleItem(key("brightbronze_ingot"), CreativeModeTabs.INGREDIENTS));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_NUGGET = ITEMS.register(
            "brightbronze_nugget",
            () -> Versioned.items().simpleItem(key("brightbronze_nugget"), CreativeModeTabs.INGREDIENTS));

    // ===== Brightbronze Block Item =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_BLOCK_ITEM = ITEMS.register(
            "brightbronze_block",
            () -> Versioned.items().blockItem(
                    ModBlocks.BRIGHTBRONZE_BLOCK.get(),
                    key("brightbronze_block"),
                    CreativeModeTabs.BUILDING_BLOCKS));

    // ===== Brightbronze Tool Material =====
    
    /**
     * Tool material handle - the actual type is version-specific.
     * Lazily initialized to ensure Versioned is ready.
     */
    private static Object toolMaterial;
    
    /**
     * Returns the Brightbronze tool material.
     * Positioned between Iron and Diamond:
     * <ul>
     *   <li>Durability: 905 (Iron: 250, Diamond: 1561)</li>
     *   <li>Speed: 7.0f (Iron: 6.0f, Diamond: 8.0f)</li>
     *   <li>Attack Damage Bonus: 2.5f (Iron: 2.0f, Diamond: 3.0f)</li>
     *   <li>Enchantability: 18 (Iron: 14, Diamond: 10)</li>
     * </ul>
     */
    public static Object getToolMaterial() {
        if (toolMaterial == null) {
            toolMaterial = Versioned.tools().createToolMaterial(
                    BlockTags.INCORRECT_FOR_IRON_TOOL,  // What this tier cannot mine (same as iron)
                    905,                                 // Durability
                    7.0f,                               // Mining speed
                    2.5f,                               // Attack damage bonus
                    18,                                 // Enchantability
                    ModTags.Items.BRIGHTBRONZE_TOOL_MATERIALS  // Repair items tag
            );
        }
        return toolMaterial;
    }

    // ===== Brightbronze Tools =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_SWORD = ITEMS.register(
            "brightbronze_sword",
            () -> Versioned.tools().createSword(
                    key("brightbronze_sword"),
                    getToolMaterial(),
                    3.0f, -2.4f,
                    CreativeModeTabs.COMBAT));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_PICKAXE = ITEMS.register(
            "brightbronze_pickaxe",
            () -> Versioned.tools().createPickaxe(
                    key("brightbronze_pickaxe"),
                    getToolMaterial(),
                    1.0f, -2.8f,
                    CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AXE = ITEMS.register(
            "brightbronze_axe",
            () -> Versioned.tools().createAxe(
                    key("brightbronze_axe"),
                    getToolMaterial(),
                    6.0f, -3.1f,
                    CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_SHOVEL = ITEMS.register(
            "brightbronze_shovel",
            () -> Versioned.tools().createShovel(
                    key("brightbronze_shovel"),
                    getToolMaterial(),
                    1.5f, -3.0f,
                    CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_HOE = ITEMS.register(
            "brightbronze_hoe",
            () -> Versioned.tools().createHoe(
                    key("brightbronze_hoe"),
                    getToolMaterial(),
                    -2.0f, -1.0f,
                    CreativeModeTabs.TOOLS_AND_UTILITIES));

    // ===== Brightbronze Armor (defined in ModArmorMaterials) =====

    // ===== Chunk Spawner Block Items =====

    public static final RegistrySupplier<Item> COAL_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "coal_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.COAL_CHUNK_SPAWNER.get(),
                    key("coal_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    public static final RegistrySupplier<Item> COPPER_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "copper_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.COPPER_CHUNK_SPAWNER.get(),
                    key("copper_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    public static final RegistrySupplier<Item> IRON_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "iron_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.IRON_CHUNK_SPAWNER.get(),
                    key("iron_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    public static final RegistrySupplier<Item> GOLD_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "gold_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.GOLD_CHUNK_SPAWNER.get(),
                    key("gold_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    public static final RegistrySupplier<Item> EMERALD_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "emerald_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.EMERALD_CHUNK_SPAWNER.get(),
                    key("emerald_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    public static final RegistrySupplier<Item> DIAMOND_CHUNK_SPAWNER_ITEM = ITEMS.register(
            "diamond_chunk_spawner",
            () -> Versioned.items().blockItem(
                    ModBlocks.DIAMOND_CHUNK_SPAWNER.get(),
                    key("diamond_chunk_spawner"),
                    CreativeModeTabs.FUNCTIONAL_BLOCKS));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod items");
    }
}
