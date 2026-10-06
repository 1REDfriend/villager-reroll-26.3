package com.joplayx.client.state;

import com.joplayx.VillagerReroll;
import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.util.HotbarUtil;
import com.joplayx.client.util.OffersStore;
import com.joplayx.client.util.TradeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * State machine driving the reroll loop.
 *
 * API references confirmed from:
 *  - VillagerRoller.java (working meteor mod): Villager package, interact() signature,
 *    profession check, EnchantmentHelper usage
 *  - KeyMappingsTest.java: tick event pattern
 *  - HudTests.java: rendering pattern
 */
public class RerollController {

	private RerollState state = RerollState.IDLE;
	private String statusMessage = "Idle";
	private String lastTradeDescription = "";
	private String lastOfferSeen = "";
	private java.util.List<TradeUtil.TargetStatus> lastStatuses = java.util.List.of();
	private int attempts = 0;
	private String errorReason = "";

	private int tickDelay = 0;
	private int professionWaitTicks = 0;
	private static final int MAX_PROFESSION_WAIT = 200;
	private int breakTicks = 0;
	// Breaking by hand (no axe) takes ~75 ticks in survival, so leave headroom
	private static final int MAX_BREAK_TICKS = 300;
	private int screenWaitTicks = 0;
	private static final int MAX_SCREEN_WAIT = 100;
	private int pickupWaitTicks = 0;
	private static final int MAX_PICKUP_WAIT = 60;
	private static final int MIN_AXE_DURABILITY = 10;

	// Auto-pickup walk: sneak speed is ~1.3 blocks/s, so 200 ticks covers ~13 blocks
	private static final int MAX_WALK_TICKS = 200;
	private static final double HOME_TOLERANCE = 0.2;
	private static final double ITEM_SEARCH_RADIUS = 6.0;
	private int walkTicks = 0;
	private int walkItemId = -1;
	private Vec3 homePos = null;
	private float homeYaw = 0f;
	private float homePitch = 0f;

	// Store by entity ID — safe across ticks on the client
	private int targetEntityId = -1;
	// Villager picked by the player (look at it + key or /reroll villager); -1 = use nearest
	private int selectedVillagerId = -1;

	public RerollState getState() { return state; }
	public String getStatusMessage() { return statusMessage; }
	public String getLastTradeDescription() { return lastTradeDescription; }
	public String getLastOfferSeen() { return lastOfferSeen; }
	public java.util.List<TradeUtil.TargetStatus> getLastStatuses() { return lastStatuses; }
	public int getAttempts() { return attempts; }
	public String getErrorReason() { return errorReason; }
	public int getSelectedVillagerId() { return selectedVillagerId; }

	public boolean isRunning() {
		return state != RerollState.IDLE && state != RerollState.FOUND && state != RerollState.ERROR;
	}

	public void toggleStartStop(Minecraft client) {
		if (isRunning()) stop(client, "Stopped by player.");
		else start(client);
	}

	public void stopByPlayer(Minecraft client) {
		if (isRunning()) stop(client, "Stopped by player.");
	}

	public void emergencyStop(Minecraft client) {
		stop(client, "Emergency stop.");
		if (client.player != null)
			client.player.sendSystemMessage(Component.literal("[Reroller] Emergency stopped."));
	}

	/** Locks the reroller onto this villager instead of "nearest within radius". */
	public void selectVillager(Villager villager) {
		selectedVillagerId = villager.getId();
		if (isRunning()) targetEntityId = selectedVillagerId;
	}

	public void clearSelectedVillager() {
		selectedVillagerId = -1;
	}

	/** The selected villager if it's still loaded and alive, otherwise null. */
	public Villager getSelectedVillager(ClientLevel level) {
		if (selectedVillagerId == -1 || level == null) return null;
		Entity e = level.getEntity(selectedVillagerId);
		return (e instanceof Villager v && v.isAlive()) ? v : null;
	}

	public void tick(Minecraft client) {
		if (!isRunning()) return;
		if (tickDelay > 0) { tickDelay--; return; }

		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) { stop(client, "Player or world is null."); return; }

