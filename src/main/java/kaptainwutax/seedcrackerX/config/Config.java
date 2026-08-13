package kaptainwutax.seedcrackerX.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seedfinding.mccore.version.MCVersion;
import kaptainwutax.seedcrackerX.Features;
import kaptainwutax.seedcrackerX.util.FeatureToggle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger("config");

    private static Config INSTANCE = new Config();
    public FeatureToggle buriedTreasure = new FeatureToggle(true);
    public FeatureToggle desertTemple = new FeatureToggle(true);
    public FeatureToggle endCity = new FeatureToggle(true);
    public FeatureToggle jungleTemple = new FeatureToggle(true);
    public FeatureToggle monument = new FeatureToggle(true);
    public FeatureToggle swampHut = new FeatureToggle(true);
    public FeatureToggle shipwreck = new FeatureToggle(true);
    public FeatureToggle outpost = new FeatureToggle(true);
    public FeatureToggle igloo = new FeatureToggle(true);
    public FeatureToggle trialChambers = new FeatureToggle(true);
    public FeatureToggle endPillars = new FeatureToggle(true);
    public FeatureToggle endGateway = new FeatureToggle(false);
    public FeatureToggle dungeon = new FeatureToggle(true);
    public FeatureToggle emeraldOre = new FeatureToggle(false);
    public FeatureToggle desertWell = new FeatureToggle(false);
    public FeatureToggle warpedFungus = new FeatureToggle(false);
    public FeatureToggle biome = new FeatureToggle(false);
    public RenderType render = RenderType.XRAY;
    public boolean active = true;
    public boolean debug = false;
    public boolean antiXrayBypass = true;
    public boolean resilientMode = true;
    public AntiDataPackConfig antiDataPack = new AntiDataPackConfig();
    public Long knownWorldSeed;
    public Map<String, ServerProfile> serverProfiles = new LinkedHashMap<>();
    private transient String activeServerKey;
    private transient ServerProfile activeServerProfile;
    private MCVersion version = MCVersion.latest();
    public boolean databaseSubmits = false;
    public boolean anonymusSubmits = false;

    public static void save() {
        File file = configFile();
        if (INSTANCE.resilientMode) {
            INSTANCE.applyResilientDefaults();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // make sure that the config directory exists
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            logger.error("seedcracker couldn't save config", e);
        }
    }

    public static void load() {
        File file = configFile();
        Gson gson = new Gson();

        if (!file.exists()) {
            INSTANCE.applyResilientDefaults();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            INSTANCE = gson.fromJson(reader, Config.class);
            if (INSTANCE.resilientMode) {
                INSTANCE.applyResilientDefaults();
            }
        } catch (Exception e) {
            logger.error("seedcracker couldn't load config, deleting it...", e);
            file.delete();
        }
    }

    public static Config get() {
        return INSTANCE;
    }

    private static File configFile() {
        return new File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(),
                "seedcracker-resilient.json");
    }

    public void applyResilientDefaults() {
        databaseSubmits = false;
        endCity.set(false);
        endPillars.set(false);
        endGateway.set(false);
        biome.set(false);
        dungeon.set(true);
    }

    public MCVersion getVersion() {
        return version;
    }

    public void setVersion(MCVersion version) {
        if (this.version == version) return;
        this.version = version;
        Features.init(version);
    }

    public AntiDataPackConfig getAntiDataPack() {
        if (activeServerProfile != null) return activeServerProfile.antiDataPack();
        if (antiDataPack == null) antiDataPack = new AntiDataPackConfig();
        return antiDataPack;
    }

    public Long getKnownWorldSeed() {
        return activeServerProfile == null ? knownWorldSeed : activeServerProfile.knownWorldSeed;
    }

    public void setKnownWorldSeed(Long seed) {
        knownWorldSeed = seed;
        if (activeServerProfile != null) activeServerProfile.knownWorldSeed = seed;
    }

    /** Selects persistent state for one server. Legacy global state migrates once. */
    public boolean activateServerProfile(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) serverKey = "unknown";
        if (serverKey.equals(activeServerKey) && activeServerProfile != null) return false;
        if (serverProfiles == null) serverProfiles = new LinkedHashMap<>();
        boolean created = !serverProfiles.containsKey(serverKey);
        if (created) {
            ServerProfile profile = serverProfiles.isEmpty()
                    ? new ServerProfile(antiDataPack, knownWorldSeed)
                    : new ServerProfile();
            serverProfiles.put(serverKey, profile);
        }
        activeServerKey = serverKey;
        activeServerProfile = serverProfiles.get(serverKey);
        if (activeServerProfile.antiDataPack == null) activeServerProfile.antiDataPack = new AntiDataPackConfig();
        antiDataPack = activeServerProfile.antiDataPack;
        knownWorldSeed = activeServerProfile.knownWorldSeed;
        return created;
    }

    public String getActiveServerKey() {
        return activeServerKey == null ? "global" : activeServerKey;
    }

    public static class ServerProfile {
        public AntiDataPackConfig antiDataPack;
        public Long knownWorldSeed;

        public ServerProfile() {
            this(new AntiDataPackConfig(), null);
        }

        public ServerProfile(AntiDataPackConfig antiDataPack, Long knownWorldSeed) {
            this.antiDataPack = antiDataPack == null ? new AntiDataPackConfig() : antiDataPack;
            this.knownWorldSeed = knownWorldSeed;
        }

        private AntiDataPackConfig antiDataPack() {
            if (antiDataPack == null) antiDataPack = new AntiDataPackConfig();
            return antiDataPack;
        }
    }

    public enum RenderType {
        OFF, ON, XRAY
    }
}
