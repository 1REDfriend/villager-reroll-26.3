package com.joplayx.mixin;

import com.joplayx.util.OffersStore;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts merchant offers by injecting into the packet's handle() method.
 * Targets a pure vanilla class, so this mixin works identically under both
 * Fabric and NeoForge - only the per-loader mixin config JSON differs.
 */
@Mixin(ClientboundMerchantOffersPacket.class)
public class MerchantOffersPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"))
    private void onHandle(ClientGamePacketListener listener, CallbackInfo ci) {
        OffersStore.set(((ClientboundMerchantOffersPacket) (Object) this).getOffers());
    }
}
