package com.joplayx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared entrypoint logic, called from each loader's own entrypoint class
 * (VillagerRerollFabric / VillagerRerollNeoForge).
 */
public class VillagerReroll {

	public static final String MOD_ID = "villager-reroll";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void init() {
		// Server-side init — nothing needed for this client-only mod.
		LOGGER.info("[VillagerReroll] Mod loaded ({}).", com.joplayx.platform.PlatformHelper.getLoaderName());
	}
}
