package com.joplayx.util;

import com.joplayx.config.RerollerConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

/**
 * Trade reading and evaluation utilities.
 *
 * Reads from OffersStore which is populated by MerchantOffersPacketMixin
 * when the server sends ClientboundMerchantOffersPacket.
 */
public class TradeUtil {

    public record TradeResult(boolean found, String description) {}

    public static TradeResult checkTrades(RerollerConfig.Config cfg) {
        MerchantOffers offers = OffersStore.get();

        if (offers == null || offers.isEmpty()) {
            return new TradeResult(false, "No trades available");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return new TradeResult(false, "No level");

        var reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> targets = cfg.targetList();

        String bestSeen = "Nothing";

        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) continue;

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet()) {
                Holder<Enchantment> enchHolder = entry.getKey();
                int level = entry.getIntValue();

                String enchantmentId = reg.getKey(enchHolder.value()).toString();

                // getCostA() (not getBaseCostA()) - this is the ADJUSTED price after
                // demand and any Hero of the Village discount are applied, which is
                // what actually gets charged in-game. getBaseCostA() ignores discounts
                // entirely, which is what the old single-loader version used.
                int cost = offer.getCostA().getCount();
                int baseCost = offer.getBaseCostA().getCount();
                boolean discounted = cost < baseCost;

                String desc = enchantmentId + " " + toRoman(level) + " for " + cost + " emeralds"
                        + (discounted ? " (marked down from " + baseCost + ")" : "");
                bestSeen = desc;

                if (targets.contains(enchantmentId)
                        && level >= cfg.minLevel
                        && cost <= cfg.maxEmeraldCost) {
                    return new TradeResult(true, desc);
                }
            }
        }

        return new TradeResult(false, bestSeen);
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
