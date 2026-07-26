package com.joplayx;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Shared KeyMapping instances. Constructing a KeyMapping has no side effects
 * on its own (it does not appear in the Controls screen or get persisted until
 * something actually registers it), so these plain vanilla objects can live in
 * common code. Each loader's entrypoint is responsible for the actual
 * registration call:
 *   - Fabric:   KeyMappingHelper.registerKeyMapping(...)
 *   - NeoForge: RegisterKeyMappingsEvent#register(...)
 */
public class ModKeybinds {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VillagerReroll.MOD_ID, "villager_reroller")
    );

    public static final KeyMapping START_STOP = new KeyMapping(
            "key.villager-reroll.start_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping EMERGENCY_STOP = new KeyMapping(
            "key.villager-reroll.emergency_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    public static final KeyMapping SET_POSITION = new KeyMapping(
            "key.villager-reroll.set_position",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY
    );

    /**
     * Called once from each loader's client entrypoint. Uses Architectury's
     * cross-platform KeyMappingRegistry, so this single call registers all
     * three keybinds correctly on both Fabric and NeoForge - no per-loader
     * registration code needed.
     */
    public static void init() {
        KeyMappingRegistry.register(START_STOP);
        KeyMappingRegistry.register(EMERGENCY_STOP);
        KeyMappingRegistry.register(SET_POSITION);
    }
}
