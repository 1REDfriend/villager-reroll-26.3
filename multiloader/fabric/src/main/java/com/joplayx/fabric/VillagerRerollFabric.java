package com.joplayx.fabric;

import com.joplayx.VillagerReroll;
import net.fabricmc.api.ModInitializer;

public class VillagerRerollFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VillagerReroll.init();
    }
}
