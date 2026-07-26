package com.joplayx.client.hud;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.state.RerollController;
import com.joplayx.client.state.RerollState;
import com.joplayx.client.util.TradeUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD overlay for the Villager Trade Reroller.
 *
 * Implements HudElement by providing extractRenderState(GuiGraphicsExtractor, DeltaTracker).
 * Method name and signature confirmed from HudElement.java and HudTests.java in references.
 *
 * Shows one row per configured enchantment target, live-updated against the
 * last read villager so it's clear at a glance which of your targets (e.g.
 * mending + protection IV + unbreaking III) are currently satisfied together,
 * plus a "Last Offer" row showing the last enchanted book actually seen so
 * you can confirm the mod is reading trades even before anything matches.
 */
public class RerollerHud {

	private static final int PADDING = 6;
	private static final int LINE_HEIGHT = 10;
	private static final int X = 4; // left edge, same margin as before
	private static final int BORDER = 1;
	private static final int LABEL_VALUE_GAP = 8;
	private static final int SECTION_GAP = 3;

	private static final int COLOR_BG     = 0xCC0A0A0A; // dark translucent panel
	private static final int COLOR_BORDER = 0x80FFAA00; // subtle gold accent border
	private static final int COLOR_DIVIDER = 0x40FFFFFF; // faint separator line
	private static final int COLOR_GOLD   = 0xFFFFAA00;
	private static final int COLOR_WHITE  = CommonColors.WHITE;
	private static final int COLOR_GRAY   = 0xFFAAAAAA;
	private static final int COLOR_DIM    = 0xFF777777;
	private static final int COLOR_GREEN  = 0xFF55FF55;
	private static final int COLOR_RED    = 0xFFFF5555;
	private static final int COLOR_YELLOW = 0xFFFFFF55;

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		RerollerConfig.Config cfg = RerollerConfig.get();
		if (!cfg.hudEnabled) return;

		RerollController ctrl = VillagerRerollClient.CONTROLLER;
		RerollState state = ctrl.getState();
		if (state == RerollState.IDLE) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden()) return;

		int statusColor = switch (state) {
			case FOUND -> COLOR_GREEN;
			case ERROR -> COLOR_RED;
			case WAIT_FOR_PROFESSION, WAIT_FOR_SCREEN,
				 WAIT_AFTER_CLOSE, WAIT_BEFORE_RETRY,
				 WAIT_BREAK_COMPLETE -> COLOR_YELLOW;
			default -> COLOR_WHITE;
		};

		String startStopName = VillagerRerollClient.startStopKey.getTranslatedKeyMessage().getString();
		String emergencyStopName = VillagerRerollClient.emergencyStopKey.getTranslatedKeyMessage().getString();
		String setPositionName = VillagerRerollClient.setPositionKey.getTranslatedKeyMessage().getString();

		String lecternPos = cfg.hasLecternPos() ? cfg.lecternPosString() : "NOT SET (" + setPositionName + ")";

		String header = "VILLAGER REROLLER";
		String footer = startStopName + " Start/Stop    " + emergencyStopName + " Stop    " + setPositionName + " Set Pos";

		// Fixed rows first
		List<String> labels = new ArrayList<>(List.of("Status", "Lectern", "Attempts"));
		List<String> values = new ArrayList<>(List.of(
				ctrl.getStatusMessage(),
				lecternPos,
				String.valueOf(ctrl.getAttempts())
		));
		List<Integer> valueColors = new ArrayList<>(List.of(
				statusColor,
				cfg.hasLecternPos() ? COLOR_GRAY : COLOR_RED,
				COLOR_WHITE
		));

		// One row per configured enchant target, matched against the last read
		// villager's trades so multiple targets show their live found/not-found state.
		List<TradeUtil.TargetStatus> lastStatuses = ctrl.getLastStatuses();
		if (cfg.targets.isEmpty()) {
			labels.add("Targets");
			values.add("none configured");
			valueColors.add(COLOR_RED);
		} else {
			for (RerollerConfig.EnchantTarget target : cfg.targets) {
				String shortId = target.enchantmentId.contains(":")
						? target.enchantmentId.substring(target.enchantmentId.indexOf(':') + 1)
						: target.enchantmentId;
				if (shortId.isEmpty()) shortId = "(not set)";

				TradeUtil.TargetStatus match = lastStatuses.stream()
						.filter(s -> s.target() == target)
						.findFirst()
						.orElse(null);

				String value;
				int color;
				if (match == null) {
					// Not checked yet this cycle - show the criteria plainly, no "checking..."
					// spam repeated on every row (the Status row already says what's happening).
					value = "Lv" + target.minLevel + "+, " + target.maxEmeraldCost + "g max";
					color = COLOR_GRAY;
				} else if (match.satisfied()) {
					value = match.description();
					color = COLOR_GREEN;
				} else {
					value = match.description();
					color = COLOR_RED;
				}

				labels.add(shortId);
				values.add(value);
				valueColors.add(color);
			}
		}

		labels.add("Last Offer");
		values.add(ctrl.getLastOfferSeen().isEmpty() ? "-" : ctrl.getLastOfferSeen());
		valueColors.add(COLOR_DIM);

		// --- Measure ---
		int labelWidth = 0;
		for (String label : labels) labelWidth = Math.max(labelWidth, mc.font.width(label));
		int labelColW = labelWidth + LABEL_VALUE_GAP;

		int contentWidth = Math.max(mc.font.width(header), mc.font.width(footer));
		for (int i = 0; i < labels.size(); i++) {
			contentWidth = Math.max(contentWidth, labelColW + mc.font.width(values.get(i)));
		}

		int rowsHeight = labels.size() * LINE_HEIGHT;
		int contentHeight = LINE_HEIGHT
				+ SECTION_GAP + 1 + SECTION_GAP
				+ rowsHeight
				+ SECTION_GAP + 1 + SECTION_GAP
				+ LINE_HEIGHT;

		int boxX = X;
		int boxY = (graphics.guiHeight() - contentHeight) / 2;

		int bgX1 = boxX - PADDING;
		int bgY1 = boxY - PADDING;
		int bgX2 = boxX + contentWidth + PADDING;
		int bgY2 = boxY + contentHeight + PADDING;

		graphics.fill(bgX1 - BORDER, bgY1 - BORDER, bgX2 + BORDER, bgY2 + BORDER, COLOR_BORDER);
		graphics.fill(bgX1, bgY1, bgX2, bgY2, COLOR_BG);

		int cursorY = boxY;

		int headerX = boxX + (contentWidth - mc.font.width(header)) / 2;
		graphics.text(mc.font, header, headerX, cursorY, COLOR_GOLD);
		cursorY += LINE_HEIGHT + SECTION_GAP;

		graphics.fill(boxX, cursorY, boxX + contentWidth, cursorY + 1, COLOR_DIVIDER);
		cursorY += 1 + SECTION_GAP;

		for (int i = 0; i < labels.size(); i++) {
			graphics.text(mc.font, labels.get(i), boxX, cursorY, COLOR_GRAY);
			graphics.text(mc.font, values.get(i), boxX + labelColW, cursorY, valueColors.get(i));
			cursorY += LINE_HEIGHT;
		}

		cursorY += SECTION_GAP;
		graphics.fill(boxX, cursorY, boxX + contentWidth, cursorY + 1, COLOR_DIVIDER);
		cursorY += 1 + SECTION_GAP;

		int footerX = boxX + (contentWidth - mc.font.width(footer)) / 2;
		graphics.text(mc.font, footer, footerX, cursorY, COLOR_DIM);
	}
}
