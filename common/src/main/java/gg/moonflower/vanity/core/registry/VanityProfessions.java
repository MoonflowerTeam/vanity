package gg.moonflower.vanity.core.registry;

import com.google.common.collect.ImmutableSet;
import gg.moonflower.pollen.api.event.events.entity.ModifyTradesEvents;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import gg.moonflower.vanity.api.concept.ConceptArt;
import gg.moonflower.vanity.common.concept.ServerConceptArtManager;
import gg.moonflower.vanity.common.item.ConceptArtItem;
import gg.moonflower.vanity.core.Vanity;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class VanityProfessions {

    public static final PollinatedRegistry<VillagerProfession> PROFESSIONS = PollinatedRegistry.create(Registry.VILLAGER_PROFESSION, Vanity.MOD_ID);
    public static final PollinatedRegistry<PoiType> POI_TYPES = PollinatedRegistry.create(Registry.POINT_OF_INTEREST_TYPE, Vanity.MOD_ID);

    public static final Supplier<PoiType> STYLIST_POI = POI_TYPES.register("stylist", () -> PoiType.registerBlockStates(new PoiType(Vanity.MOD_ID + ":stylist", ImmutableSet.<BlockState>builder().addAll(VanityBlocks.STYLING_TABLE.get().getStateDefinition().getPossibleStates()).build(), 1, 1)));
    public static final Supplier<VillagerProfession> STYLIST = PROFESSIONS.register("stylist", () -> new VillagerProfession(Vanity.MOD_ID + ":stylist", STYLIST_POI.get(), ImmutableSet.of(), ImmutableSet.of(), VanitySounds.UI_STYLING_TABLE_TAKE_RESULT.get()));

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
        public MerchantOffer getOffer(Entity entity, Random random) {
            ItemStack emeralds = new ItemStack(Items.EMERALD, EMERALD_COST + (random.nextInt(16)));

            RANDOM.setSeed(entity.getUUID().getMostSignificantBits());

            List<ResourceLocation> conceptArt = new ArrayList<>(this.availableArt);
            Collections.shuffle(conceptArt, random);

            ItemStack item = new ItemStack(VanityItems.CONCEPT_ART.get());
            ConceptArtItem.setConceptArt(item, conceptArt.get(this.index));

            return new MerchantOffer(emeralds, item, USES, XP_GAIN, PRICE_MULTIPLIER);
        }
    }
}
