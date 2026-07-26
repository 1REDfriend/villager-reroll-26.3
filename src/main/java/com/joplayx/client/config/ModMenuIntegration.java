package com.joplayx.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Enchantment targets are the thing you edit constantly, so that screen
        // IS the config entry point now instead of being one button buried in a
        // YACL category. Everything else (lectern, timing, display) is one
        // "More Settings" click away from there.
        return EnchantTargetListScreen::new;
    }
}
