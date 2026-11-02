package dev.avalon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config (config/avalon.json). Every gameplay number lives here. */
public final class AvalonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AvalonConfig instance = new AvalonConfig();

    // ---- testing ----
    /** Allows /avalon start with a single player (for solo testing). */
    public boolean soloTest = false;
    /** Forces the first player's role while testing, e.g. "ASSASSIN", "MERLIN", "MORGANA". Empty = off. */
    public String debugRole = "";

    // ---- players / rules ----
    public int minPlayers = 5;
    public int maxPlayers = 10;
    /** Team size per pillar (Pillar 1, 2, 3). */
    public int[] teamSizes = {3, 4, 4};
    public int maxRejections = 5;
    /** Number of sabotaged batteries needed to make a pillar unstable. */
    public int sabotageThreshold = 1;
    /** How many times one EVIL player may sabotage over the whole game. */
    public int sabotageUsesPerPlayer = 3;
    /** Players who do not vote in time count as reject (true) or approve (false). */
    public boolean missingVoteIsReject = true;

    // ---- timings (seconds) ----
    public int roleIntroSeconds = 12;
    public int teamSelectSeconds = 120;
    public int voteSeconds = 180;
    public int missionSeconds = 300;
    public int discussionSeconds = 180;
    public int ejectVoteSeconds = 60;
    public int assassinSeconds = 180;
    public int endScreenSeconds = 15;

    // ---- optional features ----
    public boolean ladyOfTheLakeEnabled = true;
    public boolean assassinEnabled = true;
    /** GOOD may try to eject a suspect after every mission discussion. */
    public boolean ejectAfterEveryMission = true;
    /** Extra interaction distance validation (blocks). */
    public double interactDistance = 6.0;

    // ---- map ----
    public int arenaX = 0;
    public int arenaY = 64;
    public int arenaZ = 0;
    public int hallRadius = 24;
    public int pillarDistance = 70;

    // ---- lobby (where players gather before the portal intro) ----
    public String lobbyDimension = "minecraft:overworld";
    public double lobbyX = 0.5;
    public double lobbyY = 100;
    public double lobbyZ = 0.5;

    public static AvalonConfig get() {
        return instance;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("avalon.json");
        try {
            if (Files.exists(file)) {
                AvalonConfig loaded = GSON.fromJson(Files.readString(file), AvalonConfig.class);
                if (loaded != null) instance = loaded;
            }
            save();
        } catch (Exception e) {
            AvalonMod.LOGGER.error("Could not read avalon.json, using defaults", e);
        }
    }

    public static void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("avalon.json");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(instance));
        } catch (IOException e) {
            AvalonMod.LOGGER.error("Could not write avalon.json", e);
        }
    }
}
