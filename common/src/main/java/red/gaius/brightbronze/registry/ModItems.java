package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for all mod items.
 * 
 * In MC 1.21.10+, items require their ID to be set on the properties BEFORE construction.
 * This is done via Item.Properties.setId(ResourceKey).
 * 
 * Tools are created via Item.Properties methods like sword(), pickaxe(), etc.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.ITEM);

    // ===== Helper method to create ResourceKey =====
    
    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, 
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, name));
    }

    // ===== Brightbronze Materials =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AMALGAM = ITEMS.register(
            "brightbronze_amalgam",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_amalgam"))
                    .arch$tab(CreativeModeTabs.INGREDIENTS)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_INGOT = ITEMS.register(
            "brightbronze_ingot",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_ingot"))
                    .arch$tab(CreativeModeTabs.INGREDIENTS)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_NUGGET = ITEMS.register(
            "brightbronze_nugget",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_nugget"))
                    .arch$tab(CreativeModeTabs.INGREDIENTS)));

    // ===== Brightbronze Block Item =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_BLOCK_ITEM = ITEMS.register(
            "brightbronze_block",
            () -> new BlockItem(ModBlocks.BRIGHTBRONZE_BLOCK.get(), 
                    new Item.Properties()
                            .setId(key("brightbronze_block"))
                            .arch$tab(CreativeModeTabs.BUILDING_BLOCKS)));

    // ===== Brightbronze Tool Material (1.21.10 API) =====

    /**
     * Brightbronze tool material - positioned between Iron and Diamond.
     * Durability: 350 (Iron: 250, Diamond: 1561)
     * Speed: 6.5f (Iron: 6.0f, Diamond: 8.0f)
     * Attack Damage Bonus: 2.5f (Iron: 2.0f, Diamond: 3.0f)
     * Enchantability: 18 (Iron: 14, Diamond: 10)
     */
    public static final ToolMaterial BRIGHTBRONZE_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,  // What this tier cannot mine
            350,                                 // Durability
            6.5f,                               // Mining speed
            2.5f,                               // Attack damage bonus
            18,                                 // Enchantability
            ItemTags.IRON_TOOL_MATERIALS        // Repair items tag
    );

    // ===== Brightbronze Tools (1.21.10+ API using Item.Properties) =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_SWORD = ITEMS.register(
            "brightbronze_sword",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_sword"))
                    .arch$tab(CreativeModeTabs.COMBAT)
                    .sword(BRIGHTBRONZE_TOOL_MATERIAL, 3.0f, -2.4f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_PICKAXE = ITEMS.register(
            "brightbronze_pickaxe",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_pickaxe"))
                    .arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .pickaxe(BRIGHTBRONZE_TOOL_MATERIAL, 1.0f, -2.8f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AXE = ITEMS.register(
            "brightbronze_axe",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_axe"))
                    .arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .axe(BRIGHTBRONZE_TOOL_MATERIAL, 6.0f, -3.1f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_SHOVEL = ITEMS.register(
            "brightbronze_shovel",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_shovel"))
                    .arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .shovel(BRIGHTBRONZE_TOOL_MATERIAL, 1.5f, -3.0f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_HOE = ITEMS.register(
            "brightbronze_hoe",
            () -> new Item(new Item.Properties()
                    .setId(key("brightbronze_hoe"))
                    .arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .hoe(BRIGHTBRONZE_TOOL_MATERIAL, -2.0f, -1.0f)));

    // ===== Brightbronze Armor (defined in ModArmorMaterials) =====

    // ===== Chunk Spawners (to be added in Phase 3) =====

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod items");
    }
}
