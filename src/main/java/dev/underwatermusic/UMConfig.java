package dev.underwatermusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class UMConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("underwater_music.json");
    private static UMConfig instance;

    // Underwater effect
    public boolean enabled = true;
    public boolean lowPassEnabled = true;
    public boolean volumeReductionEnabled = true;
    public boolean pitchReductionEnabled = true;

    // Audio customization
    public double underwaterVolume = 60.0;     // percent, 0-100
    public double lowPassFrequency = 800.0;    // Hz, 200-5000
    public double pitchReduction = 5.0;        // percent, 0-20
    public double transitionSeconds = 1.0;     // 0.1-5.0
    public double effectIntensity = 100.0;     // percent, 0-100

    // Behavior
    public boolean detectEyes = true;
    public boolean detectSwimming = false;
    public boolean fadeIn = true;
    public boolean fadeOut = true;

    public static UMConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        UMConfig loaded = null;
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    loaded = GSON.fromJson(r, UMConfig.class);
                }
            }
        } catch (Exception e) {
            UnderwaterMusicClient.LOGGER.warn("Could not read config, using defaults", e);
        }
        instance = loaded != null ? loaded : new UMConfig();
        instance.clamp();
        save();
    }

    public static void save() {
        if (instance == null) return;
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            UnderwaterMusicClient.LOGGER.warn("Could not save config", e);
        }
    }

    private void clamp() {
        underwaterVolume = Math.max(0, Math.min(100, underwaterVolume));
        lowPassFrequency = Math.max(200, Math.min(5000, lowPassFrequency));
        pitchReduction = Math.max(0, Math.min(20, pitchReduction));
        transitionSeconds = Math.max(0.1, Math.min(5.0, transitionSeconds));
        effectIntensity = Math.max(0, Math.min(100, effectIntensity));
    }
}
