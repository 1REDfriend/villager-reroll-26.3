package com.joplayx.neoforge;

import com.joplayx.ModKeybinds;
import com.joplayx.VillagerReroll;
import com.joplayx.config.RerollerConfig;
import com.joplayx.config.RerollerConfigScreen;
import com.joplayx.hud.RerollerHud;
import com.joplayx.state.RerollController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entrypoint - NeoForge only instantiates this class on the
 * physical client because of dist = Dist.CLIENT below, mirroring the
 * "client" entrypoint the Fabric version had in fabric.mod.json.
 *
 * NOTE: RegisterGuiLayersEvent + LayeredDraw.Layer is the one API surface here
 * I couldn't fully verify against local reference source (the NeoForge repo's
 * patch-based layout made grepping for it impractical this session) - I confirmed
 * the event/method names (registerAboveAll) from NeoForge's own docs and the
 * 26.1 release notes confirming GuiGraphics -> GuiGraphicsExtractor, but if this
 * specific line fails to compile, that's the first place to check: RerollerHud's
 * extractRenderState(GuiGraphicsExtractor, DeltaTracker) signature needs to match
 * LayeredDraw.Layer's functional method exactly.
 */
@Mod(value = VillagerReroll.MOD_ID, dist = Dist.CLIENT)
public class VillagerRerollNeoForgeClient {

    private static final Identifier HUD_LAYER_ID =
            Identifier.fromNamespaceAndPath(VillagerReroll.MOD_ID, "reroller_hud");

    public VillagerRerollNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        RerollerConfig.load();
        ModKeybinds.init();

        modEventBus.addListener(this::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);

        // NeoForge has no Mod Menu equivalent to hook into - this registers our
        // YACL screen directly as this mod's own config-button target instead.
        // Verified against a real crash report from another mod (AbstractMethodError)
        // that createScreen's signature is (ModContainer, Screen) on current NeoForge,
        // not the older (Minecraft, Screen) some outdated examples online still show.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, modListScreen) -> RerollerConfigScreen.create(modListScreen));
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD_LAYER_ID, RerollerHud::extractRenderState);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        while (ModKeybinds.START_STOP.consumeClick()) {
            RerollController.INSTANCE.toggleStartStop(client);
        }
        while (ModKeybinds.EMERGENCY_STOP.consumeClick()) {
            RerollController.INSTANCE.emergencyStop(client);
        }
        while (ModKeybinds.SET_POSITION.consumeClick()) {
            if (client.hitResult instanceof BlockHitResult blockHit) {
                var pos = blockHit.getBlockPos();
                RerollerConfig.get().setLecternPos(pos);
                RerollerConfig.save();
                client.player.sendSystemMessage(Component.literal(
                        "[Reroller] Lectern position set to " +
                        pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            } else {
                client.player.sendSystemMessage(Component.literal(
                        "[Reroller] Look at a block first, then press " +
                        ModKeybinds.SET_POSITION.getTranslatedKeyMessage().getString() + "."));
            }
        }

        RerollController.INSTANCE.tick(client);
    }
}
