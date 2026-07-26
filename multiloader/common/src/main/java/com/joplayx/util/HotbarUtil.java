package com.joplayx.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hotbar slot management utilities.
 *
 * Inventory.selected is a public field in 26.1.2 — confirmed not in the
 * transitive access wideners (meaning it's already accessible).
 */
public class  HotbarUtil {

	public static boolean hasItemInHotbar(Player player, Item item) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).is(item)) return true;
		}
		return false;
	}

	public static void selectItem(Player player, Item item) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).is(item)) {
				inv.selected = i;
				return;
			}
		}
	}

	public static void selectBestAxe(Player player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).getItem() instanceof AxeItem) {
				inv.selected = i;
				return;
			}
		}
		// No axe found — leave current slot as-is
	}

	public static boolean isSelectedItemLowDurability(Player player, int threshold) {
		ItemStack selected = player.getMainHandItem();
		if (selected.isEmpty() || !selected.isDamageableItem()) return false;
		return (selected.getMaxDamage() - selected.getDamageValue()) <= threshold;
	}
}
