package com.joplayx.neoforge;

import com.joplayx.VillagerReroll;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Main entrypoint, loaded on both physical sides (matches the "main" entrypoint
 * from the Fabric version, which also did nothing except log). All the real
 * client-only work happens in VillagerRerollNeoForgeClient, which NeoForge only
 * loads on the physical client thanks to dist = Dist.CLIENT on that class.
 */
@Mod(VillagerReroll.MOD_ID)
public class VillagerRerollNeoForge {

    public VillagerRerollNeoForge(IEventBus modEventBus) {
        VillagerReroll.init();
    }
}
