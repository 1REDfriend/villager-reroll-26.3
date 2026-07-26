package com.joplayx.fabric;

import com.joplayx.ModKeybinds;
import com.joplayx.VillagerReroll;
import com.joplayx.config.RerollerConfig;
import com.joplayx.hud.RerollerHud;
import com.joplayx.state.RerollController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;

public class VillagerRerollFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RerollerConfig.load();

        // Registers all three keybinds cross-platform via Architectury's
        // KeyMappingRegistry (see ModKeybinds.init() in common).
        ModKeybinds.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (ModKeybinds.START_STOP.consumeClick()) {
                RerollController.INSTANCE.toggleStartStop(client);
            }
            while (ModKeybinds.EMERGENCY_STOP.consumeClick()) {
                RerollController.INSTANCE.emergencyStop(client);
            }
            while (ModKeybinds.SET_POSITION.consumeClick()) {
                if (client.hitResult instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    RerollerConfig.get().setLecternPos(pos);
                    RerollerConfig.save();
                    client.player.sendSystemMessage(Component.literal(
                            "[Reroller] Lectern position set to " +
                            pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                            " (block you are looking at)"));
                } else {
                    client.player.sendSystemMessage(Component.literal(
                            "[Reroller] Look at a block first, then press " +
                            ModKeybinds.SET_POSITION.getTranslatedKeyMessage().getString() + "."));
                }
            }

            RerollController.INSTANCE.tick(client);
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(VillagerReroll.MOD_ID, "reroller_hud"),
                RerollerHud::extractRenderState
        );

        VillagerReroll.LOGGER.info("[VillagerReroll] Fabric client initialized.");
    }
}
