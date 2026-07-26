package com.joplayx.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * Boundary between common code and loader-specific APIs.
 * Common code calls these static methods; each loader (fabric/neoforge)
 * provides its own implementation class at the same package path
 * (com.joplayx.fabric.platform.PlatformHelperImpl / com.joplayx.neoforge.platform.PlatformHelperImpl),
 * wired together automatically by the architectury-loom @ExpectPlatform annotation processor.
 */
public class PlatformHelper {

    @ExpectPlatform
    public static Path getConfigDir() {
        // Replaced at compile time by the loader-specific implementation.
        throw new AssertionError("PlatformHelper.getConfigDir() was not transformed - check @ExpectPlatform setup");
    }

    @ExpectPlatform
    public static String getLoaderName() {
        throw new AssertionError("PlatformHelper.getLoaderName() was not transformed - check @ExpectPlatform setup");
    }
}
