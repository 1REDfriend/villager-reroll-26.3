package com.joplayx.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import com.joplayx.client.VillagerRerollClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class RerollerConfigScreen {

    public static Screen create(Screen parent) {
        RerollerConfig.Config cfg = RerollerConfig.get();
        Minecraft mc = Minecraft.getInstance();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Villager Trade Reroller"))
                .save(RerollerConfig::save)

                // -------------------------------------------------------
                // Category 1: Lectern Setup
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Lectern Setup"))
                        .tooltip(Component.literal(
                                "Set the block position where the mod will place and break the lectern.\n\n" +
                                "Stand on the block, then click 'Use My Position'.\n" +
                                "Or type the coordinates manually below."
                        ))

                        // "Use My Position" button — sets X/Y/Z to player's feet position
                        .option(ButtonOption.createBuilder()
                                .name(Component.literal("Use My Position"))
                                .description(OptionDescription.of(Component.literal(
                                "Sets the lectern position to the block you are currently looking at."
                                + "\n\nAim your crosshair at the block where you want"
                                + "\nthe lectern placed, then click this button."
                                  + "\n\nOr press " + VillagerRerollClient.setPositionKey.getTranslatedKeyMessage().getString() + " in-game while looking at the block."
				)))
                                .text(Component.literal(
                                        mc.player != null
                                        ? "Set to current position (" +
                                          (int) mc.player.getX() + ", " +
                                          (int) mc.player.getY() + ", " +
                                          (int) mc.player.getZ() + ")"
                                        : "Set to current position"
                                ))
                                .action((screen, opt) -> {
                                    if (mc.player == null) return;
                                    String message = VillagerRerollClient.setLecternFromCrosshair(mc);
                                    if (cfg.hasLecternPos()) mc.gui.setScreen(create(parent));
                                    mc.player.sendSystemMessage(Component.literal(message));
                                })
                                .build()
                        )

                        // "Clear Position" button
                        .option(ButtonOption.createBuilder()
                                .name(Component.literal("Clear Position"))
                                .description(OptionDescription.of(Component.literal(
                                        "Clears the saved lectern position.\n" +
                                        "The reroller will not start until a new position is set."
                                )))
                                .text(Component.literal(
                                        cfg.hasLecternPos()
                                        ? "Currently: " + cfg.lecternPosString()
                                        : "Not set"
                                ))
                                .action((screen, opt) -> {
                                    cfg.clearLecternPos();
                                    RerollerConfig.save();
                                    mc.gui.setScreen(create(parent));
                                })
                                .build()
                        )

                        // Manual X input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Lectern X"))
                                .description(OptionDescription.of(Component.literal(
                                        "X coordinate of the lectern position.\n" +
                                        "Press F3 in-game to see your coordinates."
                                )))
                                .binding(
                                        0,
                                        () -> cfg.lecternX == Integer.MIN_VALUE ? 0 : cfg.lecternX,
                                        val -> cfg.lecternX = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        // Manual Y input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Lectern Y"))
                                .description(OptionDescription.of(Component.literal(
                                        "Y coordinate of the lectern position.\n" +
                                        "Press F3 in-game to see your coordinates."
                                )))
                                .binding(
                                        64,
                                        () -> cfg.lecternY == Integer.MIN_VALUE ? 64 : cfg.lecternY,
                                        val -> cfg.lecternY = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        // Manual Z input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Lectern Z"))
                                .description(OptionDescription.of(Component.literal(
                                        "Z coordinate of the lectern position.\n" +
                                        "Press F3 in-game to see your coordinates."
                                )))
                                .binding(
                                        0,
                                        () -> cfg.lecternZ == Integer.MIN_VALUE ? 0 : cfg.lecternZ,
                                        val -> cfg.lecternZ = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        .build()
                )

                // -------------------------------------------------------
                // Category 2: Timing
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Timing"))
                        .tooltip(Component.literal(
                                "Adjust delays to make the reroller act human-like.\n" +
                                "Increase these if your server desyncs or kicks you."
                        ))

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Close Screen Delay (ticks)"))
                                .description(OptionDescription.of(Component.literal(
                                        "How long to wait after closing the trade screen\n" +
                                        "before breaking the lectern.\n\n" +
                                        "20 ticks = 1 second. Default: 20.\n" +
                                        "Increase this if the server seems to desync."
                                )))
                                .binding(20, () -> cfg.closeDelayTicks, val -> cfg.closeDelayTicks = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 100).step(5))
                                .build()
                        )

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Retry Delay (ticks)"))
                                .description(OptionDescription.of(Component.literal(
                                        "How long to wait after breaking the lectern\n" +
                                        "before placing a new one.\n\n" +
                                        "20 ticks = 1 second. Default: 40.\n" +
                                        "Increase this on laggier servers."
                                )))
                                .binding(40, () -> cfg.retryDelayTicks, val -> cfg.retryDelayTicks = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 200).step(10))
                                .build()
                        )

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Walk To Pick Up Lectern"))
                                .description(OptionDescription.of(Component.literal(
                                        "If the broken lectern lands out of pickup range, walk over\n" +
                                        "(sneaking, so you can't fall off edges), pick it up, then\n" +
                                        "walk back to where you were standing.\n\n" +
                                        "Default: on. Command: /reroll pickup true|false"
                                )))
                                .binding(true, () -> cfg.autoPickup, val -> cfg.autoPickup = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Villager Search Radius"))
                                .description(OptionDescription.of(Component.literal(
                                        "How far to look for the nearest villager when none is selected.\n\n" +
                                        "Default: 6 blocks. Command: /reroll radius <1-16>"
                                )))
                                .binding(6, () -> cfg.searchRadius, val -> cfg.searchRadius = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 16).step(1))
                                .build()
                        )

                        .build()
                )

                // -------------------------------------------------------
                // Category 3: Display
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Display"))
                        .tooltip(Component.literal("HUD and overlay settings."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Show HUD Overlay"))
                                .description(OptionDescription.of(Component.literal(
                                        "Show the status overlay while the reroller is running.\n\n" +
                                        "Displays: status, target, attempt count, last trade seen.\n" +
                                        "Default: on. Command: /reroll hud on|off"
                                )))
                                .binding(true, () -> cfg.hudEnabled, val -> cfg.hudEnabled = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("HUD Scale"))
                                .description(OptionDescription.of(Component.literal(
                                        "Size of the HUD overlay.\n\n" +
                                        "Default: 1.0. Command: /reroll hud scale <0.5-2.0>"
                                )))
                                .binding(1.0f, () -> cfg.hudScale, val -> cfg.hudScale = val)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(RerollerConfig.MIN_HUD_SCALE, RerollerConfig.MAX_HUD_SCALE)
                                        .step(0.05f))
                                .build()
                        )

                        .option(Option.<RerollerConfig.HudCorner>createBuilder()
                                .name(Component.literal("HUD Position"))
                                .description(OptionDescription.of(Component.literal(
                                        "Which edge/corner of the screen the HUD sits in.\n\n" +
                                        "Default: MIDDLE_LEFT. Command: /reroll hud pos <corner>"
                                )))
                                .binding(RerollerConfig.HudCorner.MIDDLE_LEFT, () -> cfg.hudCorner, val -> cfg.hudCorner = val)
                                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(RerollerConfig.HudCorner.class))
                                .build()
                        )

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Compact HUD"))
                                .description(OptionDescription.of(Component.literal(
                                        "Show only a single status line instead of the full panel.\n\n" +
                                        "Default: off. Command: /reroll hud compact true|false"
                                )))
                                .binding(false, () -> cfg.hudCompact, val -> cfg.hudCompact = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Outline Selected Villager"))
                                .description(OptionDescription.of(Component.literal(
                                        "Draw a glowing outline around the villager you selected.\n" +
                                        "Only visible to you.\n\n" +
                                        "Default: on. Command: /reroll highlight true|false"
                                )))
                                .binding(true, () -> cfg.highlightVillager, val -> cfg.highlightVillager = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .build()
                )

                .build()
                .generateScreen(parent);
    }
}
