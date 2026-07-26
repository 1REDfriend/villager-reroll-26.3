package com.joplayx.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Fabric-side implementation of com.joplayx.platform.PlatformHelper.
 * Wired automatically by architectury-loom's @ExpectPlatform transformer
 * because this class sits in the "fabric" subpackage of the common class's package.
 */
public class PlatformHelperImpl {

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static String getLoaderName() {
        return "Fabric";
    }
}
