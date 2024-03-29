package gg.moonflower.vanity.core;

import gg.moonflower.pollen.api.client.util.CreativeModeTabBuilder;
import gg.moonflower.pollen.api.datagen.provider.loot_table.PollinatedLootTableProvider;
import gg.moonflower.pollen.api.event.events.registry.client.RegisterAtlasSpriteEvent;
import gg.moonflower.pollen.api.platform.Platform;
import gg.moonflower.pollen.api.registry.client.ModelRegistry;
import gg.moonflower.pollen.api.registry.client.ScreenRegistry;
import gg.moonflower.pollen.api.util.PollinatedModContainer;
import gg.moonflower.vanity.client.concept.ClientConceptArtManager;
import gg.moonflower.vanity.client.screen.StylingScreen;
import gg.moonflower.vanity.common.concept.ServerConceptArtManager;
import gg.moonflower.vanity.common.item.ConceptArtItem;
import gg.moonflower.vanity.common.menu.StylingMenu;
import gg.moonflower.vanity.common.network.VanityMessages;
import gg.moonflower.vanity.core.datagen.VanityBlockLootGenerator;
import gg.moonflower.vanity.core.datagen.VanityLanguageGenerator;
import gg.moonflower.vanity.core.datagen.VanityRecipeGenerator;
import gg.moonflower.vanity.core.datagen.VanityTagGenerator;
import gg.moonflower.vanity.core.registry.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class Vanity {
    public static final String MOD_ID = "vanity";
    public static final Platform PLATFORM = Platform.builder(MOD_ID)
            .clientInit(() -> Vanity::onClientInit)
            .clientPostInit(() -> Vanity::onClientPostInit)
            .commonInit(Vanity::onCommonInit)
            .dataInit(Vanity::onDataInit)
            .build();

    public static final CreativeModeTab TAB = CreativeModeTabBuilder.builder(new ResourceLocation(Vanity.MOD_ID, "concept_art_tab"))
            .setIcon(() -> new ItemStack(VanityBlocks.STYLING_TABLE.get()))
            .setItems(list -> ClientConceptArtManager.INSTANCE.getAllConceptArtIds().forEach(location -> {
                ItemStack stack = new ItemStack(VanityItems.CONCEPT_ART.get());
                ConceptArtItem.setConceptArt(stack, location);
                list.add(stack);
            })).build();

    public static void onCommonInit() {
        VanityBlocks.BLOCKS.register(Vanity.PLATFORM);
        VanityBlocks.BLOCK_ENTITIES.register(Vanity.PLATFORM);

        VanityItems.REGISTRY.register(Vanity.PLATFORM);
        VanityMenuTypes.REGISTRY.register(Vanity.PLATFORM);
        VanitySounds.REGISTRY.register(Vanity.PLATFORM);

        VanityProfessions.PROFESSIONS.register(Vanity.PLATFORM);
        VanityProfessions.POI_TYPES.register(Vanity.PLATFORM);
        VanityProfessions.registerTrades();

        VanityMessages.init();
        ServerConceptArtManager.init();
    }

    public static void onClientInit() {
        ModelRegistry.registerFactory((resourceManager, out) -> {
            for (ResourceLocation location : resourceManager.listResources("models/item/vanity_concept_art", name -> name.endsWith(".json"))) {
                out.accept(new ModelResourceLocation(new ResourceLocation(location.getNamespace(), location.getPath().substring(12, location.getPath().length() - 5)), "inventory"));
            }
        });
    }

    public static void onClientPostInit(Platform.ModSetupContext ctx) {
        ctx.enqueueWork(() -> {
            RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> registry.accept(StylingMenu.EMPTY_CONCEPT_ART_SLOT));
            ScreenRegistry.register(VanityMenuTypes.STYLING_MENU.get(), StylingScreen::new);
        });
    }

    public static void onDataInit(Platform.DataSetupContext ctx) {
        DataGenerator generator = ctx.getGenerator();
        PollinatedModContainer container = ctx.getMod();

        generator.addProvider(new VanityLanguageGenerator(generator, container));
        generator.addProvider(new VanityTagGenerator(generator, container));
        generator.addProvider(new VanityRecipeGenerator(generator));

        PollinatedLootTableProvider lootProvider = new PollinatedLootTableProvider(generator);
        lootProvider.add(LootContextParamSets.BLOCK, new VanityBlockLootGenerator(container));

        generator.addProvider(lootProvider);
    }
}
