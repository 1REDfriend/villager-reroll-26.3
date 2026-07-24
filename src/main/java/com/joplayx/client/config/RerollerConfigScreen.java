package com.joplayx.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
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
                // Category 1: Target — what to search for
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Target"))
                        .tooltip(Component.literal("Configure what enchanted book to search for."))

                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Target Enchantment"))
                                .description(OptionDescription.of(Component.literal(
                                        "The namespaced enchantment ID to search for.\n\n" +
                                        "Examples:\n" +
                                        "  minecraft:mending\n" +
                                        "  minecraft:protection\n" +
                                        "  minecraft:sharpness\n" +
                                        "  minecraft:efficiency\n\n" +
                                        "Always use the full minecraft: prefix."
                                )))
                                .binding("", () -> cfg.targetEnchantment, val -> cfg.targetEnchantment = val)
                                .controller(StringControllerBuilder::create)
                                .build()
                        )

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Minimum Level"))
                                .description(OptionDescription.of(Component.literal(
                                        "The minimum enchantment level to accept.\n\n" +
                                        "Set to 1 to accept any level.\n" +
                                        "Set to 4 to only accept level IV or higher."
                                )))
                                .binding(1, () -> cfg.minLevel, val -> cfg.minLevel = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 5).step(1))
                                .build()
                        )

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Max Emerald Cost"))
                                .description(OptionDescription.of(Component.literal(
                                        "The maximum emerald price you will accept.\n\n" +
                                        "Trades that cost more than this will be skipped\n" +
                                        "and the lectern will be rerolled."
                                )))
                                .binding(64, () -> cfg.maxEmeraldCost, val -> cfg.maxEmeraldCost = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 64).step(1))
                                .build()
                        )

                        .build()
                )

                // -------------------------------------------------------
                // Category 2: Lectern Setup
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
                                if (mc.player != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                                BlockPos pos = blockHit.getBlockPos();
                                cfg.setLecternPos(pos);
                                RerollerConfig.save();
                                mc.gui.setScreen(create(parent));
                                } else if (mc.player != null) {
                                 mc.player.sendSystemMessage(Component.literal(
                                    "[Reroller] Aim at a block first, then click this button."
							));
						}
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
                // Category 3: Timing
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

                        .build()
                )

                // -------------------------------------------------------
                // Category 4: Display
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Display"))
                        .tooltip(Component.literal("HUD and overlay settings."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Show HUD Overlay"))
                                .description(OptionDescription.of(Component.literal(
                                        "Show the status overlay in the top-left corner\n" +
                                        "while the reroller is running.\n\n" +
                                        "Displays: status, target, attempt count, last trade seen."
                                )))
                                .binding(true, () -> cfg.hudEnabled, val -> cfg.hudEnabled = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .build()
                )

                .build()
                .generateScreen(parent);
    }
}
