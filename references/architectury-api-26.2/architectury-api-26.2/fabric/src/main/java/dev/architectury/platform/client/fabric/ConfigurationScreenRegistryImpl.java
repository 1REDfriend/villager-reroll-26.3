/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.platform.client.fabric;

import dev.architectury.platform.Mod;
import dev.architectury.platform.client.ConfigurationScreenRegistry.ConfigurationScreenProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationScreenRegistryImpl {
    public static final Map<String, ConfigurationScreenProvider> CONFIG_SCREENS = new ConcurrentHashMap<>();
    
    public static void register(Mod mod, ConfigurationScreenProvider provider) {
        if (CONFIG_SCREENS.containsKey(mod.getModId()))
            throw new IllegalStateException("Can not register configuration screen for mod '" + mod.getModId() + "' because it was already registered!");
        CONFIG_SCREENS.put(mod.getModId(), provider);
    }
}
