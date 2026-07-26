package com.joplayx.state;

import com.joplayx.ModKeybinds;
import com.joplayx.VillagerReroll;
import com.joplayx.config.RerollerConfig;
import com.joplayx.util.HotbarUtil;
import com.joplayx.util.OffersStore;
import com.joplayx.util.TradeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * State machine driving the reroll loop. Loader-agnostic — lives in common.
 */
public class RerollController {

	public static final RerollController INSTANCE = new RerollController();

	private RerollState state = RerollState.IDLE;
	private String statusMessage = "Idle";
	private String lastTradeDescription = "";
	private int attempts = 0;
	private String errorReason = "";

	private int tickDelay = 0;
	private int professionWaitTicks = 0;
	private static final int MAX_PROFESSION_WAIT = 200;
	private int breakTicks = 0;
	private static final int MAX_BREAK_TICKS = 100;

	// Store by entity ID — safe across ticks on the client
	private int targetEntityId = -1;

	public RerollState getState() { return state; }
	public String getStatusMessage() { return statusMessage; }
	public String getLastTradeDescription() { return lastTradeDescription; }
	public int getAttempts() { return attempts; }
	public String getErrorReason() { return errorReason; }

	public void toggleStartStop(Minecraft client) {
		if (state == RerollState.IDLE) start(client);
		else stop(client, "Stopped by player.");
	}

	public void emergencyStop(Minecraft client) {
		stop(client, "Emergency stop.");
		if (client.player != null)
			client.player.sendSystemMessage(Component.literal("[Reroller] Emergency stopped."));
	}

	public void tick(Minecraft client) {
		if (state == RerollState.IDLE || state == RerollState.FOUND || state == RerollState.ERROR) return;
		if (tickDelay > 0) { tickDelay--; return; }

		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) { stop(client, "Player or world is null."); return; }

