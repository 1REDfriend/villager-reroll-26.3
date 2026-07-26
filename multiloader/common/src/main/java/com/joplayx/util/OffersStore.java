package com.joplayx.util;

import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Stores the most recently received MerchantOffers from the server packet.
 *
 * The working meteor mod (VillagerRoller.java line 695-696) intercepts
 * ClientboundMerchantOffersPacket directly rather than reading from MerchantMenu,
 * because the MerchantMenu field name changed/is inaccessible in 26.1.2.
 *
 * We register a packet listener in the client entrypoint and store offers here.
 * TradeUtil reads from this store instead of using a mixin.
 */
public class OffersStore {

    private static MerchantOffers latestOffers = null;

    public static void set(MerchantOffers offers) {
        latestOffers = offers;
    }

    public static MerchantOffers get() {
        return latestOffers;
    }

    public static void clear() {
        latestOffers = null;
    }
}
