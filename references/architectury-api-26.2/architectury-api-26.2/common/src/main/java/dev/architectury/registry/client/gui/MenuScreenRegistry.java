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

package dev.architectury.registry.client.gui;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class MenuScreenRegistry {
    /**
     * Registers a Screen Factory on the client to display.
     *
     * @param type    The {@link MenuType} the screen visualizes
     * @param factory A functional interface that is used to create new {@link Screen}s
     * @param <H>     The type of {@link AbstractContainerMenu} for the screen
     * @param <S>     The type for the {@link Screen}
     */
    @ExpectPlatform
    public static <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void registerScreenFactory(MenuType<? extends H> type, ScreenFactory<H, S> factory) {
        throw new AssertionError();
    }
    
    /**
     * Creates new screens.
     *
     * @param <H> The type of {@link AbstractContainerMenu} for the screen
     * @param <S> The type for the {@link Screen}
     */
    @FunctionalInterface
    public interface ScreenFactory<H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> {
        /**
         * Creates a new {@link S} that extends {@link Screen}
         *
         * @param containerMenu The {@link AbstractContainerMenu} that controls the game logic for the screen
         * @param inventory     The {@link Inventory} for the screen
         * @param component     The {@link Component} for the screen
         * @return A new {@link S} that extends {@link Screen}
         */
        S create(H containerMenu, Inventory inventory, Component component);
    }
}