		switch (state) {

			case PRE_CHECK -> {
				statusMessage = "Checking setup...";
				if (!HotbarUtil.hasItemInHotbar(player, Items.LECTERN)) {
					stop(client, "No lectern in hotbar!"); return;
				}
				if (RerollerConfig.get().targetList().isEmpty()) {
					stop(client, "No target enchantments set in config!"); return;
				}
				if (!RerollerConfig.get().hasLecternPos()) {
				stop(client, "No lectern position set! Use " +
						ModKeybinds.SET_POSITION.getTranslatedKeyMessage().getString() +
						" key or Mod Menu config."); return;
				}
				Villager v = findNearestVillager(player, level);
				if (v == null) {
					stop(client, "No villager found within 6 blocks!"); return;
				}
				targetEntityId = v.getId();
				setState(RerollState.PLACE_LECTERN, "Placing lectern...", 5);
			}

			case PLACE_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.LECTERN)) {
					stop(client, "Lectern position is blocked!"); return;
				}
				HotbarUtil.selectItem(player, Items.LECTERN);
				BlockHitResult hit = new BlockHitResult(
						Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false
				);
				if (client.gameMode != null) {
					client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
				}
				professionWaitTicks = 0;
				setState(RerollState.WAIT_FOR_PROFESSION, "Waiting for librarian...", 20);
			}

			case WAIT_FOR_PROFESSION -> {
				professionWaitTicks++;
				Entity entity = level.getEntity(targetEntityId);
				if (entity == null || !entity.isAlive()) {
					stop(client, "Villager disappeared!"); return;
				}

				if (entity instanceof Villager villager) {
					villager.getVillagerData().profession().unwrapKey().ifPresent(profKey -> {
					if (!profKey.identifier().getPath().equals("none")) {
							setState(RerollState.OPEN_VILLAGER, "Librarian found! Opening trades...", 10);
						}
					});
				}

				if (professionWaitTicks >= MAX_PROFESSION_WAIT) {
					setState(RerollState.BREAK_LECTERN, "Profession wait timeout — retrying...", 5);
				}
			}

			case OPEN_VILLAGER -> {
				Entity entity = level.getEntity(targetEntityId);
				if (entity == null || !entity.isAlive()) {
					stop(client, "Villager disappeared!"); return;
				}
				if (client.gameMode != null) {
					EntityHitResult entityHit = new EntityHitResult(entity);
					client.gameMode.interact(player, entity, entityHit, InteractionHand.MAIN_HAND);
				}
				setState(RerollState.WAIT_FOR_SCREEN, "Waiting for trade screen...", 10);
			}

		case WAIT_FOR_SCREEN -> {
				if (OffersStore.get() != null) {
					setState(RerollState.READ_TRADES, "Reading trades...", 2);
				} else {
					tickDelay = 5;
				}
			}

			case READ_TRADES -> {
				TradeUtil.TradeResult result = TradeUtil.checkTrades(RerollerConfig.get());
				attempts++;
				lastTradeDescription = result.description();

				if (result.found()) {
					setState(RerollState.FOUND, "FOUND! " + result.description(), 0);
					player.closeContainer();
					player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
					player.sendSystemMessage(Component.literal(
							"[Reroller] Found " + result.description() + " after " + attempts + " attempt(s)!"));
				} else {
					setState(RerollState.CLOSE_SCREEN, "No match — closing...", 2);
				}
			}

			case CLOSE_SCREEN -> {
				if (player.containerMenu instanceof MerchantMenu) player.closeContainer();
				setState(RerollState.WAIT_AFTER_CLOSE, "Waiting before break...", RerollerConfig.get().closeDelayTicks);
			}

			case WAIT_AFTER_CLOSE -> {
				breakTicks = 0;
				setState(RerollState.BREAK_LECTERN, "Breaking lectern...", 2);
			}

			case BREAK_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				statusMessage = "Breaking lectern...";

				if (level.getBlockState(pos).isAir()) {
					breakTicks = 0;
					if (client.gameMode != null) client.gameMode.stopDestroyBlock();
					setState(RerollState.WAIT_BEFORE_RETRY, "Lectern broken. Waiting...", RerollerConfig.get().retryDelayTicks);
					return;
				}

				if (client.gameMode != null) {
					if (breakTicks == 0) {
						HotbarUtil.selectBestAxe(player);
						client.gameMode.startDestroyBlock(pos, Direction.UP);
					} else {
						client.gameMode.continueDestroyBlock(pos, Direction.UP);
					}
				}

				if (++breakTicks >= MAX_BREAK_TICKS) stop(client, "Could not break lectern — is it out of reach?");
			}

			case WAIT_BREAK_COMPLETE ->
				setState(RerollState.WAIT_BEFORE_RETRY, "Waiting before retry...", RerollerConfig.get().retryDelayTicks);

			case WAIT_BEFORE_RETRY -> {
				if (!HotbarUtil.hasItemInHotbar(player, Items.LECTERN)) {
					stop(client, "Ran out of lecterns!"); return;
				}
				setState(RerollState.PLACE_LECTERN, "Attempt " + (attempts + 1) + "...", 5);
			}

			default -> stop(client, "Unknown state: " + state);
		}
	}

	private void start(Minecraft client) {
		if (client.level == null || client.player == null) return;
		attempts = 0;
		errorReason = "";
		lastTradeDescription = "";
		targetEntityId = -1;
		OffersStore.clear();
		setState(RerollState.PRE_CHECK, "Starting...", 2);
		client.player.sendSystemMessage(Component.literal("[Reroller] Started. Press " +
				ModKeybinds.EMERGENCY_STOP.getTranslatedKeyMessage().getString() +
				" to emergency stop."));
	}

	private void stop(Minecraft client, String reason) {
		errorReason = reason;
		statusMessage = reason;
		state = (reason.equals("Stopped by player.") || reason.equals("Emergency stop."))
				? RerollState.IDLE : RerollState.ERROR;
		tickDelay = 0;
		VillagerReroll.LOGGER.info("[Reroller] Stopped: {}", reason);
		if (client.player != null)
			client.player.sendSystemMessage(Component.literal("[Reroller] Stopped: " + reason));
	}

	private void setState(RerollState newState, String message, int delay) {
		state = newState;
		statusMessage = message;
		tickDelay = delay;
		VillagerReroll.LOGGER.debug("[Reroller] -> {} | {}", newState, message);
	}

	private Villager findNearestVillager(LocalPlayer player, ClientLevel level) {
		AABB box = player.getBoundingBox().inflate(6.0);
		List<Entity> entities = level.getEntities(player, box,
				e -> e instanceof Villager && e.isAlive());
		return entities.stream()
				.map(e -> (Villager) e)
				.min((a, b) -> Double.compare(
						a.position().distanceToSqr(player.position()),
						b.position().distanceToSqr(player.position())))
				.orElse(null);
	}
}
