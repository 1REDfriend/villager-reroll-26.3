package com.joplayx.hud;

import com.joplayx.ModKeybinds;
import com.joplayx.config.RerollerConfig;
import com.joplayx.state.RerollController;
import com.joplayx.state.RerollState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.CommonColors;

/**
 * HUD overlay for the Villager Trade Reroller.
 *
 * extractRenderState(GuiGraphicsExtractor, DeltaTracker) itself is loader-agnostic —
 * GuiGraphicsExtractor/DeltaTracker and graphics.text()/graphics.fill() are all vanilla.
 * Only the *registration* of this method as a HUD element differs per loader:
 *   - Fabric:   HudElementRegistry.attachElementBefore(..., RerollerHud::extractRenderState)
 *   - NeoForge: wired via RegisterGuiLayersEvent in the NeoForge entrypoint
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

		RerollController ctrl = RerollController.INSTANCE;
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

		String startStopName = ModKeybinds.START_STOP.getTranslatedKeyMessage().getString();
		String emergencyStopName = ModKeybinds.EMERGENCY_STOP.getTranslatedKeyMessage().getString();
		String setPositionName = ModKeybinds.SET_POSITION.getTranslatedKeyMessage().getString();

		String target = cfg.targetList().isEmpty() ? "not set" : String.join(", ", cfg.targetList());
		String lecternPos = cfg.hasLecternPos() ? cfg.lecternPosString() : "NOT SET (" + setPositionName + ")";

		String header = "VILLAGER REROLLER";
		String footer = startStopName + " Start/Stop    " + emergencyStopName + " Stop    " + setPositionName + " Set Pos";

		String[] labels = { "Status", "Target", "Lectern", "Max Cost", "Attempts", "Last Seen" };
		String[] values = {
			ctrl.getStatusMessage(),
			target,
			lecternPos,
			cfg.maxEmeraldCost + " emeralds",
			String.valueOf(ctrl.getAttempts()),
			ctrl.getLastTradeDescription()
		};
		int[] valueColors = {
			statusColor,
			COLOR_WHITE,
			cfg.hasLecternPos() ? COLOR_GRAY : COLOR_RED,
			COLOR_GRAY,
			COLOR_WHITE,
			COLOR_GRAY
		};

		int labelWidth = 0;
		for (String label : labels) labelWidth = Math.max(labelWidth, mc.font.width(label));
		int labelColW = labelWidth + LABEL_VALUE_GAP;

		int contentWidth = Math.max(mc.font.width(header), mc.font.width(footer));
		for (int i = 0; i < labels.length; i++) {
			contentWidth = Math.max(contentWidth, labelColW + mc.font.width(values[i]));
		}

		int rowsHeight = labels.length * LINE_HEIGHT;
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

		for (int i = 0; i < labels.length; i++) {
			graphics.text(mc.font, labels[i], boxX, cursorY, COLOR_GRAY);
			graphics.text(mc.font, values[i], boxX + labelColW, cursorY, valueColors[i]);
			cursorY += LINE_HEIGHT;
		}

		cursorY += SECTION_GAP;
		graphics.fill(boxX, cursorY, boxX + contentWidth, cursorY + 1, COLOR_DIVIDER);
		cursorY += 1 + SECTION_GAP;

		int footerX = boxX + (contentWidth - mc.font.width(footer)) / 2;
		graphics.text(mc.font, footer, footerX, cursorY, COLOR_DIM);
	}
}
