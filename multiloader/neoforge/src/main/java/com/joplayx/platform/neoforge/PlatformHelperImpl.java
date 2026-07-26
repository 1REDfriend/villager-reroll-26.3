package com.joplayx.platform.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * NeoForge-side implementation of com.joplayx.platform.PlatformHelper.
 * Wired automatically by architectury-loom's @ExpectPlatform transformer
 * because this class sits in the "neoforge" subpackage of the common class's package.
 */
public class PlatformHelperImpl {

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getLoaderName() {
        return "NeoForge";
    }
}
