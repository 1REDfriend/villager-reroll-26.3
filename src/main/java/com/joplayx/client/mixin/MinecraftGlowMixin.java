package com.joplayx.client.mixin;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.RerollerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Outlines the selected villager. Purely client-side: this only changes how the
 * entity is drawn on this client, nothing is sent to the server.
 */
@Mixin(Minecraft.class)
public class MinecraftGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void villagerReroll$glowSelected(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RerollerConfig.get().highlightVillager
                && entity.getId() == VillagerRerollClient.CONTROLLER.getSelectedVillagerId()) {
            cir.setReturnValue(true);
        }
    }
}
