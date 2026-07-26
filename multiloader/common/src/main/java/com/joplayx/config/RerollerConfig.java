package com.joplayx.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.joplayx.VillagerReroll;
import com.joplayx.platform.PlatformHelper;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RerollerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = PlatformHelper.getConfigDir().resolve("villager-reroll.json");

    private static Config instance = new Config();

    public static Config get() { return instance; }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, Config.class);
                if (instance == null) instance = new Config();
                VillagerReroll.LOGGER.info("[Reroller] Config loaded.");
            } catch (IOException e) {
                VillagerReroll.LOGGER.error("[Reroller] Failed to load config.", e);
                instance = new Config();
            }
        } else {
            instance = new Config();
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
            VillagerReroll.LOGGER.info("[Reroller] Config saved.");
        } catch (IOException e) {
            VillagerReroll.LOGGER.error("[Reroller] Failed to save config.", e);
        }
    }

    public static class Config {
        // Target enchantments - comma-separated namespaced IDs, e.g.
        // "minecraft:mending, minecraft:protection, minecraft:unbreaking"
        // A trade matches if it offers ANY enchantment in this list.
        public String targetEnchantments = "";
        public int minLevel = 1;
        public int maxEmeraldCost = 64;

        /** Parses targetEnchantments into a clean list, ignoring blanks/whitespace. */
        public java.util.List<String> targetList() {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String part : targetEnchantments.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) out.add(trimmed);
            }
            return out;
        }

        // Lectern position stored as separate ints so YACL can edit them directly
        // Integer.MIN_VALUE means "not set"
        public int lecternX = Integer.MIN_VALUE;
        public int lecternY = Integer.MIN_VALUE;
        public int lecternZ = Integer.MIN_VALUE;

        // Timing
        public int closeDelayTicks = 20;
        public int retryDelayTicks = 40;

        // Display
        public boolean hudEnabled = true;

        /**
         * Returns the lectern BlockPos, or null if not set.
         */
        public BlockPos lecternPos() {
            if (lecternX == Integer.MIN_VALUE) return null;
            return new BlockPos(lecternX, lecternY, lecternZ);
        }

        /**
         * Returns true if the lectern position has been set.
         */
        public boolean hasLecternPos() {
            return lecternX != Integer.MIN_VALUE;
        }

        /**
         * Sets the lectern position from a BlockPos.
         */
        public void setLecternPos(BlockPos pos) {
            lecternX = pos.getX();
            lecternY = pos.getY();
            lecternZ = pos.getZ();
        }

        /**
         * Clears the lectern position.
         */
        public void clearLecternPos() {
            lecternX = Integer.MIN_VALUE;
            lecternY = Integer.MIN_VALUE;
            lecternZ = Integer.MIN_VALUE;
        }

        /**
         * Returns a human-readable string of the lectern position.
         */
        public String lecternPosString() {
            if (!hasLecternPos()) return "Not set";
            return "X: " + lecternX + "  Y: " + lecternY + "  Z: " + lecternZ;
        }
    }
}
