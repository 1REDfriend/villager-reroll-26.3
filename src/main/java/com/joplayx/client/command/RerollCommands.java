package com.joplayx.client.command;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.EnchantTargetListScreen;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.config.RerollerConfig.Config;
import com.joplayx.client.config.RerollerConfig.EnchantTarget;
import com.joplayx.client.config.RerollerConfig.HudCorner;
import com.joplayx.client.config.RerollerConfigScreen;
import com.joplayx.client.util.HotbarUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /reroll client commands. Registered through Fabric's client command API, so they
 * are handled entirely on this client and never sent to the server.
 */
public final class RerollCommands {

	private static final String PREFIX = "[Reroller] ";

	private RerollCommands() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
		dispatcher.register(literal("reroll")
				.executes(RerollCommands::help)
				.then(literal("help").executes(RerollCommands::help))

				.then(literal("start").executes(ctx -> {
					VillagerRerollClient.CONTROLLER.start(ctx.getSource().getClient());
					return 1;
				}))
				.then(literal("stop").executes(ctx -> {
					VillagerRerollClient.CONTROLLER.stopByPlayer(ctx.getSource().getClient());
					return 1;
				}))

				.then(literal("pos")
						.executes(ctx -> say(ctx, VillagerRerollClient.setLecternFromCrosshair(ctx.getSource().getClient())))
						.then(literal("clear").executes(ctx -> {
							RerollerConfig.get().clearLecternPos();
							RerollerConfig.save();
							return say(ctx, PREFIX + "Lectern position cleared.");
						}))
						.then(argument("x", IntegerArgumentType.integer())
								.then(argument("y", IntegerArgumentType.integer())
										.then(argument("z", IntegerArgumentType.integer())
												.executes(RerollCommands::setPos)))))

				.then(literal("villager")
						.executes(ctx -> say(ctx, VillagerRerollClient.selectVillagerFromCrosshair(ctx.getSource().getClient())))
						.then(literal("clear").executes(ctx -> {
							VillagerRerollClient.CONTROLLER.clearSelectedVillager();
							return say(ctx, PREFIX + "Villager selection cleared - nearest villager will be used.");
						})))

				.then(literal("add")
						.then(argument("enchant", IdentifierArgument.id())
								.suggests(ENCHANT_SUGGESTIONS)
								.executes(ctx -> addTarget(ctx, 1, 64))
								.then(argument("minLevel", IntegerArgumentType.integer(1, 5))
										.executes(ctx -> addTarget(ctx, IntegerArgumentType.getInteger(ctx, "minLevel"), 64))
										.then(argument("maxCost", IntegerArgumentType.integer(1, 64))
												.executes(ctx -> addTarget(ctx,
														IntegerArgumentType.getInteger(ctx, "minLevel"),
														IntegerArgumentType.getInteger(ctx, "maxCost")))))))
				.then(literal("remove")
						.then(argument("enchant", IdentifierArgument.id())
								.suggests(TARGET_SUGGESTIONS)
								.executes(RerollCommands::removeTarget)))
				.then(literal("list").executes(RerollCommands::listTargets))
				.then(literal("clear").executes(ctx -> {
					RerollerConfig.get().targets.clear();
					RerollerConfig.save();
					return say(ctx, PREFIX + "Target list cleared.");
				}))

				.then(literal("hud")
						.then(literal("on").executes(ctx -> setHud(ctx, true)))
						.then(literal("off").executes(ctx -> setHud(ctx, false)))
						.then(literal("compact")
								.then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
									RerollerConfig.get().hudCompact = BoolArgumentType.getBool(ctx, "enabled");
									RerollerConfig.save();
									return say(ctx, PREFIX + "HUD compact mode: " + onOff(RerollerConfig.get().hudCompact));
								})))
						.then(literal("scale")
								.then(argument("scale", FloatArgumentType.floatArg(
										RerollerConfig.MIN_HUD_SCALE, RerollerConfig.MAX_HUD_SCALE)).executes(ctx -> {
									RerollerConfig.get().hudScale = FloatArgumentType.getFloat(ctx, "scale");
									RerollerConfig.save();
									return say(ctx, PREFIX + "HUD scale: " + RerollerConfig.get().hudScale);
								})))
						.then(literal("pos")
								.then(argument("corner", com.mojang.brigadier.arguments.StringArgumentType.word())
										.suggests(CORNER_SUGGESTIONS)
										.executes(RerollCommands::setCorner))))

				.then(literal("delay")
						.then(literal("close")
								.then(argument("ticks", IntegerArgumentType.integer(5, 100)).executes(ctx -> {
									RerollerConfig.get().closeDelayTicks = IntegerArgumentType.getInteger(ctx, "ticks");
									RerollerConfig.save();
									return say(ctx, PREFIX + "Close delay: " + RerollerConfig.get().closeDelayTicks + " ticks");
								})))
						.then(literal("retry")
								.then(argument("ticks", IntegerArgumentType.integer(10, 200)).executes(ctx -> {
									RerollerConfig.get().retryDelayTicks = IntegerArgumentType.getInteger(ctx, "ticks");
									RerollerConfig.save();
									return say(ctx, PREFIX + "Retry delay: " + RerollerConfig.get().retryDelayTicks + " ticks");
								}))))

				.then(literal("radius")
						.then(argument("blocks", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
							RerollerConfig.get().searchRadius = IntegerArgumentType.getInteger(ctx, "blocks");
							RerollerConfig.save();
							return say(ctx, PREFIX + "Villager search radius: " + RerollerConfig.get().searchRadius + " blocks");
						})))

				.then(literal("highlight")
						.then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
							RerollerConfig.get().highlightVillager = BoolArgumentType.getBool(ctx, "enabled");
							RerollerConfig.save();
							return say(ctx, PREFIX + "Selected villager outline: " + onOff(RerollerConfig.get().highlightVillager));
						})))

				.then(literal("pickup")
						.then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
							RerollerConfig.get().autoPickup = BoolArgumentType.getBool(ctx, "enabled");
							RerollerConfig.save();
							return say(ctx, PREFIX + "Walk to pick up dropped lectern: " + onOff(RerollerConfig.get().autoPickup));
						})))

				.then(literal("settings").executes(RerollCommands::showSettings))

				.then(literal("config")
						.executes(ctx -> {
							VillagerRerollClient.openScreenNextTick(() -> new EnchantTargetListScreen(null));
							return 1;
						})
						.then(literal("more").executes(ctx -> {
							VillagerRerollClient.openScreenNextTick(() -> RerollerConfigScreen.create(null));
							return 1;
						})))

				.then(literal("debug").executes(RerollCommands::debug))
		);
	}

	// ---------------------------------------------------------------- suggestions

	private static final SuggestionProvider<FabricClientCommandSource> ENCHANT_SUGGESTIONS = (ctx, builder) -> {
		Minecraft mc = ctx.getSource().getClient();
		if (mc.level != null) {
			return SharedSuggestionProvider.suggestResource(
					mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
							.listElements().map(ref -> ref.key().identifier()),
					builder);
		}
		return SharedSuggestionProvider.suggestResource(
				EnchantTargetListScreen.FALLBACK_ENCHANTMENT_IDS.stream().map(Identifier::parse), builder);
	};

	private static final SuggestionProvider<FabricClientCommandSource> TARGET_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(
					RerollerConfig.get().targets.stream()
							.map(t -> Identifier.tryParse(t.enchantmentId))
							.filter(id -> id != null),
					builder);

	private static final SuggestionProvider<FabricClientCommandSource> CORNER_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(
					Arrays.stream(HudCorner.values()).map(c -> c.name().toLowerCase(Locale.ROOT)), builder);

	// ---------------------------------------------------------------- handlers

	private static int help(CommandContext<FabricClientCommandSource> ctx) {
		return say(ctx, String.join("\n",
				PREFIX + "Client-side commands (never sent to the server):",
				"/reroll start | stop",
				"/reroll pos [x y z | clear]  - lectern spot (no args = where you look)",
				"/reroll villager [clear]  - select the villager you look at",
				"/reroll add <enchant> [minLevel] [maxCost]",
				"/reroll remove <enchant> | list | clear",
				"/reroll hud on|off | scale <0.5-2> | pos <corner> | compact <true|false>",
				"/reroll delay close|retry <ticks> | radius <blocks> | highlight <true|false>",
				"/reroll pickup <true|false>  - walk to a dropped lectern and back",
				"/reroll settings | config [more] | debug"));
	}

	private static int setPos(CommandContext<FabricClientCommandSource> ctx) {
		Config cfg = RerollerConfig.get();
		cfg.lecternX = IntegerArgumentType.getInteger(ctx, "x");
		cfg.lecternY = IntegerArgumentType.getInteger(ctx, "y");
		cfg.lecternZ = IntegerArgumentType.getInteger(ctx, "z");
		RerollerConfig.save();
		return say(ctx, PREFIX + "Lectern position set to " + cfg.lecternPosString());
	}

	private static int addTarget(CommandContext<FabricClientCommandSource> ctx, int minLevel, int maxCost) {
		Identifier id = ctx.getArgument("enchant", Identifier.class);
		Minecraft mc = ctx.getSource().getClient();
		if (mc.level != null && mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.listElements().noneMatch(ref -> ref.key().identifier().equals(id))) {
			ctx.getSource().sendError(Component.literal(PREFIX + "Unknown enchantment: " + id));
			return 0;
		}
		String idString = id.toString();
		List<EnchantTarget> targets = RerollerConfig.get().targets;
		EnchantTarget existing = targets.stream()
				.filter(t -> t.enchantmentId.equals(idString))
				.findFirst().orElse(null);
		if (existing != null) {
			existing.minLevel = minLevel;
			existing.maxEmeraldCost = maxCost;
		} else {
			targets.add(new EnchantTarget(idString, minLevel, maxCost));
		}
		RerollerConfig.save();
		return say(ctx, PREFIX + (existing != null ? "Updated " : "Added ")
				+ idString + " (level " + minLevel + "+, max " + maxCost + " emeralds)");
	}

	private static int removeTarget(CommandContext<FabricClientCommandSource> ctx) {
		String id = ctx.getArgument("enchant", Identifier.class).toString();
		boolean removed = RerollerConfig.get().targets.removeIf(t -> t.enchantmentId.equals(id));
		if (!removed) {
			ctx.getSource().sendError(Component.literal(PREFIX + id + " is not in the target list."));
			return 0;
		}
		RerollerConfig.save();
		return say(ctx, PREFIX + "Removed " + id);
	}

	private static int listTargets(CommandContext<FabricClientCommandSource> ctx) {
		List<EnchantTarget> targets = RerollerConfig.get().targets;
		if (targets.isEmpty()) return say(ctx, PREFIX + "Target list is empty. Add one with /reroll add <enchant>.");
		StringBuilder sb = new StringBuilder(PREFIX + "Targets:");
		for (EnchantTarget t : targets) {
			sb.append("\n- ").append(t.enchantmentId)
					.append("  level ").append(t.minLevel).append("+")
					.append(", max ").append(t.maxEmeraldCost).append(" emeralds");
		}
		return say(ctx, sb.toString());
	}

	private static int setHud(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
		RerollerConfig.get().hudEnabled = enabled;
		RerollerConfig.save();
		return say(ctx, PREFIX + "HUD: " + onOff(enabled));
	}

	private static int setCorner(CommandContext<FabricClientCommandSource> ctx) {
		String raw = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "corner");
		HudCorner corner;
		try {
			corner = HudCorner.valueOf(raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			ctx.getSource().sendError(Component.literal(PREFIX + "Unknown corner: " + raw));
			return 0;
		}
		RerollerConfig.get().hudCorner = corner;
		RerollerConfig.save();
		return say(ctx, PREFIX + "HUD position: " + corner.name().toLowerCase(Locale.ROOT));
	}

	private static int showSettings(CommandContext<FabricClientCommandSource> ctx) {
		Config cfg = RerollerConfig.get();
		return say(ctx, String.join("\n",
				PREFIX + "Current settings (default in brackets):",
				"Lectern: " + cfg.lecternPosString() + "  [not set]",
				"Targets: " + cfg.targets.size() + "  [0]",
				"Close delay: " + cfg.closeDelayTicks + " ticks  [20]",
				"Retry delay: " + cfg.retryDelayTicks + " ticks  [40]",
				"Search radius: " + cfg.searchRadius + " blocks  [6]",
				"Villager outline: " + onOff(cfg.highlightVillager) + "  [on]",
				"Auto pickup walk: " + onOff(cfg.autoPickup) + "  [on]",
				"HUD: " + onOff(cfg.hudEnabled) + "  [on]",
				"HUD scale: " + cfg.hudScale + "  [1.0]",
				"HUD position: " + cfg.hudCorner.name().toLowerCase(Locale.ROOT) + "  [middle_left]",
				"HUD compact: " + onOff(cfg.hudCompact) + "  [off]"));
	}

	private static int debug(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft mc = ctx.getSource().getClient();
		var player = ctx.getSource().getPlayer();
		String hotbar = HotbarUtil.describeHotbar(player);
		Villager selected = VillagerRerollClient.CONTROLLER.getSelectedVillager(mc.level);
		return say(ctx, String.join("\n",
				PREFIX + "Debug",
				"Game mode: " + (mc.gameMode != null ? mc.gameMode.getPlayerMode().getName() : "?"),
				"State: " + VillagerRerollClient.CONTROLLER.getState()
						+ " - " + VillagerRerollClient.CONTROLLER.getStatusMessage(),
				"Lectern found in: " + HotbarUtil.findLectern(player)
						+ " (total " + HotbarUtil.countLecterns(player) + ")",
				"Selected villager: " + (selected != null
						? selected.getDisplayName().getString() + " at " + selected.blockPosition().toShortString()
						: "none (nearest is used)"),
				"Hotbar (* = selected):",
				hotbar.isEmpty() ? "(empty)" : hotbar));
	}

	private static int say(CommandContext<FabricClientCommandSource> ctx, String message) {
		ctx.getSource().sendFeedback(Component.literal(message));
		return 1;
	}

	private static String onOff(boolean value) {
		return value ? "on" : "off";
	}
}
