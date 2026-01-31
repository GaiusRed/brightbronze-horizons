package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for all mod items.
 * 
 * In MC 1.21.10+, tools are created via Item.Properties methods like
 * sword(), pickaxe(), axe(), shovel(), hoe() instead of specific tool item classes.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.ITEM);

    // ===== Brightbronze Materials =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AMALGAM = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_amalgam"),
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_INGOT = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_ingot"),
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_NUGGET = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_nugget"),
            () -> new Item(new Item.Properties()));

    // ===== Brightbronze Block Item =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_BLOCK_ITEM = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_block"),
            () -> new BlockItem(ModBlocks.BRIGHTBRONZE_BLOCK.get(), new Item.Properties()));

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
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_sword"),
            () -> new Item(new Item.Properties()
                    .sword(BRIGHTBRONZE_TOOL_MATERIAL, 3.0f, -2.4f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_PICKAXE = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_pickaxe"),
            () -> new Item(new Item.Properties()
                    .pickaxe(BRIGHTBRONZE_TOOL_MATERIAL, 1.0f, -2.8f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_AXE = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_axe"),
            () -> new Item(new Item.Properties()
                    .axe(BRIGHTBRONZE_TOOL_MATERIAL, 6.0f, -3.1f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_SHOVEL = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_shovel"),
            () -> new Item(new Item.Properties()
                    .shovel(BRIGHTBRONZE_TOOL_MATERIAL, 1.5f, -3.0f)));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_HOE = ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_hoe"),
            () -> new Item(new Item.Properties()
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