		switch (state) {

			case PRE_CHECK -> {
				statusMessage = "Checking setup...";
				if (HotbarUtil.findLectern(player) == HotbarUtil.LecternLocation.NONE) {
					stop(client, "No lectern in hotbar, offhand or inventory! (/reroll debug shows what the mod sees)"); return;
				}
				if (RerollerConfig.get().targets.isEmpty()) {
					stop(client, "No target enchantments set in config!"); return;
				}
				if (!RerollerConfig.get().hasLecternPos()) {
					stop(client, "No lectern position set! Use " +
							VillagerRerollClient.setPositionKey.getTranslatedKeyMessage().getString() +
							" key or /reroll pos."); return;
				}
				Villager v;
				if (selectedVillagerId != -1) {
					v = getSelectedVillager(level);
					if (v == null) {
						selectedVillagerId = -1;
						stop(client, "Selected villager is gone - select another one."); return;
					}
				} else {
					v = findNearestVillager(player, level);
					if (v == null) {
						stop(client, "No villager found within " + RerollerConfig.get().searchRadius + " blocks!"); return;
					}
				}
				targetEntityId = v.getId();
				setState(RerollState.PLACE_LECTERN, "Placing lectern...", 5);
			}

			case PLACE_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.LECTERN)) {
					stop(client, "Lectern position is blocked!"); return;
				}
				if (level.getBlockState(pos).isAir()) {
					InteractionHand hand = HotbarUtil.prepareLectern(client, player);
					if (hand == null) {
						stop(client, "Ran out of lecterns!"); return;
					}
					BlockHitResult hit = new BlockHitResult(
							Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false
					);
					if (client.gameMode != null) {
						client.gameMode.useItemOn(player, hand, hit);
					}
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
					// Profession check confirmed from working mod (VillagerRoller.java line 895)
					// unwrapKey() returns ResourceKey<VillagerProfession>
					// "none" path = unemployed/no profession
					villager.getVillagerData().profession().unwrapKey().ifPresent(profKey -> {
						if (!profKey.identifier().getPath().equals("none")) {
							setState(RerollState.OPEN_VILLAGER, "Librarian found! Opening trades...", 10);
						}
					});
				}

				if (state == RerollState.WAIT_FOR_PROFESSION && professionWaitTicks >= MAX_PROFESSION_WAIT) {
					breakTicks = 0;
					setState(RerollState.BREAK_LECTERN, "Profession wait timeout — retrying...", 5);
				}
			}

			case OPEN_VILLAGER -> {
				Entity entity = level.getEntity(targetEntityId);
				if (entity == null || !entity.isAlive()) {
					stop(client, "Villager disappeared!"); return;
				}
				// Drop the previous attempt's offers so WAIT_FOR_SCREEN only accepts a fresh packet
				OffersStore.clear();
				screenWaitTicks = 0;
				if (client.gameMode != null) {
					// 4-arg interact() confirmed from working mod (VillagerRoller.java)
					EntityHitResult entityHit = new EntityHitResult(entity);
					client.gameMode.interact(player, entity, entityHit, InteractionHand.MAIN_HAND);
				}
				setState(RerollState.WAIT_FOR_SCREEN, "Waiting for trade screen...", 10);
			}

			case WAIT_FOR_SCREEN -> {
				// Wait for the packet mixin to populate OffersStore
				if (OffersStore.get() != null) {
					setState(RerollState.READ_TRADES, "Reading trades...", 2);
				} else if ((screenWaitTicks += 5) >= MAX_SCREEN_WAIT) {
					setState(RerollState.OPEN_VILLAGER, "No trade screen - retrying...", 5);
				} else {
					tickDelay = 5;
				}
			}

			case READ_TRADES -> {
				TradeUtil.TradeResult result = TradeUtil.checkTrades(RerollerConfig.get());
				attempts++;
				lastStatuses = result.statuses();
				lastOfferSeen = result.lastOfferSeen();

				if (result.anyFound()) {
					RerollerConfig.Config cfg = RerollerConfig.get();
					RerollerConfig.EnchantTarget found = result.foundTarget();
					String desc = result.descriptionFor(found);
					lastTradeDescription = desc;

					setState(RerollState.FOUND, "FOUND! " + desc, 0);
					player.closeContainer();
					player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

					// Wishlist model: you can't hold multiple simultaneous matches on one
					// villager (rerolling wipes the whole trade list), so the first target
					// found stops the reroller AND gets removed from the list - the next
					// time you start it (presumably on a different villager), it's only
					// looking for whatever targets are left.
					cfg.targets.remove(found);
					RerollerConfig.save();
					// This villager is done; the next run is meant for a different one
					selectedVillagerId = -1;

					if (cfg.targets.isEmpty()) {
						player.sendSystemMessage(Component.literal(
								"[Reroller] Found " + desc + " after " + attempts + " attempt(s)! All targets found - list is now empty."));
					} else {
						player.sendSystemMessage(Component.literal(
								"[Reroller] Found " + desc + " after " + attempts + " attempt(s)! " +
								cfg.targets.size() + " target(s) left - move to a new villager and start again."));
					}
				} else {
					setState(RerollState.CLOSE_SCREEN, "No match — closing...", 2);
				}
			}

			case CLOSE_SCREEN -> {
				if (player.containerMenu instanceof MerchantMenu) player.closeContainer();
				setState(RerollState.WAIT_AFTER_CLOSE, "Waiting before break...", RerollerConfig.get().closeDelayTicks);
			}

			case WAIT_AFTER_CLOSE -> {
				breakTicks = 0; // Reset so BREAK_LECTERN starts fresh on tick 0
				setState(RerollState.BREAK_LECTERN, "Breaking lectern...", 2);
			}

			case BREAK_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				statusMessage = "Breaking lectern...";

				// Block already gone — move on
				if (level.getBlockState(pos).isAir()) {
					breakTicks = 0;
					pickupWaitTicks = 0;
					if (client.gameMode != null) client.gameMode.stopDestroyBlock();
					setState(RerollState.WAIT_BEFORE_RETRY, "Lectern broken. Waiting...", RerollerConfig.get().retryDelayTicks);
					return;
				}

				if (client.gameMode != null) {
					if (breakTicks == 0) {
						// First tick — select axe once and start breaking (no axe = break by hand)
						if (HotbarUtil.selectBestAxe(player)
								&& HotbarUtil.isSelectedItemLowDurability(player, MIN_AXE_DURABILITY)) {
							stop(client, "Axe is almost broken - swap in a fresh one."); return;
						}
						client.gameMode.startDestroyBlock(pos, Direction.UP);
					} else {
						// Subsequent ticks — continue breaking without re-selecting
						client.gameMode.continueDestroyBlock(pos, Direction.UP);
					}
				}

				if (++breakTicks >= MAX_BREAK_TICKS) stop(client, "Could not break lectern — is it out of reach?");
			}

			case WAIT_BREAK_COMPLETE ->
				setState(RerollState.WAIT_BEFORE_RETRY, "Waiting before retry...", RerollerConfig.get().retryDelayTicks);

			case WAIT_BEFORE_RETRY -> {
				if (HotbarUtil.findLectern(player) == HotbarUtil.LecternLocation.NONE) {
					// In survival the broken lectern drops as an item; give pickup a moment
					if (pickupWaitTicks < MAX_PICKUP_WAIT) {
						pickupWaitTicks += 5;
						statusMessage = "Waiting to pick up lectern...";
						tickDelay = 5;
						return;
					}
					ItemEntity dropped = findDroppedLectern(player, level);
					if (dropped == null) {
						stop(client, "Ran out of lecterns!");
					} else if (!RerollerConfig.get().autoPickup) {
						stop(client, "Lectern dropped out of pickup range - stand closer to the lectern spot.");
					} else {
						// Remember where we were standing so the loop resumes from the same spot
						homePos = player.position();
						homeYaw = player.getYRot();
						homePitch = player.getXRot();
						walkItemId = dropped.getId();
						walkTicks = 0;
						setState(RerollState.WALK_TO_ITEM, "Walking to pick up lectern...", 0);
					}
					return;
				}
				setState(RerollState.PLACE_LECTERN, "Attempt " + (attempts + 1) + "...", 5);
			}

			case WALK_TO_ITEM -> {
				if (HotbarUtil.findLectern(player) != HotbarUtil.LecternLocation.NONE) {
					walkTicks = 0;
					setState(RerollState.WALK_BACK, "Got it - walking back...", 0);
					return;
				}
				Entity item = level.getEntity(walkItemId);
				if (!(item instanceof ItemEntity) || !item.isAlive()) {
					// Picked up by something else, or despawned
					ItemEntity other = findDroppedLectern(player, level);
					if (other == null) {
						releaseMovement(client);
						stop(client, "Lectern item disappeared before it could be picked up.");
						return;
					}
					walkItemId = other.getId();
					item = other;
				}
				if (++walkTicks > MAX_WALK_TICKS) {
					releaseMovement(client);
					stop(client, "Could not reach the dropped lectern - is something in the way?");
					return;
				}
				walkToward(client, player, item.position(), 0.0);
			}

			case WALK_BACK -> {
				if (++walkTicks > MAX_WALK_TICKS) {
					releaseMovement(client);
					stop(client, "Could not walk back to the starting spot - is something in the way?");
					return;
				}
				if (walkToward(client, player, homePos, HOME_TOLERANCE)) {
					releaseMovement(client);
					player.setYRot(homeYaw);
					player.setYHeadRot(homeYaw);
					player.setXRot(homePitch);
					// Let momentum settle before placing again
					setState(RerollState.PLACE_LECTERN, "Attempt " + (attempts + 1) + "...", 10);
				}
			}

			default -> stop(client, "Unknown state: " + state);
		}
	}

	public void start(Minecraft client) {
		if (client.level == null || client.player == null) return;
		attempts = 0;
		errorReason = "";
		lastTradeDescription = "";
		lastOfferSeen = "";
		lastStatuses = List.of();
		targetEntityId = -1;
		OffersStore.clear();
		setState(RerollState.PRE_CHECK, "Starting...", 2);
		client.player.sendSystemMessage(Component.literal("[Reroller] Started. Press " +
				VillagerRerollClient.emergencyStopKey.getTranslatedKeyMessage().getString() +
				" or type /reroll stop to stop."));
	}

	private void stop(Minecraft client, String reason) {
		// Never leave forward/sneak held down after a stop mid-walk
		if (state == RerollState.WALK_TO_ITEM || state == RerollState.WALK_BACK) releaseMovement(client);
		walkTicks = 0;
		errorReason = reason;
		statusMessage = reason;
		state = (reason.equals("Stopped by player.") || reason.equals("Emergency stop."))
				? RerollState.IDLE : RerollState.ERROR;
		tickDelay = 0;
		if (client.gameMode != null && breakTicks > 0) client.gameMode.stopDestroyBlock();
		breakTicks = 0;
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

	/** The dropped lectern item closest to the player, searched around the lectern spot. */
	private ItemEntity findDroppedLectern(LocalPlayer player, ClientLevel level) {
		BlockPos pos = RerollerConfig.get().lecternPos();
		if (pos == null) return null;
		AABB box = new AABB(pos).inflate(ITEM_SEARCH_RADIUS);
		return level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && HotbarUtil.isLectern(e.getItem()))
				.stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
				.orElse(null);
	}

	/**
	 * Steers the player toward target by facing it and holding forward + sneak.
	 * Sneaking keeps the walk slow enough to stop on the spot and stops the player
	 * from walking off ledges. Returns true once within tolerance (horizontally).
	 */
	private boolean walkToward(Minecraft client, LocalPlayer player, Vec3 target, double tolerance) {
		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist <= tolerance) return true;

		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		client.options.keyUp.setDown(true);
		client.options.keyShift.setDown(true);
		return false;
	}

	private void releaseMovement(Minecraft client) {
		client.options.keyUp.setDown(false);
		client.options.keyShift.setDown(false);
	}

	private Villager findNearestVillager(LocalPlayer player, ClientLevel level) {
		// Villager package: net.minecraft.world.entity.npc.villager.Villager
		// Confirmed from working mod (VillagerRoller.java import line)
		AABB box = player.getBoundingBox().inflate(RerollerConfig.get().searchRadius);
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
