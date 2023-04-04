package gg.moonflower.vanity.core.registry;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.RegistrySupplier;
import gg.moonflower.pollen.api.event.entity.v1.ModifyTradesEvents;
import gg.moonflower.pollen.api.registry.wrapper.v1.PollinatedVillagerRegistry;
import gg.moonflower.vanity.api.concept.ConceptArt;
import gg.moonflower.vanity.common.concept.ServerConceptArtManager;
import gg.moonflower.vanity.common.item.ConceptArtItem;
import gg.moonflower.vanity.core.Vanity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VanityProfessions {

    public static final PollinatedVillagerRegistry REGISTRY = PollinatedVillagerRegistry.create(Vanity.MOD_ID);

    public static final RegistrySupplier<PoiType> STYLIST_POI = REGISTRY.registerPoiType("stylist", () -> new PoiType(ImmutableSet.<BlockState>builder().addAll(VanityBlocks.STYLING_TABLE.get().getStateDefinition().getPossibleStates()).build(), 1, 1));
    public static final RegistrySupplier<VillagerProfession> STYLIST = REGISTRY.register("stylist", () -> new VillagerProfession(Vanity.MOD_ID + ":stylist", poi -> poi.is(STYLIST_POI.getId()), poi -> poi.is(STYLIST_POI.getId()), ImmutableSet.of(), ImmutableSet.of(), VanitySounds.UI_STYLING_TABLE_TAKE_RESULT.get()));

    public static void registerTrades() {
        ModifyTradesEvents.VILLAGER.register(context -> {
            if (context.getProfession() != STYLIST.get())
                return;

            List<ResourceLocation> availableArt = ServerConceptArtManager.INSTANCE.getAllConceptArt()
                    .filter(ConceptArt::sold)
                    .flatMap(art -> ServerConceptArtManager.INSTANCE.getConceptArtId(art)
                            .stream())
                    .toList();
            if (availableArt.isEmpty())
                return;

            int basicTrades = Math.min(context.getMaxTier() - context.getMinTier() - 1, availableArt.size());
            for (int i = 0; i < basicTrades; i++) {
                ModifyTradesEvents.TradeRegistry trades = context.getTrades(context.getMinTier() + i);
                trades.add(new ConceptArtTrade(availableArt, i));
            }

            for (int i = 0; i < Math.min(2, (availableArt.size() - basicTrades) / 2); i++) {
                ModifyTradesEvents.TradeRegistry trades = context.getTrades(context.getMaxTier() - 1 + i);
                trades.add(new ConceptArtTrade(availableArt, basicTrades + i * 2));
                trades.add(new ConceptArtTrade(availableArt, basicTrades + i * 2 + 1));
            }
        });
    }

    static class ConceptArtTrade implements VillagerTrades.ItemListing {

        private static final int USES = 4;
        private static final int EMERALD_COST = 15;
        private static final int XP_GAIN = 24;
        private static final float PRICE_MULTIPLIER = 0.05F;
        private static final Random RANDOM = new Random(42L);

        private final List<ResourceLocation> availableArt;
        private final int index;

        private ConceptArtTrade(List<ResourceLocation> availableArt, int index) {
            this.availableArt = availableArt;
            this.index = index;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            ItemStack emeralds = new ItemStack(Items.EMERALD, EMERALD_COST + (random.nextInt(16)));

            RANDOM.setSeed(entity.getUUID().getMostSignificantBits());

            List<ResourceLocation> conceptArt = new ArrayList<>(this.availableArt);
            Collections.shuffle(conceptArt, RANDOM);

            ItemStack item = new ItemStack(VanityItems.CONCEPT_ART.get());
            ConceptArtItem.setConceptArt(item, conceptArt.get(this.index));

            return new MerchantOffer(emeralds, item, USES, XP_GAIN, PRICE_MULTIPLIER);
        }
    }
}
